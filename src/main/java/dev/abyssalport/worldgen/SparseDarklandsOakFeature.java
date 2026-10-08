package dev.abyssalport.worldgen;

import dev.abyssalport.content.DarklandsWoodContent;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;

/** Plains/highlands: 4/5 selection reaches a 1/10 tree roll in the source. */
public final class SparseDarklandsOakFeature extends Feature<ProbabilityFeatureConfiguration> {
    public SparseDarklandsOakFeature() { super(ProbabilityFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<ProbabilityFeatureConfiguration> context) {
        if(context.random().nextFloat()>=context.config().probability) return false;
        return DarklandsWoodContent.TREE.get().place(new FeaturePlaceContext<>(java.util.Optional.empty(),context.level(),context.chunkGenerator(),
                context.random(),context.origin(),NoneFeatureConfiguration.INSTANCE));
    }
}
