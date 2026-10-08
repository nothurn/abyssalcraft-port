package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class WastelandMineralsAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static JsonObject json(String path) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void everyOreAndRefinedPieceHasOwnAssetsAndLocalization() throws Exception {
        for (var name : new String[]{"nitre_ore", "abyssal_nitre_ore", "abyssal_coralium_ore", "abyssal_iron_ore", "abyssal_gold_ore",
                "abyssal_diamond_ore", "liquified_coralium_ore", "pearlescent_coralium_ore", "refined_coralium_block", "nitre",
                "refined_coralium_ingot", "refined_coralium_nugget", "refined_coralium_pickaxe", "refined_coralium_axe", "refined_coralium_shovel",
                "refined_coralium_sword", "refined_coralium_hoe", "refined_coralium_helmet", "refined_coralium_chestplate", "refined_coralium_leggings", "refined_coralium_boots"}) {
            boolean block = name.endsWith("_ore") || name.endsWith("_block");
            json("assets/abyssalport/models/item/"+name+".json");
            if (block) { json("assets/abyssalport/blockstates/"+name+".json"); json("data/abyssalport/loot_table/blocks/"+name+".json"); }
            var image = ImageIO.read(ROOT.resolve("assets/abyssalport/textures/"+(block ? "block/" : "item/")+name+".png").toFile());
            assertEquals(16, image.getWidth()); assertEquals(16, image.getHeight());
            for (var locale : new String[]{"en_us", "de_de"}) assertTrue(json("assets/abyssalport/lang/"+locale+".json").has((block ? "block." : "item.")+"abyssalport."+name));
        }
        for (int layer = 1; layer <= 2; layer++) {
            var image = ImageIO.read(ROOT.resolve("assets/abyssalport/textures/models/armor/refined_coralium_layer_"+layer+".png").toFile());
            assertEquals(64, image.getWidth()); assertEquals(32, image.getHeight());
            assertEquals(0, image.getRGB(11, 12) >>> 24); assertEquals(255, image.getRGB(23, 23) >>> 24);
        }
    }
    @Test void placementAndRecipesRetainSourceMechanics() throws Exception {
        var vein = json("data/abyssalport/worldgen/configured_feature/ore_liquified_coralium_ore.json").getAsJsonObject("config");
        assertEquals(1, vein.get("min_size").getAsInt()); assertEquals(4, vein.get("max_size").getAsInt());
        assertEquals("abyssalport:abyssal_ore_replaceables", vein.getAsJsonObject("target").get("tag").getAsString());
        var placement = json("data/abyssalport/worldgen/placed_feature/ore_liquified_coralium_ore.json").getAsJsonArray("placement");
        assertEquals(8, placement.get(0).getAsJsonObject().get("count").getAsInt());
        assertEquals(34, placement.get(2).getAsJsonObject().getAsJsonObject("height").getAsJsonObject("max_inclusive").get("absolute").getAsInt());
        var furnace = json("data/abyssalport/recipe/liquified_coralium_ore_smelting.json");
        assertEquals("abyssalport:refined_coralium_ingot", furnace.getAsJsonObject("result").get("id").getAsString());
        assertEquals(3, furnace.get("experience").getAsInt());
        for (var locale : new String[]{"en_us", "de_de"}) {
            var lang = json("assets/abyssalport/lang/"+locale+".json");
            for (var key : new String[]{"page.14", "locked.14", "wasteland_mining", "refined_coralium_note"}) assertTrue(lang.has("screen.abyssalport."+key));
        }
    }
}
