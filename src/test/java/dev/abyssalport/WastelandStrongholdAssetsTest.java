package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class WastelandStrongholdAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void originalRingFactsAndGuaranteeUseNativeDataNotCreativeRecipes()throws Exception {
        var placement=read("data/abyssalport/worldgen/structure_set/wasteland_strongholds.json").getAsJsonObject("placement");
        assertEquals("minecraft:concentric_rings",placement.get("type").getAsString());
        assertEquals(0,placement.get("salt").getAsInt());
        assertEquals(128,placement.get("count").getAsInt()); assertEquals(32,placement.get("distance").getAsInt()); assertEquals(3,placement.get("spread").getAsInt());
        var structure=read("data/abyssalport/worldgen/structure/wasteland_stronghold.json");
        assertEquals("minecraft:jigsaw",structure.get("type").getAsString()); assertEquals("underground_structures",structure.get("step").getAsString());
        assertFalse(Files.exists(ROOT.resolve("data/abyssalport/recipe/dreadlands_infused_powerstone.json")),"First Powerstone must be found before Dreadlands infusion is reachable");
        var result=read("data/abyssalport/recipe/powerstone_tracker.json");
        assertEquals("abyssalport:coralium_gem",result.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        assertEquals(4,result.getAsJsonObject("result").get("count").getAsInt());
    }
    @Test void ownTexturesModelsAndGuideHaveNoMissingMedia()throws Exception {
        for(String name:new String[]{"abyssal_stone_bricks","cracked_abyssal_stone_bricks","chiseled_abyssal_stone_bricks","dreadlands_infused_powerstone"}) {
            var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/block/"+name+".png").toFile());
            assertEquals(16,image.getWidth()); assertEquals(16,image.getHeight());
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)assertEquals(255,image.getRGB(x,y)>>>24);
        }
        for(String locale:new String[]{"en_us","de_de"}) {
            var text=read("assets/abyssalport/lang/"+locale+".json");
            for(String key:new String[]{"screen.abyssalport.page.18","screen.abyssalport.stronghold","screen.abyssalport.stronghold_note","research.abyssalport.powerstone","item.abyssalport.powerstone_tracker"})assertTrue(text.has(key),key);
        }
        assertFalse(dev.abyssalport.research.ResearchMilestones.pageAvailable(18,0));
        assertTrue(dev.abyssalport.research.ResearchMilestones.pageAvailable(18,dev.abyssalport.research.ResearchMilestones.WASTELAND));
        assertTrue(dev.abyssalport.research.ResearchMilestones.pageAvailable(18,dev.abyssalport.research.ResearchMilestones.POWERSTONE));
    }
}
