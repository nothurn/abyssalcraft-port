package dev.abyssalport.worldgen;

import dev.abyssalport.AbyssalPort;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A small Overworld habitat, not a replacement for the missing Darklands biome. */
public final class ShadowGroveFeature extends Feature<NoneFeatureConfiguration> {
    public ShadowGroveFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        int radius = 3 + random.nextInt(2);
        int placed = 0;
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        int centerX = (origin.getX() & ~15) + Math.clamp(origin.getX() & 15, radius, 15 - radius);
        int centerZ = (origin.getZ() & ~15) + Math.clamp(origin.getZ() & 15, radius, 15 - radius);

        // Stay inside the generating chunk; never read or write a neighboring chunk.
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                if ((x >> 4) != chunkX || (z >> 4) != chunkZ
                        || dx * dx + dz * dz > radius * radius + random.nextInt(5) - 2) {
                    continue;
                }
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos ground = new BlockPos(x, y, z);
                if (Math.abs(y - origin.getY()) > 8 || !level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                        || !level.getFluidState(ground.above()).isEmpty()) {
                    continue;
                }
                if (level.setBlock(ground, AbyssalPort.DARKLANDS_SOIL.get().defaultBlockState(), 2)) {
                    placed++;
                }
            }
        }
        return placed > 0;
    }
}
