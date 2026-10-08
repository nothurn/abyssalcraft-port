package dev.abyssalport.worldgen;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.abyssalport.config.MobEcologyConfig;
import dev.abyssalport.content.MobEcologyContent;
import dev.abyssalport.entity.CorruptedAnimalEntity.Kind;
import net.minecraft.core.*;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.neoforged.neoforge.common.world.*;

/** Real native, configurable source spawn weights; data packs can replace/disable each modifier. */
public record MobEcologySpawns(HolderSet<Biome> biomes,String group,boolean dense) implements BiomeModifier {
    public static final MapCodec<MobEcologySpawns> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
        Biome.LIST_CODEC.fieldOf("biomes").forGetter(MobEcologySpawns::biomes),
        Codec.STRING.validate(value->java.util.Set.of("evil","demon","offspring").contains(value)?DataResult.success(value):DataResult.error(()->"Unknown ecology group"))
            .fieldOf("group").forGetter(MobEcologySpawns::group),
        Codec.BOOL.optionalFieldOf("dense",false).forGetter(MobEcologySpawns::dense)).apply(instance,MobEcologySpawns::new));
    @Override public MapCodec<? extends BiomeModifier> codec() { return CODEC; }
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if(phase!=Phase.ADD||!biomes.contains(biome))return;
        int weight=MobEcologyConfig.weight(group)*(dense?2:1);if(weight<=0)return;
        if(group.equals("offspring"))builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(MobEcologyContent.OFFSPRING.get(),weight,1,3));
        else for(Kind kind:Kind.values())builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(MobEcologyContent.type(kind,group.equals("demon")),weight,1,3));
    }
}
