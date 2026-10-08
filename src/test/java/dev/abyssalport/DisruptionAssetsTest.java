package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;

class DisruptionAssetsTest {
    private static Map<String,String> declarations()throws Exception {
        // JUnit intentionally has no bootstrapped Minecraft registries/codecs.
        // Actual enum initialization/selection is covered by the native GameTest.
        String source=Files.readString(Path.of("src/main/java/dev/abyssalport/energy/DisruptionCatalogue.java"));
        var matcher=Pattern.compile("[A-Z_]+\\(\"([^\"]+)\"(?:,Deity\\.([A-Z_]+))?\\)").matcher(source);
        var result=new LinkedHashMap<String,String>();
        while(matcher.find())assertNull(result.put(matcher.group(1),matcher.group(2)),"Duplicate source ID");
        return result;
    }
    @Test void exactRegisteredCatalogueUsesEighteenCommonAndNineBoundEntries()throws Exception {
        var entries=declarations();assertEquals(27,entries.size());
        assertEquals(18,entries.values().stream().filter(Objects::isNull).count());
        assertEquals(4,entries.values().stream().filter("SHUB_NIGGURATH"::equals).count());
        assertFalse(entries.containsValue("CTHULHU"));
    }
    @Test void everyRealEffectAndBoundedHelpHasBothOwnedLanguageLabels()throws Exception {
        for(String locale:new String[]{"en_us","de_de"}) {
            var json=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/abyssalport/lang/"+locale+".json"))).getAsJsonObject();
            for(var id:declarations().keySet())assertTrue(json.has("disruption.abyssalport."+id)&&!json.get("disruption.abyssalport."+id).getAsString().isBlank());
            assertTrue(json.get("message.abyssalport.disruption").getAsString().contains("%s"));
            assertTrue(json.get("screen.abyssalport.disruptions.guide").getAsString().contains("100"));
        }
    }
}
