package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class StatueAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static final List<String> NAMES=List.of("cthulhu","hastur","jzahar","azathoth","nyarlathotep","yog_sothoth","shub_niggurath");
    private static JsonObject read(String path)throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void sevenOwnSculpturesHaveDistinctBoundedGeometryRatherThanRenamedEffigies()throws Exception {
        var silhouettes=new HashSet<String>();
        for(var name:NAMES) {
            var model=read("assets/abyssalport/models/block/"+name+"_statue.json"); var elements=model.getAsJsonArray("elements");
            assertTrue(elements.size()>=10); assertTrue(silhouettes.add(elements.toString()),"Duplicate sculpture: "+name);
            for(var entry:elements) {
                var element=entry.getAsJsonObject(); var lower=element.getAsJsonArray("from"); var upper=element.getAsJsonArray("to");
                for(int axis=0;axis<3;axis++) {
                    assertTrue(lower.get(axis).getAsDouble()>=0&&upper.get(axis).getAsDouble()<=16);
                    assertTrue(lower.get(axis).getAsDouble()<upper.get(axis).getAsDouble());
                }
                assertEquals(6,element.getAsJsonObject("faces").size());
            }
            assertEquals("abyssalport:block/"+name+"_statue",model.getAsJsonObject("textures").get("stone").getAsString());
            var pixels=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/block/"+name+"_statue.png").toFile());
            assertEquals(16,pixels.getWidth()); assertEquals(16,pixels.getHeight());
            var colors=new HashSet<Integer>();
            for(int x=0;x<16;x++) for(int y=0;y<16;y++) { assertEquals(255,pixels.getRGB(x,y)>>>24); colors.add(pixels.getRGB(x,y)); }
            assertTrue(colors.size()>16);
        }
    }
    @Test void realBlockstatesItemsDropsAndTranslationsCoverEveryDeityWithoutInventedRecipes()throws Exception {
        var tool=read("data/minecraft/tags/block/mineable/pickaxe.json").getAsJsonArray("values").toString();
        for(var name:NAMES) {
            var id="abyssalport:"+name+"_statue";
            var variants=read("assets/abyssalport/blockstates/"+name+"_statue.json").getAsJsonObject("variants");
            assertEquals(4,variants.size()); int rotation=0;
            for(var facing:List.of("north","east","south","west")) {
                var variant=variants.getAsJsonObject("facing="+facing); assertEquals(rotation,variant.get("y").getAsInt()); rotation+=90;
                assertEquals("abyssalport:block/"+name+"_statue",variant.get("model").getAsString());
            }
            assertEquals("abyssalport:block/"+name+"_statue",read("assets/abyssalport/models/item/"+name+"_statue.json").get("parent").getAsString());
            assertTrue(read("data/abyssalport/loot_table/blocks/"+name+"_statue.json").toString().contains(id)); assertTrue(tool.contains(id));
            assertFalse(Files.exists(ROOT.resolve("data/abyssalport/recipe/"+name+"_statue.json")),"Source monolith sculptures are not freely craftable");
            for(var locale:List.of("en_us","de_de")) assertTrue(read("assets/abyssalport/lang/"+locale+".json").has("block.abyssalport."+name+"_statue"));
        }
    }
}
