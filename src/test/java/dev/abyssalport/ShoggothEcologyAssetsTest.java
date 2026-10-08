package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ShoggothEcologyAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static JsonObject read(String path)throws Exception { return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject(); }
    private static String id(String kind,String habitat) { return (kind.equals("normal")?"":kind+"_")+(habitat.equals("overworld")?"":habitat+"_")+"shoggoth"; }
    @Test void NineNativeLootTablesUseMatchingRemainsAndKeepLegalZeroRollsWithoutGreaterBonus()throws Exception {
        var water=read("data/minecraft/tags/entity_type/can_breathe_under_water.json").getAsJsonArray("values");
        for(String habitat:new String[]{"overworld","abyssal","dreaded"}) for(String kind:new String[]{"lesser","normal","greater"}) {
            String id=id(kind,habitat);
            var loot=read("data/abyssalport/loot_table/entities/"+id+".json");
            assertEquals(1,loot.getAsJsonArray("pools").size());
            var entry=loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("abyssalport:"+habitat+"_shoggoth_flesh",entry.get("name").getAsString());
            var count=entry.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count");
            assertEquals(0,count.get("min").getAsInt()); assertEquals(2,count.get("max").getAsInt());
            var looting=entry.getAsJsonArray("functions").get(1).getAsJsonObject().getAsJsonObject("count");
            assertEquals(0,looting.get("min").getAsInt()); assertEquals(1,looting.get("max").getAsInt());
            assertTrue(water.contains(new JsonPrimitive("abyssalport:"+id)));
            assertEquals("minecraft:item/template_spawn_egg",read("assets/abyssalport/models/item/"+id+"_spawn_egg.json").get("parent").getAsString());
            for(String locale:new String[]{"en_us","de_de"}) {
                var lang=read("assets/abyssalport/lang/"+locale+".json"); assertTrue(lang.has("entity.abyssalport."+id));
                assertTrue(lang.has("item.abyssalport."+id+"_spawn_egg")); assertTrue(lang.has("screen.abyssalport.shoggoth_growth"));
            }
        }
    }
    @Test void DimensionalLairsShareOwnNativePlansAndDistinctHabitatsAndOwnedUvAtlases()throws Exception {
        for(String name:new String[]{"wasteland_shoggoth_lair","dreadlands_shoggoth_lair"}) {
            var structure=read("data/abyssalport/worldgen/structure/"+name+".json");
            assertEquals("abyssalport:shoggoth_lair/start",structure.get("start_pool").getAsString());
            assertEquals(-9,structure.getAsJsonObject("start_height").get("absolute").getAsInt()); assertEquals(1,structure.get("size").getAsInt());
            assertEquals(name.startsWith("wasteland")?"#abyssalport:wasteland_graveyards":"abyssalport:dreadlands",structure.get("biomes").getAsString());
            var placement=read("data/abyssalport/worldgen/structure_set/"+name+".json").getAsJsonObject("placement");
            assertEquals(24,placement.get("spacing").getAsInt()); assertEquals(10,placement.get("separation").getAsInt());
        }
        for(String habitat:new String[]{"abyssal","dreaded"}) {
            var bitmap=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/"+habitat+"_shoggoth.png").toFile());
            var owned=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/lesser_shoggoth.png").toFile());
            assertEquals(128,bitmap.getWidth()); assertEquals(128,bitmap.getHeight());
            // The owned atlas already contains filtered transparent edges;
            // dimensional palettes must preserve its alpha, not fill them in.
            for(int x=0;x<128;x++) for(int y=0;y<128;y++) assertEquals(owned.getRGB(x,y)>>>24,bitmap.getRGB(x,y)>>>24);
        }
    }
    @Test void AcidUsesOwnSpriteAndAbsoluteDamageWithoutInvulnerabilityOrFakeResistantEquipment()throws Exception {
        assertEquals("minecraft:item/generated",read("assets/abyssalport/models/item/shoggoth_acid_glob.json").get("parent").getAsString());
        var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/shoggoth_acid_glob.png").toFile());
        assertEquals(16,image.getWidth()); assertEquals(16,image.getHeight());
        assertEquals(0,image.getRGB(0,0)>>>24); assertEquals(255,image.getRGB(8,8)>>>24);
        var type=read("data/abyssalport/damage_type/shoggoth_acid.json"); assertEquals("shoggoth_acid",type.get("message_id").getAsString());
        assertEquals("never",type.get("scaling").getAsString());
        for(String tag:new String[]{"bypasses_armor","bypasses_enchantments","bypasses_effects","bypasses_resistance"})
            assertTrue(read("data/minecraft/tags/damage_type/"+tag+".json").getAsJsonArray("values").contains(new JsonPrimitive("abyssalport:shoggoth_acid")));
        var invulnerable=ROOT.resolve("data/minecraft/tags/damage_type/bypasses_invulnerability.json");
        if(Files.exists(invulnerable)) assertFalse(read("data/minecraft/tags/damage_type/bypasses_invulnerability.json").getAsJsonArray("values").contains(new JsonPrimitive("abyssalport:shoggoth_acid")));
        assertTrue(read("data/abyssalport/tags/item/acid_resistant_armor.json").getAsJsonArray("values").isEmpty());
        for(String locale:new String[]{"en_us","de_de"}) {
            var lang=read("assets/abyssalport/lang/"+locale+".json"); assertTrue(lang.has("death.attack.shoggoth_acid")); assertTrue(lang.has("death.attack.shoggoth_acid.player"));
        }
    }
}
