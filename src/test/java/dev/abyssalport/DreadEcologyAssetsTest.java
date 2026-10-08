package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DreadEcologyAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject json(String path) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void biomeHasTheSourceNormalWeightsAndRealFragmentOrShardLoot() throws Exception {
        var rows=json("data/abyssalport/worldgen/biome/dreadlands.json").getAsJsonObject("spawners").getAsJsonArray("monster");
        for(var entry:Map.of("dread_spawn",30,"greater_dread_spawn",5,"lesser_dreadbeast",1).entrySet()) {
            var row=rows.asList().stream().map(JsonElement::getAsJsonObject).filter(e->e.get("type").getAsString().equals("abyssalport:"+entry.getKey())).findFirst().orElseThrow();
            assertEquals(entry.getValue(),row.get("weight").getAsInt()); assertTrue(row.get("minCount").getAsInt()>=1);
            var loot=json("data/abyssalport/loot_table/entities/"+entry.getKey()+".json");
            String text=loot.toString(); assertTrue(text.contains(entry.getKey().equals("dread_spawn")?"abyssalport:dread_fragment":"abyssalport:dreaded_shard"));
            assertTrue(text.contains("minecraft:looting")); assertFalse(text.contains("ore"));
        }
    }
    @Test void independentAtlasesCoverTheArticulatedModelAndDoNotReferenceOriginalMedia() throws Exception {
        for(String name:List.of("dread_spawn","greater_dread_spawn","lesser_dreadbeast")) {
            var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/"+name+".png").toFile());
            assertEquals(64,image.getWidth()); assertEquals(64,image.getHeight());
            for(int[] point:List.of(new int[]{12,12},new int[]{8,28},new int[]{35,23},new int[]{35,29},new int[]{36,36})) assertEquals(255,image.getRGB(point[0],point[1])>>>24);
            assertEquals("minecraft:item/template_spawn_egg",json("assets/abyssalport/models/item/"+name+"_spawn_egg.json").get("parent").getAsString());
        }
        for(String locale:List.of("en_us","de_de")) assertFalse(json("assets/abyssalport/lang/"+locale+".json").get("screen.abyssalport.dreadlands.ecology").getAsString().isBlank());
    }
}
