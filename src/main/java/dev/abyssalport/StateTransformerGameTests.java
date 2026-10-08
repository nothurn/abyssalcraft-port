package dev.abyssalport;

import com.mojang.authlib.GameProfile;
import dev.abyssalport.block.StateTransformerBlock;
import dev.abyssalport.block.entity.StateTransformerBlockEntity;
import dev.abyssalport.content.*;
import dev.abyssalport.energy.PotentialEnergyItems;
import dev.abyssalport.item.StoneTabletItem;
import dev.abyssalport.menu.StateTransformerMenu;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StateTransformerGameTests {
    private StateTransformerGameTests() { }
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level;
        final BlockPos pos;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final List<ItemEntity> drops = new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> capture;
        Fixture(GameTestHelper helper) {
            level = helper.getLevel(); pos = helper.absolutePos(new BlockPos(3, 25, 3));
            for (var p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
                helper.assertTrue(level.getBlockEntity(p) == null, "Transformer fixture overlaps another inventory");
                saved.put(p.immutable(), level.getBlockState(p));
            }
            saved.keySet().forEach(p -> level.setBlock(p, p.getY() == pos.getY() - 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2));
            for (int x = (pos.getX() - 2) >> 4; x <= (pos.getX() + 2) >> 4; x++) for (int z = (pos.getZ() - 2) >> 4; z <= (pos.getZ() + 2) >> 4; z++) {
                var chunk = new ChunkPos(x, z); if (!level.getForcedChunks().contains(chunk.toLong())) { forced.add(chunk); level.setChunkForced(x, z, true); }
            }
            capture = event -> { if (event.getLevel() == level && event.getEntity() instanceof ItemEntity item && item.position().distanceTo(pos.getCenter()) < 5) drops.add(item); };
            NeoForge.EVENT_BUS.addListener(capture);
        }
        StateTransformerBlockEntity machine() {
            level.setBlock(pos, StateTransformerContent.BLOCK.get().defaultBlockState(), 2);
            return (StateTransformerBlockEntity) level.getBlockEntity(pos);
        }
        FakePlayer player() {
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "tablet-audit"));
            player.setGameMode(GameType.SURVIVAL); player.moveTo(pos.getCenter().add(0, 0, -2)); return player;
        }
        void tick(StateTransformerBlockEntity be, int count) {
            for (int tick = 0; tick < count; tick++) StateTransformerBlockEntity.serverTick(level, pos, be.getBlockState(), be);
        }
        @Override public void close() {
            NeoForge.EVENT_BUS.unregister(capture);
            saved.keySet().forEach(p -> { if (level.getBlockEntity(p) instanceof Container container) container.clearContent(); });
            saved.forEach((p, state) -> level.setBlock(p, state, 2)); drops.forEach(Entity::discard);
            forced.forEach(chunk -> level.setChunkForced(chunk.x, chunk.z, false));
        }
    }
    private static ItemStack tablet() { return new ItemStack(StateTransformerContent.TABLET.get()); }
    private static ItemStack named(Item item, int count, String name) {
        var stack = new ItemStack(item, count); stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)); return stack;
    }
    private static NonNullList<ItemStack> cargo() {
        var cargo = NonNullList.withSize(49, ItemStack.EMPTY);
        cargo.set(0, named(Items.STONE, 23, "Remembered stones"));
        cargo.set(7, new ItemStack(Items.SNOWBALL, 16));
        var sword = named(Items.IRON_SWORD, 1, "Worn blade"); sword.setDamageValue(79); cargo.set(48, sword);
        return cargo;
    }
    private static ItemStack personalTablet() {
        var tablet = tablet(); tablet.set(DataComponents.CUSTOM_NAME, Component.literal("Personal cargo"));
        var tag = new CompoundTag(); tag.putString("PrivateNote", "retain"); tablet.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); return tablet;
    }
    private static void loadCargo(StateTransformerBlockEntity be, List<ItemStack> items) {
        for (int slot = 1; slot < 50; slot++) be.setItem(slot, items.get(slot - 1).copy());
    }

    @GameTest(template="empty", batch="tablet_canonical_survival")
    public static void RealNativeCraftsConsumeSourceMaterialsAndExactlyOneDamagedCatalystUse(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var player = fixture.player(); fixture.level.setBlock(fixture.pos, Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
            var craft = new CraftingMenu(5, player.getInventory(), ContainerLevelAccess.create(fixture.level, fixture.pos));
            for (int slot = 0; slot < 9; slot++) craft.getSlot(slot + 1).set(new ItemStack(slot == 4 ? AbyssalPort.SHADOW_GEM.get()
                    : slot % 2 == 0 ? AbyssalPort.SHADOW_SHARD.get() : ShoggothLairContent.MONOLITH_STONE.get().asItem()));
            helper.assertTrue(craft.getSlot(0).getItem().is(StateTransformerContent.TABLET.get()), "Canonical Stone Tablet craft is unavailable");
            craft.quickMoveStack(player, 0);
            helper.assertTrue(player.getInventory().countItem(StateTransformerContent.TABLET.get()) == 1, "Native tablet craft loses or duplicates output");
            for (int slot = 1; slot <= 9; slot++) helper.assertTrue(!craft.getSlot(slot).hasItem(), "Stone Tablet craft leaves unpaid ingredients");
            for (int damage : List.of(0, 8, 9)) {
                player.getInventory().clearContent(); craft = new CraftingMenu(6, player.getInventory(), ContainerLevelAccess.create(fixture.level, fixture.pos));
                var gem = named(CoraliumContent.TRANSMUTATION_GEM.get(), 1, "Used catalyst"); gem.setDamageValue(damage);
                for (int slot = 0; slot < 9; slot++) craft.getSlot(slot + 1).set(slot == 4 ? gem : new ItemStack(slot == 7 ? Items.CHEST : ShoggothLairContent.MONOLITH_STONE.get().asItem()));
                helper.assertTrue(craft.getSlot(0).getItem().is(StateTransformerContent.MACHINE.get()), "Real wooden-chest or damaged-gem transformer craft missing");
                craft.quickMoveStack(player, 0);
                helper.assertTrue(player.getInventory().countItem(StateTransformerContent.MACHINE.get()) == 1, "Actual native transformer result is not singular");
                var remainder = craft.getSlot(5).getItem();
                helper.assertTrue(damage == 9 ? remainder.isEmpty() : remainder.is(CoraliumContent.TRANSMUTATION_GEM.get()) && remainder.getDamageValue() == damage + 1
                        && remainder.getHoverName().getString().equals("Used catalyst"), "Crafting refreshes or incorrectly charges the transmutation catalyst");
                for (int slot = 1; slot <= 9; slot++) if (slot != 5) helper.assertTrue(!craft.getSlot(slot).hasItem(), "Transformer craft leaves an unpaid chest/stone");
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="tablet_atomic_roundtrip")
    public static void PackUnpackPreservesExactSlotsComponentsValuesAndMidProcessNativeSave(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var be = fixture.machine(); var original = personalTablet(); var cargo = cargo();
            be.setItem(0, original.copy()); loadCargo(be, cargo); fixture.tick(be, 111);
            helper.assertTrue(be.progress() == 111 && !StoneTabletItem.isFilled(be.getItem(0)) && ItemStack.matches(be.getItem(49), cargo.get(48)), "Early pack consumes live inventory");
            var saved = be.saveWithFullMetadata(fixture.level.registryAccess());
            var restored = new StateTransformerBlockEntity(fixture.pos, be.getBlockState()); restored.loadWithComponents(saved, fixture.level.registryAccess());
            helper.assertTrue(restored.progress() == 111 && restored.mode() == 0, "Native reload discards a valid pending transaction");
            fixture.tick(restored, 88); helper.assertTrue(restored.progress() == 199 && ItemStack.matches(restored.getItem(49), cargo.get(48)), "Pack duration differs from source 200 ticks");
            fixture.tick(restored, 1); var packed = restored.getItem(0);
            helper.assertTrue(StoneTabletItem.isFilled(packed) && StoneTabletItem.storedValue(packed) == 151 && packed.hasFoil()
                    && !PotentialEnergyItems.isContainer(packed), "Derived source cargo value or non-rechargeable tablet role differs");
            helper.assertTrue(ItemStack.listMatches(StoneTabletItem.cargo(packed), cargo)
                    && packed.getHoverName().getString().equals("Personal cargo") && packed.get(DataComponents.CUSTOM_DATA).copyTag().getString("PrivateNote").equals("retain"), "Sealing loses native components or positions");
            for (int slot = 1; slot < 50; slot++) helper.assertTrue(restored.getItem(slot).isEmpty(), "Packed cargo also remains live");
            var disk = ItemStack.parse(fixture.level.registryAccess(), packed.save(fixture.level.registryAccess())).orElseThrow();
            helper.assertTrue(ItemStack.matches(packed, disk) && StoneTabletItem.isFilled(disk), "Native tablet serialization loses cargo");
            var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), fixture.level.registryAccess());
            try { ItemStack.STREAM_CODEC.encode(buffer, disk); helper.assertTrue(ItemStack.matches(disk, ItemStack.STREAM_CODEC.decode(buffer)), "Native tablet slot codec changes its data"); }
            finally { buffer.release(); }
            helper.assertTrue(restored.setMode(1), "Idle unpack mode cannot be selected"); fixture.tick(restored, 199);
            helper.assertTrue(restored.getItem(49).isEmpty() && StoneTabletItem.isFilled(restored.getItem(0)), "Unpack writes before 200 ticks");
            var unpackSaved = restored.saveWithFullMetadata(fixture.level.registryAccess());
            var unpack = new StateTransformerBlockEntity(fixture.pos, be.getBlockState()); unpack.loadWithComponents(unpackSaved, fixture.level.registryAccess()); fixture.tick(unpack, 1);
            helper.assertTrue(ItemStack.matches(unpack.getItem(0), original) && !unpack.getItem(0).hasFoil(), "Unpack loses tablet name/private data or retains spent value");
            for (int slot = 1; slot < 50; slot++) helper.assertTrue(ItemStack.matches(unpack.getItem(slot), cargo.get(slot - 1)), "Unpack moves/discards a native cargo slot " + slot);
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="tablet_native_menu_guards")
    public static void NativeMenuClicksBothOperatorsAndModePacketsCannotMutateProcessingSlots(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var be = fixture.machine(); var player = fixture.player(); var other = fixture.player();
            var menu = new StateTransformerMenu(8, player.getInventory(), be); var second = new StateTransformerMenu(9, other.getInventory(), be);
            player.containerMenu = menu; other.containerMenu = second;
            helper.assertTrue(menu.slots.size() == 86 && menu.stillValid(player) && !menu.clickMenuButton(player, -1)
                    && !menu.clickMenuButton(player, 2) && menu.clickMenuButton(player, 1) && menu.clickMenuButton(player, 0), "Native menu mode is not bounded/server-owned");
            player.getInventory().setItem(9, personalTablet()); player.getInventory().setItem(10, new ItemStack(Items.STONE, 17));
            menu.quickMoveStack(player, 50); menu.quickMoveStack(player, 51);
            helper.assertTrue(StoneTabletItem.isBlank(be.getItem(0)) && be.getItem(1).getCount() == 17 && player.getInventory().getItem(10).isEmpty(), "Idle native shift insertion failed");
            fixture.tick(be, 1); var beforeTablet = be.getItem(0).copy(); var beforeCargo = be.getItem(1).copy();
            for (var operator : List.of(player, other)) {
                var activeMenu = (StateTransformerMenu)operator.containerMenu;
                for (var click : List.of(ClickType.PICKUP, ClickType.QUICK_MOVE, ClickType.THROW, ClickType.SWAP)) {
                    activeMenu.clicked(0, 0, click, operator); activeMenu.clicked(1, 0, click, operator);
                }
                activeMenu.setCarried(new ItemStack(Items.STONE, 5)); activeMenu.clicked(1, 0, ClickType.PICKUP_ALL, operator);
                activeMenu.clicked(52, 0, ClickType.PICKUP_ALL, operator);
                helper.assertTrue(activeMenu.getCarried().getCount() == 5 && activeMenu.quickMoveStack(operator, 1).isEmpty()
                        && !activeMenu.clickMenuButton(operator, 1) && !activeMenu.getSlot(0).mayPickup(operator)
                        && !activeMenu.getSlot(1).mayPlace(new ItemStack(Items.STONE)) && !activeMenu.canDragTo(activeMenu.getSlot(1)), "Processing slot/mode locked only for one operator or click type");
                activeMenu.setCarried(ItemStack.EMPTY);
            }
            helper.assertTrue(ItemStack.matches(beforeTablet, be.getItem(0)) && ItemStack.matches(beforeCargo, be.getItem(1)), "Actual native clicks changed pending inventory");
            player.moveTo(fixture.pos.getCenter().add(20, 0, 0)); helper.assertTrue(!menu.stillValid(player) && !menu.clickMenuButton(player, 0), "Distant menu retains machine authority");
            fixture.tick(be, 199); other.moveTo(fixture.pos.getCenter().add(0, 0, -2));
            second.quickMoveStack(other, 0); helper.assertTrue(be.getItem(0).isEmpty() && other.getInventory().countItem(StateTransformerContent.TABLET.get()) == 1, "Native completed tablet is unusable");
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="tablet_invalid_data")
    public static void NestedInventoriesMalformedCursesOccupiedOutputsAndLateMutationNeverOverwriteOrDuplicate(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var be = fixture.machine(); be.setItem(0, personalTablet());
            for (Item item : List.of(Items.SHULKER_BOX, Items.BUNDLE, CrystalBagContent.SMALL.get(), StateTransformerContent.TABLET.get())) {
                var forbidden = new ItemStack(item); be.setItem(1, forbidden); fixture.tick(be, 220);
                helper.assertTrue(be.progress() == 0 && be.getItem(1) == forbidden && StoneTabletItem.isBlank(be.getItem(0)), "Nested inventory becomes sealed cargo");
            }
            var componentContainer = new ItemStack(Items.STICK); componentContainer.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            helper.assertTrue(!StoneTabletItem.acceptsCargo(componentContainer), "Component-bearing inventory laundered as ordinary item");
            var blockInventory = new ItemStack(Items.CHEST); var blockTag = new CompoundTag(); blockTag.put("Items", new net.minecraft.nbt.ListTag());
            blockInventory.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockTag));
            helper.assertTrue(!StoneTabletItem.acceptsCargo(blockInventory), "Block-entity inventory bypasses nested-storage rejection");
            var corrupt = new ItemStack(Items.IRON_SWORD, 2); be.setItem(1, corrupt); fixture.tick(be, 200);
            helper.assertTrue(be.getItem(1).getCount() == 2 && be.progress() == 0, "Invalid stack silently truncated or processed");
            var originals = cargo(); be.clearContent(); be.setItem(0, StoneTabletItem.seal(personalTablet(), originals)); be.setMode(1);
            var occupied = named(Items.DIAMOND, 5, "Must remain"); be.setItem(49, occupied); fixture.tick(be, 220);
            helper.assertTrue(ItemStack.matches(be.getItem(49), occupied) && be.progress() == 0 && StoneTabletItem.isFilled(be.getItem(0)), "Unpack overwrites an occupied destination");
            be.setItem(49, ItemStack.EMPTY); fixture.tick(be, 199);
            be.setItem(48, occupied.copy()); fixture.tick(be, 1);
            helper.assertTrue(be.progress() == 0 && ItemStack.matches(be.getItem(48), occupied) && StoneTabletItem.isFilled(be.getItem(0)), "Late external write bypasses atomic unpack revalidation");
            be.clearContent(); be.setMode(0); var named = personalTablet(); be.setItem(0, named); be.setItem(1, new ItemStack(Items.STONE)); fixture.tick(be, 199);
            helper.assertTrue(be.progress() == 199, "Pack mutation fixture is not at the transaction boundary");
            be.getItem(1).grow(1); fixture.tick(be, 1);
            helper.assertTrue(be.progress() == 0 && StoneTabletItem.isBlank(named) && be.getItem(1).getCount() == 2, "Direct in-place mutation completes an old pack snapshot");
            fixture.tick(be, 200); helper.assertTrue(StoneTabletItem.storedValue(be.getItem(0)) == 2, "Fresh transaction does not use actual mutated cargo");
            for (float badEnergy : new float[]{Float.NaN, Float.POSITIVE_INFINITY, -1, 50000}) {
                var bad = StoneTabletItem.seal(personalTablet(), originals); var tag = bad.get(DataComponents.CUSTOM_DATA).copyTag(); tag.putFloat("PotEnergy", badEnergy); bad.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                be.clearContent(); be.setMode(1); be.setItem(0, bad); var before = bad.copy(); fixture.tick(be, 210);
                helper.assertTrue(!StoneTabletItem.isFilled(bad) && be.progress() == 0 && ItemStack.matches(before, be.getItem(0)), "Malformed tablet is rewritten/emptied or used for forged PE");
            }
            var cursed = StoneTabletItem.seal(personalTablet(), originals); var curse = cursed.get(DataComponents.CUSTOM_DATA).copyTag(); curse.putBoolean("Cursed", false); cursed.set(DataComponents.CUSTOM_DATA, CustomData.of(curse));
            be.clearContent(); be.setItem(0, cursed); fixture.tick(be, 210);
            helper.assertTrue(StoneTabletItem.isCursed(cursed) && StoneTabletItem.isFilled(cursed) && be.progress() == 0 && !be.canPlaceItem(0, cursed), "Source curse-key presence is ignored");
            var oversize = new ArrayList<>(originals); oversize.add(new ItemStack(Items.STONE)); var invalid = tablet();
            invalid.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(oversize)); var tag = new CompoundTag(); tag.putFloat("PotEnergy", 152); invalid.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            helper.assertTrue(!StoneTabletItem.isFilled(invalid), "Oversized tablet has a reachable non-existent cargo slot");
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="tablet_sided_automation")
    public static void NativeSidedCapabilitiesFollowBothModesAndCannotBypassBusyOrNestedInventoryGates(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var be = fixture.machine();
            helper.assertTrue(fixture.level.getCapability(Capabilities.ItemHandler.BLOCK, fixture.pos, null) == null, "Source disallows a null-side bypass");
            var top = fixture.level.getCapability(Capabilities.ItemHandler.BLOCK, fixture.pos, Direction.UP);
            var side = fixture.level.getCapability(Capabilities.ItemHandler.BLOCK, fixture.pos, Direction.WEST);
            var bottom = fixture.level.getCapability(Capabilities.ItemHandler.BLOCK, fixture.pos, Direction.DOWN);
            helper.assertTrue(top.getSlots() == 50 && side.getSlots() == 1 && side.insertItem(0, personalTablet(), false).isEmpty(), "Pack cannot accept a native side tablet");
            helper.assertTrue(!top.insertItem(0, tablet(), false).isEmpty() && top.insertItem(49, new ItemStack(Items.SNOWBALL, 16), false).isEmpty()
                    && !top.insertItem(1, new ItemStack(Items.SHULKER_BOX), false).isEmpty(), "Native top admits tablet or nested cargo");
            fixture.tick(be, 1);
            helper.assertTrue(!top.insertItem(1, new ItemStack(Items.STONE), false).isEmpty() && side.extractItem(0, 1, false).isEmpty()
                    && bottom.extractItem(0, 1, false).isEmpty(), "Busy sided wrappers bypass transaction lock");
            fixture.tick(be, 199); var packed = bottom.extractItem(0, 1, false);
            helper.assertTrue(StoneTabletItem.isFilled(packed) && StoneTabletItem.storedValue(packed) == 64 && be.isEmpty(), "Actual native pack output extraction loses or duplicates cargo");
            be.setMode(1);
            helper.assertTrue(side.getSlots() == 0 && top.getSlots() == 1 && bottom.getSlots() == 50, "Retained wrapper has stale mode slot mapping");
            helper.assertTrue(top.insertItem(0, packed, false).isEmpty() && !bottom.insertItem(1, new ItemStack(Items.STONE), false).isEmpty(), "Unpack admits bottom insertion");
            fixture.tick(be, 200);
            helper.assertTrue(StoneTabletItem.isBlank(bottom.extractItem(0, 1, false)) && bottom.extractItem(49, 64, false).getCount() == 16
                    && be.isEmpty(), "Unpack automation cannot return original blank and exact last slot");
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="tablet_native_place_mine")
    public static void NativePlacementFacingStateMenuAndMiningPreserveAllLiveContentsExactlyOnce(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var player = fixture.player(); player.setYRot(0); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(StateTransformerContent.MACHINE.get()));
            helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(fixture.pos.below().getCenter().add(0, .5, 0), Direction.UP, fixture.pos.below(), false))).consumesAction(), "Native transformer BlockItem placement rejected");
            var be = (StateTransformerBlockEntity)fixture.level.getBlockEntity(fixture.pos); var initial = be.getBlockState();
            helper.assertTrue(initial.getValue(StateTransformerBlock.FACING) == player.getDirection().getOpposite()
                    && initial.rotate(Rotation.CLOCKWISE_90).getValue(StateTransformerBlock.FACING) == initial.getValue(StateTransformerBlock.FACING).getClockWise(), "Source orientation is not a real native property");
            var original = personalTablet(); be.setItem(0, original.copy()); loadCargo(be, cargo());
            fixture.tick(be, 1);
            helper.assertTrue(fixture.level.getBlockEntity(fixture.pos) == be && be.getBlockState().getValue(StateTransformerBlock.TABLET)
                    && be.getBlockState().getAnalogOutputSignal(fixture.level, fixture.pos) == 0, "Tablet visual state replaces BE or busy comparator stays charged");
            var originals = new ArrayList<ItemStack>(); for (int slot = 0; slot < 50; slot++) if (!be.getItem(slot).isEmpty()) originals.add(be.getItem(slot).copy());
            helper.assertTrue(fixture.level.destroyBlock(fixture.pos, true), "Actual native transformer break rejected");
            helper.assertTrue(fixture.drops.stream().filter(drop -> drop.getItem().is(StateTransformerContent.MACHINE.get())).count() == 1, "Native mining loses/duplicates the machine");
            for (var item : originals) helper.assertTrue(fixture.drops.stream().filter(drop -> ItemStack.isSameItemSameComponents(drop.getItem(), item)).mapToInt(drop -> drop.getItem().getCount()).sum() == item.getCount(), "Busy mining duplicates or loses live contents");
            helper.assertTrue(fixture.drops.stream().noneMatch(drop -> StoneTabletItem.isFilled(drop.getItem())), "Busy break both seals and drops cargo");
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="tablet_registered_live", timeoutTicks=480)
    public static void RegisteredTransformerTickerCompletesBothNativeTenSecondModesWithoutManualTicking(GameTestHelper helper) { live(helper); }
    @GameTest(template="empty", templateNamespace="abyssalport_worldtest", batch="tablet_normal_live", timeoutTicks=480)
    public static void NormalWorldTransformerTickerCompletesSurvivalItemStorageAndRestoresComponents(GameTestHelper helper) { live(helper); }

    @GameTest(template="empty", batch="tablet_native_open_and_data")
    public static void RealServerPlayerRightClickOpensRegisteredMenuAndRejectsStaleModeAndDataWrites(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var be = fixture.machine();
            var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "tablet-menu-audit"), false);
            var player = new ServerPlayer(fixture.level.getServer(), fixture.level, cookie.gameProfile(), cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
            try {
                net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
                fixture.level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
                player.setGameMode(GameType.SURVIVAL); player.setNoGravity(true);
                player.teleportTo(fixture.level, fixture.pos.getX() + .5, fixture.pos.getY() + .5, fixture.pos.getZ() - 1.5, 0, 0);
                var hit = new BlockHitResult(fixture.pos.getCenter(), Direction.NORTH, fixture.pos, false);
                helper.assertTrue(be.getBlockState().useWithoutItem(fixture.level, player, hit).consumesAction()
                        && player.containerMenu instanceof StateTransformerMenu, "Actual right-click never opens the registered server menu");
                var menu = (StateTransformerMenu)player.containerMenu;
                helper.assertTrue(menu.getType() == StateTransformerContent.MENU.get() && menu.slots.size() == 86
                        && menu.clickMenuButton(player, 1) && be.mode() == 1, "Actual server container has no authorized mode control");
                menu.setData(0, 199); menu.setData(1, 0);
                helper.assertTrue(be.progress() == 0 && be.mode() == 1, "DataSlot writes assign authoritative process or mode values");
                var clientMenu = StateTransformerContent.MENU.get().create(24, player.getInventory());
                clientMenu.setData(0, 173); clientMenu.setData(1, 1);
                helper.assertTrue(clientMenu.progress() == 173 && clientMenu.mode() == 1 && clientMenu.slots.size() == 86,
                        "Native client menu loses synchronized progress/mode or slot structure");
                player.closeContainer(); helper.assertTrue(!menu.clickMenuButton(player, 0) && be.mode() == 1, "Closed menu can change remote machine mode");
                fixture.level.setBlock(fixture.pos, Blocks.AIR.defaultBlockState(), 3);
                helper.assertTrue(!menu.stillValid(player), "Removed machine leaves an authoritative live menu");
                helper.succeed();
            } finally {
                player.closeContainer(); fixture.level.getServer().getPlayerList().remove(player); channel.finishAndReleaseAll();
            }
        }
    }

    @GameTest(template="empty", batch="tablet_capacity_and_bad_save")
    public static void All49SlotsMaximumValueAndInvalidPendingSaveRemainAtomic(GameTestHelper helper) {
        try (var fixture = new Fixture(helper)) {
            var be = fixture.machine(); be.setItem(0, personalTablet());
            for (int slot = 1; slot < 50; slot++) be.setItem(slot, named(Items.STONE, 64, "Slot " + slot));
            helper.assertTrue(be.comparatorSignal() == 14, "Source comparator must divide 49 cargo slots by the full 50-slot machine");
            fixture.tick(be, 200);
            helper.assertTrue(StoneTabletItem.storedValue(be.getItem(0)) == 3136 && StoneTabletItem.cargo(be.getItem(0)).size() == 49,
                    "Full native cargo exceeds source capacity or loses its last slot");
            be.setMode(1); fixture.tick(be, 87); var saved = be.saveWithFullMetadata(fixture.level.registryAccess());
            saved.remove("ActiveItems"); saved.putInt("ProcessingTime", Integer.MAX_VALUE);
            var reload = new StateTransformerBlockEntity(fixture.pos, be.getBlockState()); reload.loadWithComponents(saved, fixture.level.registryAccess());
            helper.assertTrue(reload.progress() == 0 && StoneTabletItem.storedValue(reload.getItem(0)) == 3136,
                    "Untrusted/missing snapshot awards a near-complete transaction or discards cargo");
            fixture.tick(reload, 200);
            for (int slot = 1; slot < 50; slot++) helper.assertTrue(reload.getItem(slot).getCount() == 64
                    && reload.getItem(slot).getHoverName().getString().equals("Slot " + slot), "Native full tablet moved or lost named slot " + slot);
            helper.assertTrue(StoneTabletItem.isBlank(reload.getItem(0)), "Full unpack does not return the named blank tablet"); helper.succeed();
        }
    }

    @GameTest(template="empty", batch="tablet_real_hoppers", timeoutTicks=300)
    public static void RealRegisteredHoppersFeedAndExtractExactlyOneSealedTabletWithoutManualMachineTicks(GameTestHelper helper) {
        var fixture = new Fixture(helper);
        try {
            var be = fixture.machine();
            fixture.level.setBlock(fixture.pos.above(), Blocks.HOPPER.defaultBlockState(), 3);
            fixture.level.setBlock(fixture.pos.west(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST), 3);
            fixture.level.setBlock(fixture.pos.below(), Blocks.HOPPER.defaultBlockState(), 3);
            fixture.level.setBlock(fixture.pos.below(2), Blocks.CHEST.defaultBlockState(), 3);
            var input = (net.minecraft.world.level.block.entity.HopperBlockEntity)fixture.level.getBlockEntity(fixture.pos.above());
            var side = (net.minecraft.world.level.block.entity.HopperBlockEntity)fixture.level.getBlockEntity(fixture.pos.west());
            var bottom = (net.minecraft.world.level.block.entity.HopperBlockEntity)fixture.level.getBlockEntity(fixture.pos.below());
            var output = (net.minecraft.world.level.block.entity.ChestBlockEntity)fixture.level.getBlockEntity(fixture.pos.below(2));
            var sword = named(Items.IRON_SWORD, 1, "Hopper blade"); sword.setDamageValue(43);
            input.setItem(0, sword.copy()); side.setItem(0, personalTablet());
            helper.runAfterDelay(260, () -> {
                try {
                    var sealed = output.getItem(0);
                    helper.assertTrue(StoneTabletItem.isFilled(sealed) && StoneTabletItem.storedValue(sealed) == 64
                            && ItemStack.matches(StoneTabletItem.cargo(sealed).getFirst(), sword) && be.isEmpty()
                            && input.isEmpty() && side.isEmpty() && bottom.isEmpty(), "Real world hopper feed/extraction loses native cargo, starts no transaction or duplicates a tablet");
                    com.mojang.logging.LogUtils.getLogger().info("NATIVE TABLET HOPPER AUDIT: side blank + top damaged sword -> registered 200-tick pack -> bottom hopper -> one chest tablet, 64 derived PE");
                    helper.succeed();
                } finally { input.clearContent(); side.clearContent(); bottom.clearContent(); output.clearContent(); fixture.close(); }
            });
        } catch (RuntimeException error) { fixture.close(); throw error; }
    }

    private static void live(GameTestHelper helper) {
        var fixture = new Fixture(helper);
        try {
            var be = fixture.machine(); var original = personalTablet(); var contents = cargo(); be.setItem(0, original.copy()); loadCargo(be, contents);
            helper.runAfterDelay(205, () -> {
                try {
                    helper.assertTrue(StoneTabletItem.isFilled(be.getItem(0)) && be.progress() == 0 && StoneTabletItem.storedValue(be.getItem(0)) == 151
                            && be.getItem(49).isEmpty(), "Actual registered ticker does not seal native cargo");
                    helper.assertTrue(be.setMode(1), "Idle mode does not change after registered pack");
                    helper.runAfterDelay(205, () -> {
                        try {
                            helper.assertTrue(ItemStack.matches(be.getItem(0), original) && be.progress() == 0, "Actual registered unpack fails to clear only cargo/value");
                            for (int slot = 1; slot < 50; slot++) helper.assertTrue(ItemStack.matches(be.getItem(slot), contents.get(slot - 1)), "Registered unpack loses cargo slot " + slot);
                            com.mojang.logging.LogUtils.getLogger().info("NATIVE STATE TRANSFORMER WORLD AUDIT: pack=200 ticks, cargoPE=151, unpack=200 ticks, 49 exact slots/name/damage/components restored");
                            helper.succeed();
                        } finally { fixture.close(); }
                    });
                } catch (RuntimeException error) { fixture.close(); throw error; }
            });
        } catch (RuntimeException error) { fixture.close(); throw error; }
    }
}
