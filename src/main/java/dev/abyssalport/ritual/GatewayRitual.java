package dev.abyssalport.ritual;

import dev.abyssalport.content.GatewayContent;
import dev.abyssalport.item.GatewayKeyItem;
import dev.abyssalport.world.GatewayTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Source-informed 1,000-PE portal ritual: key returned, eight stands become pillars, altar becomes anchor. */
public final class GatewayRitual {
    private GatewayRitual() { }
    public static boolean canComplete(ServerLevel level, BlockPos altar, ItemStack key) {
        var target = GatewayKeyItem.destination(key);
        if (!(key.getItem() instanceof GatewayKeyItem gatewayKey) || !gatewayKey.permits(level.dimension(), target)) return false;
        var destination = level.getServer().getLevel(target);
        if (destination == null || !level.getServer().isLevelEnabled(destination)) return false;
        for (int y = 1; y <= 2; y++) if (!level.getBlockState(altar.above(y)).isAir() || !level.getFluidState(altar.above(y)).isEmpty()) return false;
        return true;
    }
    public static boolean permitted(ServerLevel level, BlockPos altar, ServerPlayer player, ItemStack key) {
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            BlockPos pos = altar.offset(offset);
            if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, Direction.UP, key)) return false;
        }
        for (int y = 0; y <= 2; y++) if (!level.mayInteract(player, altar.above(y))
                || !player.mayUseItemAt(altar.above(y), Direction.UP, key)) return false;
        return true;
    }
    public static void complete(ServerLevel level, BlockPos altar, ServerPlayer player, ItemStack key) {
        var destination = GatewayKeyItem.destination(key);
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) level.setBlock(altar.offset(offset), GatewayContent.MONOLITH_PILLAR.get().defaultBlockState(), 3);
        Direction.Axis axis = player.getDirection().getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        GatewayTravel.createAnchor(level, altar, destination, axis);
        if (!player.addItem(key)) player.drop(key, false);
    }
}
