package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class GraveyardAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void nativeSavedGenerationAndBoundedSpawnsUseRealContent()throws Exception {
        for(String name:new String[]{"graveyard","darklands_graveyard"}) {
            var structure=read("data/abyssalport/worldgen/structure/"+name+".json"); assertEquals(1,structure.get("size").getAsInt());
            assertEquals("WORLD_SURFACE_WG",structure.get("project_start_to_heightmap").getAsString());
            assertEquals(3,read("data/abyssalport/worldgen/template_pool/"+name+"/start.json").getAsJsonArray("elements").size());
        }
        var placement=read("data/abyssalport/worldgen/structure_set/graveyards.json").getAsJsonObject("placement");
        assertEquals(24,placement.get("spacing").getAsInt()); assertEquals(14,placement.get("separation").getAsInt());
        var spawn=read("data/abyssalport/neoforge/biome_modifier/ghouls.json").getAsJsonObject("spawners");
        assertEquals(1,spawn.get("weight").getAsInt()); assertEquals(1,spawn.get("minCount").getAsInt()); assertEquals(3,spawn.get("maxCount").getAsInt());
        var rules=read("data/abyssalport/worldgen/processor_list/graveyard.json").getAsJsonArray("processors").get(0).getAsJsonObject().getAsJsonArray("rules");
        assertEquals(.5,rules.get(0).getAsJsonObject().getAsJsonObject("input_predicate").get("probability").getAsDouble());
        assertEquals("minecraft:clear",rules.get(0).getAsJsonObject().getAsJsonObject("block_entity_modifier").get("type").getAsString());
        for(String marker:new String[]{"stone_tombstone","darkstone_tombstone"}) assertTrue(read("data/abyssalport/loot_table/blocks/"+marker+".json").getAsJsonArray("pools").isEmpty());
    }
    @Test void ownMobAtlasCoversAllActualCubeUvsAndFleshIconsAreBounded()throws Exception {
        for(String name:new String[]{"ghoul","shadow_ghoul","depths_ghoul","depths_ghoul_1","depths_ghoul_2","depths_ghoul_3","dreaded_ghoul"}) {
            var bitmap=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/"+name+".png").toFile());
            assertEquals(64,bitmap.getWidth()); assertEquals(64,bitmap.getHeight());
            for(var rect:new int[][]{{0,0,32,16},{0,16,26,33},{32,16,44,26},{32,28,44,38},{0,40,12,49},{16,40,28,49}})
                for(int x=rect[0];x<rect[2];x++) for(int y=rect[1];y<rect[3];y++) assertEquals(255,bitmap.getRGB(x,y)>>>24,"Missing actual Ghoul UV pixel");
        }
        for(String name:new String[]{"ghoul_flesh","shadow_ghoul_flesh","overworld_shoggoth_flesh","depths_ghoul_flesh","dreaded_ghoul_flesh","abyssal_shoggoth_flesh","dreaded_shoggoth_flesh"}) {
            var bitmap=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/"+name+".png").toFile()); assertEquals(16,bitmap.getWidth()); assertEquals(16,bitmap.getHeight());
        }
    }
    @Test void dimensionalGravesHeadsAndRecipesUseRealVariantData()throws Exception {
        for(String name:new String[]{"wasteland_graveyard","dreadlands_graveyard"}) {
            assertEquals(6,read("data/abyssalport/worldgen/template_pool/"+name+"/start.json").getAsJsonArray("elements").size());
            assertEquals(1,read("data/abyssalport/worldgen/structure/"+name+".json").get("size").getAsInt());
            var entries=read("data/abyssalport/loot_table/chests/"+name+".json").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
            assertEquals(7,entries.size());
            for(var entry:entries) if(entry.getAsJsonObject().has("name")&&entry.getAsJsonObject().get("name").getAsString().contains("skin_of"))
                assertEquals(2,entry.getAsJsonObject().getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count").get("max").getAsInt());
        }
        for(String name:new String[]{"depths_ghoul_flesh","dreaded_ghoul_flesh","overworld_shoggoth_flesh","abyssal_shoggoth_flesh","dreaded_shoggoth_flesh"}) {
            var recipe=read("data/abyssalport/recipe/crystallization/"+name+".json");
            assertEquals(name.contains("shoggoth")?8:12,recipe.getAsJsonObject("result").get("count").getAsInt()); assertEquals(.2,recipe.get("experience").getAsDouble());
            if(!name.startsWith("overworld")) assertEquals(name.contains("shoggoth")?2:1,recipe.getAsJsonObject("secondary").has("count")?recipe.getAsJsonObject("secondary").get("count").getAsInt():1);
        }
        for(String variant:new String[]{"depths_ghoul","depths_ghoul_1","depths_ghoul_2","depths_ghoul_3"}) {
            var rare=read("data/abyssalport/loot_table/entities/"+variant+".json").getAsJsonArray("pools").get(1).getAsJsonObject();
            assertEquals("minecraft:killed_by_player",rare.getAsJsonArray("conditions").get(0).getAsJsonObject().get("condition").getAsString());
            assertEquals(.025,rare.getAsJsonArray("conditions").get(1).getAsJsonObject().get("unenchanted_chance").getAsDouble());
        }
    }
    @Test void sourceFleshRecipesAndGuideDoNotInventHigherDimensionLoot()throws Exception {
        for(String name:new String[]{"ghoul_flesh","shadow_ghoul_flesh"}) {
            var recipe=read("data/abyssalport/recipe/crystallization/"+name+".json"); assertEquals(12,recipe.getAsJsonObject("result").get("count").getAsInt());
            assertEquals(.2,recipe.get("experience").getAsDouble());
            if(name.startsWith("shadow")) assertEquals("abyssalport:shadow_shard",recipe.getAsJsonObject("secondary").get("id").getAsString()); else assertFalse(recipe.has("secondary"));
        }
        String loot=Files.readString(ROOT.resolve("data/abyssalport/loot_table/chests/graveyard.json"));
        assertTrue(loot.contains("overworld_shoggoth_flesh")); assertFalse(loot.contains("powerstone")); assertFalse(loot.contains("skin_of"));
        for(String locale:new String[]{"en_us","de_de"}) assertTrue(read("assets/abyssalport/lang/"+locale+".json").has("screen.abyssalport.ghouls.guide"));
    }
}
