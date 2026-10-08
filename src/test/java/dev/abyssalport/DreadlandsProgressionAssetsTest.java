package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DreadlandsProgressionAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject data(String file) throws Exception {
        return JsonParser.parseString(Files.readString(ROOT.resolve(file))).getAsJsonObject();
    }
    @Test void recipesKeepTheActualEightFragmentAndPreviousBookGates() throws Exception {
        var skin=data("data/abyssalport/recipe/skin_of_the_dreadlands.json");
        assertEquals("FEF",skin.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("abyssalport:dread_fragment",skin.getAsJsonObject("key").getAsJsonObject("F").get("item").getAsString());
        assertEquals("abyssalport:dreadlands_essence",skin.getAsJsonObject("key").getAsJsonObject("E").get("item").getAsString());
        var book=data("data/abyssalport/recipe/dreadlands_necronomicon.json");
        assertEquals("abyssalport:book_upgrade",book.get("type").getAsString());
        assertEquals("SB",book.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("abyssalport:abyssal_wasteland_necronomicon",book.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
    }
    @Test void laterPowerstoneCannotBypassTheFirstStrongholdAndStaffRetainsData() throws Exception {
        var staff=data("data/abyssalport/rituals/dreadlands_staff.json");
        assertEquals(2000,staff.get("energy_cost").getAsInt()); assertEquals(20,staff.get("duration_ticks").getAsInt());
        assertTrue(staff.get("preserve_components").getAsBoolean());
        assertEquals("abyssalport:abyssal_wasteland_staff_of_rending",staff.get("altar_input").getAsString());
        assertEquals(8,staff.getAsJsonArray("offerings").size()); assertFalse(staff.has("offering_layout"));
        var stone=data("data/abyssalport/rituals/dreadlands_powerstone.json");
        assertEquals(5000,stone.get("energy_cost").getAsInt()); assertEquals(60,stone.get("duration_ticks").getAsInt());
        assertEquals("abyssalport:coralium_infused_stone",stone.get("altar_input").getAsString());
        for(var ritual:new JsonObject[]{staff,stone}) {
            assertEquals(2,ritual.get("book_tier").getAsInt()); assertEquals(2,ritual.get("altar_tier").getAsInt());
            assertEquals("abyssalport:dreadlands",ritual.get("dimension").getAsString());
        }
        for(var offering:stone.getAsJsonArray("offerings")) assertEquals("abyssalport:dreadlands_essence",offering.getAsString());
    }
    @Test void ownTierMediaAndBothLanguageGuidesArePackedWithoutBlankPlaceholders() throws Exception {
        for(String name:new String[]{"dreadlands_necronomicon","dreadlands_staff_of_rending"}) {
            var model=data("assets/abyssalport/models/item/"+name+".json");
            assertTrue(model.getAsJsonArray("elements").size()>=6);
            assertTrue(model.getAsJsonObject("display").has("firstperson_righthand"));
        }
        var old=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/wasteland_book_cover.png").toFile());
        var newer=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/dreadlands_book_cover.png").toFile());
        assertEquals(16,newer.getWidth()); assertNotEquals(old.getRGB(3,3),newer.getRGB(3,3));
        for(String locale:new String[]{"en_us","de_de"}) {
            var lang=data("assets/abyssalport/lang/"+locale+".json");
            for(String key:new String[]{"screen.abyssalport.page.21","screen.abyssalport.dreadlands_book","screen.abyssalport.dreadlands_book_note",
                    "screen.abyssalport.dreadlands_rituals","tooltip.abyssalport.rending.dread","research.abyssalport.dreadlands_book"})
                assertTrue(lang.has(key) && !lang.get(key).getAsString().isBlank(),key);
        }
    }
}
