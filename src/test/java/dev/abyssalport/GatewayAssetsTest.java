package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class GatewayAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static JsonObject json(String relative) throws Exception {
        return JsonParser.parseString(Files.readString(ROOT.resolve(relative))).getAsJsonObject();
    }
    @Test void portalRitualReturnsKeyAndUsesCardinalGems() throws Exception {
        var ritual = json("data/abyssalport/rituals/gateway.json");
        assertEquals(1000, ritual.get("energy_cost").getAsInt());
        assertEquals(40, ritual.get("duration_ticks").getAsInt());
        assertEquals("gateway", ritual.get("outcome").getAsString());
        assertEquals("abyssalport:gateway_key", ritual.get("altar_input").getAsString());
        var layout = ritual.getAsJsonArray("offering_layout");
        assertEquals(8, layout.size());
        for (int i = 0; i < 8; i++) {
            if (i < 4) assertEquals("abyssalport:shadow_gem", layout.get(i).getAsString());
            else assertTrue(layout.get(i).isJsonNull());
        }
        assertEquals("abyssalport:gateway_key", ritual.getAsJsonObject("result").get("item").getAsString());
        var recipe = json("data/abyssalport/recipe/gateway_key.json");
        assertEquals("abyssalport:oblivion_catalyst", recipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString());
    }
    @Test void dimensionHasNativeNoiseAndCoraliumInsteadOfVanillaWater() throws Exception {
        var dimension = json("data/abyssalport/dimension/abyssal_wasteland.json");
        assertEquals("minecraft:noise", dimension.getAsJsonObject("generator").get("type").getAsString());
        var noise = json("data/abyssalport/worldgen/noise_settings/abyssal_wasteland.json");
        assertEquals(55, noise.get("sea_level").getAsInt());
        assertEquals("abyssalport:liquid_coralium", noise.getAsJsonObject("default_fluid").get("Name").getAsString());
        assertEquals("abyssalport:abyssal_stone", noise.getAsJsonObject("default_block").get("Name").getAsString());
        var type = json("data/abyssalport/dimension_type/abyssal_wasteland.json");
        assertEquals(1.0, type.get("coordinate_scale").getAsDouble());
        assertEquals(256, type.get("height").getAsInt());
        assertFalse(type.get("bed_works").getAsBoolean());
        assertFalse(type.get("respawn_anchor_works").getAsBoolean());
    }
    @Test void ownAnimatedFluidsAndItemModelsArePackaged() throws Exception {
        for (String name : new String[]{"gateway_portal", "coralium_still", "coralium_flow"}) {
            var image = ImageIO.read(ROOT.resolve("assets/abyssalport/textures/block/"+name+".png").toFile());
            assertEquals(image.getWidth()*8, image.getHeight());
            assertEquals(4, json("assets/abyssalport/textures/block/"+name+".png.mcmeta").getAsJsonObject("animation").get("frametime").getAsInt());
        }
        assertEquals("minecraft:item/handheld", json("assets/abyssalport/models/item/gateway_key.json").get("parent").getAsString());
        for (String locale : new String[]{"en_us", "de_de"}) {
            var lang = json("assets/abyssalport/lang/"+locale+".json");
            for (String key : new String[]{"item.abyssalport.gateway_key", "item.abyssalport.coralium_bucket", "effect.abyssalport.coralium_plague",
                    "tooltip.abyssalport.gateway_key", "dimension.abyssalport.abyssal_wasteland",
                    "screen.abyssalport.page.12", "screen.abyssalport.page.13", "screen.abyssalport.locked.12", "screen.abyssalport.locked.13",
                    "screen.abyssalport.gateway", "screen.abyssalport.gateway_note", "screen.abyssalport.wasteland", "screen.abyssalport.wasteland_note"}) assertTrue(lang.has(key));
        }
    }
}
