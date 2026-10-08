package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CrystallizerAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject json(String path) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void machineAndEveryFractionKeepConcreteReversibleNativeRecipes() throws Exception {
        var machine=json("data/abyssalport/recipe/crystallizer.json");
        assertEquals(List.of("BBB","RFR","BBB"),machine.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList());
        assertEquals("abyssalport:dreadium_block",machine.getAsJsonObject("key").getAsJsonObject("R").get("item").getAsString());
        assertEquals("abyssalport:dreadstone_bricks",machine.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        var tags=json("data/abyssalport/tags/item/crystals.json").getAsJsonArray("values"); assertEquals(88,tags.size());
        Set<String> values=new HashSet<>(); tags.forEach(value->assertTrue(values.add(value.getAsString())));
        try(var files=Files.list(ROOT.resolve("data/abyssalport/recipe/crystals"))) {
            var all=files.toList(); assertEquals(132,all.size());
            for(var file:all) {
                var recipe=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                boolean unpack=recipe.get("type").getAsString().equals("minecraft:crafting_shapeless");
                if(unpack) assertEquals(9,recipe.getAsJsonObject("result").get("count").getAsInt());
                else assertEquals(List.of("###","###","###"),recipe.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList());
                assertTrue(values.contains(recipe.getAsJsonObject("result").get("id").getAsString()));
            }
        }
    }
    @Test void allReachableRecipesHaveValidOutputsAndOnlyWaterPotionGetsAContainerRule() throws Exception {
        Set<String> materials=new HashSet<>();
        try(var files=Files.list(ROOT.resolve("data/abyssalport/recipe/crystallization"))) {
            var all=files.toList(); assertEquals(63,all.size());
            for(var file:all) {
                var recipe=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                assertEquals("abyssalport:crystallization",recipe.get("type").getAsString());
                for(String key:List.of("result","secondary")) if(recipe.has(key)) {
                    var output=recipe.getAsJsonObject(key); String id=output.get("id").getAsString();
                    if(id.contains("_crystal")) materials.add(id.substring(id.indexOf(':')+1,id.indexOf("_crystal")));
                    assertTrue(!output.has("count") || output.get("count").getAsInt()>=1 && output.get("count").getAsInt()<=64);
                }
                boolean bottle=file.getFileName().toString().equals("water_bottle.json");
                assertEquals(bottle,recipe.has("water_only") && recipe.get("water_only").getAsBoolean());
                assertTrue(recipe.get("experience").getAsFloat()>=0);
            }
        }
        assertEquals(22,materials.size(),"A registered crystal family has no real processing source");
        var water=json("data/abyssalport/recipe/crystallization/water_bucket.json");
        assertEquals(12,water.getAsJsonObject("result").get("count").getAsInt()); assertEquals(6,water.getAsJsonObject("secondary").get("count").getAsInt());
    }
    @Test void ownTexturesModelsAndBothGuidesAreComplete() throws Exception {
        assertEquals(8,json("assets/abyssalport/blockstates/crystallizer.json").getAsJsonObject("variants").size());
        var ids=json("data/abyssalport/tags/item/crystals.json").getAsJsonArray("values");
        for(var value:ids) {
            String name=value.getAsString().substring("abyssalport:".length());
            boolean block=name.endsWith("_cluster");
            var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/"+(block?"block/":"item/")+name+".png").toFile());
            assertEquals(16,image.getWidth()); assertEquals(16,image.getHeight());
            if(!block) assertEquals(0,image.getRGB(0,0)>>>24);
            json("assets/abyssalport/models/item/"+name+".json");
        }
        for(String locale:List.of("en_us","de_de")) {
            var lang=json("assets/abyssalport/lang/"+locale+".json");
            for(var value:ids) {
                String name=value.getAsString().substring("abyssalport:".length());
                assertFalse(lang.get((name.endsWith("_cluster")?"block.":"item.")+"abyssalport."+name).getAsString().isBlank());
            }
            for(String key:List.of("screen.abyssalport.page.24","screen.abyssalport.crystallizer","screen.abyssalport.crystallizer_fractions",
                    "screen.abyssalport.crystallizer_fuel","research.abyssalport.crystallization")) assertFalse(lang.get(key).getAsString().isBlank());
        }
    }
    @Test void compoundRecipesKeepSixtyFourCrystalsObsidianAndSevenNitreCosts() throws Exception {
        var carbon=json("data/abyssalport/recipe/carbon_cluster.json");
        assertEquals(List.of("###","# #","###"),carbon.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList());
        assertEquals("abyssalport:carbon_crystal",carbon.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
        var dense=json("data/abyssalport/recipe/dense_carbon_cluster.json");
        assertEquals("minecraft:obsidian",dense.getAsJsonObject("key").getAsJsonObject("O").get("item").getAsString());
        assertEquals("abyssalport:carbon_cluster",dense.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
        var powder=json("data/abyssalport/recipe/gunpowder_from_nitre.json");
        assertEquals(List.of("NSN","NCN","NNN"),powder.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList());
        assertEquals("minecraft:charcoal",powder.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString());
        assertEquals(4,powder.getAsJsonObject("result").get("count").getAsInt());
    }
    @Test void sourceRecoveryIsExplicitlyLossyAndPresentForEveryNativeSupportedMetal() throws Exception {
        for(String material:List.of("abyssalnite","coralium","dreadium","iron","gold")) {
            var cluster=json("data/abyssalport/recipe/transmutation/"+material+"_crystal_cluster_recovery.json");
            var single=json("data/abyssalport/recipe/transmutation/"+material+"_crystal_recovery.json");
            assertEquals(1,cluster.getAsJsonObject("result").get("count").getAsInt()); assertEquals(.2F,cluster.get("experience").getAsFloat());
            assertTrue(single.getAsJsonObject("result").get("id").getAsString().endsWith("nugget"));
        }
        for(String name:List.of("carbon_cluster","dense_carbon_cluster","sulfur")) {
            var icon=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/"+name+".png").toFile());
            assertEquals(16,icon.getWidth()); assertEquals(16,icon.getHeight());
            json("assets/abyssalport/models/item/"+name+".json");
        }
    }
}
