package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AnimalEcologyAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();}
    @Test void actualConfigurableModifiersSeparateOverworldNetherAndDenseForests()throws Exception{
        for(String name:new String[]{"evil_animals","demon_animals","shub_forests","shub_dark_forests"}){
            var json=read("data/abyssalport/neoforge/biome_modifier/"+name+".json");assertEquals("abyssalport:animal_ecology_spawns",json.get("type").getAsString());
            assertFalse(json.has("spawners"),"Configurable weights must not be frozen into a static array");assertEquals(name.equals("shub_dark_forests"),json.get("dense").getAsBoolean());
            if(name.equals("demon_animals"))assertEquals("#minecraft:is_nether",json.get("biomes").getAsString());
            else assertFalse(json.toString().contains("is_nether"));
        }
        var food=read("data/abyssalport/tags/entity_type/shoggoth_food.json").toString();var immune=read("data/abyssalport/tags/entity_type/dread_immune.json").toString();
        for(String kind:new String[]{"cow","chicken","pig","sheep"}){assertTrue(food.contains("abyssalport:evil_"+kind));assertTrue(food.contains("abyssalport:demon_"+kind));assertTrue(immune.contains("abyssalport:demon_"+kind));assertFalse(immune.contains("abyssalport:evil_"+kind));}
    }
    @Test void realSourceDropsRemainSpeciesSpecificAndOffspringDoesNotInventLoot()throws Exception{
        for(String kind:new String[]{"cow","chicken","pig","sheep"}){
            var demon=read("data/abyssalport/loot_table/entities/demon_"+kind+".json").getAsJsonArray("pools");assertEquals(1,demon.size());
            var entry=demon.get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();assertEquals("minecraft:rotten_flesh",entry.get("name").getAsString());
            var count=entry.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count");assertEquals(0,count.get("min").getAsInt());assertEquals(2,count.get("max").getAsInt());
            assertTrue(read("data/abyssalport/loot_table/entities/evil_"+kind+".json").getAsJsonArray("pools").size()>=1);
        }
        assertEquals(0,read("data/abyssalport/loot_table/entities/shub_offspring.json").getAsJsonArray("pools").size());
        assertTrue(read("data/minecraft/tags/block/fire.json").toString().contains("abyssalport:mimic_fire"));
        assertTrue(read("assets/abyssalport/blockstates/mimic_fire.json").getAsJsonArray("multipart").size()>=6);
    }
    @Test void ownedAtlasesCoverActualNativeBoxUvsAndEveryEggAndGuideExists()throws Exception{
        for(boolean demon:new boolean[]{false,true})for(String kind:new String[]{"cow","chicken","pig","sheep"}){
            String name=(demon?"demon_":"evil_")+kind;var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/"+name+".png").toFile());assertEquals(128,image.getWidth());assertEquals(128,image.getHeight());
            int[][] cubes=kind.equals("chicken")?new int[][]{{0,0,4,5,4},{40,0,4,2,2},{56,0,2,2,2},{0,32,6,6,6},{64,32,1,5,6},{80,32,1,5,6},{0,64,1,3,1},{24,64,1,3,1},{48,64,3,1,4}}:
                new int[][]{{0,0,kind.equals("cow")?8:6,kind.equals("cow")?8:6,kind.equals("pig")?7:6},{40,0,4,3,2},{56,0,3,2,2},{0,32,10,kind.equals("pig")?7:10,kind.equals("pig")?14:16},
                    {0,64,3,kind.equals("pig")?6:10,3},{24,64,3,kind.equals("pig")?6:10,3},{48,64,3,kind.equals("pig")?6:10,3},{72,64,3,kind.equals("pig")?6:10,3}};
            for(var cube:cubes)for(int x=cube[0];x<cube[0]+2*(cube[2]+cube[4]);x++)for(int y=cube[1];y<cube[1]+cube[3]+cube[4];y++)assertEquals(255,image.getRGB(x,y)>>>24,name+" actual UV hole");
            assertEquals("minecraft:item/template_spawn_egg",read("assets/abyssalport/models/item/"+name+"_spawn_egg.json").get("parent").getAsString());
        }
        var offspring=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/shub_offspring.png").toFile());assertEquals(128,offspring.getHeight());
        for(int[] cube:new int[][]{{0,0,8,8,8},{0,24,10,16,8},{104,56,3,1,4},{104,24,1,5,1},{0,56,3,7,3},{24,56,3,7,3},{48,56,3,7,3},{72,56,3,7,3},{0,80,2,5,2},{24,80,2,5,2},{48,80,2,5,2},{72,80,2,5,2}})
            for(int x=cube[0];x<cube[0]+2*(cube[2]+cube[4]);x++)for(int y=cube[1];y<cube[1]+cube[3]+cube[4];y++)assertEquals(255,offspring.getRGB(x,y)>>>24);
        for(String locale:new String[]{"en_us","de_de"})assertTrue(read("assets/abyssalport/lang/"+locale+".json").has("screen.abyssalport.animal_ecology.guide"));
    }
}
