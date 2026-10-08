package dev.abyssalport;

import dev.abyssalport.content.AbyssalniteContent;
import dev.abyssalport.content.CoraliumContent;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandMineralsContent;
import dev.abyssalport.item.RefinedCoraliumArmorEffects;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.research.RefinedCoraliumResearch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastelandMineralsGameTests {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final List<String> WASTELAND_ORES = List.of("abyssal_nitre_ore", "abyssal_coralium_ore", "abyssal_iron_ore",
            "abyssal_gold_ore", "abyssal_diamond_ore", "liquified_coralium_ore", "pearlescent_coralium_ore");
    private WastelandMineralsGameTests() { }

    @GameTest(template = "empty", batch = "wasteland_mining")
    public static void miningTiersAndRealOreLootCannotSkipProgression(GameTestHelper helper) {
        var liquified = WastelandMineralsContent.LIQUIFIED_CORALIUM_ORE.get().defaultBlockState();
        var pearlescent = WastelandMineralsContent.PEARLESCENT_CORALIUM_ORE.get().defaultBlockState();
        for (var tool : List.of(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE)) {
            helper.assertTrue(!new ItemStack(tool).isCorrectToolForDrops(liquified) && !new ItemStack(tool).isCorrectToolForDrops(pearlescent), "Vanilla tool bypassed a higher ore tier: "+tool);
        }
        var abyssalnite = new ItemStack(AbyssalniteContent.PICKAXE.get());
        var refined = new ItemStack(WastelandMineralsContent.PICKAXE.get());
        helper.assertTrue(abyssalnite.isCorrectToolForDrops(liquified) && !abyssalnite.isCorrectToolForDrops(pearlescent)
                && refined.isCorrectToolForDrops(pearlescent), "Abyssalnite -> Refined Coralium mining chain is broken");
        helper.assertTrue(!new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(WastelandMineralsContent.ABYSSAL_CORALIUM_ORE.get().defaultBlockState())
                && new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(WastelandMineralsContent.ABYSSAL_CORALIUM_ORE.get().defaultBlockState()), "Abyssal Coralium is not diamond-tier");
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var blocks = helper.getLevel().registryAccess().registryOrThrow(Registries.BLOCK);
        var silk = new ItemStack(WastelandMineralsContent.PICKAXE.get());
        silk.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        var fortune = refined.copy();
        fortune.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 3);
        Map<String, Item> outputs = Map.of("nitre_ore", WastelandMineralsContent.NITRE.get(), "abyssal_nitre_ore", WastelandMineralsContent.NITRE.get(),
                "abyssal_coralium_ore", CoraliumContent.GEM.get(), "abyssal_diamond_ore", Items.DIAMOND, "pearlescent_coralium_ore", CoraliumContent.PEARL.get());
        for (var entry : outputs.entrySet()) {
            var block = blocks.get(id(entry.getKey()));
            var state = block.defaultBlockState();
            var silkDrops = Block.getDrops(state, helper.getLevel(), pos, null, null, silk);
            helper.assertTrue(silkDrops.size() == 1 && silkDrops.getFirst().is(block.asItem()) && silkDrops.getFirst().getCount() == 1, "Silk Touch did not preserve "+entry.getKey());
            int plainMax = entry.getKey().contains("nitre") || entry.getKey().equals("abyssal_coralium_ore") ? 3 : entry.getKey().contains("pearlescent") ? 2 : 1;
            boolean fortuneObserved = false;
            for (int sample = 0; sample < 64; sample++) {
                var drops = Block.getDrops(state, helper.getLevel(), pos, null, null, refined);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(entry.getValue()) && drops.getFirst().getCount() >= 1
                        && drops.getFirst().getCount() <= plainMax, "Incorrect ordinary ore drop: "+entry.getKey());
                var bonus = Block.getDrops(state, helper.getLevel(), pos, null, null, fortune);
                helper.assertTrue(bonus.size() == 1 && bonus.getFirst().is(entry.getValue()) && bonus.getFirst().getCount() <= plainMax*4, "Fortune yield exceeded source bound");
                fortuneObserved |= bonus.getFirst().getCount() > plainMax;
            }
            helper.assertTrue(fortuneObserved, "Fortune never improved ore output: "+entry.getKey());
        }
        var drops = Block.getDrops(liquified, helper.getLevel(), pos, null, null, abyssalnite);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(WastelandMineralsContent.LIQUIFIED_CORALIUM_ORE_ITEM.get()), "Liquified ore must drop itself before smelting");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "wasteland_equipment")
    public static void smeltingEquipmentCompressionAndRepairsUseRealRecipes(GameTestHelper helper) {
        Map<Item, Item> smelting = Map.of(WastelandMineralsContent.LIQUIFIED_CORALIUM_ORE_ITEM.get(), WastelandMineralsContent.INGOT.get(),
                WastelandMineralsContent.PEARLESCENT_CORALIUM_ORE_ITEM.get(), CoraliumContent.PEARL.get(),
                WastelandMineralsContent.ABYSSAL_NITRE_ORE_ITEM.get(), WastelandMineralsContent.NITRE.get(),
                WastelandMineralsContent.NITRE_ORE_ITEM.get(), WastelandMineralsContent.NITRE.get(),
                WastelandMineralsContent.ABYSSAL_IRON_ORE_ITEM.get(), Items.IRON_INGOT, WastelandMineralsContent.ABYSSAL_GOLD_ORE_ITEM.get(), Items.GOLD_INGOT,
                WastelandMineralsContent.ABYSSAL_DIAMOND_ORE_ITEM.get(), Items.DIAMOND, WastelandMineralsContent.ABYSSAL_CORALIUM_ORE_ITEM.get(), CoraliumContent.GEM.get());
        for (var entry : smelting.entrySet()) {
            var input = new SingleRecipeInput(new ItemStack(entry.getKey()));
            var furnace = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, helper.getLevel()).orElseThrow();
            var blast = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.BLASTING, input, helper.getLevel()).orElseThrow();
            helper.assertTrue(furnace.value().assemble(input, helper.getLevel().registryAccess()).is(entry.getValue()) && furnace.value().getCookingTime() == 200
                    && blast.value().assemble(input, helper.getLevel().registryAccess()).is(entry.getValue()) && blast.value().getCookingTime() == 100, "Smelting route did not load");
            if (entry.getKey() == WastelandMineralsContent.LIQUIFIED_CORALIUM_ORE_ITEM.get()) helper.assertTrue(furnace.value().getExperience() == 3, "Refining XP differs from original");
        }
        craft(helper, "pickaxe", new String[]{"III", " S ", " S "}, WastelandMineralsContent.PICKAXE.get(), 1);
        craft(helper, "axe", new String[]{"II", "IS", " S"}, WastelandMineralsContent.AXE.get(), 1);
        craft(helper, "shovel", new String[]{"I", "S", "S"}, WastelandMineralsContent.SHOVEL.get(), 1);
        craft(helper, "sword", new String[]{"I", "I", "S"}, WastelandMineralsContent.SWORD.get(), 1);
        craft(helper, "hoe", new String[]{"II", " S", " S"}, WastelandMineralsContent.HOE.get(), 1);
        craft(helper, "helmet", new String[]{"III", "I I"}, WastelandMineralsContent.HELMET.get(), 1);
        craft(helper, "chestplate", new String[]{"I I", "III", "III"}, WastelandMineralsContent.CHESTPLATE.get(), 1);
        craft(helper, "leggings", new String[]{"III", "I I", "I I"}, WastelandMineralsContent.LEGGINGS.get(), 1);
        craft(helper, "boots", new String[]{"I I", "I I"}, WastelandMineralsContent.BOOTS.get(), 1);
        craft(helper, "block", new String[]{"III", "III", "III"}, WastelandMineralsContent.BLOCK_ITEM.get(), 1);
        craft(helper, "ingot_from_block", new String[]{"B"}, WastelandMineralsContent.INGOT.get(), 9);
        craft(helper, "nugget", new String[]{"I"}, WastelandMineralsContent.NUGGET.get(), 9);
        craft(helper, "ingot_from_nuggets", new String[]{"NNN", "NNN", "NNN"}, WastelandMineralsContent.INGOT.get(), 1);
        var pick = new ItemStack(WastelandMineralsContent.PICKAXE.get());
        helper.assertTrue(pick.getMaxDamage() == 1800 && pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 12
                && pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Refined tool values are incorrect");
        helper.assertTrue(WastelandMineralsContent.HELMET.get().getDefense() == 3 && WastelandMineralsContent.CHESTPLATE.get().getDefense() == 8
                && WastelandMineralsContent.LEGGINGS.get().getDefense() == 6 && WastelandMineralsContent.BOOTS.get().getDefense() == 3
                && WastelandMineralsContent.CHESTPLATE.get().getToughness() == 1 && new ItemStack(WastelandMineralsContent.CHESTPLATE.get()).getMaxDamage() == 16*37, "Refined armor values are incorrect");
        var player = helper.makeMockServerPlayerInLevel();
        for (var item : List.of(WastelandMineralsContent.PICKAXE.get(), WastelandMineralsContent.CHESTPLATE.get())) {
            var damaged = new ItemStack(item); damaged.setDamageValue(400);
            var anvil = new AnvilMenu(1, player.getInventory());
            anvil.getSlot(0).set(damaged); anvil.getSlot(1).set(new ItemStack(WastelandMineralsContent.INGOT.get()));
            helper.assertTrue(!anvil.getSlot(2).getItem().isEmpty() && anvil.getSlot(2).getItem().getDamageValue() < 400, "Refined ingot cannot repair equipment");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "wasteland_armor")
    public static void armorBuffsExpireAndRefinedResearchIsPlayerScoped(GameTestHelper helper) {
        var wearer = helper.makeMockServerPlayerInLevel(); var other = helper.makeMockServerPlayerInLevel();
        wearer.setData(AbyssalPort.EARLY_RESEARCH.get(), 0); other.setData(AbyssalPort.EARLY_RESEARCH.get(), 0);
        wearer.getInventory().setItem(0, new ItemStack(WastelandMineralsContent.HELMET.get()));
        RefinedCoraliumArmorEffects.apply(wearer);
        helper.assertTrue(!wearer.hasEffect(MobEffects.WATER_BREATHING), "Inventory armor activated buffs");
        wearer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(WastelandMineralsContent.HELMET.get()));
        wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(WastelandMineralsContent.CHESTPLATE.get()));
        wearer.setItemSlot(EquipmentSlot.FEET, new ItemStack(WastelandMineralsContent.BOOTS.get()));
        RefinedCoraliumArmorEffects.apply(wearer);
        helper.assertTrue(!RefinedCoraliumResearch.discover(wearer, new ItemStack(CoraliumContent.GEM.get())), "Unrefined material unlocked the refined chapter");
        helper.assertTrue(RefinedCoraliumResearch.discover(wearer, new ItemStack(WastelandMineralsContent.PICKAXE.get()))
                && !RefinedCoraliumResearch.discover(wearer, new ItemStack(WastelandMineralsContent.INGOT.get())) && !EarlyResearch.has(other, EarlyResearch.REFINED_CORALIUM), "Refined discovery leaked or repeated");
        for (var effect : List.of(MobEffects.WATER_BREATHING, MobEffects.DAMAGE_RESISTANCE, MobEffects.MOVEMENT_SPEED)) {
            helper.assertTrue(wearer.hasEffect(effect) && wearer.getEffect(effect).getDuration() == 20 && !other.hasEffect(effect), "Equipped buff is missing or leaks");
        }
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET)) wearer.setItemSlot(slot, ItemStack.EMPTY);
        for (int tick = 0; tick < 25; tick++) {
            if (tick % 10 == 0) RefinedCoraliumArmorEffects.apply(wearer);
            for (var effect : List.of(MobEffects.WATER_BREATHING, MobEffects.DAMAGE_RESISTANCE, MobEffects.MOVEMENT_SPEED)) {
                var instance = wearer.getEffect(effect);
                if (instance != null && !instance.tick(wearer, () -> { })) wearer.removeEffect(effect);
            }
        }
        helper.assertTrue(wearer.getActiveEffects().isEmpty(), "Unequipped armor effects did not expire");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "wasteland_features", timeoutTicks = 200)
    public static void configuredVeinsLoadAndOnlyReplaceTheirNaturalHost(GameTestHelper helper) {
        var level = helper.getLevel(); var registry = level.registryAccess();
        var configured = registry.registryOrThrow(Registries.CONFIGURED_FEATURE);
        var placed = registry.registryOrThrow(Registries.PLACED_FEATURE);
        var biomes = registry.registryOrThrow(Registries.BIOME);
        var aw = biomes.get(id("abyssal_wasteland")); var lake = biomes.get(id("coralium_lake"));
        var center = helper.absolutePos(new BlockPos(6, 3, 6));
        Map<BlockPos, BlockState> old = new HashMap<>();
        try {
            for (var pos : BlockPos.betweenClosed(center.offset(-7, -5, -7), center.offset(7, 5, 7))) old.put(pos.immutable(), level.getBlockState(pos));
            for (var name : WASTELAND_ORES) {
                var feature = configured.get(id("ore_"+name)); var decoration = placed.get(id("ore_"+name));
                helper.assertTrue(feature != null && aw.getGenerationSettings().hasFeature(decoration) && lake.getGenerationSettings().hasFeature(decoration)
                        && !biomes.getHolderOrThrow(Biomes.PLAINS).value().getGenerationSettings().hasFeature(decoration), "Wrong ore biome: "+name);
                old.keySet().forEach(pos -> level.setBlock(pos, WastelandContent.STONE.get().defaultBlockState(), 2));
                level.setBlock(center, Blocks.STONE.defaultBlockState(), 2); level.setBlock(center.east(), Blocks.CHEST.defaultBlockState(), 2);
                old.keySet().stream().map(net.minecraft.world.level.ChunkPos::new).distinct().forEach(chunk -> Heightmap.primeHeightmaps(level.getChunk(chunk.x, chunk.z), Set.of(Heightmap.Types.OCEAN_FLOOR_WG)));
                var random = RandomSource.create(8103);
                for (int attempt = 0; attempt < 96; attempt++) feature.place(level, level.getChunkSource().getGenerator(), random, center);
                var expected = registry.registryOrThrow(Registries.BLOCK).get(id(name));
                helper.assertTrue(old.keySet().stream().anyMatch(pos -> level.getBlockState(pos).is(expected)), "No ore formed: "+name);
                helper.assertTrue(level.getBlockState(center).is(Blocks.STONE) && level.getBlockState(center.east()).is(Blocks.CHEST), "Ore replaced non-host terrain or a container");
            }
            helper.assertTrue(biomes.getHolderOrThrow(Biomes.PLAINS).value().getGenerationSettings().hasFeature(placed.get(id("ore_nitre_ore")))
                    && !aw.getGenerationSettings().hasFeature(placed.get(id("ore_nitre_ore"))), "Overworld Nitre modifier has wrong scope");
        } finally { old.forEach((pos, state) -> level.setBlock(pos, state, 2)); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "wasteland_natural_ores", timeoutTicks = 1200)
    public static void actualNormalDimensionGeneratesEveryWastelandOre(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION);
        helper.assertTrue(level != null, "Actual Wasteland dimension was not created");
        Map<Block, Integer> counts = new HashMap<>(); Map<Block, int[]> heights = new HashMap<>();
        for (var name : WASTELAND_ORES) {
            var block = level.registryAccess().registryOrThrow(Registries.BLOCK).get(id(name));
            counts.put(block, 0); heights.put(block, new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE});
        }
        // Distant natural chunks, not structure fixtures. This never runs in a user world.
        for (int cx = 96; cx < 100; cx++) for (int cz = 96; cz < 100; cz++) {
            var chunk = level.getChunk(cx, cz);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 85; y++) {
                var block = chunk.getBlockState(new BlockPos(cx*16+x, y, cz*16+z)).getBlock();
                if (counts.containsKey(block)) {
                    counts.put(block, counts.get(block)+1);
                    int[] range = heights.get(block); range[0] = Math.min(range[0], y); range[1] = Math.max(range[1], y);
                }
            }
        }
        for (var entry : counts.entrySet()) {
            var key = level.registryAccess().registryOrThrow(Registries.BLOCK).getKey(entry.getKey()); var range = heights.get(entry.getKey());
            LOGGER.info("Wasteland natural ore sample: {} = {} blocks, Y {}..{}, 16 chunks", key, entry.getValue(), range[0], range[1]);
            helper.assertTrue(entry.getValue() > 0, "No naturally generated "+key+" in sampled chunks");
        }
        helper.succeed();
    }

    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, name); }
    private static void craft(GameTestHelper helper, String suffix, String[] pattern, Item output, int count) {
        var slots = new ArrayList<ItemStack>();
        for (String row : pattern) for (char key : row.toCharArray()) slots.add(switch (key) {
            case 'I' -> new ItemStack(WastelandMineralsContent.INGOT.get()); case 'N' -> new ItemStack(WastelandMineralsContent.NUGGET.get());
            case 'B' -> new ItemStack(WastelandMineralsContent.BLOCK_ITEM.get()); case 'S' -> new ItemStack(Items.STICK); default -> ItemStack.EMPTY;
        });
        var input = CraftingInput.of(pattern[0].length(), pattern.length, slots);
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
        var result = recipe.value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(recipe.id().equals(id("refined_coralium_"+suffix)) && result.is(output) && result.getCount() == count, "Wrong refined recipe: "+suffix);
    }
}
