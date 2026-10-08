package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SacrificialAltarAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception {return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();}
    @Test void fourRealTiersHaveOwnGeometryPixelsAndExactPersistedCounterLoot()throws Exception {
        for(String prefix:List.of("","overworld_","abyssal_wasteland_","dreadlands_")) {
            String id=prefix+"sacrificial_altar";
            var state=read("assets/abyssalport/blockstates/"+id+".json").getAsJsonObject("variants");
            assertEquals(1,state.size());assertTrue(state.has(""));
            var model=read("assets/abyssalport/models/block/"+id+".json");assertEquals(9,model.getAsJsonArray("elements").size());
            for(var part:model.getAsJsonArray("elements"))for(int axis=0;axis<3;axis++) {
                assertTrue(part.getAsJsonObject().getAsJsonArray("from").get(axis).getAsDouble()>=0);
                assertTrue(part.getAsJsonObject().getAsJsonArray("to").get(axis).getAsDouble()<=16);
            }
            for(var entry:model.getAsJsonObject("textures").entrySet()) {
                String texture=entry.getValue().getAsString();if(texture.startsWith("#"))continue;
                assertTrue(texture.startsWith("abyssalport:block/"));
                var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/"+texture.substring(12)+".png").toFile());
                assertEquals(16,image.getWidth());assertEquals(16,image.getHeight());
            }
            assertEquals("abyssalport:block/"+id,read("assets/abyssalport/models/item/"+id+".json").get("parent").getAsString());
            var entry=read("data/abyssalport/loot_table/blocks/"+id+".json").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("abyssalport:"+id,entry.get("name").getAsString());
            var fields=new HashSet<String>();for(var op:entry.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonArray("ops"))fields.add(op.getAsJsonObject().get("source").getAsString());
            assertEquals(Set.of("PotEnergy","CollectionLimit","CoolDown"),fields);
            for(String locale:List.of("en_us","de_de"))assertTrue(read("assets/abyssalport/lang/"+locale+".json").has("block.abyssalport."+id));
        }
        assertFalse(Files.exists(ROOT.resolve("assets/abyssalport/blockstates/omothol_sacrificial_altar.json")));
    }
    @Test void realRecipeAndReachableBilingualBookExplainNoInventedMechanics()throws Exception {
        var recipe=read("data/abyssalport/recipe/sacrificial_altar.json");
        assertEquals("[\"#%#\",\"&$&\",\"&&&\"]",recipe.getAsJsonArray("pattern").toString());
        var key=recipe.getAsJsonObject("key");
        assertEquals("abyssalport:shadow_fragment",key.getAsJsonObject("#").get("item").getAsString());
        assertEquals("abyssalport:monolith_stone",key.getAsJsonObject("&").get("item").getAsString());
        assertEquals("abyssalport:coralium_pearl",key.getAsJsonObject("%").get("item").getAsString());
        assertEquals("abyssalport:shadow_gem",key.getAsJsonObject("$").get("item").getAsString());
        assertEquals("abyssalport:sacrificial_altar",recipe.getAsJsonObject("result").get("id").getAsString());
        assertTrue(Files.readString(Path.of("src/main/java/dev/abyssalport/client/NecronomiconScreen.java")).contains("screen.abyssalport.sacrificial_altar"));
        for(String locale:List.of("en_us","de_de")) {
            var lang=read("assets/abyssalport/lang/"+locale+".json");
            assertTrue(lang.get("screen.abyssalport.sacrificial_altar").getAsString().length()>500);
            assertEquals(5,lang.get("message.abyssalport.sacrificial_altar_status").getAsString().split("%s",-1).length-1);
        }
    }
}
