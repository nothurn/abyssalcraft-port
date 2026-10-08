package dev.abyssalport.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/** Keeps the original per-attempt random vein sizes, with native ore placement. */
public final class RangedOreFeature extends Feature<RangedOreFeature.Configuration> {
    public record Configuration(RuleTest target, BlockState state, int minSize, int maxSize) implements FeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RuleTest.CODEC.fieldOf("target").forGetter(Configuration::target),
                BlockState.CODEC.fieldOf("state").forGetter(Configuration::state),
                Codec.intRange(1, 64).fieldOf("min_size").forGetter(Configuration::minSize),
                Codec.intRange(1, 64).fieldOf("max_size").forGetter(Configuration::maxSize)
        ).apply(instance, Configuration::new));
        public Configuration {
            if (maxSize < minSize) throw new IllegalArgumentException("Ore size range is reversed");
        }
    }
    public RangedOreFeature() { super(Configuration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<Configuration> context) {
        var config = context.config();
        int size = config.minSize() + context.random().nextInt(config.maxSize() - config.minSize() + 1);
        return Feature.ORE.place(new OreConfiguration(config.target(), config.state(), size),
                context.level(), context.chunkGenerator(), context.random(), context.origin());
    }
}
