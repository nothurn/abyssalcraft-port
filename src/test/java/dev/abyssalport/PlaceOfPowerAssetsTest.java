package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceOfPowerAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void formedBaseUsesOwnStoneAndDropsOrdinaryStoneWithoutAnItemOrRecipe()throws Exception {
        assertEquals("abyssalport:block/monolith_stone",read("assets/abyssalport/models/block/place_of_power_base.json").get("parent").getAsString());
        assertTrue(read("assets/abyssalport/blockstates/place_of_power_base.json").toString().contains("abyssalport:block/place_of_power_base"));
        assertTrue(read("data/abyssalport/loot_table/blocks/place_of_power_base.json").toString().contains("abyssalport:monolith_stone"));
        assertTrue(read("data/minecraft/tags/block/mineable/pickaxe.json").toString().contains("abyssalport:place_of_power_base"));
        assertFalse(Files.exists(ROOT.resolve("data/abyssalport/recipe/place_of_power_base.json")));
        assertFalse(Files.exists(ROOT.resolve("assets/abyssalport/models/item/place_of_power_base.json")));
    }
    @Test void threeSourceLayoutsHaveBilingualBuildInstructionsAndNativeSharedDiagrams()throws Exception {
        for(var locale:List.of("en_us","de_de")) {
            var lang=read("assets/abyssalport/lang/"+locale+".json");
            for(var id:List.of("basic","totempole","archway")) {
                assertTrue(lang.has("screen.abyssalport.power."+id));
                assertTrue(lang.has("screen.abyssalport.power.materials."+id));
            }
            for(int page=27;page<=29;page++) assertTrue(lang.has("screen.abyssalport.page."+page));
            assertTrue(lang.get("screen.abyssalport.power.materials.basic").getAsString().contains("15"));
            assertTrue(lang.get("screen.abyssalport.power.guide").getAsString().contains("PE"));
        }
        var screen=Files.readString(Path.of("src/main/java/dev/abyssalport/client/NecronomiconScreen.java"));
        assertTrue(screen.contains("PlaceOfPowerLayout.values()[page-27]")&&screen.contains("piece.previewState(Direction.EAST)")&&screen.contains("drawPowerDiagram"));
        for(int page=27;page<=29;page++) assertTrue(dev.abyssalport.research.ResearchMilestones.pageAvailable(page,0));
    }
}
