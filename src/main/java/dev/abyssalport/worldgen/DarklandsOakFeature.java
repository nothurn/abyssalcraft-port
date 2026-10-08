package dev.abyssalport.worldgen;

import dev.abyssalport.block.DarklandsLogBlock;
import dev.abyssalport.content.DarklandsWoodContent;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** WorldGenDLT's 9..11 trunk / six-block ascending branches, with a non-destructive preflight. */
public final class DarklandsOakFeature extends Feature<NoneFeatureConfiguration> {
    public DarklandsOakFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var level = context.level(); var base = context.origin(); var random = context.random();
        if (!level.getBlockState(base.below()).is(BlockTags.DIRT)) return false;
        int height = 9 + random.nextInt(3), leafHeight = random.nextInt(3), branches = 4 + random.nextInt(8);
        if (base.getY() <= level.getMinBuildHeight() || base.getY() + height + 3 >= level.getMaxBuildHeight()) return false;
        Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        var leaves = new LinkedHashSet<BlockPos>();
        for (int y = 0; y < height; y++) logs.put(base.above(y), log(random, false));
        leaves.add(base.above(height));
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            int rootHeight = random.nextInt(3);
            for (int y = 0; y <= rootHeight; y++) logs.put(base.relative(direction).above(y), log(random, y == rootHeight));
        }
        double angle = random.nextInt(360 / branches);
        for (int branch = 0; branch < branches; branch++) {
            angle += 360.0 / branches;
            double branchY = height - random.nextFloat() * leafHeight - 2.0;
            for (int step = 1; step <= 6; step++) {
                branchY += .5;
                var pos = base.offset((int)(step * Math.cos(Math.toRadians(angle))), (int)branchY,
                        (int)(step * Math.sin(Math.toRadians(angle))));
                logs.put(pos, log(random, true)); leaves.add(pos.above());
            }
        }
        leaves.removeAll(logs.keySet());
        // Check the entire planned trunk and crown before touching soil or consuming a sapling.
        for (var pos : logs.keySet()) if (!replaceable(level.getBlockState(pos))) return false;
        for (var pos : leaves) if (!replaceable(level.getBlockState(pos))) return false;
        level.setBlock(base.below(), Blocks.DIRT.defaultBlockState(), 2);
        logs.forEach((pos, state) -> level.setBlock(pos, state, 3));
        var leaf = DarklandsWoodContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE, 1).setValue(LeavesBlock.PERSISTENT, false);
        for (var pos : leaves) level.setBlock(pos, leaf, 3);
        return true;
    }
    private static boolean replaceable(BlockState state) {
        return state.getFluidState().isEmpty() && (state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES)
                || state.is(DarklandsWoodContent.SAPLING.get()));
    }
    private static BlockState log(RandomSource random, boolean bark) {
        return (random.nextInt(10) == 0 ? DarklandsWoodContent.HOLLOW_LOG : DarklandsWoodContent.LOG)
                .get().defaultBlockState().setValue(DarklandsLogBlock.BARK, bark);
    }
}
