package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CrystalBagAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject json(String path) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void reachableTiersKeepOriginalIngredientCostsAndExplicitComponentPreservingUpgrades() throws Exception {
        var small=json("data/abyssalport/recipe/small_crystal_bag.json");
        assertEquals("minecraft:crafting_shaped",small.get("type").getAsString());
        assertEquals(List.of("SLS","LGL","LLL"),small.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList());
        assertEquals("minecraft:gold_ingot",small.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        for(var pair:List.of(List.of("medium","small","skin_of_the_abyssal_wasteland"),List.of("large","medium","skin_of_the_dreadlands"))) {
            var upgrade=json("data/abyssalport/recipe/"+pair.get(0)+"_crystal_bag.json");
            assertEquals("abyssalport:crystal_bag_upgrade",upgrade.get("type").getAsString());
            assertEquals(List.of("SSS","SBS","SSS"),upgrade.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList());
            assertEquals("abyssalport:"+pair.get(1)+"_crystal_bag",upgrade.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
            assertEquals("abyssalport:"+pair.get(2),upgrade.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        }
    }
    @Test void eachOwnBagIconHasTransparentPixelEdgesAndNativeGeneratedModel() throws Exception {
        Set<Integer> signatures=new HashSet<>();
        for(String tier:List.of("small","medium","large")) {
            String name=tier+"_crystal_bag";
            var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/"+name+".png").toFile());
            assertEquals(16,image.getWidth()); assertEquals(16,image.getHeight()); assertEquals(0,image.getRGB(0,0)>>>24);
            assertEquals(255,image.getRGB(7,8)>>>24); assertTrue(signatures.add(image.getRGB(8,9)),"Bag tiers cannot be distinguished");
            var model=json("assets/abyssalport/models/item/"+name+".json");
            assertEquals("minecraft:item/generated",model.get("parent").getAsString());
            assertEquals("abyssalport:item/"+name,model.getAsJsonObject("textures").get("layer0").getAsString());
            var advancement=json("data/abyssalport/advancement/recipes/"+name+".json");
            assertEquals("abyssalport:"+name,advancement.getAsJsonObject("rewards").getAsJsonArray("recipes").get(0).getAsString());
        }
    }
    @Test void bothLanguagesExplainStorageCostsSafetyAndTheGenuinelyUnavailableFourthTier() throws Exception {
        for(String locale:List.of("en_us","de_de")) {
            var lang=json("assets/abyssalport/lang/"+locale+".json");
            for(String key:List.of("item.abyssalport.small_crystal_bag","item.abyssalport.medium_crystal_bag","item.abyssalport.large_crystal_bag",
                    "screen.abyssalport.page.25","screen.abyssalport.crystal_bags","screen.abyssalport.crystal_bags.upgrade","research.abyssalport.crystal_bags",
                    "tooltip.abyssalport.crystal_bag","message.abyssalport.crystal_bag.invalid")) assertFalse(lang.get(key).getAsString().isBlank());
            assertTrue(lang.get("screen.abyssalport.crystal_bags.upgrade").getAsString().contains("72"));
        }
    }
}
