package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AbyssalniteAssetsTest {
    private JsonObject json(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void completeMaterialAndEquipmentHaveOwnPackagedAssets() throws Exception {
        for (String name : new String[]{"ore", "block", "ingot", "nugget", "pickaxe", "axe", "shovel",
                "sword", "hoe", "helmet", "chestplate", "leggings", "boots"}) {
            String id = "abyssalnite_" + name;
            JsonObject model = json("/assets/abyssalport/models/item/" + id + ".json");
            boolean block = name.equals("ore") || name.equals("block");
            if (block) {
                json("/assets/abyssalport/blockstates/" + id + ".json");
                json("/assets/abyssalport/models/block/" + id + ".json");
                json("/data/abyssalport/loot_table/blocks/" + id + ".json");
            } else {
                assertEquals("abyssalport:item/" + id, model.getAsJsonObject("textures").get("layer0").getAsString());
            }
            String path = "/assets/abyssalport/textures/" + (block ? "block/" : "item/") + id + ".png";
            try (var stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, path);
                var image = ImageIO.read(stream);
                assertNotNull(image);
                assertEquals(16, image.getWidth());
                assertEquals(16, image.getHeight());
            }
            for (String language : new String[]{"en_us", "de_de"}) {
                assertTrue(json("/assets/abyssalport/lang/" + language + ".json")
                        .has((block ? "block." : "item.") + "abyssalport." + id));
            }
        }
        for (int layer = 1; layer <= 2; layer++) {
            try (var stream = getClass().getResourceAsStream(
                    "/assets/abyssalport/textures/models/armor/abyssalnite_layer_" + layer + ".png")) {
                assertNotNull(stream);
                var image = ImageIO.read(stream);
                assertEquals(64, image.getWidth());
                assertEquals(32, image.getHeight());
                assertEquals(255, image.getRGB(23, 23) >>> 24, "Chest face is untextured");
                assertEquals(255, image.getRGB(5, 24) >>> 24, "Leg face is untextured");
                assertEquals(0, image.getRGB(11, 12) >>> 24, "Helmet must leave the face visible");
            }
        }
    }

    @Test
    void orePlacementKeepsSourceHeightCountAndBiomeFilter() throws Exception {
        var placed = json("/data/abyssalport/worldgen/placed_feature/ore_abyssalnite.json");
        var placements = placed.getAsJsonArray("placement");
        var counts = placements.get(0).getAsJsonObject().getAsJsonObject("count");
        assertEquals(3, counts.get("min_inclusive").getAsInt());
        assertEquals(8, counts.get("max_inclusive").getAsInt());
        var height = placements.get(2).getAsJsonObject().getAsJsonObject("height");
        assertEquals(4, height.getAsJsonObject("min_inclusive").get("absolute").getAsInt());
        assertEquals(31, height.getAsJsonObject("max_inclusive").get("absolute").getAsInt());
        assertEquals("minecraft:biome", placements.get(3).getAsJsonObject().get("type").getAsString());
    }
}
