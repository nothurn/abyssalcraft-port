package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class WastelandBossAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources");
    @Test void summonUsesSevenRealOriginalInspiredOfferingsAndNoItemOutput() throws Exception {
        var ritual = JsonParser.parseString(Files.readString(ROOT.resolve("data/abyssalport/rituals/summon_asorah.json"))).getAsJsonObject();
        assertEquals("summon", ritual.get("outcome").getAsString());
        assertEquals("abyssalport:asorah", ritual.get("entity").getAsString());
        assertEquals(1000, ritual.get("energy_cost").getAsInt()); assertEquals(20, ritual.get("duration_ticks").getAsInt());
        assertEquals(1, ritual.get("book_tier").getAsInt()); assertEquals(1, ritual.get("altar_tier").getAsInt());
        assertEquals("abyssalport:abyssal_wasteland", ritual.get("dimension").getAsString());
        var items = ritual.getAsJsonArray("offerings"); assertEquals(7, items.size());
        assertEquals(4, items.asList().stream().filter(value -> value.getAsString().equals("minecraft:gold_ingot")).count());
        assertFalse(ritual.has("result")); assertFalse(ritual.has("offering_layout"));
        assertFalse(items.asList().stream().anyMatch(value -> value.getAsString().equals("minecraft:air")));
    }
    @Test void everyDragonCubeFaceIsCoveredByOwnAtlasPixels() throws Exception {
        int[][] boxes = {{0,0,12,10,24}, {80,0,8,8,8}, {116,0,10,8,12}, {180,0,3,6,3}, {0,40,10,3,12},
                {80,40,28,1,18}, {80,62,20,1,14}, {0,64,6,6,16}, {0,90,4,4,15}, {180,40,4,8,4}, {200,40,5,2,8}};
        for (String name : new String[]{"asorah", "spectral_dragon"}) {
            var atlas = ImageIO.read(ROOT.resolve("assets/abyssalport/textures/entity/"+name+".png").toFile());
            assertEquals(256, atlas.getWidth()); assertEquals(128, atlas.getHeight());
            for (int[] box : boxes) {
                int u=box[0], v=box[1], w=box[2], h=box[3], d=box[4];
                int[][] faces = {{u+d,v,w,d},{u+d+w,v,w,d},{u,v+d,d,h},{u+d,v+d,w,h},{u+d+w,v+d,d,h},{u+2*d+w,v+d,w,h}};
                for (int[] face : faces) for (int y=face[1];y<face[1]+face[3];y++) for (int x=face[0];x<face[0]+face[2];x++)
                    assertEquals(255, atlas.getRGB(x,y)>>>24, name+" UV "+x+","+y);
            }
        }
        assertEquals(16, ImageIO.read(ROOT.resolve("assets/abyssalport/textures/item/eye_of_the_abyss.png").toFile()).getWidth());
    }
    @Test void naturalHelperLootAndGuideAreConnected() throws Exception {
        for (String biome : new String[]{"abyssal_wasteland", "coralium_lake"}) {
            var data = JsonParser.parseString(Files.readString(ROOT.resolve("data/abyssalport/worldgen/biome/"+biome+".json"))).getAsJsonObject();
            var spawner = data.getAsJsonObject("spawners").getAsJsonArray("monster").asList().stream()
                    .map(value -> value.getAsJsonObject()).filter(value -> value.get("type").getAsString().equals("abyssalport:spectral_dragon")).findFirst().orElseThrow();
            assertEquals(1, spawner.get("weight").getAsInt()); assertEquals(1, spawner.get("minCount").getAsInt());
        }
        var loot = JsonParser.parseString(Files.readString(ROOT.resolve("data/abyssalport/loot_table/entities/asorah.json"))).getAsJsonObject();
        var eye = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals("abyssalport:eye_of_the_abyss", eye.get("name").getAsString());
        assertEquals(1, eye.getAsJsonArray("functions").get(0).getAsJsonObject().get("count").getAsInt());
        for (String locale : new String[]{"en_us", "de_de"}) {
            var text = JsonParser.parseString(Files.readString(ROOT.resolve("assets/abyssalport/lang/"+locale+".json"))).getAsJsonObject();
            for (String key : new String[]{"screen.abyssalport.page.17", "screen.abyssalport.asorah", "screen.abyssalport.asorah_fight",
                    "message.abyssalport.summon_blocked", "research.abyssalport.asorah", "entity.abyssalport.asorah"}) assertTrue(text.has(key), key);
        }
        assertFalse(dev.abyssalport.research.ResearchMilestones.pageAvailable(17, 0));
        assertTrue(dev.abyssalport.research.ResearchMilestones.pageAvailable(17, dev.abyssalport.research.ResearchMilestones.WASTELAND_BOOK));
    }
}
