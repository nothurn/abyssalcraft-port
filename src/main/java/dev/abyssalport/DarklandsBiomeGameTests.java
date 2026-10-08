package dev.abyssalport;

import dev.abyssalport.worldgen.*;
import dev.abyssalport.research.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DarklandsBiomeGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private DarklandsBiomeGameTests() { }
    @GameTest(template="empty",batch="darklands_family_loaded")
    public static void EveryVariantIsMappedLoadedAndKeepsDiscoveryOreAndEarlyPE(GameTestHelper helper) {
        var level=helper.getLevel(); var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        var mapped=new HashMap<ResourceKey<Biome>,List<Climate.ParameterPoint>>();
        new DarklandsRegion(DarklandsWorldgen.REGION,1).addBiomes(biomes,pair->mapped.computeIfAbsent(pair.getSecond(),ignored->new ArrayList<>()).add(pair.getFirst()));
        var preset=new MultiNoiseBiomeSourceParameterList(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD,biomes.asLookup());
        @SuppressWarnings("unchecked")
        var selector=(terrablender.worldgen.IExtendedParameterList<Holder<Biome>>)(Object)preset.parameters();
        selector.initializeForTerraBlender(level.registryAccess(),terrablender.api.RegionType.OVERWORLD,12345L);
        int region=terrablender.api.Regions.getIndex(terrablender.api.RegionType.OVERWORLD,DarklandsWorldgen.REGION);
        BlockPos regionPos=null;
        search: for(int x=-256;x<=256;x+=8) for(int z=-256;z<=256;z+=8)
            if(selector.getUniqueness(x,0,z)==region) { regionPos=new BlockPos(x,0,z); break search; }
        helper.assertTrue(regionPos!=null,"Native parameter list cannot select the registered Darklands region");
        var monolith=level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(WastelandContentId("abandoned_monolith"));
        var ore=level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(WastelandContentId("ore_abyssalnite"));
        var player=helper.makeMockServerPlayerInLevel(); var plain=biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
        for(var key:DarklandsWorldgen.FAMILY) {
            var holder=biomes.getHolderOrThrow(key); var biome=holder.value();
            helper.assertTrue(mapped.containsKey(key)&&DarklandsWorldgen.isDarklands(holder),"Variant is a registry-only stub: "+key);
            boolean selected=false;
            for(var point:mapped.get(key)) {
                var target=new Climate.TargetPoint(midpoint(point.temperature()),midpoint(point.humidity()),midpoint(point.continentalness()),
                        midpoint(point.erosion()),midpoint(point.depth()),midpoint(point.weirdness()));
                if(selector.findValuePositional(target,regionPos.getX(),0,regionPos.getZ()).is(key)) { selected=true; break; }
            }
            helper.assertTrue(selected,"Variant's own mapped climate cannot actually select it: "+key);
            helper.assertTrue(biome.getGenerationSettings().hasFeature(monolith)&&biome.getGenerationSettings().hasFeature(ore),"Variant loses monolith/ore route: "+key);
            player.setData(AbyssalPort.EARLY_RESEARCH.get(),0);
            helper.assertTrue(DarklandsResearch.discover(player,holder)&&EarlyResearch.has(player,EarlyResearch.DARKLANDS),"Variant does not unlock the actual common discovery: "+key);
            helper.assertTrue(dev.abyssalport.entity.ShadowCreatureEntity.canSpawnOnSurface(AbyssalPort.DARKSTONE.get().defaultBlockState(),holder),"Bare Darklands stone cannot host its actual Shadow material carrier");
        }
        helper.assertTrue(!dev.abyssalport.entity.ShadowCreatureEntity.canSpawnOnSurface(AbyssalPort.DARKSTONE.get().defaultBlockState(),plain),"Arbitrary Darkstone in vanilla Plains became a new spawn habitat");
        helper.succeed();
    }
    private static long midpoint(Climate.Parameter parameter) { return (parameter.min()+parameter.max())/2; }
    private static ResourceLocation WastelandContentId(String name) { return ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,name); }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="darklands_family_natural",timeoutTicks=1800)
    public static void NativeFreshTerrainActuallySelectsAllVariantsAndUsesTheirDistinctSurfaces(GameTestHelper helper) {
        var level=helper.getLevel(); var generator=level.getChunkSource().getGenerator();
        for(var key:List.of(DarklandsWorldgen.PLAINS,DarklandsWorldgen.FOREST,DarklandsWorldgen.HIGHLANDS,DarklandsWorldgen.MOUNTAINS)) {
            var found=generator.getBiomeSource().findBiomeHorizontal(120008,64,120008,8192,8,b->b.is(key),level.random,true,level.getChunkSource().randomState().sampler());
            helper.assertTrue(found!=null,"Bounded native terrain sample cannot select variant: "+key);
            int valid=0,stone=0,soil=0,logs=0; int cx=found.getFirst().getX()>>4,cz=found.getFirst().getZ()>>4;
            for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
                var chunk=level.getChunk(cx+dx,cz+dz);
                for(int x=0;x<16;x++) for(int z=0;z<16;z++) {
                    int y=chunk.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z); var pos=new BlockPos(((cx+dx)<<4)+x,y,((cz+dz)<<4)+z);
                    if(!level.getBiome(pos).is(key)) continue;
                    valid++; var top=chunk.getBlockState(pos);
                    if(top.is(AbyssalPort.DARKSTONE.get())) stone++;
                    if(top.is(AbyssalPort.DARKLANDS_SOIL.get())) soil++;
                    for(int scan=Math.max(level.getMinBuildHeight(),y-24);scan<=y+3;scan++)
                        if(chunk.getBlockState(pos.atY(scan)).getBlock() instanceof dev.abyssalport.block.DarklandsLogBlock) logs++;
                }
            }
            helper.assertTrue(valid>=10,"Found climate point does not produce actual variant terrain: "+key+" "+found.getFirst());
            if(key.equals(DarklandsWorldgen.MOUNTAINS)) helper.assertTrue(stone>=10&&logs==0,"Mountain variant lacks exposed Darkstone or contains trees");
            else helper.assertTrue(soil>=10,"Non-mountain variant loses native Darklands surface: "+key);
            if(key.equals(DarklandsWorldgen.FOREST)) helper.assertTrue(logs>=9,"Actual forest terrain contains no grown Oak");
            LOGGER.info("DARKLANDS VARIANT WORLD AUDIT: {} sample={}, columns={}, darkstone={}, soil={}, logs={}",key.location(),found.getFirst(),valid,stone,soil,logs);
        }
        helper.succeed();
    }
}
