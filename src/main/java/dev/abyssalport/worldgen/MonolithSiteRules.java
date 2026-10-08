package dev.abyssalport.worldgen;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.content.StatueContent;
import dev.abyssalport.content.ShoggothLairContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Checks completed pillars without scanning every block in nearby columns. */
public final class MonolithSiteRules {
    private MonolithSiteRules() {
    }

    public static boolean farFromExistingMonoliths(ServerLevel level, BlockPos center) {
        int radius = MonolithSpacing.MIN_BLOCKS;
        if (!level.hasChunksAt(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
            return false;
        }
        for (int chunkX = (center.getX() - radius) >> 4; chunkX <= (center.getX() + radius) >> 4; chunkX++) {
            for (int chunkZ = (center.getZ() - radius) >> 4; chunkZ <= (center.getZ() + radius) >> 4; chunkZ++) {
                for (BlockPos existing : level.getChunk(chunkX, chunkZ).getBlockEntitiesPos()) {
                    if (MonolithSpacing.tooClose(center.getX(), center.getZ(), existing.getX(), existing.getZ())
                            && isFinishedMonolith(level, existing)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isFinishedMonolith(ServerLevel level, BlockPos effigy) {
        var statue=level.getBlockState(effigy);
        boolean legacy=statue.is(AbyssalPort.PROTOTYPE_EFFIGY.get());
        var stone=legacy?AbyssalPort.DARKSTONE.get():ShoggothLairContent.MONOLITH_STONE.get();
        var foundation=level.getBlockState(effigy.below(5));
        if (!StatueContent.isStatue(statue)
                || !(foundation.is(stone)||!legacy&&foundation.is(ShoggothLairContent.BIOMASS.get()))) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.getBlockState(effigy.offset(dx, -1, dz)).is(stone)) {
                    return false;
                }
            }
        }
        return true;
    }
}
