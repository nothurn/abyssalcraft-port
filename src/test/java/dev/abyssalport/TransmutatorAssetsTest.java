package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class TransmutatorAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject data(String file) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(file))).getAsJsonObject(); }
    @Test void nativeCraftingKeepsFiveBricksTwoRefinedBlocksAndFluidCatalyst() throws Exception {
        var recipe=data("data/abyssalport/recipe/transmutator.json");
        assertEquals("BBB",recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("BGB",recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("RCR",recipe.getAsJsonArray("pattern").get(2).getAsString());
        for(var pair:new String[][]{{"B","coralium_brick"},{"G","transmutation_gem"},{"R","refined_coralium_block"},{"C","coralium_bucket"}})
            assertEquals("abyssalport:"+pair[1],recipe.getAsJsonObject("key").getAsJsonObject(pair[0]).get("item").getAsString());
        var brick=data("data/abyssalport/recipe/coralium_brick.json");
        assertEquals("minecraft:smelting",brick.get("type").getAsString()); assertEquals(200,brick.get("cookingtime").getAsInt());
        assertEquals("abyssalport:coralium_stone",brick.getAsJsonObject("ingredient").get("item").getAsString());
    }
    @Test void allInitialProcessingRecipesAreConcreteAndThePlateauIsANativeBiome() throws Exception {
        var paths=ROOT.resolve("data/abyssalport/recipe/transmutation");
        try(var files=Files.list(paths)) {
            var all=files.toList(); assertEquals(22,all.size());
            for(var path:all) {
                var recipe=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                assertEquals("abyssalport:transmutation",recipe.get("type").getAsString());
                boolean shard=path.getFileName().toString().equals("dreaded_shard_to_dreadium.json");
                String name=path.getFileName().toString();
                float xp=shard?.2F:0F;
                if(name.endsWith("_recovery.json")) xp=name.equals("dense_carbon_cluster_recovery.json")?.5F:
                        java.util.Set.of("abyssalnite_crystal_recovery.json","coralium_crystal_recovery.json","dreadium_crystal_recovery.json").contains(name)?.1F:.2F;
                assertEquals(xp,recipe.get("experience").getAsFloat());
                assertEquals(1,recipe.getAsJsonObject("result").get("count").getAsInt());
                if(shard) assertEquals("abyssalport:dreadium_ingot",recipe.getAsJsonObject("result").get("id").getAsString());
            }
        }
        var dimension=data("data/abyssalport/dimension/abyssal_wasteland.json");
        assertTrue(dimension.getAsJsonObject("generator").getAsJsonObject("biome_source").getAsJsonArray("biomes").asList().stream()
                .anyMatch(b->b.getAsJsonObject().get("biome").getAsString().equals("abyssalport:abyssal_plateau")));
        var settings=data("data/abyssalport/worldgen/noise_settings/abyssal_wasteland.json").getAsJsonObject("surface_rule").toString();
        assertTrue(settings.contains("abyssalport:abyssal_plateau") && settings.contains("abyssalport:coralium_stone"));
        var plateau=data("data/abyssalport/worldgen/biome/abyssal_plateau.json");
        assertEquals(data("data/abyssalport/worldgen/biome/abyssal_wasteland.json").get("features"),plateau.get("features"));
    }
    @Test void ownLitModelsCoverAllEightStatesAndTheGuideExplainsRealFuel() throws Exception {
        var states=data("assets/abyssalport/blockstates/transmutator.json").getAsJsonObject("variants"); assertEquals(8,states.size());
        for(String facing:new String[]{"north","east","south","west"}) for(boolean lit:new boolean[]{false,true})
            assertEquals("abyssalport:block/transmutator"+(lit?"_on":""),states.getAsJsonObject("facing="+facing+",lit="+lit).get("model").getAsString());
        for(String name:new String[]{"transmutator_front","transmutator_front_on","transmutator_side","transmutator_top","coralium_stone","coralium_cobblestone"}) {
            var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/block/"+name+".png").toFile()); assertEquals(16,image.getWidth()); assertEquals(16,image.getHeight());
        }
        for(String locale:new String[]{"en_us","de_de"}) {
            var lang=data("assets/abyssalport/lang/"+locale+".json");
            for(String key:new String[]{"screen.abyssalport.page.22","screen.abyssalport.transmutator","screen.abyssalport.transmutator_fuel",
                    "screen.abyssalport.transmutator_automation","research.abyssalport.processing"}) assertFalse(lang.get(key).getAsString().isBlank(),key);
        }
    }
}
