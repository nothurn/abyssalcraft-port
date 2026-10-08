package dev.abyssalport.energy;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.content.*;
import dev.abyssalport.worldgen.DarklandsWorldgen;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;

/** TileEntityEnergyDepositioner.processItem, copyright 2012-2026 Shinoow, LGPL-3.0.
 * Modern biomes have 4x4 horizontal cells: only full cells INSIDE the original
 * 17x17 footprint change. No force-loading, outside columns, or BE replacement.
 */
public final class DarklandsDeposition {
    private DarklandsDeposition() { }
    public static BlockState replacement(BlockState state) {
        Block target = null;
        if (state.is(Blocks.STONE)) target = AbyssalPort.DARKSTONE.get();
        else if (state.is(Blocks.COBBLESTONE)) target = AbyssalPort.DARKSTONE_COBBLESTONE.get();
        else if (state.is(Blocks.STONE_BRICKS) || state.is(Blocks.MOSSY_STONE_BRICKS)) target = AbyssalPort.DARKSTONE_BRICKS.get();
        else if (state.is(Blocks.CHISELED_STONE_BRICKS)) target = AbyssalPort.CHISELED_DARKSTONE_BRICKS.get();
        else if (state.is(Blocks.CRACKED_STONE_BRICKS)) target = AbyssalPort.CRACKED_DARKSTONE_BRICKS.get();
        else if (state.is(Blocks.COBBLESTONE_WALL)) target = EnergyDepositionerContent.COBBLE_WALL.get();
        else if (state.is(Blocks.STONE_BRICK_SLAB)) target = AbyssalPort.DARKSTONE_BRICK_SLAB.get();
        else if (state.is(Blocks.COBBLESTONE_SLAB)) target = EnergyDepositionerContent.COBBLE_SLAB.get();
        else if (state.is(Blocks.STONE_SLAB) || state.is(Blocks.SMOOTH_STONE_SLAB)) target = EnergyDepositionerContent.STONE_SLAB.get();
        else if (state.is(Blocks.STONE_BRICK_STAIRS)) target = AbyssalPort.DARKSTONE_BRICK_STAIRS.get();
        else if (state.is(Blocks.COBBLESTONE_STAIRS)) target = EnergyDepositionerContent.COBBLE_STAIRS.get();
        // The old Blocks.LOG/LEAVES include these four species, not acacia/dark oak.
        else if (state.is(Blocks.OAK_LOG) || state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.BIRCH_LOG) || state.is(Blocks.JUNGLE_LOG)) target = DarklandsWoodContent.LOG.get();
        else if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.BIRCH_LEAVES) || state.is(Blocks.JUNGLE_LEAVES)) target = DarklandsWoodContent.LEAVES.get();
        else if (state.is(Blocks.OAK_PLANKS) || state.is(Blocks.SPRUCE_PLANKS) || state.is(Blocks.BIRCH_PLANKS)
                || state.is(Blocks.JUNGLE_PLANKS) || state.is(Blocks.ACACIA_PLANKS) || state.is(Blocks.DARK_OAK_PLANKS)) target = DarklandsWoodContent.PLANKS.get();
        else if (state.is(Blocks.OAK_STAIRS)) target = DarklandsWoodContent.STAIRS.get();
        else if (state.is(Blocks.OAK_SLAB) || state.is(Blocks.SPRUCE_SLAB) || state.is(Blocks.BIRCH_SLAB)
                || state.is(Blocks.JUNGLE_SLAB) || state.is(Blocks.ACACIA_SLAB) || state.is(Blocks.DARK_OAK_SLAB)) target = DarklandsWoodContent.SLAB.get();
        else if (state.is(Blocks.OAK_FENCE)) target = DarklandsWoodContent.FENCE.get();
        if (target == null) return state;
        var result = target.defaultBlockState();
        for (var property : state.getProperties()) if (result.hasProperty(property)) result = copy(result, state, property);
        return result;
    }
    private static <T extends Comparable<T>> BlockState copy(BlockState target, BlockState source, net.minecraft.world.level.block.state.properties.Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }
    public static float apply(ServerLevel level, BlockPos anchor, RandomSource random) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return 0;
        int dark = 0;
        for (int x = anchor.getX() - 8; x <= anchor.getX() + 8; x++) for (int z = anchor.getZ() - 8; z <= anchor.getZ() + 8; z++) {
            var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk != null && DarklandsWorldgen.isDarklands(level.getBiome(new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z)))) dark++;
        }
        var changed = new LinkedHashSet<ChunkAccess>();
        var emitter = level.getBlockEntity(anchor);
        int minX = -Math.floorDiv(-(anchor.getX() - 8), 4) * 4, minZ = -Math.floorDiv(-(anchor.getZ() - 8), 4) * 4;
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(DarklandsWorldgen.DARKLANDS);
        for (int x = minX; x + 3 <= anchor.getX() + 8; x += 4) for (int z = minZ; z + 3 <= anchor.getZ() + 8; z += 4) {
            if (emitter != null && level.getBlockEntity(anchor) != emitter) break;
            var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null || !level.getWorldBorder().isWithinBounds(new BlockPos(x, anchor.getY(), z))
                    || !level.getWorldBorder().isWithinBounds(new BlockPos(x + 3, anchor.getY(), z + 3))
                    || DarklandsWorldgen.isDarklands(level.getBiome(new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z)))
                    || random.nextInt(289) > dark) continue;
            if (NeoForge.EVENT_BUS.post(new DepositionEvent(level, anchor, x, z)).isCanceled()
                    || emitter != null && level.getBlockEntity(anchor) != emitter) continue;
            int previouslyDark = 0;
            for (int dx = 0; dx < 4; dx++) for (int dz = 0; dz < 4; dz++)
                if (DarklandsWorldgen.isDarklands(level.getBiome(new BlockPos(x + dx,
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz), z + dz)))) previouslyDark++;
            var rollback = new ArrayList<BlockSnapshot>(); boolean denied = false;
            outer: for (int section = 0; section < chunk.getSections().length; section++) {
                if (chunk.getSections()[section].hasOnlyAir()) continue;
                int low = level.getMinBuildHeight() + section * 16;
                for (int y = low; y < low + 16; y++) for (int dx = 0; dx < 4; dx++) for (int dz = 0; dz < 4; dz++) {
                    var pos = new BlockPos(x + dx, y, z + dz); var state = level.getBlockState(pos); var replacement = replacement(state);
                    if (state == replacement || level.getBlockEntity(pos) != null) continue;
                    var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
                    if (level.setBlock(pos, replacement, 2)) {
                        rollback.add(snapshot);
                        if (NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos), level.getBlockState(pos.below()), null)).isCanceled()
                                || emitter != null && level.getBlockEntity(anchor) != emitter) {
                            denied = true; break outer;
                        }
                    }
                }
            }
            if (denied) { Collections.reverse(rollback); rollback.forEach(BlockSnapshot::restore); continue; }
            int qx = x >> 2, qz = z >> 2;
            // Each section resolver reads its previous untouched cells; no logical/physical Y mix.
            for (int section = 0; section < chunk.getSections().length; section++) {
                var storage = chunk.getSections()[section];
                storage.fillBiomesFromNoise((bx, by, bz, sampler) -> bx == qx && bz == qz ? biome : storage.getNoiseBiome(bx & 3, by & 3, bz & 3),
                        level.getChunkSource().randomState().sampler(), chunk.getPos().x * 4, (level.getMinBuildHeight() >> 2) + section * 4, chunk.getPos().z * 4);
            }
            chunk.setUnsaved(true); changed.add(chunk);
            // Count the actual newly changed surface columns, not cells as single columns.
            // The surface resolver is jittered at cell borders; do not double-count it.
            dark = Math.min(289, dark + 16 - previouslyDark);
        }
        if (!changed.isEmpty()) level.getChunkSource().chunkMap.resendBiomesForChunks(new ArrayList<>(changed));
        return dark / 289F;
    }
}
