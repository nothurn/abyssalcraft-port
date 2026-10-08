package dev.abyssalport.worldgen;

import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.ParameterUtils.Continentalness;
import terrablender.api.ParameterUtils.Depth;
import terrablender.api.ParameterUtils.Erosion;
import terrablender.api.ParameterUtils.Humidity;
import terrablender.api.ParameterUtils.ParameterPointListBuilder;
import terrablender.api.ParameterUtils.Temperature;
import terrablender.api.ParameterUtils.Weirdness;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

/** Small humid inland pockets, rather than replacing every forest. */
public final class DarklandsRegion extends Region {
    public DarklandsRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry,
            Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        VanillaParameterOverlayBuilder overlay = new VanillaParameterOverlayBuilder();
        new ParameterPointListBuilder()
                .temperature(Temperature.COOL, Temperature.NEUTRAL)
                .humidity(Humidity.WET, Humidity.HUMID)
                .continentalness(Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_3, Erosion.EROSION_4)
                .depth(Depth.SURFACE)
                .weirdness(Weirdness.span(Weirdness.LOW_SLICE_NORMAL_DESCENDING,
                        Weirdness.LOW_SLICE_VARIANT_ASCENDING))
                .build().forEach(point -> overlay.add(point, DarklandsWorldgen.DARKLANDS));
        // Keep the existing biome key/range for saves; extend only disjoint native
        // humidity/erosion ranges. Native noise, not legacy baseHeight, controls terrain.
        new ParameterPointListBuilder().temperature(Temperature.COOL,Temperature.NEUTRAL)
                .humidity(Humidity.NEUTRAL).continentalness(Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_4).depth(Depth.SURFACE)
                .weirdness(Weirdness.span(Weirdness.LOW_SLICE_NORMAL_DESCENDING,Weirdness.LOW_SLICE_VARIANT_ASCENDING))
                .build().forEach(point->overlay.add(point,DarklandsWorldgen.PLAINS));
        new ParameterPointListBuilder().temperature(Temperature.COOL,Temperature.NEUTRAL)
                .humidity(Humidity.WET,Humidity.HUMID).continentalness(Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_5).depth(Depth.SURFACE)
                .weirdness(Weirdness.span(Weirdness.LOW_SLICE_NORMAL_DESCENDING,Weirdness.LOW_SLICE_VARIANT_ASCENDING))
                .build().forEach(point->overlay.add(point,DarklandsWorldgen.FOREST));
        new ParameterPointListBuilder().temperature(Temperature.COOL,Temperature.NEUTRAL)
                .humidity(Humidity.WET,Humidity.HUMID).continentalness(Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_2).depth(Depth.SURFACE)
                .weirdness(Weirdness.span(Weirdness.LOW_SLICE_NORMAL_DESCENDING,Weirdness.LOW_SLICE_VARIANT_ASCENDING))
                .build().forEach(point->overlay.add(point,DarklandsWorldgen.HIGHLANDS));
        new ParameterPointListBuilder().temperature(Temperature.COOL,Temperature.NEUTRAL)
                .humidity(Humidity.WET,Humidity.HUMID).continentalness(Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_0,Erosion.EROSION_1).depth(Depth.SURFACE)
                .weirdness(Weirdness.span(Weirdness.LOW_SLICE_NORMAL_DESCENDING,Weirdness.LOW_SLICE_VARIANT_ASCENDING))
                .build().forEach(point->overlay.add(point,DarklandsWorldgen.MOUNTAINS));
        overlay.build().forEach(mapper);
    }
}
