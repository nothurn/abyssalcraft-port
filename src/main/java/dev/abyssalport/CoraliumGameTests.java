package dev.abyssalport;

import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.content.CoraliumContent;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.RitualCatalog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoraliumGameTests {
    private CoraliumGameTests() { }
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, name); }

    @GameTest(template = "empty", batch = "coralium_ore")
    public static void oceanSwampFeatureLoadsAndGeneratesRealOre(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var features = helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var oceans = features.get(id("ore_coralium_oceans"));
        var deep = features.get(id("ore_coralium_deep_oceans"));
        var swamps = features.get(id("ore_coralium_swamps"));
        helper.assertTrue(biomes.getHolderOrThrow(Biomes.OCEAN).value().getGenerationSettings().hasFeature(oceans), "Ocean has no Coralium");
        helper.assertTrue(biomes.getHolderOrThrow(Biomes.DEEP_OCEAN).value().getGenerationSettings().hasFeature(deep), "Deep ocean has no Coralium");
        helper.assertTrue(biomes.getHolderOrThrow(Biomes.SWAMP).value().getGenerationSettings().hasFeature(swamps), "Swamp has no Coralium");
        helper.assertTrue(!biomes.getHolderOrThrow(Biomes.PLAINS).value().getGenerationSettings().hasFeature(oceans), "Coralium leaked into plains");
        var configured = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(id("ore_coralium"));
        BlockPos center = helper.absolutePos(new BlockPos(7, 2, 7));
        var originals = new HashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        try {
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -5, -8), center.offset(8, 5, 8))) {
                originals.put(pos.immutable(), helper.getLevel().getBlockState(pos));
                helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            }
            originals.keySet().stream().map(net.minecraft.world.level.ChunkPos::new).distinct().forEach(chunk ->
                    Heightmap.primeHeightmaps(helper.getLevel().getChunk(chunk.x, chunk.z), Set.of(Heightmap.Types.OCEAN_FLOOR_WG)));
            for (int i = 0; i < 20; i++) configured.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(i), center);
            helper.assertTrue(originals.keySet().stream().anyMatch(p -> helper.getLevel().getBlockState(p).is(CoraliumContent.ORE.get())), "No Coralium ore generated");
        } finally {
            originals.forEach((pos, state) -> helper.getLevel().setBlock(pos, state, 2));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "coralium_crafting")
    public static void minedGemsHaveReversibleClustersAndTwoPearlRoutes(GameTestHelper helper) {
        var state = CoraliumContent.ORE.get().defaultBlockState();
        helper.assertTrue(state.is(BlockTags.NEEDS_IRON_TOOL) && !new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state), "Coralium tool requirement is wrong");
        for (int i = 0; i < 15; i++) {
            var drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), null, null, new ItemStack(Items.IRON_PICKAXE));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(CoraliumContent.GEM.get()) && drops.getFirst().getCount() >= 1 && drops.getFirst().getCount() <= 3, "Coralium did not yield 1–3 gems");
        }
        for (int n = 2; n <= 9; n++) {
            int gemCount = n;
            Item cluster = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id("coralium_gem_cluster_"+n));
            var input = CraftingInput.of(3, 3, java.util.stream.IntStream.range(0, 9).mapToObj(i -> i < gemCount ? new ItemStack(CoraliumContent.GEM.get()) : ItemStack.EMPTY).toList());
            helper.assertTrue(craft(helper, input).is(cluster), "Cannot assemble cluster of " + n);
            var unpack = craft(helper, CraftingInput.of(1, 1, List.of(new ItemStack(cluster))));
            helper.assertTrue(unpack.is(CoraliumContent.GEM.get()) && unpack.getCount() == n, "Cluster conversion loses or duplicates gems");
        }
        var dense = new ArrayList<ItemStack>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        for (int i = 0; i < 9; i++) dense.set(i, new ItemStack(i == 4 ? CoraliumContent.CLUSTER_9.get() : Items.STONE));
        ItemStack stone = craft(helper, CraftingInput.of(3, 3, dense));
        helper.assertTrue(stone.is(CoraliumContent.INFUSED_STONE_ITEM.get()), "Dense cluster cannot become infused stone");
        var triplets = new ArrayList<ItemStack>();
        for (int i = 0; i < 9; i++) triplets.add(new ItemStack(i >= 3 && i < 6 ? CoraliumContent.CLUSTER_3.get() : Items.STONE));
        helper.assertTrue(craft(helper, CraftingInput.of(3, 3, triplets)).is(CoraliumContent.INFUSED_STONE_ITEM.get()), "Three triplets cannot become infused stone");
        var input = new SingleRecipeInput(stone);
        var smelt = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, helper.getLevel()).orElseThrow();
        helper.assertTrue(smelt.value().assemble(input, helper.getLevel().registryAccess()).is(CoraliumContent.PEARL.get())
                && smelt.value().getCookingTime() == 200 && smelt.value().getExperience() == 3.0F, "Infused stone cannot be smelted into a pearl");
        helper.assertTrue(!new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(CoraliumContent.INFUSED_STONE.get().defaultBlockState())
                && new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(CoraliumContent.INFUSED_STONE.get().defaultBlockState()), "Infused stone should require a diamond-tier pickaxe");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "coralium_infusion")
    public static void realAltarInteractionPersistsAndCompletesOneInfusion(GameTestHelper helper) {
        BlockPos relative = new BlockPos(4, 1, 4);
        helper.setBlock(relative, AbyssalPort.RITUAL_ALTAR.get());
        BlockPos pos = helper.absolutePos(relative);
        RitualAltarBlockEntity altar = (RitualAltarBlockEntity) helper.getBlockEntity(relative);
        altar.removeInput();
        var player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ItemStack pearls = new ItemStack(CoraliumContent.PEARL.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, pearls);
        var response = helper.getBlockState(relative).useItemOn(pearls, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(response.consumesAction() && pearls.getCount() == 1 && altar.getInput().is(CoraliumContent.PEARL.get()), "Altar insertion failed or consumed two pearls");
        Item[] offerings = {Items.DIAMOND, Items.BLAZE_POWDER, Items.ENDER_PEARL, Items.BLAZE_POWDER,
                Items.DIAMOND, Items.BLAZE_POWDER, Items.ENDER_PEARL, Items.BLAZE_POWDER};
        for (int i = 0; i < 8; i++) {
            BlockPos pedestalPos = relative.offset(BasicScrollRitual.pedestalOffsets().get(i));
            helper.setBlock(pedestalPos, AbyssalPort.RITUAL_PEDESTAL.get());
            var pedestal = (RitualPedestalBlockEntity) helper.getBlockEntity(pedestalPos);
            pedestal.removeOne();
            helper.assertTrue(pedestal.placeOne(new ItemStack(offerings[i])), "Could not initialize infusion pedestal " + i);
        }
        var recipe = RitualCatalog.INSTANCE.get(id("transmutation_gem")).orElseThrow();
        helper.assertTrue(recipe.energyCost() == 300, "Infusion recipe has wrong energy cost");
        helper.assertTrue(recipe.matches(helper.getLevel(), pos), "Fresh eight-pedestal fixture does not match infusion");
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        NecronomiconItem.setPotentialEnergy(book, 299);
        helper.assertTrue(!altar.start(helper.getLevel(), player) && altar.getInput().is(CoraliumContent.PEARL.get()), "Undercharged infusion consumed the input");
        NecronomiconItem.setPotentialEnergy(book, 300);
        helper.getBlockState(relative).useItemOn(book, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(altar.isRunning(), "Book interaction did not start infusion");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.getBlockState(relative).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(altar.getInput().is(CoraliumContent.PEARL.get()), "Empty hand removed a running ingredient");
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        var restored = new RitualAltarBlockEntity(pos, helper.getBlockState(relative));
        restored.loadWithComponents(altar.saveWithoutMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(restored);
        helper.assertTrue(restored.getInput().is(CoraliumContent.PEARL.get()) && restored.isRunning(), "Infusion save/load lost the central ingredient");
        for (int i = 0; i < recipe.durationTicks(); i++) RitualAltarBlockEntity.serverTick(helper.getLevel(), pos, helper.getBlockState(relative), restored);
        helper.assertTrue(!restored.isRunning() && restored.getInput().is(CoraliumContent.TRANSMUTATION_GEM.get())
                && restored.getInput().getCount() == 1 && NecronomiconItem.getPotentialEnergy(book) == 0, "Infusion did not produce exactly one gem for exactly 300 PE");
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) helper.assertTrue(((RitualPedestalBlockEntity) helper.getBlockEntity(relative.offset(offset))).getOffering().isEmpty(), "Offering not consumed");
        for (int i = 0; i < 5; i++) RitualAltarBlockEntity.serverTick(helper.getLevel(), pos, helper.getBlockState(relative), restored);
        helper.assertTrue(restored.getInput().getCount() == 1 && EarlyResearch.has(player, EarlyResearch.TRANSMUTATION), "Infusion repeated output or missed discovery");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.getBlockState(relative).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(restored.getInput().isEmpty() && player.getInventory().contains(new ItemStack(CoraliumContent.TRANSMUTATION_GEM.get())), "Finished gem cannot be collected");
        cleanRitualFixture(helper, relative);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "coralium_catalyst")
    public static void catalystGivesExactlyTenCraftsAndBreakReturnsStoredInput(GameTestHelper helper) {
        ItemStack gem = new ItemStack(CoraliumContent.TRANSMUTATION_GEM.get());
        for (int use = 0; use < 10; use++) {
            var stacks = new ArrayList<ItemStack>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
            for (int slot : new int[]{1, 3, 5, 7}) stacks.set(slot, new ItemStack(AbyssalPort.SHADOW_GEM.get()));
            stacks.set(4, gem);
            var input = CraftingInput.of(3, 3, stacks);
            var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
            helper.assertTrue(recipe.value().assemble(input, helper.getLevel().registryAccess()).is(CoraliumContent.OBLIVION_SHARD.get()), "Catalyst did not craft an Oblivion Shard");
            gem = recipe.value().getRemainingItems(input).get(4);
            helper.assertTrue(use == 9 ? gem.isEmpty() : gem.is(CoraliumContent.TRANSMUTATION_GEM.get()) && gem.getDamageValue() == use+1,
                    "Catalyst gained durability, disappeared early or survived the eleventh use");
        }
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, AbyssalPort.RITUAL_ALTAR.get());
        var altar = (RitualAltarBlockEntity) helper.getBlockEntity(relative);
        altar.removeInput();
        altar.placeInput(new ItemStack(CoraliumContent.PEARL.get()));
        BlockPos pos = helper.absolutePos(relative);
        AABB box = new AABB(pos).inflate(2);
        Set<UUID> before = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        helper.getLevel().destroyBlock(pos, false);
        var returned = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).stream()
                .filter(e -> !before.contains(e.getUUID()) && e.getItem().is(CoraliumContent.PEARL.get())).toList();
        helper.assertTrue(returned.size() == 1 && returned.getFirst().getItem().getCount() == 1, "Breaking an altar lost or duplicated the central input");
        returned.forEach(ItemEntity::discard);
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, CraftingInput input) {
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow()
                .value().assemble(input, helper.getLevel().registryAccess());
    }

    @GameTest(template = "empty", batch = "coralium_abort")
    public static void changedCentralIngredientAndDefinitionCancelWithoutCharge(GameTestHelper helper) {
        BlockPos relative = new BlockPos(4, 1, 4);
        helper.setBlock(relative, AbyssalPort.RITUAL_ALTAR.get());
        BlockPos pos = helper.absolutePos(relative);
        var altar = (RitualAltarBlockEntity) helper.getBlockEntity(relative);
        altar.removeInput();
        var player = helper.makeMockServerPlayerInLevel();
        player.teleportTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5);
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        NecronomiconItem.setPotentialEnergy(book, 300);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        Item[] items = {Items.DIAMOND, Items.DIAMOND, Items.ENDER_PEARL, Items.ENDER_PEARL,
                Items.BLAZE_POWDER, Items.BLAZE_POWDER, Items.BLAZE_POWDER, Items.BLAZE_POWDER};
        for (int i = 0; i < 8; i++) {
            BlockPos pedestal = relative.offset(BasicScrollRitual.pedestalOffsets().get(i));
            helper.setBlock(pedestal, AbyssalPort.RITUAL_PEDESTAL.get());
            var stand = (RitualPedestalBlockEntity) helper.getBlockEntity(pedestal);
            stand.removeOne();
            helper.assertTrue(stand.placeOne(new ItemStack(items[i])), "Could not initialize cancellation pedestal " + i);
        }
        altar.placeInput(new ItemStack(Items.STONE));
        helper.assertTrue(!altar.start(helper.getLevel(), player), "Wrong central ingredient started infusion");
        altar.removeInput();
        altar.placeInput(new ItemStack(CoraliumContent.PEARL.get()));
        helper.assertTrue(altar.start(helper.getLevel(), player), "Valid unordered infusion failed");
        var stale = altar.saveWithoutMetadata(helper.getLevel().registryAccess());
        stale.putString("RitualSignature", "changed-altar-input");
        var restored = new RitualAltarBlockEntity(pos, helper.getBlockState(relative));
        restored.loadWithComponents(stale, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(restored);
        RitualAltarBlockEntity.serverTick(helper.getLevel(), pos, helper.getBlockState(relative), restored);
        helper.assertTrue(!restored.isRunning() && restored.getInput().is(CoraliumContent.PEARL.get())
                && NecronomiconItem.getPotentialEnergy(book) == 300, "Definition change consumed PE or the pearl");
        helper.assertTrue(restored.start(helper.getLevel(), player), "Canceled infusion cannot restart");
        restored.removeInput();
        RitualAltarBlockEntity.serverTick(helper.getLevel(), pos, helper.getBlockState(relative), restored);
        helper.assertTrue(!restored.isRunning() && NecronomiconItem.getPotentialEnergy(book) == 300,
                "Removing the central ingredient produced an output or charged PE");
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            helper.assertTrue(!((RitualPedestalBlockEntity) helper.getBlockEntity(relative.offset(offset))).getOffering().isEmpty(), "Cancellation lost pedestal offerings");
        }
        cleanRitualFixture(helper, relative);
        helper.succeed();
    }

    private static void cleanRitualFixture(GameTestHelper helper, BlockPos relative) {
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            BlockPos pos = relative.offset(offset);
            if (helper.getBlockEntity(pos) instanceof RitualPedestalBlockEntity stand) stand.removeOne();
            helper.setBlock(pos, Blocks.AIR);
        }
        helper.setBlock(relative, Blocks.AIR);
    }
}
