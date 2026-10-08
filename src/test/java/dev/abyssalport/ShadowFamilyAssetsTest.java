package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ShadowFamilyAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    @Test void fiveRealBiomeFamiliesHaveSourceWeightsAndNoDuplicateFallback()throws Exception {
        for(String name:new String[]{"darklands","darklands_plains","darklands_forest","darklands_highlands","darklands_mountains"}) {
            var value=read("data/abyssalport/neoforge/biome_modifier/shadow_family_"+name+".json");
            assertEquals("abyssalport:"+name,value.getAsJsonArray("biomes").get(0).getAsString());
            var mobs=value.getAsJsonArray("spawners"); assertEquals(3,mobs.size());
            int[] weights=name.endsWith("mountains")?new int[]{100,80,40}:new int[]{70,50,20};
            String[] types={"shadow_creature","shadow_monster","shadow_beast"};
            for(int i=0;i<3;i++) { var mob=mobs.get(i).getAsJsonObject(); assertEquals("abyssalport:"+types[i],mob.get("type").getAsString());
                assertEquals(weights[i],mob.get("weight").getAsInt()); assertEquals(3-i,mob.get("minCount").getAsInt()); assertEquals(3-i,mob.get("maxCount").getAsInt()); }
        }
        var old=read("data/abyssalport/neoforge/biome_modifier/shadow_creature_spawns.json");
        assertEquals(2,old.getAsJsonArray("biomes").size()); assertFalse(old.toString().contains("abyssalport:darklands"));
    }
    @Test void nativeLootAndAbsoluteDamageKeepActualSourceRules()throws Exception {
        for(String name:new String[]{"shadow_monster","shadow_beast"}) {
            var entry=read("data/abyssalport/loot_table/entities/"+name+".json").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("abyssalport:"+(name.endsWith("monster")?"shadow_shard":"shadow_gem"),entry.get("name").getAsString());
            var count=entry.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count"); assertEquals(0,count.get("min").getAsInt()); assertEquals(2,count.get("max").getAsInt());
            assertEquals("minecraft:looting",entry.getAsJsonArray("functions").get(1).getAsJsonObject().get("enchantment").getAsString());
        }
        for(String tag:new String[]{"bypasses_armor","bypasses_effects","bypasses_enchantments","bypasses_resistance","bypasses_shield"})
            assertTrue(read("data/minecraft/tags/damage_type/"+tag+".json").toString().contains("abyssalport:shadow"));
        assertFalse(Files.exists(ROOT.resolve("data/minecraft/tags/damage_type/bypasses_cooldown.json"))&&Files.readString(ROOT.resolve("data/minecraft/tags/damage_type/bypasses_cooldown.json")).contains("abyssalport:shadow"));
    }
    @Test void independentRigUvsHaveOpaquePixelsAndNoNamespaceBorrowing()throws Exception {
        for(boolean beast:new boolean[]{false,true}) {
            var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/"+(beast?"shadow_beast":"shadow_monster")+".png").toFile());
            assertEquals(128,image.getWidth()); assertEquals(128,image.getHeight());
            int h=beast?10:8,w=beast?12:8,t=beast?18:10,aw=beast?4:3,ah=beast?18:12;
            for(int[] cube:new int[][]{{0,0,h,h,h},{0,24,w,t,6},{48,24,aw,ah,4},{72,24,aw,ah,4},
                    {0,56,4,beast?8:6,4},{24,56,4,beast?8:6,4},{48,56,4,beast?6:4,4},{72,56,4,beast?6:4,4},{104,24,beast?1:0,beast?4:0,beast?1:0}})
                for(int x=cube[0];x<cube[0]+2*(cube[2]+cube[4]);x++) for(int y=cube[1];y<cube[1]+cube[4]+cube[3];y++) assertEquals(255,image.getRGB(x,y)>>>24);
        }
        for(String locale:new String[]{"en_us","de_de"}) assertTrue(read("assets/abyssalport/lang/"+locale+".json").has("screen.abyssalport.shadow_family.guide"));
        var targets=read("data/abyssalport/tags/entity_type/shadow_rending_targets.json").getAsJsonArray("values");
        for(String mob:new String[]{"shadow_creature","shadow_monster","shadow_beast","shadow_ghoul"}) assertTrue(targets.toString().contains("abyssalport:"+mob));
    }
}
