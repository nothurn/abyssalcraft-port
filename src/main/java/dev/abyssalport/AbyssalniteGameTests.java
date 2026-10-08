package dev.abyssalport;

import dev.abyssalport.content.AbyssalniteContent;
import dev.abyssalport.item.AbyssalniteArmorEffects;
import dev.abyssalport.research.AbyssalniteResearch;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.worldgen.DarklandsWorldgen;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AbyssalniteGameTests {
    private AbyssalniteGameTests() { }

    @GameTest(template = "empty", batch = "abyssalnite_ore")
    public static void oreFormsInStoneAndOnlyDarklandsHasTheFeature(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess();
        var id = ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "ore_abyssalnite");
        var configured = registry.registryOrThrow(Registries.CONFIGURED_FEATURE).get(id);
        var placed = registry.registryOrThrow(Registries.PLACED_FEATURE).get(id);
        var biomes = registry.registryOrThrow(Registries.BIOME);
        check(configured != null && placed != null, "Abyssalnite worldgen data did not load");
        check(biomes.getHolderOrThrow(DarklandsWorldgen.DARKLANDS).value().getGenerationSettings().hasFeature(placed),
                "Darklands has no Abyssalnite ore");
        check(!biomes.getHolderOrThrow(Biomes.PLAINS).value().getGenerationSettings().hasFeature(placed),
                "Abyssalnite leaked into plains");
        BlockPos center = helper.absolutePos(new BlockPos(7, 2, 7));
        Map<BlockPos, BlockState> original = new HashMap<>();
        try {
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -5, -8), center.offset(8, 5, 8))) {
                original.put(pos.immutable(), helper.getLevel().getBlockState(pos));
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            helper.getLevel().setBlock(center, Blocks.GRANITE.defaultBlockState(), 2);
            helper.getLevel().setBlock(center.east(), Blocks.DIRT.defaultBlockState(), 2);
            original.keySet().stream().map(net.minecraft.world.level.ChunkPos::new).distinct().forEach(chunk ->
                    Heightmap.primeHeightmaps(helper.getLevel().getChunk(chunk.x, chunk.z), Set.of(Heightmap.Types.OCEAN_FLOOR_WG)));
            RandomSource random = RandomSource.create(41);
            for (int attempt = 0; attempt < 64; attempt++) {
                configured.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(), random, center);
            }
            check(original.keySet().stream().anyMatch(pos -> helper.getLevel().getBlockState(pos).is(AbyssalniteContent.ORE.get())),
                    "Configured feature produced no mineable Abyssalnite");
            check(helper.getLevel().getBlockState(center).is(Blocks.GRANITE)
                    && helper.getLevel().getBlockState(center.east()).is(Blocks.DIRT), "Ore replaced non-stone terrain");
        } finally {
            original.forEach((pos, state) -> helper.getLevel().setBlock(pos, state, 2));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "abyssalnite_equipment")
    public static void minedOreSmeltsAndCraftsEveryToolAndArmorPiece(GameTestHelper helper) {
        BlockState ore = AbyssalniteContent.ORE.get().defaultBlockState();
        check(ore.is(BlockTags.MINEABLE_WITH_PICKAXE) && ore.is(BlockTags.NEEDS_IRON_TOOL), "Wrong ore tool requirement");
        check(!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ore), "Stone pickaxe bypassed iron requirement");
        check(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(ore), "Iron pickaxe cannot mine ore");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var drops = Block.getDrops(ore, helper.getLevel(), pos, null, null, new ItemStack(Items.IRON_PICKAXE));
        check(drops.size() == 1 && drops.getFirst().is(AbyssalniteContent.ORE_ITEM.get()) && drops.getFirst().getCount() == 1,
                "Ordinary ore mining does not drop one smeltable ore");
        var input = new SingleRecipeInput(drops.getFirst());
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, helper.getLevel()).orElseThrow();
        check(recipe.value().assemble(input, helper.getLevel().registryAccess()).is(AbyssalniteContent.INGOT.get())
                && recipe.value().getCookingTime() == 200 && recipe.value().getExperience() == 3.0F, "Wrong smelting route");
        var blast = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.BLASTING, input, helper.getLevel()).orElseThrow();
        check(blast.value().getCookingTime() == 100, "Blast furnace recipe missing");
        craft(helper, "abyssalnite_pickaxe", new String[]{"III", " S ", " S "}, AbyssalniteContent.PICKAXE.get(), 1);
        craft(helper, "abyssalnite_axe", new String[]{"II", "IS", " S"}, AbyssalniteContent.AXE.get(), 1);
        craft(helper, "abyssalnite_shovel", new String[]{"I", "S", "S"}, AbyssalniteContent.SHOVEL.get(), 1);
        craft(helper, "abyssalnite_sword", new String[]{"I", "I", "S"}, AbyssalniteContent.SWORD.get(), 1);
        craft(helper, "abyssalnite_hoe", new String[]{"II", " S", " S"}, AbyssalniteContent.HOE.get(), 1);
        craft(helper, "abyssalnite_helmet", new String[]{"III", "I I"}, AbyssalniteContent.HELMET.get(), 1);
        craft(helper, "abyssalnite_chestplate", new String[]{"I I", "III", "III"}, AbyssalniteContent.CHESTPLATE.get(), 1);
        craft(helper, "abyssalnite_leggings", new String[]{"III", "I I", "I I"}, AbyssalniteContent.LEGGINGS.get(), 1);
        craft(helper, "abyssalnite_boots", new String[]{"I I", "I I"}, AbyssalniteContent.BOOTS.get(), 1);
        craft(helper, "abyssalnite_block", new String[]{"III", "III", "III"}, AbyssalniteContent.BLOCK_ITEM.get(), 1);
        craft(helper, "abyssalnite_ingot_from_block", new String[]{"B"}, AbyssalniteContent.INGOT.get(), 9);
        craft(helper, "abyssalnite_nugget", new String[]{"I"}, AbyssalniteContent.NUGGET.get(), 9);
        craft(helper, "abyssalnite_ingot_from_nuggets", new String[]{"NNN", "NNN", "NNN"}, AbyssalniteContent.INGOT.get(), 1);
        ItemStack pick = new ItemStack(AbyssalniteContent.PICKAXE.get());
        check(pick.getMaxDamage() == 1261 && pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 10.0F
                && pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Abyssalnite tool tier is not functional");
        check(AbyssalniteContent.HELMET.get().getDefense() == 3 && AbyssalniteContent.CHESTPLATE.get().getDefense() == 8
                && AbyssalniteContent.LEGGINGS.get().getDefense() == 6 && AbyssalniteContent.BOOTS.get().getDefense() == 3
                && AbyssalniteContent.CHESTPLATE.get().getToughness() == 1.0F, "Armor material differs from source values");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        pick.setDamageValue(600);
        AnvilMenu anvil = new AnvilMenu(1, player.getInventory());
        anvil.getSlot(0).set(pick);
        anvil.getSlot(1).set(new ItemStack(AbyssalniteContent.INGOT.get()));
        check(!anvil.getSlot(2).getItem().isEmpty() && anvil.getSlot(2).getItem().getDamageValue() < 600,
                "Ingot cannot repair a damaged tool in the anvil");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "abyssalnite_effects", timeoutTicks = 60)
    public static void armorEffectsRequireEquippedSlotsAndResearchIsPlayerScoped(GameTestHelper helper) {
        ServerPlayer wearer = helper.makeMockServerPlayerInLevel();
        ServerPlayer bystander = helper.makeMockServerPlayerInLevel();
        wearer.setData(AbyssalPort.EARLY_RESEARCH.get(), 0);
        bystander.setData(AbyssalPort.EARLY_RESEARCH.get(), 0);
        check(!AbyssalniteResearch.discover(wearer, new ItemStack(Items.IRON_INGOT)), "Iron unlocked Abyssalnite research");
        check(AbyssalniteResearch.discover(wearer, new ItemStack(AbyssalniteContent.INGOT.get()))
                && !AbyssalniteResearch.discover(wearer, new ItemStack(AbyssalniteContent.INGOT.get()))
                && !EarlyResearch.has(bystander, EarlyResearch.ABYSSALNITE), "Discovery leaked or repeated");
        wearer.getInventory().setItem(0, new ItemStack(AbyssalniteContent.HELMET.get()));
        AbyssalniteArmorEffects.apply(wearer);
        check(!wearer.hasEffect(MobEffects.WATER_BREATHING), "Inventory helmet incorrectly activates armor effects");
        wearer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(AbyssalniteContent.HELMET.get()));
        wearer.setItemSlot(EquipmentSlot.FEET, new ItemStack(AbyssalniteContent.BOOTS.get()));
        AbyssalniteArmorEffects.apply(wearer);
        check(wearer.hasEffect(MobEffects.WATER_BREATHING) && wearer.hasEffect(MobEffects.MOVEMENT_SPEED)
                && !bystander.hasEffect(MobEffects.MOVEMENT_SPEED), "Equipped armor effects are missing or leak to another player");
        wearer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        wearer.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        helper.assertTrue(wearer.getEffect(MobEffects.WATER_BREATHING).getDuration() == 20
                && wearer.getEffect(MobEffects.MOVEMENT_SPEED).getDuration() == 20,
                "Armor effects must have a finite one-second duration");
        // FakePlayer.tick() is deliberately empty. Advance vanilla effect lifetimes
        // explicitly, including repeated armor checks after both pieces were removed.
        for (int tick = 0; tick < 25; tick++) {
            if (tick % 10 == 0) AbyssalniteArmorEffects.apply(wearer);
            for (var effect : java.util.List.of(MobEffects.WATER_BREATHING, MobEffects.MOVEMENT_SPEED)) {
                var instance = wearer.getEffect(effect);
                if (instance != null && !instance.tick(wearer, () -> { })) wearer.removeEffect(effect);
            }
        }
        helper.assertTrue(!wearer.hasEffect(MobEffects.WATER_BREATHING) && !wearer.hasEffect(MobEffects.MOVEMENT_SPEED),
                "Unequipped armor effects did not expire");
        helper.succeed();
    }

    private static void craft(GameTestHelper helper, String name, String[] pattern, Item output, int count) {
        var stacks = new ArrayList<ItemStack>();
        for (String row : pattern) {
            for (char key : row.toCharArray()) {
                stacks.add(switch (key) {
                    case 'I' -> new ItemStack(AbyssalniteContent.INGOT.get());
                    case 'N' -> new ItemStack(AbyssalniteContent.NUGGET.get());
                    case 'B' -> new ItemStack(AbyssalniteContent.BLOCK_ITEM.get());
                    case 'S' -> new ItemStack(Items.STICK);
                    default -> ItemStack.EMPTY;
                });
            }
        }
        var input = CraftingInput.of(pattern[0].length(), pattern.length, stacks);
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
        ItemStack result = recipe.value().assemble(input, helper.getLevel().registryAccess());
        check(recipe.id().equals(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, name))
                && result.is(output) && result.getCount() == count, "Incorrect crafting result for " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
