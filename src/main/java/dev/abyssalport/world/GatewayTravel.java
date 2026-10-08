package dev.abyssalport.world;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.GatewayAnchorBlock;
import dev.abyssalport.block.GatewayPortalBlock;
import dev.abyssalport.block.entity.GatewayAnchorBlockEntity;
import dev.abyssalport.content.GatewayContent;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.item.GatewayKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/** Bounded portal search and reciprocal persistent anchors, using a one-to-one dimension scale. */
public final class GatewayTravel {
    private static final TagKey<Block> NATURAL_FLOOR = TagKey.create(Registries.BLOCK, WastelandContent.id("gateway_natural_floor"));
    private GatewayTravel() { }

    public static GatewayAnchorBlockEntity anchorAt(ServerLevel level, BlockPos portal) {
        for (int down = 1; down <= 2; down++) if (level.getBlockEntity(portal.below(down)) instanceof GatewayAnchorBlockEntity anchor) return anchor;
        return null;
    }

    public static GatewayAnchorBlockEntity createAnchor(ServerLevel level, BlockPos pos, ResourceKey<Level> destination, Direction.Axis axis) {
        level.setBlock(pos, GatewayContent.ANCHOR.get().defaultBlockState().setValue(GatewayAnchorBlock.ACTIVE, true), 2);
        var anchor = (GatewayAnchorBlockEntity) level.getBlockEntity(pos);
        anchor.configure(destination);
        for (int y = 1; y <= 2; y++) level.setBlock(pos.above(y), GatewayContent.PORTAL.get().defaultBlockState().setValue(GatewayPortalBlock.AXIS, axis), 2);
        return anchor;
    }

