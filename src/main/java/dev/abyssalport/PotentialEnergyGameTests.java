package dev.abyssalport;

import com.mojang.authlib.GameProfile;
import dev.abyssalport.block.EnergyRelayBlock;
import dev.abyssalport.block.entity.EnergyContainerBlockEntity;
import dev.abyssalport.block.entity.EnergyRelayBlockEntity;
import dev.abyssalport.energy.EnergyStorageBlockEntity;
import dev.abyssalport.energy.PotentialEnergyItems;
import dev.abyssalport.item.EnergyStorageItem;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.menu.EnergyContainerMenu;
import dev.abyssalport.content.EnergyDeviceContent;
import dev.abyssalport.content.EnergyDeviceContent.Kind;
import dev.abyssalport.content.WastelandProgressionContent;
import dev.abyssalport.content.DreadlandsProgressionContent;
import dev.abyssalport.energy.EnergyDeviceTier;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.RitualCatalog;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PotentialEnergyGameTests {
    private PotentialEnergyGameTests() { }

    /** Own a bounded six-axis path; do not load chunks beyond the test fixture. */
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level;
        final BlockPos node;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        Fixture(GameTestHelper helper) { this(helper, 5); }
        Fixture(GameTestHelper helper, int radius) {
            level = helper.getLevel(); node = helper.absolutePos(new BlockPos(3, 14, 3));
            saved.put(node, level.getBlockState(node));
            for (var direction : Direction.values()) for (int step = 1; step <= radius; step++) {
                var pos = node.relative(direction, step);
                helper.assertTrue(level.getBlockEntity(pos) == null, "PE fixture overlaps an existing block entity");
                saved.put(pos, level.getBlockState(pos));
            }
            clear();
            for (var pos : saved.keySet()) {
                var chunk = new ChunkPos(pos);
                if (!level.getForcedChunks().contains(chunk.toLong()) && !forced.contains(chunk)) {
                    level.setChunkForced(chunk.x, chunk.z, true); forced.add(chunk);
                }
            }
        }
        void clear() {
            for (var pos : saved.keySet()) {
                if (level.getBlockEntity(pos) instanceof EnergyContainerBlockEntity container)
                    for (int slot = 0; slot < 2; slot++) container.items().setStackInSlot(slot, ItemStack.EMPTY);
                if (level.getBlockEntity(pos) instanceof dev.abyssalport.block.entity.EnergyPedestalBlockEntity pedestal) pedestal.removeItem();
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        EnergyStorageBlockEntity place(BlockPos pos, Block block) {
            level.setBlock(pos, block.defaultBlockState(), 2);
            return (EnergyStorageBlockEntity) level.getBlockEntity(pos);
        }
        EnergyRelayBlockEntity relay(Direction facing) {
            var state = AbyssalPort.ENERGY_RELAY.get().defaultBlockState().setValue(EnergyRelayBlock.FACING, facing);
            level.setBlock(node, state, 2);
            // Exact ticks use an independent native BE, not a concurrently ticked world BE.
            var relay = new EnergyRelayBlockEntity(node, state); relay.setLevel(level); return relay;
        }
        void tick(EnergyRelayBlockEntity relay, int ticks) {
            for (int tick = 0; tick < ticks; tick++)
                EnergyRelayBlockEntity.serverTick(level, node, level.getBlockState(node), relay);
        }
        @Override public void close() {
            clear(); saved.forEach((pos, state) -> level.setBlock(pos, state, 2));
            forced.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
        }
    }
    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PEAudit"));
        player.setGameMode(GameType.SURVIVAL); return player;
    }
    private static ItemStack charged(Item item, float amount) {
        var stack = new ItemStack(item); PotentialEnergyItems.add(stack, amount); return stack;
    }

    @GameTest(template="empty", batch="pe_container_roles")
    public static void AllNativeStoreRolesTransferInAllSixDirectionsAndConserveEnergy(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            Block[] stores = {AbyssalPort.ENERGY_COLLECTOR.get(), AbyssalPort.ENERGY_RELAY.get(), AbyssalPort.ENERGY_CONTAINER.get(),
                    EnergyDeviceContent.block(Kind.PEDESTAL, EnergyDeviceTier.BASE)};
            for (var direction : Direction.values()) for (var sourceBlock : stores) for (var targetBlock : stores) {
                fixture.clear(); var relay = fixture.relay(direction);
                var source = fixture.place(fixture.node.relative(direction.getOpposite()), sourceBlock);
                var target = fixture.place(fixture.node.relative(direction, 4), targetBlock);
                source.addEnergy(100); fixture.tick(relay, 40);
                helper.assertTrue(source.energy() == 80 && target.energy() == 20 && relay.energy() == 0,
                        "PE role/direction rejected or lost energy: " + direction + ", " + sourceBlock + " -> " + targetBlock
                        + ": " + source.energy() + "/" + relay.energy() + "/" + target.energy());
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="pe_beam_roles")
    public static void FirstFullReceiverAndFullBlockStopBeamButPartialShapesDoNot(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var relay = fixture.relay(Direction.EAST);
            var first = fixture.place(fixture.node.east(2), AbyssalPort.ENERGY_COLLECTOR.get());
            var further = fixture.place(fixture.node.east(4), AbyssalPort.ENERGY_CONTAINER.get());
            first.addEnergy(first.capacity()); relay.addEnergy(100); fixture.tick(relay, 40);
            helper.assertTrue(relay.energy() == 100 && further.energy() == 0, "Beam skipped its full first receiver");
            first.consumeEnergy(7); fixture.tick(relay, 40);
            helper.assertTrue(first.energy() == first.capacity() && relay.energy() == 93 && further.energy() == 0,
                    "First receiver overflow lost PE or leaked into another store");
            fixture.level.setBlock(fixture.node.east(2), Blocks.AIR.defaultBlockState(), 2);
            fixture.level.setBlock(fixture.node.east(), Blocks.STONE.defaultBlockState(), 2); fixture.tick(relay, 40);
            helper.assertTrue(relay.energy() == 93 && further.energy() == 0, "Beam crossed a full collision shape");
            fixture.level.setBlock(fixture.node.east(), Blocks.OAK_SLAB.defaultBlockState(), 2); fixture.tick(relay, 40);
            helper.assertTrue(relay.energy() == 73 && further.energy() == 20, "Partial source-permitted shape blocked beam");
            fixture.level.setBlock(fixture.node.above(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 2); fixture.tick(relay, 80);
            helper.assertTrue(relay.energy() == 73 && further.energy() == 20, "Powered PE role transfer did not pause");
            fixture.level.setBlock(fixture.node.above(), Blocks.AIR.defaultBlockState(), 2); fixture.tick(relay, 40);
            helper.assertTrue(relay.energy() == 53 && further.energy() == 40, "Role transfer failed to resume");
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="pe_facing_native")
    public static void ClickedFacePlacementRotationAndCyclingKeepNativeBalanceAndState(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var player = player(helper); player.moveTo(fixture.node.getCenter());
            var block = AbyssalPort.ENERGY_RELAY.get();
            for (var direction : Direction.values()) {
                var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                        new ItemStack(AbyssalPort.ENERGY_RELAY_ITEM.get()),
                        new BlockHitResult(fixture.node.getCenter(), direction, fixture.node, false));
                var state = block.getStateForPlacement(context);
                helper.assertTrue(state.getValue(EnergyRelayBlock.FACING) == direction, "Placement ignores clicked face");
                helper.assertTrue(state.rotate(Rotation.CLOCKWISE_90).getValue(EnergyRelayBlock.FACING) == Rotation.CLOCKWISE_90.rotate(direction)
                        && state.mirror(Mirror.LEFT_RIGHT).getValue(EnergyRelayBlock.FACING) == Mirror.LEFT_RIGHT.mirror(direction),
                        "Native structure rotation/mirror changes PE orientation incorrectly");
                var bounds = state.getShape(fixture.level, fixture.node).bounds();
                if (direction == Direction.UP || direction == Direction.DOWN)
                    helper.assertTrue(bounds.minY == (direction == Direction.UP ? 0 : .25)
                            && bounds.maxY == (direction == Direction.UP ? .75 : 1), "Vertical selection shape differs from owned model");
                helper.assertTrue(!Block.isShapeFullBlock(state.getCollisionShape(fixture.level, fixture.node)), "Relay is a full cube");
            }
            fixture.relay(Direction.NORTH);
            var actual = (EnergyRelayBlockEntity) fixture.level.getBlockEntity(fixture.node); actual.addEnergy(417);
            var seen = EnumSet.noneOf(Direction.class);
            for (int step = 0; step < 6; step++) {
                var state = fixture.level.getBlockState(fixture.node); seen.add(state.getValue(EnergyRelayBlock.FACING));
                state.useWithoutItem(fixture.level, player, new BlockHitResult(fixture.node.getCenter(), Direction.UP, fixture.node, false));
                helper.assertTrue(fixture.level.getBlockEntity(fixture.node) == actual && actual.energy() == 417, "Facing cycle replaced or cleared real native BE");
            }
            helper.assertTrue(seen.size() == 6 && fixture.level.getBlockState(fixture.node).getValue(EnergyRelayBlock.FACING) == Direction.NORTH,
                    "Empty-hand cycle does not visit/restore all six source directions");
            player.getAbilities().mayBuild = false; var prior = fixture.level.getBlockState(fixture.node);
            prior.useWithoutItem(fixture.level, player, new BlockHitResult(fixture.node.getCenter(), Direction.UP, fixture.node, false));
            helper.assertTrue(fixture.level.getBlockState(fixture.node) == prior, "Adventure player reoriented a PE machine");
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="pe_native_item_balances")
    public static void DeviceItemSlotsRoundTripComponentsBalancesAndMalformedStacksSafely(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var container = (EnergyContainerBlockEntity) fixture.place(fixture.node, AbyssalPort.ENERGY_CONTAINER.get());
            var input = charged(AbyssalPort.ENERGY_COLLECTOR_ITEM.get(), 31.5F);
            var output = charged(AbyssalPort.ENERGY_RELAY_ITEM.get(), 495.25F);
            input.set(DataComponents.CUSTOM_NAME, Component.literal("Named input"));
            var tag = input.get(DataComponents.CUSTOM_DATA).copyTag(); tag.putString("ThirdParty", "untouched");
            input.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            helper.assertTrue(input.getMaxStackSize() == 1 && output.getMaxStackSize() == 1 && container.items().getSlotLimit(0) == 1
                    && container.items().isItemValid(0, input) && container.items().isItemValid(1, output), "Device items have no safe native inventory role");
            container.items().setStackInSlot(0, input); container.items().setStackInSlot(1, output);
            EnergyContainerBlockEntity.serverTick(fixture.level, fixture.node, container.getBlockState(), container);
            helper.assertTrue(EnergyStorageItem.storedEnergy(input) == 11.5F && EnergyStorageItem.storedEnergy(output) == 500
                    && container.energy() == 15.25F && input.getHoverName().getString().equals("Named input")
                    && input.get(DataComponents.CUSTOM_DATA).copyTag().getString("ThirdParty").equals("untouched"), "Item transfer overflow lost fractional PE or private components");
            var saved = container.saveWithFullMetadata(fixture.level.registryAccess());
            var loaded = new EnergyContainerBlockEntity(fixture.node, container.getBlockState()); loaded.loadWithComponents(saved, fixture.level.registryAccess());
            helper.assertTrue(loaded.energy() == 15.25F && ItemStack.matches(input, loaded.items().getStackInSlot(0))
                    && ItemStack.matches(output, loaded.items().getStackInSlot(1)), "Native item slots/PE components did not survive save");
            var malformed = input.copyWithCount(2); container.items().setStackInSlot(0, malformed);
            container.items().setStackInSlot(1, ItemStack.EMPTY); var before = malformed.copy();
            EnergyContainerBlockEntity.serverTick(fixture.level, fixture.node, container.getBlockState(), container);
            helper.assertTrue(!container.items().isItemValid(0, malformed) && container.energy() == 15.25F
                    && ItemStack.matches(before, malformed) && PotentialEnergyItems.consume(malformed, 10) == 0
                    && PotentialEnergyItems.add(malformed, 10) == 10, "Malformed stack duplicated/consumed a shared PE component");
            var nan = new CompoundTag(); nan.putFloat("PotEnergy", Float.NaN); nan.putString("ThirdParty", "nan-preserved");
            var corrupt = new ItemStack(AbyssalPort.ENERGY_COLLECTOR_ITEM.get()); corrupt.set(DataComponents.CUSTOM_DATA, CustomData.of(nan));
            helper.assertTrue(PotentialEnergyItems.stored(corrupt) == 0 && PotentialEnergyItems.add(corrupt, 1) == 0
                    && PotentialEnergyItems.stored(corrupt) == 1 && corrupt.get(DataComponents.CUSTOM_DATA).copyTag().getString("ThirdParty").equals("nan-preserved"), "Corrupt balance poisoned PE or destroyed private data");
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="pe_native_menu")
    public static void NativeMenuRoutesDeviceItemsAndReconstructsLargeSignedPacketWords(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var player = player(helper); player.moveTo(fixture.node.getCenter());
            var container = (EnergyContainerBlockEntity) fixture.place(fixture.node, AbyssalPort.ENERGY_CONTAINER.get());
            var menu = new EnergyContainerMenu(9, player.getInventory(), container);
            var input = charged(AbyssalPort.ENERGY_COLLECTOR_ITEM.get(), 37);
            var output = new ItemStack(AbyssalPort.ENERGY_RELAY_ITEM.get());
            var expectedInput = input.copy(); var expectedOutput = output.copy();
            player.getInventory().setItem(9, input); player.getInventory().setItem(10, output);
            helper.assertTrue(!menu.quickMoveStack(player, 2).isEmpty() && !menu.quickMoveStack(player, 3).isEmpty()
                    && ItemStack.matches(container.items().getStackInSlot(0), expectedInput) && ItemStack.matches(container.items().getStackInSlot(1), expectedOutput)
                    && player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(10).isEmpty(),
                    "Native shift-click does not route charged/empty device items");
            container.addEnergy(123); menu.setData(0, -1); menu.setData(1, -1);
            helper.assertTrue(menu.energy() == 123 && menu.capacity() == 10_000 && menu.stillValid(player), "Client data writes alter server balance or native validity");
            var client = new EnergyContainerMenu(9, player.getInventory());
            for (int value : new int[]{0, 10_000, 60_000, 65_535, 65_536, 240_000, 1_200_000}) {
                client.setData(0, (short) value); client.setData(1, (short) (value >>> 16));
                client.setData(2, (short) 1_200_000); client.setData(3, (short) (1_200_000 >>> 16));
                helper.assertTrue(client.energy() == value && client.capacity() == 1_200_000, "Menu truncates/sign-extends PE packet words: " + value);
            }
            helper.assertTrue(!menu.quickMoveStack(player, 0).isEmpty() && container.items().getStackInSlot(0).isEmpty(), "Native device extraction loses or locks item");
            container.items().setStackInSlot(1, ItemStack.EMPTY);
            fixture.level.setBlock(fixture.node, Blocks.AIR.defaultBlockState(), 2);
            helper.assertTrue(!menu.stillValid(player), "Menu remains valid after machine removal");
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="pe_tier_native_anvil")
    public static void EveryReachableUpgradeUsesNativeAnvilTakingAndKeepsNamedStoredPE(GameTestHelper helper) {
        var player = player(helper);
        for (var kind : Kind.values()) for (var oldTier : EnergyDeviceContent.REACHABLE) for (var newTier : EnergyDeviceContent.REACHABLE) {
            if (newTier.ordinal() <= oldTier.ordinal()) continue;
            var source = charged(EnergyDeviceContent.item(kind, oldTier), 321.5F);
            source.set(DataComponents.CUSTOM_NAME, Component.literal("Personal machine"));
            var tag = source.get(DataComponents.CUSTOM_DATA).copyTag(); tag.putString("PrivateNote", "retain");
            source.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); var snapshot = source.copy();
            player.experienceLevel = 5;
            var menu = new AnvilMenu(13, player.getInventory());
            menu.getSlot(0).set(source); menu.getSlot(1).set(new ItemStack(EnergyDeviceContent.ring(newTier), 3));
            var result = menu.getSlot(2).getItem();
            helper.assertTrue(result.is(EnergyDeviceContent.item(kind, newTier)) && result.getCount() == 1
                    && EnergyStorageItem.storedEnergy(result) == 321.5F && result.getHoverName().getString().equals("Personal machine")
                    && result.get(DataComponents.CUSTOM_DATA).copyTag().getString("PrivateNote").equals("retain")
                    && ItemStack.matches(source, snapshot) && menu.getCost() == 1 && menu.repairItemCountCost == 1,
                    "Native ring upgrade loses components, mutates preview or charges invented materials: " + kind + "/" + oldTier + "->" + newTier);
            helper.assertTrue(menu.getSlot(2).mayPickup(player), "One-level ring result cannot be taken");
            var taken = menu.getSlot(2).remove(1); menu.getSlot(2).onTake(player, taken);
            helper.assertTrue(player.experienceLevel == 4 && menu.getSlot(0).getItem().isEmpty()
                    && menu.getSlot(1).getItem().getCount() == 2 && menu.getSlot(2).getItem().isEmpty(), "Native result take duplicates device/ring or mischarges XP");
            var saved = ItemStack.parse(helper.getLevel().registryAccess(), taken.save(helper.getLevel().registryAccess())).orElseThrow();
            helper.assertTrue(ItemStack.matches(saved, taken) && saved.getMaxStackSize() == 1, "Forged device save loses native components/nonstackability");
        }
        var menu = new AnvilMenu(14, player.getInventory());
        menu.getSlot(0).set(new ItemStack(EnergyDeviceContent.item(Kind.CONTAINER, EnergyDeviceTier.BASE)));
        menu.getSlot(1).set(new ItemStack(EnergyDeviceContent.ring(EnergyDeviceTier.DREADLANDS)));
        // Native Slot.set clamps a nonstackable item before the update hook.
        // Bypass that clamp deliberately to probe a corrupted saved/input stack.
        menu.getSlot(0).getItem().setCount(2); menu.createResult();
        helper.assertTrue(!menu.getSlot(2).hasItem() && menu.getSlot(0).getItem().getCount() == 2, "Malformed device stack destroys extra machines");
        menu.getSlot(0).set(new ItemStack(EnergyDeviceContent.item(Kind.CONTAINER, EnergyDeviceTier.BASE))); player.experienceLevel = 0;
        helper.assertTrue(menu.getSlot(2).hasItem() && !menu.getSlot(2).mayPickup(player), "Ring upgrade bypasses real XP gate");
        menu.getSlot(0).set(new ItemStack(EnergyDeviceContent.item(Kind.CONTAINER, EnergyDeviceTier.DREADLANDS)));
        helper.assertTrue(!menu.getSlot(2).hasItem(), "Same-tier ring invents a second upgrade");
        menu.getSlot(1).set(new ItemStack(EnergyDeviceContent.ring(EnergyDeviceTier.OVERWORLD)));
        helper.assertTrue(!menu.getSlot(2).hasItem(), "Lower ring downgrades a charged device");
        menu.getSlot(0).set(new ItemStack(EnergyDeviceContent.ring(EnergyDeviceTier.DREADLANDS)));
        menu.getSlot(1).set(new ItemStack(EnergyDeviceContent.item(Kind.CONTAINER, EnergyDeviceTier.BASE)));
        helper.assertTrue(!menu.getSlot(2).hasItem(), "Reversed PE inputs invent an upgrade");
        helper.succeed();
    }

    @GameTest(template="empty", batch="pe_tier_native_persistence")
    public static void AllTiersHaveValidNativeBEsChargedLootPlacementAndLargeMenuBalances(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var player = player(helper); player.moveTo(fixture.node.getCenter().add(-2, 0, 0));
            for (var kind : Kind.values()) for (var tier : EnergyDeviceContent.REACHABLE) {
                fixture.clear(); var block = EnergyDeviceContent.block(kind, tier);
                var storage = fixture.place(fixture.node, block); int capacity = EnergyDeviceContent.capacity(kind, tier);
                helper.assertTrue(storage.capacity() == capacity && storage.getType().isValid(storage.getBlockState())
                        && block.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), "Tier lacks valid BE/capacity/tools: " + kind + tier);
                storage.addEnergy(capacity - 1.25F); var data = storage.saveWithFullMetadata(fixture.level.registryAccess());
                var restored = block.newBlockEntity(fixture.node, block.defaultBlockState()); restored.loadWithComponents(data, fixture.level.registryAccess());
                helper.assertTrue(((EnergyStorageBlockEntity) restored).energy() == capacity - 1.25F, "Tier save clips native PE");
                var drops = Block.getDrops(storage.getBlockState(), fixture.level, fixture.node, storage);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(EnergyDeviceContent.item(kind, tier))
                        && PotentialEnergyItems.stored(drops.getFirst()) == capacity - 1.25F, "Tier loot lost PE or returned base machine");
                fixture.level.setBlock(fixture.node, Blocks.AIR.defaultBlockState(), 2);
                fixture.level.setBlock(fixture.node.below(), Blocks.STONE.defaultBlockState(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, drops.getFirst());
                helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(fixture.node.below().getCenter().add(0, .5, 0), Direction.UP, fixture.node.below(), false))).consumesAction(),
                        "Real tier BlockItem placement failed: " + kind + tier);
                var placed = (EnergyStorageBlockEntity) fixture.level.getBlockEntity(fixture.node);
                helper.assertTrue(placed != null && placed.energy() == capacity - 1.25F && player.getMainHandItem().isEmpty(), "Placed tier balance lost/duplicated or did not consume item");
                if (placed instanceof EnergyContainerBlockEntity container) {
                    player.moveTo(fixture.node.getCenter().add(-2, 0, 0));
                    var serverMenu = new EnergyContainerMenu(15, player.getInventory(), container);
                    var clientMenu = new EnergyContainerMenu(15, player.getInventory());
                    clientMenu.setData(0, (short) serverMenu.energy()); clientMenu.setData(1, (short) (serverMenu.energy() >>> 16));
                    clientMenu.setData(2, (short) serverMenu.capacity()); clientMenu.setData(3, (short) (serverMenu.capacity() >>> 16));
                    helper.assertTrue(serverMenu.stillValid(player) && clientMenu.energy() == (int) placed.energy() && clientMenu.capacity() == capacity,
                            "Actual tier menu cannot open/sync large server balance");
                }
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="pe_tier_native_ranges")
    public static void HigherRelaysReachTheirExactSourceRangeAndNeverTheNextBlock(GameTestHelper helper) {
        try (var fixture = new Fixture(helper, 11)) {
            for (var tier : EnergyDeviceContent.REACHABLE) {
                fixture.clear(); var block = EnergyDeviceContent.block(Kind.RELAY, tier);
                var state = block.defaultBlockState().setValue(EnergyRelayBlock.FACING, Direction.EAST);
                fixture.level.setBlock(fixture.node, state, 2);
                var relay = new EnergyRelayBlockEntity(fixture.node, state); relay.setLevel(fixture.level);
                var source = fixture.place(fixture.node.west(), EnergyDeviceContent.block(Kind.COLLECTOR, tier)); source.addEnergy(1000);
                var target = fixture.place(fixture.node.east(tier.range()), EnergyDeviceContent.block(Kind.CONTAINER, tier));
                fixture.tick(relay, 40);
                helper.assertTrue(source.energy() == 1000 - 2*tier.drain() && target.energy() == tier.transfer()
                        && relay.energy() == 2*tier.drain() - tier.transfer(), "Tier range/quanta are disconnected: " + tier);
                fixture.level.setBlock(fixture.node.east(tier.range()), Blocks.AIR.defaultBlockState(), 2);
                var outside = fixture.place(fixture.node.east(tier.range()+1), EnergyDeviceContent.block(Kind.CONTAINER, tier));
                float before = relay.energy(); fixture.tick(relay, 40);
                helper.assertTrue(outside.energy() == 0 && relay.energy() == before + 2*tier.drain(), "Tier beam exceeds its actual original range");
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="pe_tier_ring_definitions")
    public static void RealBaseRingRecipeAndAllLowerRingInfusionsUseActualSourceMaterials(GameTestHelper helper) {
        var materials = List.of(new ItemStack(Items.BLAZE_POWDER), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.BLAZE_POWDER),
                new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT),
                new ItemStack(Items.BLAZE_POWDER), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.BLAZE_POWDER));
        var input = CraftingInput.of(3, 3, materials);
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
        helper.assertTrue(recipe.assemble(input, helper.getLevel().registryAccess()).is(EnergyDeviceContent.ring(EnergyDeviceTier.BASE)), "Real source base ring recipe is absent");
        for (var tier : EnergyDeviceContent.REACHABLE) for (var old : EnergyDeviceContent.REACHABLE) {
            if (tier.ordinal() <= old.ordinal()) continue;
            var id = ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, EnergyDeviceContent.ringId(tier)+"_from_"+EnergyDeviceContent.ringId(old));
            var definition = RitualCatalog.INSTANCE.get(id).orElseThrow();
            helper.assertTrue(definition.energyCost() == 400 && definition.altarInput() == EnergyDeviceContent.ring(old)
                    && definition.result() == EnergyDeviceContent.ring(tier) && definition.requirements().bookTier() == tier.ordinal()-1
                    && definition.requirements().dimension() == null && definition.requirements().altarTier() == 0 && !definition.livingSacrifice(),
                    "Ring infusion invents a dimension/sacrifice/cost or loses lower centres: " + id);
            Map<Item, Integer> expected = new HashMap<>();
            expected.put(AbyssalPort.SHADOW_SHARD.get(), 2); expected.put(dev.abyssalport.content.CoraliumContent.GEM.get(), 2);
            if (tier == EnergyDeviceTier.OVERWORLD) expected.put(Items.COBBLESTONE, 4);
            else {
                var first = tier == EnergyDeviceTier.ABYSSAL_WASTELAND ? dev.abyssalport.content.WastelandContent.COBBLESTONE.get() : dev.abyssalport.content.DreadlandsContent.COBBLESTONE.get();
                var second = tier == EnergyDeviceTier.ABYSSAL_WASTELAND ? dev.abyssalport.content.WastelandProcessingContent.COBBLESTONE.get() : dev.abyssalport.content.DreadlandsContent.ELYSIAN_COBBLESTONE.get();
                expected.put(first.asItem(), 2); expected.put(second.asItem(), 2);
            }
            helper.assertTrue(definition.offerings().equals(expected), "Ring recipe substituted source materials: " + id);
        }
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "omothol_embossed_ring")), "Unreachable Omothol placeholder registered");
        helper.succeed();
    }

    @GameTest(template="empty", batch="pe_tier_survival_ring_chain")
    public static void NativePaidRingChainUsesHeldBookGatesAndConsumesEveryOfferingExactlyOnce(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(3, 14, 3));
        Map<BlockPos, BlockState> prior = new LinkedHashMap<>();
        prior.put(pos, level.getBlockState(pos)); for (var offset : BasicScrollRitual.pedestalOffsets()) prior.put(pos.offset(offset), level.getBlockState(pos.offset(offset)));
        for (var cell : prior.keySet()) helper.assertTrue(level.getBlockEntity(cell) == null, "Ring ritual overlaps existing BE");
        var player = helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL); player.moveTo(pos.getCenter().add(0, 1, 0));
        try {
            level.setBlock(pos, AbyssalPort.RITUAL_ALTAR.get().defaultBlockState(), 2);
            for (var offset : BasicScrollRitual.pedestalOffsets()) level.setBlock(pos.offset(offset), AbyssalPort.RITUAL_PEDESTAL.get().defaultBlockState(), 2);
            var altar = (RitualAltarBlockEntity) level.getBlockEntity(pos);
            Item[] books = {AbyssalPort.NECRONOMICON.get(), WastelandProgressionContent.BOOK.get(), DreadlandsProgressionContent.BOOK.get()};
            for (int step = 1; step <= 3; step++) {
                var tier = EnergyDeviceContent.REACHABLE.get(step); var old = EnergyDeviceContent.REACHABLE.get(step-1);
                var id = ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, EnergyDeviceContent.ringId(tier)+"_from_"+EnergyDeviceContent.ringId(old));
                var definition = RitualCatalog.INSTANCE.get(id).orElseThrow();
                if (step == 1) helper.assertTrue(altar.placeInput(new ItemStack(EnergyDeviceContent.ring(old))), "Cannot place source ring");
                int slot = 0;
                for (var entry : definition.offerings().entrySet()) for (int count = 0; count < entry.getValue(); count++) {
                    var stand = (RitualPedestalBlockEntity) level.getBlockEntity(pos.offset(BasicScrollRitual.pedestalOffsets().get(slot++)));
                    helper.assertTrue(stand.placeOne(new ItemStack(entry.getKey())), "Cannot place ring offering");
                }
                var book = charged(books[step-1], 399); player.setItemInHand(InteractionHand.MAIN_HAND, book); player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                helper.assertTrue(!altar.start(level, player) && altar.getInput().is(EnergyDeviceContent.ring(old)), "Unpaid ring infusion consumes source");
                if (step > 1) {
                    var weak = charged(books[step-2], 1000); player.setItemInHand(InteractionHand.MAIN_HAND, weak);
                    helper.assertTrue(!altar.start(level, player) && PotentialEnergyItems.stored(weak) == 1000, "Lower book bypasses higher ring gate");
                }
                PotentialEnergyItems.add(book, 601); player.setItemInHand(InteractionHand.MAIN_HAND, book);
                helper.assertTrue(definition.matches(level, pos) && altar.start(level, player), "Reachable ring cannot start on native base ritual ground");
                for (int tick = 0; tick < definition.durationTicks(); tick++) RitualAltarBlockEntity.serverTick(level, pos, altar.getBlockState(), altar);
                helper.assertTrue(!altar.isRunning() && altar.getInput().is(EnergyDeviceContent.ring(tier))
                        && altar.getInput().getCount() == 1 && PotentialEnergyItems.stored(book) == 600, "Native paid ring upgrade loses centre/debit or stays running");
                for (var offset : BasicScrollRitual.pedestalOffsets()) helper.assertTrue(((RitualPedestalBlockEntity) level.getBlockEntity(pos.offset(offset))).getOffering().isEmpty(), "Ring upgrade duplicates a consumed source offering");
            }
            helper.succeed();
        } finally {
            if (level.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) altar.removeInput();
            for (var offset : BasicScrollRitual.pedestalOffsets()) if (level.getBlockEntity(pos.offset(offset)) instanceof RitualPedestalBlockEntity stand) stand.removeOne();
            prior.forEach((cell, state) -> level.setBlock(cell, state, 2)); player.getInventory().clearContent();
        }
    }

    @GameTest(template="empty", batch="pe_native_device_live", timeoutTicks=100)
    public static void RegisteredNativeTicksChargeAndDrainDeviceItemsWithoutManualCalls(GameTestHelper helper) {
        var fixture = new Fixture(helper);
        try {
            var container = (EnergyContainerBlockEntity) fixture.place(fixture.node, AbyssalPort.ENERGY_CONTAINER.get());
            var input = charged(AbyssalPort.ENERGY_COLLECTOR_ITEM.get(), 53.75F);
            var output = new ItemStack(AbyssalPort.ENERGY_RELAY_ITEM.get());
            container.items().setStackInSlot(0, input); container.items().setStackInSlot(1, output);
            helper.runAfterDelay(60, () -> {
                try {
                    helper.assertTrue(PotentialEnergyItems.stored(input) == 0 && PotentialEnergyItems.stored(output) == 53.75F
                            && container.energy() == 0, "Actual registered native ticker did not connect device item roles");
                    helper.succeed();
                } finally { fixture.close(); }
            });
        } catch (RuntimeException error) { fixture.close(); throw error; }
    }
}
