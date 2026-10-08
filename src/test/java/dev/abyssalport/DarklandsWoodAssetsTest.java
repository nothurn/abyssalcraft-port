package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DarklandsWoodAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject json(String path) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void treesAreIntegratedIntoTheActualBiomeAndEveryWoodPartHasOwnAssets() throws Exception {
        var biome=json("data/abyssalport/worldgen/biome/darklands.json");
        var vegetation=biome.getAsJsonArray("features").get(9).getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
        assertTrue(vegetation.contains("abyssalport:darklands_oak")); assertFalse(vegetation.contains("minecraft:dark_forest_vegetation"));
        assertEquals("abyssalport:darklands_oak",json("data/abyssalport/worldgen/configured_feature/darklands_oak.json").get("type").getAsString());
        assertEquals(10,json("data/abyssalport/worldgen/placed_feature/darklands_oak.json").getAsJsonArray("placement").get(0).getAsJsonObject().get("count").getAsInt());
        for(var part:List.of("log","hollow_log","planks","leaves","sapling","stairs","slab","fence","fence_gate","door","button","pressure_plate")) {
            String name="darklands_oak_"+part;
            assertFalse(json("assets/abyssalport/blockstates/"+name+".json").isEmpty());
            assertFalse(json("assets/abyssalport/models/item/"+name+".json").isEmpty());
            assertEquals("minecraft:block",json("data/abyssalport/loot_table/blocks/"+name+".json").get("type").getAsString());
        }
        for(String name:List.of("log","hollow_log","planks","leaves","sapling","door_top","door_bottom")) {
            var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/block/darklands_oak_"+name+".png").toFile());
            assertEquals(16,image.getWidth()); assertEquals(16,image.getHeight());
        }
        assertEquals(0,ImageIO.read(ROOT.resolve("assets/abyssalport/textures/block/darklands_oak_sapling.png").toFile()).getRGB(0,0)>>>24);
        for(var locale:List.of("en_us","de_de")) assertFalse(json("assets/abyssalport/lang/"+locale+".json").get("screen.abyssalport.darklands.wood").getAsString().isBlank());
    }
    @Test void renewableLeafLootUsesNativeShearsSilkAndFortuneWithoutInventedApples() throws Exception {
        var leaves=json("data/abyssalport/loot_table/blocks/darklands_oak_leaves.json").toString();
        for(String match:List.of("minecraft:shears","minecraft:silk_touch","minecraft:fortune","abyssalport:darklands_oak_sapling")) assertTrue(leaves.contains(match));
        assertFalse(leaves.contains("apple"));
        var charcoal=json("data/abyssalport/recipe/darklands_oak_charcoal.json");
        assertEquals(1.0,charcoal.get("experience").getAsDouble()); assertEquals(200,charcoal.get("cookingtime").getAsInt());
        assertEquals("minecraft:charcoal",charcoal.getAsJsonObject("result").get("id").getAsString());
        for(var pair:Map.of("planks",4,"stairs",4,"slab",6,"fence",3,"fence_gate",1,"door",3,"button",1,"pressure_plate",1).entrySet())
            assertEquals(pair.getValue(),json("data/abyssalport/recipe/darklands_oak_"+pair.getKey()+".json").getAsJsonObject("result").get("count").getAsInt());
    }
}
