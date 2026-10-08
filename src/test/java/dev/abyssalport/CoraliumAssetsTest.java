package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CoraliumAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static JsonObject json(String path) throws Exception {
        return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();
    }

    @Test
    void mineralAndInfusionModelsAreCompleteInBothLanguages() throws Exception {
        String[] names = {"coralium_gem", "coralium_pearl", "transmutation_gem", "oblivion_shard", "oblivion_catalyst",
                "coralium_gem_cluster_2", "coralium_gem_cluster_3", "coralium_gem_cluster_4", "coralium_gem_cluster_5",
                "coralium_gem_cluster_6", "coralium_gem_cluster_7", "coralium_gem_cluster_8", "coralium_gem_cluster_9"};
        for (String name : names) {
            assertEquals("abyssalport:item/"+name, json("assets/abyssalport/models/item/"+name+".json").getAsJsonObject("textures").get("layer0").getAsString());
            var image = ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/"+name+".png").toFile());
            assertEquals(16, image.getWidth());
            assertEquals(16, image.getHeight());
            for (String locale : new String[]{"en_us", "de_de"}) {
                assertTrue(json("assets/abyssalport/lang/"+locale+".json").has("item.abyssalport."+name));
            }
        }
        var ritual = json("data/abyssalport/rituals/transmutation_gem.json");
        assertEquals(300, ritual.get("energy_cost").getAsInt());
        assertEquals(20, ritual.get("duration_ticks").getAsInt());
        assertEquals("abyssalport:coralium_pearl", ritual.get("altar_input").getAsString());
        assertEquals(8, ritual.getAsJsonArray("offerings").size());
        for (String name : new String[]{"coralium_processing", "oblivion_shard"}) {
            var rewards = json("data/abyssalport/advancement/recipes/"+name+".json").getAsJsonObject("rewards").getAsJsonArray("recipes");
            for (var recipe : rewards) {
                String file = recipe.getAsString().replace("abyssalport:", "");
                assertTrue(Files.isRegularFile(ROOT.resolve("data/abyssalport/recipe/"+file+".json")), "Recipe-book reward is missing: " + file);
            }
        }
        for (String locale : new String[]{"en_us", "de_de"}) {
            var text = json("assets/abyssalport/lang/"+locale+".json");
            for (String key : new String[]{"page.9", "page.10", "page.11", "locked.9", "locked.10", "locked.11", "coralium", "coralium_note", "infusion", "infusion_note", "sacrifice", "sacrifice_note"}) {
                assertTrue(text.has("screen.abyssalport."+key));
            }
        }
    }

    @Test
    void oblivionUsesTheOriginalLivingSacrificeInfusionNotAnEasyCraft() throws Exception {
        var ritual = json("data/abyssalport/rituals/oblivion_catalyst.json");
        assertEquals(5000, ritual.get("energy_cost").getAsInt());
        assertEquals(200, ritual.get("duration_ticks").getAsInt());
        assertTrue(ritual.get("living_sacrifice").getAsBoolean());
        assertEquals("minecraft:ender_eye", ritual.get("altar_input").getAsString());
        long redstone = 0, shards = 0;
        for (var item : ritual.getAsJsonArray("offerings")) {
            if (item.getAsString().equals("minecraft:redstone")) redstone++;
            else if (item.getAsString().equals("abyssalport:oblivion_shard")) shards++;
            else fail("Unexpected sacrifice ingredient");
        }
        assertEquals(4, redstone);
        assertEquals(4, shards);
        assertFalse(Files.exists(ROOT.resolve("data/abyssalport/recipe/oblivion_catalyst.json")));
    }

    @Test
    void oceanAndSwampPlacementKeepsSourceCountAndSeparateHeightBands() throws Exception {
        for (String group : new String[]{"swamps", "oceans", "deep_oceans"}) {
            String base = "data/abyssalport/worldgen/placed_feature/ore_coralium_"+group+".json";
            var placement = json(base).getAsJsonArray("placement");
            assertEquals(group.equals("swamps") ? 6 : 12, placement.get(0).getAsJsonObject().get("count").getAsInt());
            var height = placement.get(2).getAsJsonObject().getAsJsonObject("height");
            assertEquals(group.equals("deep_oceans") ? -20 : 0, height.getAsJsonObject("min_inclusive").get("absolute").getAsInt());
            assertEquals(group.equals("deep_oceans") ? 19 : 39, height.getAsJsonObject("max_inclusive").get("absolute").getAsInt());
            var modifier = json("data/abyssalport/neoforge/biome_modifier/ore_coralium_"+group+".json");
            assertEquals("#abyssalport:coralium_"+group, modifier.get("biomes").getAsString());
        }
        assertEquals(8, json("data/abyssalport/worldgen/configured_feature/ore_coralium.json").getAsJsonObject("config").get("size").getAsInt());
    }
}
