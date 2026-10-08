package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class WastelandMobsAssetsTest {
    @Test void independentHumanoidSkinHasOpaqueGeometryFacesAndTransparentOverlay() throws Exception {
        var root = Path.of("src/main/resources/assets/abyssalport");
        var image = ImageIO.read(root.resolve("textures/entity/abyssal_zombie.png").toFile());
        assertEquals(64, image.getWidth()); assertEquals(64, image.getHeight());
        for (var point : new int[][]{{8, 8}, {24, 24}, {4, 24}, {44, 24}, {20, 56}, {36, 56}}) assertEquals(255, image.getRGB(point[0], point[1]) >>> 24);
        assertEquals(0, image.getRGB(40, 8) >>> 24);
        for (var name : new String[]{"coralium_plagued_flesh", "coralium_chunk"}) {
            var model = JsonParser.parseString(Files.readString(root.resolve("models/item/"+name+".json"))).getAsJsonObject();
            assertEquals("abyssalport:item/"+name, model.getAsJsonObject("textures").get("layer0").getAsString());
            for (var locale : new String[]{"en_us", "de_de"}) assertTrue(JsonParser.parseString(Files.readString(root.resolve("lang/"+locale+".json"))).getAsJsonObject().has("item.abyssalport."+name));
        }
    }
}
