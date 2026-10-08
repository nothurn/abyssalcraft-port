package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class EnergyDepositionerAssetsTest {
    private static JsonObject read(String path) throws Exception { return JsonParser.parseString(Files.readString(Path.of("src/main/resources").resolve(path))).getAsJsonObject(); }
    @Test void nativeSourceRecipePersistentLootAndOwnedGeometryAreConnected() throws Exception {
        var craft = read("data/abyssalport/recipe/energy_depositioner.json"); assertEquals("[\"###\",\"%&%\",\"#@#\"]", craft.getAsJsonArray("pattern").toString());
        assertEquals("abyssalport:energy_relay", craft.getAsJsonObject("key").getAsJsonObject("@").get("item").getAsString());
        assertEquals("abyssalport:shadow_gem", craft.getAsJsonObject("key").getAsJsonObject("%").get("item").getAsString());
        var loot = read("data/abyssalport/loot_table/blocks/energy_depositioner.json"); var ops = loot.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonArray("ops");
        var fields = new HashSet<String>(); ops.forEach(e -> fields.add(e.getAsJsonObject().get("source").getAsString()));
        assertEquals(Set.of("PotEnergy", "Items", "ProcessingStack", "ProcessingTime", "Tolerance", "ActiveAmplifier"), fields);
        assertEquals(8, read("assets/abyssalport/blockstates/energy_depositioner.json").getAsJsonObject("variants").size());
        assertEquals(7, read("assets/abyssalport/models/block/energy_depositioner_active.json").getAsJsonArray("elements").size());
        for (String shape : List.of("darkstone_slab", "darkstone_cobblestone_slab", "darkstone_cobblestone_stairs", "darkstone_cobblestone_wall")) {
            assertNotNull(read("assets/abyssalport/blockstates/" + shape + ".json")); assertNotNull(read("data/abyssalport/recipe/" + shape + ".json")); assertNotNull(read("data/abyssalport/loot_table/blocks/" + shape + ".json"));
        }
    }
    @Test void safetyGuideAndScreensUseBothLanguagesWithoutCopiedUpstreamMedia() throws Exception {
        for (String locale : List.of("en_us", "de_de")) {
            var lang = read("assets/abyssalport/lang/" + locale + ".json");
            assertTrue(lang.get("screen.abyssalport.energy_depositioner.guide").getAsString().length() > 1000);
            assertFalse(lang.get("screen.abyssalport.state_transformer.guide").getAsString().contains("not yet implemented"));
            assertFalse(lang.get("screen.abyssalport.state_transformer.guide").getAsString().contains("noch nicht implementiert"));
        }
        assertTrue(Files.readString(Path.of("src/main/java/dev/abyssalport/client/AbyssalPortClient.java")).contains("EnergyDepositionerScreen::new"));
        assertFalse(Files.exists(Path.of("src/main/resources/assets/abyssalcraft/textures/gui/container/energydepositioner.png")));
    }
}
