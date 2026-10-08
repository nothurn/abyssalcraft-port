package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class WastelandProgressionAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");
    @Test void upgradeAndRendingIngredientsMatchTheirActualOriginalInspiredChain() throws Exception {
        var recipes = ROOT.resolve("data/abyssalport/recipe");
        var upgrade = JsonParser.parseString(Files.readString(recipes.resolve("abyssal_wasteland_necronomicon.json"))).getAsJsonObject();
        assertEquals("abyssalport:book_upgrade", upgrade.get("type").getAsString());
        assertEquals("SS", upgrade.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("SB", upgrade.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("abyssalport:skin_of_the_abyssal_wasteland", upgrade.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        var skin = JsonParser.parseString(Files.readString(recipes.resolve("skin_of_the_abyssal_wasteland.json"))).getAsJsonObject();
        assertEquals("abyssalport:abyssal_wasteland_essence", skin.getAsJsonObject("key").getAsJsonObject("E").get("item").getAsString());
        assertEquals("abyssalport:coralium_plagued_flesh", skin.getAsJsonObject("key").getAsJsonObject("F").get("item").getAsString());
    }
    @Test void staffAndSecondBookHaveIndependentThreeDimensionalMediaAndRealGuideEntries() throws Exception {
        var assets = ROOT.resolve("assets/abyssalport");
        for (String name : new String[]{"staff_of_rending", "abyssal_wasteland_staff_of_rending", "abyssal_wasteland_necronomicon"}) {
            var model = JsonParser.parseString(Files.readString(assets.resolve("models/item/"+name+".json"))).getAsJsonObject();
            assertTrue(model.getAsJsonArray("elements").size() >= 6);
            assertTrue(model.getAsJsonObject("display").has("firstperson_righthand"));
        }
        var cover = ImageIO.read(assets.resolve("textures/item/wasteland_book_cover.png").toFile());
        assertEquals(16, cover.getWidth()); assertEquals(16, cover.getHeight());
        var base = ImageIO.read(assets.resolve("textures/item/necronomicon_cover.png").toFile());
        assertNotEquals(base.getRGB(3, 3), cover.getRGB(3, 3));
        for (String locale : new String[]{"en_us", "de_de"}) {
            var text = JsonParser.parseString(Files.readString(assets.resolve("lang/"+locale+".json"))).getAsJsonObject();
            for (String key : new String[]{"screen.abyssalport.page.15", "screen.abyssalport.page.16", "screen.abyssalport.rending",
                    "screen.abyssalport.wasteland_book", "screen.abyssalport.wasteland_rituals", "tooltip.abyssalport.staff_tier",
                    "message.abyssalport.formation.book_too_weak", "message.abyssalport.formation.wrong_dimension",
                    "research.abyssalport.rending", "research.abyssalport.wasteland_book"}) assertTrue(text.has(key), key);
        }
    }
    @Test void firstDimensionRitualIsTieredUnorderedAndPreservesChargedStaffData() throws Exception {
        var ritual = JsonParser.parseString(Files.readString(ROOT.resolve("data/abyssalport/rituals/abyssal_wasteland_staff.json"))).getAsJsonObject();
        assertEquals(1, ritual.get("book_tier").getAsInt()); assertEquals(1, ritual.get("altar_tier").getAsInt());
        assertEquals("abyssalport:abyssal_wasteland", ritual.get("dimension").getAsString());
        assertEquals(1000, ritual.get("energy_cost").getAsInt()); assertEquals(20, ritual.get("duration_ticks").getAsInt());
        assertTrue(ritual.get("preserve_components").getAsBoolean()); assertFalse(ritual.has("offering_layout"));
        assertEquals(8, ritual.getAsJsonArray("offerings").size());
        for (String name : new String[]{"abyssal_ritual_altar", "abyssal_ritual_pedestal"}) {
            var loot = JsonParser.parseString(Files.readString(ROOT.resolve("data/abyssalport/loot_table/blocks/"+name+".json"))).getAsJsonObject();
            assertEquals("abyssalport:abyssal_cobblestone", loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        }
    }
}
