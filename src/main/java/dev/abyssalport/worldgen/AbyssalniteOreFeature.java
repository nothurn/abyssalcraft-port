package dev.abyssalport.worldgen;

import dev.abyssalport.content.AbyssalniteContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;

/** Original 1–3-block vein size; count and height remain configurable placement data. */
public final class AbyssalniteOreFeature extends Feature<NoneFeatureConfiguration> {
    public AbyssalniteOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int size = 1 + context.random().nextInt(3);
        return Feature.ORE.place(new OreConfiguration(new BlockMatchTest(Blocks.STONE),
                AbyssalniteContent.ORE.get().defaultBlockState(), size), context.level(),
                context.chunkGenerator(), context.random(), context.origin());
    }
}
