package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class StatueEnergyAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();}
    @Test void sourceRangePillarRecipeAndOwnNarrowVoxelModelArePackaged()throws Exception{
        var recipe=read("data/abyssalport/recipe/monolith_pillar.json");
        assertEquals("[\"SS\",\"SS\"]",recipe.getAsJsonArray("pattern").toString());
        assertEquals("abyssalport:monolith_stone",recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        assertEquals("abyssalport:monolith_pillar",recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1,recipe.getAsJsonObject("result").get("count").getAsInt());
        var model=read("assets/abyssalport/models/block/monolith_pillar.json");var cube=model.getAsJsonArray("elements").get(0).getAsJsonObject();
        assertEquals("[4,0,4]",cube.getAsJsonArray("from").toString());assertEquals("[12,16,12]",cube.getAsJsonArray("to").toString());
        var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/block/monolith_pillar.png").toFile());assertEquals(16,image.getWidth());assertEquals(16,image.getHeight());
        assertTrue(read("data/minecraft/tags/block/mineable/pickaxe.json").toString().contains("abyssalport:monolith_pillar"));
    }
    @Test void guideDocumentsIndependentCadenceRealPillarsAndStillUnfinishedDisruptions()throws Exception{
        for(var locale:List.of("en_us","de_de")){
            var lang=read("assets/abyssalport/lang/"+locale+".json");assertTrue(lang.has("block.abyssalport.monolith_pillar"));
            var guide=lang.get("screen.abyssalport.energy_note").getAsString();
            assertTrue(guide.contains("120")&&guide.contains("80")&&guide.toLowerCase(java.util.Locale.ROOT).contains("disruption"));
        }
    }
}
