package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ShoggothLairAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void regionalLairsKeepNativePersistenceAndSourceBiomeRoleWithoutCopiedPlans()throws Exception {
        var structure=read("data/abyssalport/worldgen/structure/shoggoth_lair.json");
        assertEquals(1,structure.get("size").getAsInt()); assertEquals(-9,structure.getAsJsonObject("start_height").get("absolute").getAsInt());
        assertEquals("WORLD_SURFACE_WG",structure.get("project_start_to_heightmap").getAsString());
        assertEquals(3,read("data/abyssalport/worldgen/template_pool/shoggoth_lair/start.json").getAsJsonArray("elements").size());
        var placement=read("data/abyssalport/worldgen/structure_set/shoggoth_lairs.json").getAsJsonObject("placement");
        assertTrue((placement.get("separation").getAsInt()+1)*16>=100);
        var biomes=read("data/abyssalport/tags/worldgen/biome/shoggoth_lairs.json").getAsJsonArray("values");
        assertEquals(4,biomes.size()); assertFalse(biomes.toString().contains("ocean"));
    }
    @Test void biomassUsesOwnNativeModelPixelsAndNeverDropsEvenWithSilk()throws Exception {
        var bitmap=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/block/shoggoth_biomass.png").toFile());
        assertEquals(16,bitmap.getWidth()); assertEquals(16,bitmap.getHeight());
        for(int x=0;x<16;x++) for(int y=0;y<16;y++) assertEquals(255,bitmap.getRGB(x,y)>>>24);
        assertEquals(3,read("assets/abyssalport/models/block/shoggoth_biomass.json").getAsJsonArray("elements").size());
        assertTrue(read("data/abyssalport/loot_table/blocks/shoggoth_biomass.json").getAsJsonArray("pools").isEmpty());
        assertEquals("abyssalport:monolith_stone",read("data/abyssalport/loot_table/blocks/monolith_stone.json").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().get("name").getAsString());
        for(String locale:new String[]{"en_us","de_de"}) {
            var lang=read("assets/abyssalport/lang/"+locale+".json");
            assertTrue(lang.has("block.abyssalport.shoggoth_biomass")); assertTrue(lang.has("screen.abyssalport.shoggoth_lairs"));
        }
    }
}