    public static DimensionTransition transition(ServerLevel source, Entity entity, BlockPos portal) {
        var anchor = anchorAt(source, portal);
        if (anchor == null || !anchor.intact() || !entity.canUsePortal(false) || entity.isVehicle()
                || entity instanceof EnderDragon || entity instanceof WitherBoss
                || !GatewayKeyItem.connected(source.dimension(), anchor.destination())) return null;
        var destination = source.getServer().getLevel(anchor.destination());
        if (destination == null || !source.getServer().isLevelEnabled(destination) || !entity.canChangeDimensions(source, destination)) return null;
        var partner = validPartner(source, destination, anchor, entity);
        if (partner == null) {
            BlockPos near = anchor.getBlockPos();
            var border = destination.getWorldBorder();
            near = BlockPos.containing(Math.clamp(near.getX(), border.getMinX()+3, border.getMaxX()-3), near.getY(),
                    Math.clamp(near.getZ(), border.getMinZ()+3, border.getMaxZ()-3));
            BlockPos landing = findLanding(destination, near, entity);
            if (landing == null) return null;
            if (destination.getBlockState(landing).isAir()) {
                var floor = destination.dimension().equals(WastelandContent.DIMENSION) ? WastelandContent.STONE.get()
                        : destination.dimension().equals(dev.abyssalport.content.DreadlandsContent.DIMENSION)
                        ? dev.abyssalport.content.DreadlandsContent.STONE.get() : AbyssalPort.DARKSTONE.get();
                for (BlockPos pos : BlockPos.betweenClosed(landing.offset(-2, 0, -2), landing.offset(2, 0, 2))) destination.setBlock(pos, floor.defaultBlockState(), 3);
            }
            partner = createAnchor(destination, landing, source.dimension(), source.getBlockState(portal).getValue(GatewayPortalBlock.AXIS));
            partner.pairWith(anchor); anchor.pairWith(partner);
        }
        return new DimensionTransition(destination, arrival(partner.getBlockPos()), Vec3.ZERO, entity.getYRot(), entity.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET));
    }

    private static GatewayAnchorBlockEntity validPartner(ServerLevel source, ServerLevel destination, GatewayAnchorBlockEntity anchor, Entity entity) {
        if (anchor.partner() == null || anchor.partnerId() == null || !destination.getWorldBorder().isWithinBounds(anchor.partner())) return null;
        // A saved return anchor may be outside loaded chunks after a world restart.
        // Load its exact chunk before treating it as absent; do not silently create a duplicate.
        destination.getChunkAt(anchor.partner());
        if (!(destination.getBlockEntity(anchor.partner()) instanceof GatewayAnchorBlockEntity partner)) return null;
        return partner.intact() && source.dimension().equals(partner.destination()) && partner.portalId().equals(anchor.partnerId())
                && anchor.portalId().equals(partner.partnerId()) && anchor.getBlockPos().equals(partner.partner())
                && safeArrival(destination, partner.getBlockPos(), entity) ? partner : null;
    }

    public static BlockPos findLanding(ServerLevel destination, BlockPos near, Entity entity) {
        // Build above terrain, not in it. Even the center ground block remains untouched.
        for (int radius = 0; radius <= 16; radius++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
            int px = near.getX()+x, pz = near.getZ()+z;
            int y = destination.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
            BlockPos pos = new BlockPos(px, y, pz);
            if (destination.getWorldBorder().isWithinBounds(pos.offset(-2, 0, -2)) && destination.getWorldBorder().isWithinBounds(pos.offset(2, 0, 2))
                    && destination.getBlockState(pos.below()).is(NATURAL_FLOOR)
                    && solidGround(destination, pos.below()) && clearVolume(destination, pos)
                    && fits(destination, pos, entity)) return pos;
        }
        // Sea / void fallback: a small platform in empty air, never clearing an existing building.
        int start = Math.max(destination.getSeaLevel()+6, destination.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ())+3);
        start = Math.max(start, destination.getMinBuildHeight()+4);
        for (int y = start; y < destination.getMaxBuildHeight()-5; y++) {
            BlockPos pos = new BlockPos(near.getX(), y, near.getZ());
            if (destination.getWorldBorder().isWithinBounds(pos.offset(-2, 0, -2))
                    && destination.getWorldBorder().isWithinBounds(pos.offset(2, 0, 2))
                    && clearVolume(destination, pos) && fits(destination, pos, entity)) return pos;
        }
        return null;
    }

    private static boolean clearVolume(ServerLevel level, BlockPos floor) {
        if (floor.getY() < level.getMinBuildHeight() || floor.getY()+4 >= level.getMaxBuildHeight()) return false;
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-2, 0, -2), floor.offset(2, 4, 2))) {
            if (!level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty() || level.getBlockEntity(pos) != null) return false;
        }
        return true;
    }

    private static boolean solidGround(ServerLevel level, BlockPos floor) {
        for (BlockPos pos : BlockPos.betweenClosed(floor.offset(-2, 0, -2), floor.offset(2, 0, 2)))
            if (level.getBlockEntity(pos) != null || !level.getFluidState(pos).isEmpty()
                    || !level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)) return false;
        return true;
    }

    private static boolean fits(ServerLevel level, BlockPos anchor, Entity entity) {
        var box = entity.getBoundingBox().move(arrival(anchor).subtract(entity.position()));
        // The arrival is beside the portal, not centered over it. A broad entity
        // must fit completely over the platform, not just have its center supported.
        return box.minX >= anchor.getX()-2 && box.maxX <= anchor.getX()+3
                && box.minZ >= anchor.getZ()-2 && box.maxZ <= anchor.getZ()+3
                && box.maxY < level.getMaxBuildHeight() && level.getWorldBorder().isWithinBounds(box)
                && level.noCollision(entity, box);
    }

    public static Vec3 arrival(BlockPos anchor) { return new Vec3(anchor.getX()+1.5, anchor.getY()+1, anchor.getZ()+0.5); }
    public static boolean safeArrival(ServerLevel destination, BlockPos anchor, Entity entity) {
        Vec3 point = arrival(anchor);
        if (!destination.getWorldBorder().isWithinBounds(BlockPos.containing(point))) return false;
        var box = entity.getBoundingBox().move(point.subtract(entity.position()));
        if (!destination.getWorldBorder().isWithinBounds(box) || box.maxY >= destination.getMaxBuildHeight()
                || !destination.noCollision(entity, box)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)))
            if (!destination.getFluidState(pos).isEmpty()) return false;
        int floorY = net.minecraft.util.Mth.floor(point.y)-1;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX+.0001, floorY, box.minZ+.0001),
                BlockPos.containing(box.maxX-.0001, floorY, box.maxZ-.0001))) {
            if (!destination.getFluidState(pos).isEmpty() || !destination.getBlockState(pos).isCollisionShapeFullBlock(destination, pos)) return false;
        }
        return true;
    }
}
