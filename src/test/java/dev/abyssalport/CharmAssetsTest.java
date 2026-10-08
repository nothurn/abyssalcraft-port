package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import dev.abyssalport.energy.CharmAmplifier;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CharmAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final List<String> NAMES = List.of("ritual_charm", "range_ritual_charm", "power_ritual_charm", "duration_ritual_charm");
    private static JsonObject read(String path) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }

    @Test void sourceRecipeAndFourIndependentIconsHaveNoPrematureDeityRecipes() throws Exception {
        var recipe = read("data/abyssalport/recipe/ritual_charm.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("[\"GGG\",\"GDG\",\"GGG\"]", recipe.getAsJsonArray("pattern").toString());
        assertEquals("minecraft:gold_ingot", recipe.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        assertEquals("minecraft:diamond", recipe.getAsJsonObject("key").getAsJsonObject("D").get("item").getAsString());
        assertEquals("abyssalport:ritual_charm", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
        var fingerprints = new HashSet<Integer>();
        for (var name : NAMES) {
            var model = read("assets/abyssalport/models/item/" + name + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString());
            assertEquals("abyssalport:item/" + name, model.getAsJsonObject("textures").get("layer0").getAsString());
            var pixels = ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/" + name + ".png").toFile());
            assertEquals(16, pixels.getWidth()); assertEquals(16, pixels.getHeight());
            int[] rgba = pixels.getRGB(0, 0, 16, 16, null, 0, 16);
            assertTrue(fingerprints.add(Arrays.hashCode(rgba)), "Duplicate charm icon");
            assertTrue(Arrays.stream(rgba).anyMatch(pixel -> pixel >>> 24 == 0));
            assertTrue(Arrays.stream(rgba).filter(pixel -> pixel >>> 24 == 255).count() > 80);
            for (var locale : List.of("en_us", "de_de")) {
                var lang = read("assets/abyssalport/lang/" + locale + ".json");
                assertTrue(lang.has("item.abyssalport." + name));
                assertTrue(lang.get("screen.abyssalport.energy").getAsString().contains(locale.equals("en_us") ? "anvil" : "Amboss"));
            }
        }
        try (var paths = Files.list(ROOT.resolve("data/abyssalport/recipe"))) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().matches(".*(cthulhu|hastur|jzahar|azathoth|nyarlathotep|yog_sothoth|shub_niggurath).*charm.*")));
        }
    }

    @Test void amplifierFactorsMatchGenericAndSameDeitySourceValuesAndMalformedIdsAreInactive() {
        assertEquals(4, CharmAmplifier.RANGE.factor(false)); assertEquals(6, CharmAmplifier.RANGE.factor(true));
        assertEquals(1.5F, CharmAmplifier.POWER.factor(false)); assertEquals(2.5F, CharmAmplifier.POWER.factor(true));
        assertEquals(2, CharmAmplifier.DURATION.factor(false)); assertEquals(4, CharmAmplifier.DURATION.factor(true));
        assertEquals(0, CharmAmplifier.NONE.factor(true));
        for (var amplifier : CharmAmplifier.values()) assertSame(amplifier, CharmAmplifier.fromId(amplifier.id()));
        assertSame(CharmAmplifier.NONE, CharmAmplifier.fromId("forged-invalid"));
        assertSame(CharmAmplifier.NONE, CharmAmplifier.fromId(null));
    }
}
