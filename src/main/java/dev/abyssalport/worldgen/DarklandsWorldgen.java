package dev.abyssalport.worldgen;

import dev.abyssalport.AbyssalPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

/** Registers a rare Overworld biome without replacing the vanilla biome source. */
public final class DarklandsWorldgen {
    public static final ResourceKey<Biome> DARKLANDS = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "darklands"));
    public static final ResourceKey<Biome> PLAINS = key("darklands_plains"), FOREST = key("darklands_forest"),
            HIGHLANDS = key("darklands_highlands"), MOUNTAINS = key("darklands_mountains");
    public static final java.util.List<ResourceKey<Biome>> FAMILY = java.util.List.of(DARKLANDS, PLAINS, FOREST, HIGHLANDS, MOUNTAINS);
    private static ResourceKey<Biome> key(String name) { return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,name)); }
    public static boolean isDarklands(net.minecraft.core.Holder<Biome> biome) { return FAMILY.stream().anyMatch(biome::is); }
    public static final ResourceLocation REGION = ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,
            "darklands_region");

    private DarklandsWorldgen() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new DarklandsRegion(REGION, 1));
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD,
                    AbyssalPort.MOD_ID, surfaceRules());
        });
    }

    static SurfaceRules.RuleSource surfaceRules() {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.isBiome(MOUNTAINS),SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,SurfaceRules.state(AbyssalPort.DARKSTONE.get().defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,SurfaceRules.state(AbyssalPort.DARKSTONE.get().defaultBlockState())))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(HIGHLANDS),SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1,0),SurfaceRules.state(AbyssalPort.DARKLANDS_SOIL.get().defaultBlockState()))),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,SurfaceRules.state(AbyssalPort.DARKSTONE.get().defaultBlockState())))),
                SurfaceRules.ifTrue(SurfaceRules.isBiome(DARKLANDS,PLAINS,FOREST), SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                        SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
                                SurfaceRules.state(AbyssalPort.DARKLANDS_SOIL.get().defaultBlockState()))),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR,
                        SurfaceRules.state(Blocks.DIRT.defaultBlockState())))));
    }
}
