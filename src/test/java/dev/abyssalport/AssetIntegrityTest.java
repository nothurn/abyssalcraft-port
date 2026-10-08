package dev.abyssalport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AssetIntegrityTest {
    @Test
    void abandonedMonolithTargetsGroundBelowLeafCanopies() throws Exception {
        String path = "/data/abyssalport/worldgen/placed_feature/abandoned_monolith.json";
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing abandoned monolith placement data");
            String placement = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(placement.contains("\"heightmap\": \"MOTION_BLOCKING_NO_LEAVES\""),
                    "A leaf canopy must not move the monolith candidate above solid ground");
        }
    }

    @Test
    void peDevicesHaveModelsItemsLootRecipesAndTranslations() throws Exception {
        for (String device : new String[] {"energy_collector", "energy_relay", "energy_container"}) {
            String[] resources = {
                    "/assets/abyssalport/blockstates/" + device + ".json",
                    "/assets/abyssalport/models/block/" + device + ".json",
                    "/assets/abyssalport/models/item/" + device + ".json",
                    "/data/abyssalport/loot_table/blocks/" + device + ".json",
                    "/data/abyssalport/recipe/" + device + ".json"
            };
            for (String resource : resources) {
                try (InputStream stream = getClass().getResourceAsStream(resource)) {
                    assertNotNull(stream, "Missing PE resource: " + resource);
                    assertTrue(stream.readAllBytes().length > 20, "Empty PE resource: " + resource);
                }
            }
        }
        for (String language : new String[] {"en_us", "de_de"}) {
            try (InputStream stream = getClass().getResourceAsStream(
                    "/assets/abyssalport/lang/" + language + ".json")) {
                assertNotNull(stream);
                String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("block.abyssalport.energy_collector"));
                assertTrue(json.contains("block.abyssalport.energy_relay"));
                assertTrue(json.contains("block.abyssalport.energy_container"));
                assertTrue(json.contains("screen.abyssalport.page.5"));
            }
        }
    }

    @Test
    void peDeviceInventoryModelsUseDistinctOriginalBlockTextures() throws Exception {
        String[] textures = {
                "pe_stone", "pe_carved", "pe_crystal", "pe_collector_panel",
                "pe_relay_front", "pe_relay_side", "pe_relay_top",
                "pe_container_front", "pe_container_side", "pe_container_top", "pe_trim"
        };
        for (String texture : textures) {
            String resource = "/assets/abyssalport/textures/block/" + texture + ".png";
            try (InputStream stream = getClass().getResourceAsStream(resource)) {
                assertNotNull(stream, "Missing PE texture: " + resource);
                var image = ImageIO.read(stream);
                assertNotNull(image, "Unreadable PE texture: " + resource);
                assertEquals(16, image.getWidth(), "PE texture width: " + resource);
                assertEquals(16, image.getHeight(), "PE texture height: " + resource);
            }
        }
        String[] devices = {"energy_collector", "energy_relay", "energy_container"};
        String[] distinctFaces = {"pe_crystal", "pe_relay_front", "pe_container_front"};
        for (int index = 0; index < devices.length; index++) {
            String device = devices[index];
            String blockPath = "/assets/abyssalport/models/block/" + device + ".json";
            try (InputStream stream = getClass().getResourceAsStream(blockPath)) {
                assertNotNull(stream, "Missing PE block model: " + blockPath);
                String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("abyssalport:block/" + distinctFaces[index]),
                        "PE block model needs its distinct visual face: " + device);
                assertTrue(json.contains("\"elements\""), "PE model must remain 3D: " + device);
                assertTrue(json.contains("\"uv\""), "PE details need deliberate full-face UVs: " + device);
            }
            String itemPath = "/assets/abyssalport/models/item/" + device + ".json";
            try (InputStream stream = getClass().getResourceAsStream(itemPath)) {
                assertNotNull(stream, "Missing PE item model: " + itemPath);
                String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("abyssalport:block/" + device),
                        "Inventory item must render its block model: " + device);
                assertTrue(json.contains("\"gui\""), "Inventory item needs a GUI camera: " + device);
            }
        }
    }

    @Test
    void necronomiconUsesAnAtlasSafeThreeDimensionalItemModel() throws Exception {
        for (String texture : new String[] {"cover", "pages", "spine", "clasp"}) {
            String resource = "/assets/abyssalport/textures/item/necronomicon_" + texture + ".png";
            try (InputStream stream = getClass().getResourceAsStream(resource)) {
                assertNotNull(stream, "Missing book texture: " + resource);
                var image = ImageIO.read(stream);
                assertNotNull(image, "Unreadable book texture: " + resource);
                assertEquals(16, image.getWidth(), "Book texture width: " + resource);
                assertEquals(16, image.getHeight(), "Book texture height: " + resource);
            }
        }
        try (InputStream stream = getClass().getResourceAsStream(
                "/assets/abyssalport/models/item/necronomicon.json")) {
            assertNotNull(stream, "Missing Necronomicon item model");
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"elements\""), "Book should be three-dimensional");
            assertTrue(json.contains("\"gui\""), "Book needs a hotbar transform");
            assertTrue(json.contains("\"firstperson_righthand\""), "Book needs a held transform");
            for (String hand : new String[] {"firstperson_righthand", "firstperson_lefthand"}) {
                assertTrue(Pattern.compile("\\\"" + hand
                        + "\\\"\\s*:\\s*\\{\\s*\\\"rotation\\\"\\s*:\\s*\\[\\s*0\\s*,\\s*180\\s*,\\s*0\\s*\\]")
                        .matcher(json).find(), "Keep the chosen held-book orientation in " + hand);
            }
            for (String texture : new String[] {"cover", "pages", "spine", "clasp"}) {
                assertTrue(json.contains("abyssalport:item/necronomicon_" + texture),
                        "Book model needs its own " + texture + " texture");
            }
        }
    }

    @Test
    void shoggothSkinIsPackagedAtMipmapSafeSize() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(
                "/assets/abyssalport/textures/entity/lesser_shoggoth.png")) {
            assertNotNull(stream, "Shoggoth texture must be in the packaged resources");
            var image = ImageIO.read(stream);
            assertNotNull(image, "Shoggoth texture must be a readable PNG");
            assertEquals(128, image.getWidth());
            assertEquals(128, image.getHeight());
        }
    }

    @Test
    void shadowMaterialChainHasItsOwnAtlasSafeAssets() throws Exception {
        for (String material : new String[] {"shadow_fragment", "shadow_shard", "shadow_gem"}) {
            String texture = "/assets/abyssalport/textures/item/" + material + ".png";
            try (InputStream stream = getClass().getResourceAsStream(texture)) {
                assertNotNull(stream, "Missing Shadow material texture: " + texture);
                var image = ImageIO.read(stream);
                assertNotNull(image);
                assertEquals(16, image.getWidth());
                assertEquals(16, image.getHeight());
                assertEquals(0, image.getRGB(0, 0) >>> 24, "Item backdrop must be transparent");
            }
            String model = "/assets/abyssalport/models/item/" + material + ".json";
            try (InputStream stream = getClass().getResourceAsStream(model)) {
                assertNotNull(stream, "Missing Shadow material item model: " + model);
                assertTrue(new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                        .contains("abyssalport:item/" + material));
            }
        }
        try (InputStream stream = getClass().getResourceAsStream(
                "/assets/abyssalport/textures/entity/shadow_creature.png")) {
            assertNotNull(stream, "Shadow Creature must have its own skin");
            var image = ImageIO.read(stream);
            assertNotNull(image);
            assertEquals(64, image.getWidth());
            assertEquals(64, image.getHeight());
        }
        for (String resource : new String[] {
                "/data/abyssalport/neoforge/biome_modifier/shadow_creature_spawns.json",
                "/data/abyssalport/loot_table/entities/shadow_creature.json",
                "/data/abyssalport/recipe/shadow_shard.json",
                "/data/abyssalport/recipe/shadow_gem.json",
                "/data/abyssalport/recipe/shadow_shard_from_gem.json",
                "/data/abyssalport/recipe/shadow_fragment_from_shard.json"}) {
            try (InputStream stream = getClass().getResourceAsStream(resource)) {
                assertNotNull(stream, "Missing Shadow progression data: " + resource);
            }
        }
    }

    @Test
    void shadowGroveHasCompleteBlockAndWorldgenAssets() throws Exception {
        for (String face : new String[] {"top", "side"}) {
            String path = "/assets/abyssalport/textures/block/darklands_soil_" + face + ".png";
            try (InputStream stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, "Missing Shadowed Soil texture: " + path);
                var image = ImageIO.read(stream);
                assertNotNull(image);
                assertEquals(16, image.getWidth());
                assertEquals(16, image.getHeight());
            }
        }
        for (String path : new String[] {
                "/assets/abyssalport/blockstates/darklands_soil.json",
                "/assets/abyssalport/models/block/darklands_soil.json",
                "/assets/abyssalport/models/item/darklands_soil.json",
                "/data/abyssalport/loot_table/blocks/darklands_soil.json",
                "/data/abyssalport/worldgen/configured_feature/shadow_grove.json",
                "/data/abyssalport/worldgen/placed_feature/shadow_grove.json",
                "/data/abyssalport/neoforge/biome_modifier/shadow_grove.json"}) {
            try (InputStream stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, "Missing Shadow grove asset: " + path);
                assertTrue(stream.readAllBytes().length > 20, "Empty Shadow grove asset: " + path);
            }
        }
    }

    @Test
    void darklandsBiomeShipsWithItsRequiredWorldgenDependency() throws Exception {
        for (String path : new String[] {
                "/data/abyssalport/worldgen/biome/darklands.json",
                "/data/abyssalport/worldgen/configured_feature/ore_darkstone.json",
                "/data/abyssalport/worldgen/placed_feature/ore_darkstone.json",
                "/data/minecraft/tags/block/dirt.json",
                "/data/minecraft/tags/block/animals_spawnable_on.json",
                "/data/minecraft/tags/block/mineable/shovel.json"}) {
            try (InputStream stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, "Missing Darklands data: " + path);
                assertTrue(stream.readAllBytes().length > 20, "Empty Darklands data: " + path);
            }
        }
        try (InputStream stream = getClass().getResourceAsStream(
                "/data/abyssalport/worldgen/configured_feature/ore_darkstone.json")) {
            assertNotNull(stream);
            String ore = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(ore.contains("\"predicate_type\": \"minecraft:block_match\""));
            assertTrue(ore.contains("\"block\": \"minecraft:stone\""));
        }
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/neoforge.mods.toml")) {
            assertNotNull(stream, "The shipped mod needs dependency metadata");
            String metadata = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(Pattern.compile("modId=\"terrablender\"\\s*type=\"required\"\\s*"
                    + "versionRange=\"\\[4\\.1\\.0\\.8,5\\)\"").matcher(metadata).find(),
                    "Normal installations must require the compatible TerraBlender version");
        }
    }

    @Test
    void darkstoneProcessingShipsWithBlockModelsLootAndFurnaceRecipe() throws Exception {
        for (String path : new String[] {
                "/assets/abyssalport/blockstates/darkstone_cobblestone.json",
                "/assets/abyssalport/models/block/darkstone_cobblestone.json",
                "/assets/abyssalport/models/item/darkstone_cobblestone.json",
                "/data/abyssalport/loot_table/blocks/darkstone.json",
                "/data/abyssalport/loot_table/blocks/darkstone_cobblestone.json",
                "/data/abyssalport/recipe/darkstone_from_cobblestone.json"}) {
            try (InputStream stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, "Missing Darkstone processing resource: " + path);
                assertTrue(stream.readAllBytes().length > 20, "Empty Darkstone processing resource: " + path);
            }
        }
        for (String language : new String[] {"en_us", "de_de"}) {
            try (InputStream stream = getClass().getResourceAsStream(
                    "/assets/abyssalport/lang/" + language + ".json")) {
                assertNotNull(stream);
                assertTrue(new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                        .contains("block.abyssalport.darkstone_cobblestone"));
            }
        }
    }

    @Test
    void darkstoneVariantsUseDistinctOriginalOpaqueTextures() throws Exception {
        int[] firstPixels = null;
        for (String material : new String[] {"darkstone", "darkstone_cobblestone",
                "darkstone_bricks", "cracked_darkstone_bricks", "chiseled_darkstone_bricks"}) {
            String texture = "/assets/abyssalport/textures/block/" + material + ".png";
            int[] pixels = new int[16 * 16];
            try (InputStream stream = getClass().getResourceAsStream(texture)) {
                assertNotNull(stream, "Missing Darkstone texture: " + texture);
                var image = ImageIO.read(stream);
                assertNotNull(image, "Unreadable Darkstone texture: " + texture);
                assertEquals(16, image.getWidth());
                assertEquals(16, image.getHeight());
                image.getRGB(0, 0, 16, 16, pixels, 0, 16);
            }
            for (int pixel : pixels) {
                assertEquals(255, pixel >>> 24, "Block texture must be opaque: " + texture);
            }
            if (firstPixels != null) {
                assertTrue(!java.util.Arrays.equals(firstPixels, pixels),
                        "Adjacent Darkstone variants must have distinct textures");
            }
            firstPixels = pixels;
            try (InputStream stream = getClass().getResourceAsStream(
                    "/assets/abyssalport/models/block/" + material + ".json")) {
                assertNotNull(stream);
                String model = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(model.contains("abyssalport:block/" + material),
                        "The block model must use its own texture: " + material);
            }
        }
    }

    @Test
    void darkstoneMasonryShipsCompleteBlockAssetsAndRecipes() throws Exception {
        for (String material : new String[] {"darkstone_bricks", "cracked_darkstone_bricks",
                "darkstone_brick_slab", "darkstone_brick_stairs", "chiseled_darkstone_bricks"}) {
            for (String path : new String[] {
                    "/assets/abyssalport/blockstates/" + material + ".json",
                    "/assets/abyssalport/models/block/" + material + ".json",
                    "/assets/abyssalport/models/item/" + material + ".json",
                    "/data/abyssalport/loot_table/blocks/" + material + ".json",
                    "/data/abyssalport/recipe/" + material + ".json"}) {
                try (InputStream stream = getClass().getResourceAsStream(path)) {
                    assertNotNull(stream, "Missing Darkstone masonry asset: " + path);
                    assertTrue(stream.readAllBytes().length > 20, "Empty masonry asset: " + path);
                }
            }
            for (String language : new String[] {"en_us", "de_de"}) {
                try (InputStream stream = getClass().getResourceAsStream(
                        "/assets/abyssalport/lang/" + language + ".json")) {
                    assertNotNull(stream);
                    assertTrue(new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                            .contains("block.abyssalport." + material));
                }
            }
        }
        for (String path : new String[] {
                "/assets/abyssalport/models/block/darkstone_brick_slab_top.json",
                "/assets/abyssalport/models/block/darkstone_brick_stairs_inner.json",
                "/assets/abyssalport/models/block/darkstone_brick_stairs_outer.json"}) {
            try (InputStream stream = getClass().getResourceAsStream(path)) {
                assertNotNull(stream, "Missing Darkstone shape model: " + path);
            }
        }
        try (InputStream stream = getClass().getResourceAsStream(
                "/assets/abyssalport/blockstates/darkstone_brick_stairs.json")) {
            assertNotNull(stream);
            String states = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(40, states.split("facing=", -1).length - 1,
                    "Every vanilla stair state needs its own model mapping");
        }
    }

    @Test
    void oozeModelsUseAStitchedBlockTexture() throws Exception {
        String texture = "/assets/abyssalport/textures/block/shoggoth_ooze.png";
        try (InputStream stream = getClass().getResourceAsStream(texture)) {
            assertNotNull(stream, "Ooze needs its own block-atlas texture");
            var image = ImageIO.read(stream);
            assertNotNull(image, "Ooze texture must be a readable PNG");
            assertEquals(128, image.getWidth());
            assertEquals(128, image.getHeight());
        }
        for (int layers = 1; layers <= 8; layers++) {
            String model = "/assets/abyssalport/models/block/shoggoth_ooze_" + layers + ".json";
            try (InputStream stream = getClass().getResourceAsStream(model)) {
                assertNotNull(stream, "Missing ooze model for layer " + layers);
                String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("abyssalport:block/shoggoth_ooze"),
                        "Ooze layer " + layers + " must reference the stitched block texture");
            }
        }
    }
}
