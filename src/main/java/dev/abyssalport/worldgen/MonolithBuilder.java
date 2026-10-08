package dev.abyssalport.worldgen;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.content.ShoggothLairContent;
import dev.abyssalport.content.StatueContent;
import dev.abyssalport.block.DeityStatueBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Shared, non-destructive site rules for abandoned and Shoggoth-built monoliths. */
public final class MonolithBuilder {
    private MonolithBuilder() {
    }

    public static boolean canBuild(LevelAccessor level, BlockPos center, int height) {
        if (height < 5 || height > 12 || center.getY() + height >= level.getMaxBuildHeight()) {
            return false;
        }
        // Sky-light data is not finalized during feature placement; use terrain height instead.
        // This also rejects a leaf directly over the future PE effigy.
        if (level.getHeight(Heightmap.Types.WORLD_SURFACE, center.getX(), center.getZ())
                > center.getY() + height + 1) {
            return false;
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos base = center.offset(x, 0, z);
                if (!level.getBlockState(base.below()).isFaceSturdy(level, base.below(), Direction.UP)) {
                    return false;
                }
                for (int y = 0; y <= height; y++) {
                    BlockState state = level.getBlockState(base.above(y));
                    if ((!state.canBeReplaced() && !state.is(AbyssalPort.SHOGGOTH_OOZE.get()))
                            || !state.getFluidState().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean build(LevelAccessor level, BlockPos center, int height, int updateFlags) {
        return build(level,center,height,updateFlags,level.getRandom());
    }

    public static boolean build(LevelAccessor level, BlockPos center, int height, int updateFlags, net.minecraft.util.RandomSource random) {
        if (!canBuild(level, center, height)) {
            return false;
        }
        BlockState stone = ShoggothLairContent.MONOLITH_STONE.get().defaultBlockState();
        for (int y = 0; y < height; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(center.offset(x, y, z), stone, updateFlags);
                }
            }
        }
        level.setBlock(center,ShoggothLairContent.BIOMASS.get().defaultBlockState(),updateFlags);
        var deity=StatueContent.Deity.values()[random.nextInt(StatueContent.Deity.values().length)];
        var facing=Direction.from2DDataValue(random.nextInt(4));
        level.setBlock(center.above(height), StatueContent.block(deity).defaultBlockState().setValue(DeityStatueBlock.FACING,facing), updateFlags);
        return true;
    }
}
