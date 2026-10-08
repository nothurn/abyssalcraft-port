package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DreadProcessingAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject data(String file) throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(file))).getAsJsonObject(); }
    @Test void actualEliteLootNotOreCreatesDreadiumOnlyInTransmutator() throws Exception {
        var loot=data("data/abyssalport/loot_table/entities/dreadguard.json").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("abyssalport:dreaded_shard",loot.get("name").getAsString());
        var count=loot.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count"); assertEquals(0,count.get("min").getAsInt()); assertEquals(2,count.get("max").getAsInt());
        var recipe=data("data/abyssalport/recipe/transmutation/dreaded_shard_to_dreadium.json"); assertEquals("abyssalport:transmutation",recipe.get("type").getAsString());
        assertEquals("abyssalport:dreaded_shard",recipe.getAsJsonObject("ingredient").get("item").getAsString()); assertEquals(.2F,recipe.get("experience").getAsFloat());
        assertEquals("abyssalport:dreadium_ingot",recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals("abyssalport:abyssalnite_ingot",data("data/abyssalport/recipe/dreaded_abyssalnite_ore_smelting.json").getAsJsonObject("result").get("id").getAsString());
    }
    @Test void sourceMiningTierAndRecipeToolsHaveOwnPackedMediaAndNativeTags() throws Exception {
        for(String suffix:new String[]{"pickaxe","axe","shovel","sword","hoe"}) {
            var recipe=data("data/abyssalport/recipe/dreadium_"+suffix+".json");
            assertEquals("abyssalport:dreadium_ingot",recipe.getAsJsonObject("key").getAsJsonObject("I").get("item").getAsString());
            assertEquals("c:rods/wooden",recipe.getAsJsonObject("key").getAsJsonObject("S").get("tag").getAsString());
            var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/dreadium_"+suffix+".png").toFile()); assertEquals(16,image.getWidth());
        }
        for(String tier:new String[]{"abyssalnite","refined_coralium"}) assertTrue(data("data/abyssalport/tags/block/incorrect_for_"+tier+"_tool.json").toString().contains("abyssalport:dreadium_block"));
        assertTrue(data("data/minecraft/tags/entity_type/can_breathe_under_water.json").toString().contains("abyssalport:dreadguard"));
        assertTrue(data("data/minecraft/tags/damage_type/bypasses_armor.json").toString().contains("abyssalport:dread"));
        var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/dreadguard.png").toFile()); assertEquals(128,image.getWidth()); assertEquals(128,image.getHeight());
    }
    @Test void bothGuidesNameTheConcreteCreatureAndProcessingWithoutInventedBosses() throws Exception {
        for(String locale:new String[]{"en_us","de_de"}) {
            var lang=data("assets/abyssalport/lang/"+locale+".json");
            for(String key:new String[]{"screen.abyssalport.page.23","screen.abyssalport.dreadguard","screen.abyssalport.dreadium","research.abyssalport.dreadium"})
                assertFalse(lang.get(key).getAsString().isBlank(),key);
        }
    }
    @Test void fourPieceArmorHasNativeRecipesAndOwnCorrectSizedUvLayers() throws Exception {
        for(String name:new String[]{"helmet","chestplate","leggings","boots"}) {
            var recipe=data("data/abyssalport/recipe/dreadium_"+name+".json");
            assertEquals("abyssalport:dreadium_ingot",recipe.getAsJsonObject("key").getAsJsonObject("I").get("item").getAsString());
            assertEquals("abyssalport:dreadium_"+name,recipe.getAsJsonObject("result").get("id").getAsString());
            var model=data("assets/abyssalport/models/item/dreadium_"+name+".json");
            assertEquals("abyssalport:item/dreadium_"+name,model.getAsJsonObject("textures").get("layer0").getAsString());
        }
        for(int layer:new int[]{1,2}) {
            var skin=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/models/armor/dreadium_layer_"+layer+".png").toFile());
            assertEquals(64,skin.getWidth()); assertEquals(32,skin.getHeight());
            assertEquals(255,skin.getRGB(23,23)>>>24); assertEquals(0,skin.getRGB(11,12)>>>24,"Helmet closes the face");
        }
    }
    @Test void everyExistingGearTierIsInTheConcreteNativeEnchantmentCategory() throws Exception {
        for(var pair:new String[][]{{"pickaxes","pickaxe"},{"axes","axe"},{"shovels","shovel"},{"swords","sword"},{"hoes","hoe"},
                {"head_armor","helmet"},{"chest_armor","chestplate"},{"leg_armor","leggings"},{"foot_armor","boots"}}) {
            var tag=data("data/minecraft/tags/item/"+pair[0]+".json");
            assertFalse(tag.get("replace").getAsBoolean());
            for(String prefix:new String[]{"abyssalnite","refined_coralium","dreadium"})
                assertTrue(tag.getAsJsonArray("values").asList().stream().anyMatch(value->value.getAsString().equals("abyssalport:"+prefix+"_"+pair[1])));
        }
    }
}
