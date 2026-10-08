package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class DarklandsBiomeAssetsTest {
    private static final Path DATA=Path.of("src/main/resources/data/abyssalport");
    private static JsonObject json(String path) throws Exception { return JsonParser.parseString(Files.readString(DATA.resolve(path))).getAsJsonObject(); }
    @Test void allFourNativeVariantsHaveConnectedOreTreesAndSharedMonolithPaths() throws Exception {
        for(String name:List.of("darklands_plains","darklands_forest","darklands_highlands","darklands_mountains")) {
            var biome=json("worldgen/biome/"+name+".json");
            String text=biome.toString(); assertTrue(text.contains("abyssalport:ore_abyssalnite")); assertTrue(text.contains("abyssalport:ore_darkstone"));
            var vegetation=biome.getAsJsonArray("features").get(9).toString();
            if(name.endsWith("forest")) assertTrue(vegetation.contains("abyssalport:darklands_oak"));
            else if(name.endsWith("mountains")) assertFalse(vegetation.contains("darklands_oak"));
            else assertTrue(vegetation.contains("abyssalport:sparse_darklands_oak"));
            for(String modifier:List.of("abandoned_monolith","lesser_shoggoth_spawns","shadow_family_"+name))
                assertTrue(json("neoforge/biome_modifier/"+modifier+".json").getAsJsonArray("biomes").toString().contains("abyssalport:"+name));
        }
        assertEquals(.08,json("worldgen/configured_feature/sparse_darklands_oak.json").getAsJsonObject("config").get("probability").getAsDouble());
        assertEquals(1,json("worldgen/placed_feature/sparse_darklands_oak.json").getAsJsonArray("placement").get(0).getAsJsonObject().get("count").getAsInt());
    }
}
