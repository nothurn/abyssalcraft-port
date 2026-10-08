package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Checks dynamic tier IDs too; a literal-register regex alone cannot cover them. */
class EnergyDeviceAssetsTest {
    private static final Path ROOT=Path.of("src/main/resources");
    private static final List<String> TIERS=List.of("","overworld_","abyssal_wasteland_","dreadlands_");
    private static JsonObject read(String name)throws Exception{return JsonParser.parseString(Files.readString(ROOT.resolve(name))).getAsJsonObject();}
    @Test void allSixteenReachableDevicesHaveOwnModelsTexturesNativeLootAndBilingualNames()throws Exception {
        for(var tier:TIERS)for(var kind:List.of("collector","relay","container","pedestal")) {
            String name=tier+"energy_"+kind;
            var state=read("assets/abyssalport/blockstates/"+name+".json"); var variants=state.getAsJsonObject("variants");
            if(kind.equals("relay"))assertEquals(6,variants.size());
            if(kind.equals("pedestal")){assertEquals(2,variants.size());assertTrue(variants.getAsJsonObject("tilted=true").get("model").getAsString().endsWith("_tilted"));}
            var model=read("assets/abyssalport/models/block/"+name+".json"); assertTrue(model.getAsJsonArray("elements").size()>3);
            for(var texture:model.getAsJsonObject("textures").entrySet()) {
                var ref=texture.getValue().getAsString();if(ref.startsWith("#"))continue;
                assertTrue(ref.startsWith("abyssalport:block/"),"Original media or unowned texture in tier: "+name);
                var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/"+ref.substring("abyssalport:".length())+".png").toFile());
                assertEquals(16,image.getWidth());assertEquals(16,image.getHeight());
            }
            assertEquals("abyssalport:block/"+name,read("assets/abyssalport/models/item/"+name+".json").get("parent").getAsString());
            var loot=read("data/abyssalport/loot_table/blocks/"+name+".json").getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("abyssalport:"+name,loot.get("name").getAsString());assertTrue(loot.toString().contains("PotEnergy"));
            for(var locale:List.of("en_us","de_de"))assertTrue(read("assets/abyssalport/lang/"+locale+".json").has("block.abyssalport."+name));
        }
        assertFalse(Files.exists(ROOT.resolve("assets/abyssalport/blockstates/omothol_energy_collector.json")),"Unconnected Omothol tier disguised as finished");
    }
    @Test void realCollectorRelayRecipesAndUnlocksUseMonolithRatherThanPrototypeDarkstone()throws Exception {
        for(var kind:List.of("collector","relay")) {
            var craft=read("data/abyssalport/recipe/energy_"+kind+".json");
            assertEquals(kind.equals("collector")?"[\"###\",\"#&#\",\"###\"]":"[\"##%\",\"#&#\",\"##%\"]",craft.getAsJsonArray("pattern").toString());
            assertEquals("abyssalport:monolith_stone",craft.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
            assertEquals("abyssalport:shadow_gem",craft.getAsJsonObject("key").getAsJsonObject("&").get("item").getAsString());
            assertEquals("[\"abyssalport:energy_"+kind+"\"]",read("data/abyssalport/advancement/recipes/energy_"+kind+".json").getAsJsonObject("rewards").getAsJsonArray("recipes").toString());
        }
    }
    @Test void realPedestalReservoirAndAllSixRingInfusionsUseSourceValuesWithoutPrototypeBypass()throws Exception {
        var craft=read("data/abyssalport/recipe/energy_pedestal.json");assertEquals("[\"#%#\",\"#&#\",\"###\"]",craft.getAsJsonArray("pattern").toString());
        assertEquals("abyssalport:monolith_stone",craft.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
        assertEquals("abyssalport:coralium_pearl",craft.getAsJsonObject("key").getAsJsonObject("%").get("item").getAsString());
        assertEquals("abyssalport:shadow_gem",craft.getAsJsonObject("key").getAsJsonObject("&").get("item").getAsString());
        assertEquals("neoforge:false",read("data/abyssalport/recipe/energy_container.json").getAsJsonArray("neoforge:conditions").get(0).getAsJsonObject().get("type").getAsString());
        var ritual=read("data/abyssalport/rituals/energy_container.json");assertEquals(100,ritual.get("energy_cost").getAsInt());
        assertTrue(ritual.get("preserve_energy").getAsBoolean());assertFalse(ritual.has("preserve_components"));
        assertEquals("abyssalport:energy_pedestal",ritual.get("altar_input").getAsString());
        var fingerprints=new HashSet<Integer>();
        for(int index=0;index<TIERS.size();index++) {
            String ring=TIERS.get(index)+"embossed_ring";
            var image=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/"+ring+".png").toFile());
            assertEquals(16,image.getWidth());assertEquals(16,image.getHeight());
            int[] rgba=image.getRGB(0,0,16,16,null,0,16);assertTrue(fingerprints.add(Arrays.hashCode(rgba)));assertTrue(Arrays.stream(rgba).anyMatch(value->value>>>24==0));
            for(int old=0;old<index;old++) {
                var infusion=read("data/abyssalport/rituals/"+ring+"_from_"+TIERS.get(old)+"embossed_ring.json");
                assertEquals(400,infusion.get("energy_cost").getAsInt());assertEquals(index-1,infusion.get("book_tier").getAsInt());
                assertEquals(20,infusion.get("duration_ticks").getAsInt());assertEquals(0,infusion.get("altar_tier").getAsInt());assertFalse(infusion.has("dimension"));
            }
        }
    }
    @Test void actualParticleSpriteAndGuideDescribeOnlyConnectedSystems()throws Exception {
        assertEquals("[\"abyssalport:pe_stream\"]",read("assets/abyssalport/particles/pe_stream.json").getAsJsonArray("textures").toString());
        var sprite=ImageIO.read(ROOT.resolve("assets/abyssalport/textures/particle/pe_stream.png").toFile());assertEquals(8,sprite.getWidth());assertEquals(8,sprite.getHeight());
        int[] pixels=sprite.getRGB(0,0,8,8,null,0,8);assertTrue(Arrays.stream(pixels).anyMatch(pixel->pixel>>>24==0));assertTrue(Arrays.stream(pixels).anyMatch(pixel->pixel>>>24==255));
        var ui=Files.readString(Path.of("src/main/java/dev/abyssalport/client/NecronomiconScreen.java"));
        for(var key:List.of("screen.abyssalport.energy_pedestal","screen.abyssalport.energy_container_ritual")) {
            assertTrue(ui.contains(key),"Guide text must be reachable, not just localized");
            for(var locale:List.of("en_us","de_de"))assertTrue(read("assets/abyssalport/lang/"+locale+".json").get(key).getAsString().length()>120);
        }
    }
}
