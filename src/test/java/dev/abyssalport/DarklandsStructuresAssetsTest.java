package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DarklandsStructuresAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void nativeSingleSavedSurfaceStartHasAllTenRealPlansAndNoCreativeOnlyStub()throws Exception {
        var structure=read("data/abyssalport/worldgen/structure/darklands_ruins.json");
        assertEquals("minecraft:jigsaw",structure.get("type").getAsString());
        assertEquals(1,structure.get("size").getAsInt(),"Native JigsawPlacement omits even the starting piece at depth zero");
        assertEquals("WORLD_SURFACE_WG",structure.get("project_start_to_heightmap").getAsString());
        assertEquals("#abyssalport:darklands",structure.get("biomes").getAsString());
        var pool=read("data/abyssalport/worldgen/template_pool/darklands_ruins/start.json").getAsJsonArray("elements");
        assertEquals(10,pool.size());
        for(var row:pool) {
            String name=row.getAsJsonObject().getAsJsonObject("element").get("location").getAsString();
            assertTrue(name.startsWith("abyssalport:darklands_ruins/"));
            assertTrue(Files.size(ROOT.resolve("data/abyssalport/structure/"+name.substring("abyssalport:".length())+".nbt"))>200);
        }
        var placement=read("data/abyssalport/worldgen/structure_set/darklands_ruins.json").getAsJsonObject("placement");
        assertEquals("minecraft:random_spread",placement.get("type").getAsString());
        assertTrue(placement.get("spacing").getAsInt()>placement.get("separation").getAsInt());
    }
    @Test void sourceRecipeIndependentPixelsAndGroundReturnsAreExact()throws Exception {
        var recipe=read("data/abyssalport/recipe/glowing_darkstone_bricks.json");
        assertEquals(4,recipe.getAsJsonObject("result").get("count").getAsInt());
        assertEquals("minecraft:diamond",recipe.getAsJsonObject("key").getAsJsonObject("D").get("item").getAsString());
        assertEquals("minecraft:glowstone",recipe.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        assertEquals("minecraft:obsidian",recipe.getAsJsonObject("key").getAsJsonObject("O").get("item").getAsString());
        for(String image:new String[]{"glowing_darkstone_bricks","darkstone_ritual_top"}) {
            var bitmap=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/block/"+image+".png").toFile());
            assertEquals(16,bitmap.getWidth()); assertEquals(16,bitmap.getHeight());
        }
        for(String block:new String[]{"darkstone_ritual_altar","darkstone_ritual_pedestal"}) {
            var loot=read("data/abyssalport/loot_table/blocks/"+block+".json");
            assertEquals("abyssalport:darkstone_cobblestone",loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
            for(var texture:read("assets/abyssalport/models/block/"+block+".json").getAsJsonObject("textures").entrySet())
                assertTrue(texture.getValue().getAsString().startsWith("abyssalport:")||texture.getValue().getAsString().startsWith("#"));
        }
        for(String locale:new String[]{"en_us","de_de"}) assertTrue(read("assets/abyssalport/lang/"+locale+".json").has("screen.abyssalport.darklands.ruins"));
    }
}
