package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DreadlandsAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private JsonObject json(String path) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void nextKeyNeedsTheActualBossStrongholdAndHigherRitualGround() throws Exception {
        var recipe = json("data/abyssalport/rituals/dreadlands_key.json");
        assertEquals(10000, recipe.get("energy_cost").getAsInt()); assertEquals(1, recipe.get("book_tier").getAsInt());
        assertEquals(1, recipe.get("altar_tier").getAsInt()); assertEquals("abyssalport:abyssal_wasteland", recipe.get("dimension").getAsString());
        assertEquals("abyssalport:gateway_key", recipe.get("altar_input").getAsString());
        var materials = recipe.getAsJsonArray("offerings").asList().stream().map(e -> e.getAsString()).toList();
        assertEquals(3, materials.size()); assertTrue(materials.containsAll(java.util.List.of("abyssalport:transmutation_gem", "abyssalport:dreadlands_infused_powerstone", "abyssalport:eye_of_the_abyss")));
        var gateway = json("data/abyssalport/rituals/dreadlands_gateway.json");
        assertEquals("gateway", gateway.get("outcome").getAsString()); assertEquals(1000, gateway.get("energy_cost").getAsInt());
        assertEquals(gateway.get("altar_input").getAsString(), gateway.getAsJsonObject("result").get("item").getAsString());
    }
    @Test void realNativeDimensionHasThreeDepositsAndItsOwnCreature() throws Exception {
        var dim = json("data/abyssalport/dimension/dreadlands.json");
        assertEquals("minecraft:noise", dim.getAsJsonObject("generator").get("type").getAsString());
        var noise = json("data/abyssalport/worldgen/noise_settings/dreadlands.json");
        assertEquals("abyssalport:dreadstone", noise.getAsJsonObject("default_block").get("Name").getAsString());
        assertEquals(49, noise.get("sea_level").getAsInt());
        var biome = json("data/abyssalport/worldgen/biome/dreadlands.json");
        assertEquals(3, biome.getAsJsonArray("features").get(6).getAsJsonArray().size());
        var mob = biome.getAsJsonObject("spawners").getAsJsonArray("monster").get(0).getAsJsonObject();
        assertEquals("abyssalport:dreadling", mob.get("type").getAsString()); assertEquals(40, mob.get("weight").getAsInt());
        assertEquals(1.0, json("data/abyssalport/dimension_type/dreadlands.json").get("coordinate_scale").getAsDouble());
        assertFalse(json("data/abyssalport/dimension_type/dreadlands.json").get("bed_works").getAsBoolean());
        assertEquals("abyssalport:abyssalnite_ingot", json("data/abyssalport/recipe/dreaded_abyssalnite_ore_smelting.json").getAsJsonObject("result").get("id").getAsString());
    }
    @Test void ownUvFacesAreOpaqueAndAllGuidePagesAreTranslated() throws Exception {
        var skin = ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/dreadling.png").toFile());
        assertEquals(64, skin.getWidth()); assertEquals(64, skin.getHeight());
        for (int[] net : new int[][]{{0,0,8,8,8},{0,20,8,9,4},{32,20,3,7,4},{48,20,3,11,4},{0,40,4,4,4},{20,40,4,3,4}}) {
            int u=net[0], v=net[1], w=net[2], h=net[3], d=net[4];
            for (int[] face : new int[][]{{u+d,v,w,d},{u+d+w,v,w,d},{u,v+d,d,h},{u+d,v+d,w,h},{u+d+w,v+d,d,h},{u+2*d+w,v+d,w,h}})
                for(int y=face[1]; y<face[1]+face[3]; y++) for(int x=face[0]; x<face[0]+face[2]; x++) assertEquals(255, skin.getRGB(x,y)>>>24);
        }
        for(String locale : new String[]{"en_us","de_de"}) {
            var lang=json("assets/abyssalport/lang/"+locale+".json");
            for(String key : new String[]{"screen.abyssalport.page.19","screen.abyssalport.page.20","screen.abyssalport.locked.19","screen.abyssalport.locked.20",
                    "screen.abyssalport.dreadlands_key","screen.abyssalport.dreadlands_key_note","screen.abyssalport.dreadlands","screen.abyssalport.dreadlands_note",
                    "entity.abyssalport.dreadling","effect.abyssalport.dread_plague","item.abyssalport.dreadlands_infused_gateway_key"}) assertTrue(lang.has(key),key);
        }
    }
}
