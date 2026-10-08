package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class StateTransformerAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static JsonObject read(String path) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void sourceCraftsAndOwnedTabletMachineGeometryAreConnected() throws Exception {
        var tablet = read("data/abyssalport/recipe/stone_tablet.json");
        assertEquals("[\"#%#\",\"%&%\",\"#%#\"]", tablet.getAsJsonArray("pattern").toString());
        assertEquals("abyssalport:shadow_shard", tablet.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
        assertEquals("abyssalport:monolith_stone", tablet.getAsJsonObject("key").getAsJsonObject("%").get("item").getAsString());
        assertEquals("abyssalport:shadow_gem", tablet.getAsJsonObject("key").getAsJsonObject("&").get("item").getAsString());
        var machine = read("data/abyssalport/recipe/state_transformer.json");
        assertEquals("[\"###\",\"#&#\",\"#@#\"]", machine.getAsJsonArray("pattern").toString());
        assertEquals("abyssalport:transmutation_gem", machine.getAsJsonObject("key").getAsJsonObject("&").get("item").getAsString());
        assertEquals("c:chests/wooden", machine.getAsJsonObject("key").getAsJsonObject("@").get("tag").getAsString());
        var variants = read("assets/abyssalport/blockstates/state_transformer.json").getAsJsonObject("variants");
        assertEquals(8, variants.size());
        for (String facing : List.of("north", "south", "east", "west")) for (boolean present : new boolean[]{false, true})
            assertTrue(variants.has("facing=" + facing + ",tablet=" + present));
        for (String id : List.of("state_transformer", "state_transformer_tablet")) {
            var model = read("assets/abyssalport/models/block/" + id + ".json");
            assertEquals(id.endsWith("_tablet") ? 7 : 6, model.getAsJsonArray("elements").size());
            for (var element : model.getAsJsonArray("elements")) for (int axis = 0; axis < 3; axis++) {
                assertTrue(element.getAsJsonObject().getAsJsonArray("from").get(axis).getAsDouble() >= 0);
                assertTrue(element.getAsJsonObject().getAsJsonArray("to").get(axis).getAsDouble() <= (axis == 1 ? 12 : 16));
            }
        }
        var blank = read("assets/abyssalport/models/item/stone_tablet.json");
        assertEquals("abyssalport:item/stone_tablet_cursed", blank.getAsJsonArray("overrides").get(0).getAsJsonObject().get("model").getAsString());
        for (String texture : List.of("item/stone_tablet", "item/stone_tablet_cursed", "block/state_transformer_front")) {
            var image = ImageIO.read(ROOT.resolve("assets/abyssalport/textures/" + texture + ".png").toFile());
            assertEquals(16, image.getWidth()); assertEquals(16, image.getHeight());
        }
    }
    @Test void bothLanguagesGuideAndNativeScreenRegistrationExistWithoutOriginalMedia() throws Exception {
        for (String locale : List.of("en_us", "de_de")) {
            var lang = read("assets/abyssalport/lang/" + locale + ".json");
            assertTrue(lang.get("screen.abyssalport.state_transformer.guide").getAsString().length() > 600);
            assertTrue(lang.get("screen.abyssalport.state_transformer.pack").getAsString().length() < 7);
            assertTrue(lang.has("item.abyssalport.stone_tablet")); assertTrue(lang.has("block.abyssalport.state_transformer"));
        }
        assertTrue(Files.readString(Path.of("src/main/java/dev/abyssalport/client/AbyssalPortClient.java")).contains("StateTransformerScreen::new"));
        assertTrue(Files.readString(Path.of("src/main/java/dev/abyssalport/client/NecronomiconScreen.java")).contains("screen.abyssalport.state_transformer.guide"));
        assertFalse(Files.exists(ROOT.resolve("assets/abyssalcraft/textures/gui/container/statetransformer.png")));
    }
}
