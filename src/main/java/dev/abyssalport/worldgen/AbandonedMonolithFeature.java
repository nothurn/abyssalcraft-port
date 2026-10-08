package dev.abyssalport.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;

/** Temporary survival route to PE until Shoggoth-built monoliths are ported. */
public final class AbandonedMonolithFeature extends Feature<NoneFeatureConfiguration> {
    public AbandonedMonolithFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int height = 5 + context.random().nextInt(3);
        BlockPos origin = context.origin();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        int startX = Math.clamp(origin.getX() & 15, 2, 13);
        int startZ = Math.clamp(origin.getZ() & 15, 2, 13);

        // A single tree trunk or uneven patch should not discard the entire rare feature attempt.
        // Search only the selected chunk so generation never probes or writes a neighbour chunk.
        for (int distance = 0; distance <= 22; distance++) {
            for (int localX = 2; localX <= 13; localX++) {
                for (int localZ = 2; localZ <= 13; localZ++) {
                    if (Math.abs(localX - startX) + Math.abs(localZ - startZ) != distance) {
                        continue;
                    }
                    int x = (chunkX << 4) + localX;
                    int z = (chunkZ << 4) + localZ;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos center = new BlockPos(x, y, z);
                    if (MonolithBuilder.build(level, center, height, 2, context.random())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
