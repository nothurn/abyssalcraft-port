package dev.abyssalport;

import com.mojang.authlib.GameProfile;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.content.*;
import dev.abyssalport.energy.*;
import dev.abyssalport.item.StoneTabletItem;
import dev.abyssalport.menu.EnergyDepositionerMenu;
import dev.abyssalport.worldgen.DarklandsWorldgen;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnergyDepositionerGameTests {
    private EnergyDepositionerGameTests() { }
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level;
        final BlockPos pos;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Consumer<DepositionEvent> veto;
        Fixture(GameTestHelper helper) {
            level = helper.getLevel(); pos = helper.absolutePos(new BlockPos(3, 25, 3));
            for (var p : BlockPos.betweenClosed(pos.offset(-5, -2, -5), pos.offset(5, 2, 5))) {
                helper.assertTrue(level.getBlockEntity(p) == null, "Depositioner fixture overlaps live inventory"); saved.put(p.immutable(), level.getBlockState(p));
            }
            saved.keySet().forEach(p -> level.setBlock(p, p.getY() == pos.getY() - 2 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2));
            for (int x = (pos.getX() - 5) >> 4; x <= (pos.getX() + 5) >> 4; x++) for (int z = (pos.getZ() - 5) >> 4; z <= (pos.getZ() + 5) >> 4; z++) {
                var chunk = new ChunkPos(x, z); if (!level.getForcedChunks().contains(chunk.toLong())) { forced.add(chunk); level.setChunkForced(x, z, true); }
            }
            veto = event -> { if (event.level() == level && event.machine().equals(pos)) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
        }
        EnergyDepositionerBlockEntity machine() {
            level.setBlock(pos, EnergyDepositionerContent.BLOCK.get().defaultBlockState(), 2);
            return (EnergyDepositionerBlockEntity)level.getBlockEntity(pos);
        }
        FakePlayer player() { var p = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "deposition-audit")); p.setGameMode(GameType.SURVIVAL); p.moveTo(pos.getCenter().add(0, 0, -2)); return p; }
        void ticks(EnergyDepositionerBlockEntity be, int count) { for (int i = 0; i < count; i++) be.tick(level, RandomSourceZero.INSTANCE); }
        @Override public void close() {
            NeoForge.EVENT_BUS.unregister(veto);
            saved.keySet().forEach(p -> { if (level.getBlockEntity(p) instanceof Container c) c.clearContent(); });
            saved.forEach((p, state) -> level.setBlock(p, state, 2)); forced.forEach(c -> level.setChunkForced(c.x, c.z, false));
        }
    }
    private static final class RandomSourceZero extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        static final RandomSourceZero INSTANCE = new RandomSourceZero();
        RandomSourceZero() { super(771); }
        @Override public int nextInt(int bound) { return 0; }
        @Override public float nextFloat() { return .99F; }
    }
    private static ItemStack fuel(int value) {
        var cargo = NonNullList.withSize(49, ItemStack.EMPTY); cargo.set(0, new ItemStack(Items.STONE, value));
        var blank = new ItemStack(StateTransformerContent.TABLET.get()); blank.set(DataComponents.CUSTOM_NAME, Component.literal("Remembered tablet"));
        var tag = new net.minecraft.nbt.CompoundTag(); tag.putString("PrivateNote", "keep"); blank.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return StoneTabletItem.seal(blank, cargo);
    }
    @GameTest(template="empty", batch="deposition_source_recipe")
    public static void RealNativeRecipePaysFiveStonesTwoGemsRelayAndExactlyOneCatalystUse(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            f.level.setBlock(f.pos, Blocks.CRAFTING_TABLE.defaultBlockState(), 2); var p = f.player();
            var menu = new CraftingMenu(8, p.getInventory(), ContainerLevelAccess.create(f.level, f.pos));
            for (int damage : List.of(0, 8, 9)) {
                p.getInventory().clearContent();
                for (int i = 0; i < 9; i++) {
                    var stack = new ItemStack(i == 3 || i == 5 ? AbyssalPort.SHADOW_GEM.get() : i == 4 ? CoraliumContent.TRANSMUTATION_GEM.get()
                            : i == 7 ? AbyssalPort.ENERGY_RELAY_ITEM.get() : ShoggothLairContent.MONOLITH_STONE.get().asItem());
                    if (i == 4) stack.setDamageValue(damage); menu.getSlot(i + 1).set(stack);
                }
                helper.assertTrue(menu.getSlot(0).getItem().is(EnergyDepositionerContent.ITEM.get()), "Original depositioner recipe is unreachable");
                menu.quickMoveStack(p, 0);
                helper.assertTrue(p.getInventory().countItem(EnergyDepositionerContent.ITEM.get()) == 1, "Native craft duplicates or loses machine");
                for (int i = 1; i <= 9; i++) if (i != 5) helper.assertTrue(!menu.getSlot(i).hasItem(), "Native craft leaves an unpaid ingredient");
                var remainder = menu.getSlot(5).getItem();
                helper.assertTrue(damage == 9 ? remainder.isEmpty() : remainder.getDamageValue() == damage + 1, "Depositioner craft refreshes catalyst uses");
            }
            helper.succeed();
        }
    }
    @GameTest(template="empty", batch="deposition_persisted_job")
    public static void TwoHundredTicksMidProcessSaveAndOccupiedOutputCannotCloneFuelOrOverwriteItems(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var be = f.machine(); var tablet = fuel(37); be.setItem(0, tablet.copy()); f.ticks(be, 73);
            helper.assertTrue(be.hasJob() && be.getItem(0).isEmpty() && be.progress() == 73 && Math.abs(be.energy() - 37 * 73F / 200) < .001, "Fuel remains extractable after generating PE");
            be.setItem(1, new ItemStack(Items.DIAMOND)); f.ticks(be, 5);
            helper.assertTrue(be.progress() == 73 && be.getItem(1).is(Items.DIAMOND), "Occupied output is overwritten or extra PE is generated");
            var save = be.saveWithFullMetadata(f.level.registryAccess());
            var reload = new EnergyDepositionerBlockEntity(f.pos, be.getBlockState()); reload.loadWithComponents(save, f.level.registryAccess()); reload.setLevel(f.level);
            helper.assertTrue(reload.progress() == 73 && reload.hasJob() && reload.getItem(1).is(Items.DIAMOND), "Pending source job is lost at native load");
            reload.removeItem(1, 1); f.ticks(reload, 126);
            helper.assertTrue(reload.progress() == 199 && reload.getItem(1).isEmpty(), "Source process duration is not 200 ticks"); f.ticks(reload, 1);
            var blank = reload.getItem(1);
            helper.assertTrue(!reload.hasJob() && Math.abs(reload.energy() - 37) < .002 && !StoneTabletItem.isFilled(blank)
                    && blank.getHoverName().getString().equals("Remembered tablet") && blank.get(DataComponents.CUSTOM_DATA).copyTag().getString("PrivateNote").equals("keep"), "Output/value/components are not conserved");
            reload.setItem(0, fuel(13)); f.ticks(reload, 201);
            helper.assertTrue(Math.abs(reload.energy() - 37) < .002 && reload.getItem(0).has(DataComponents.CONTAINER), "Full output destroys queued sealed cargo");
            helper.succeed();
        }
    }
    @GameTest(template="empty", batch="deposition_real_mining")
    public static void ActualSurvivalMiningAndPlacementCarryOneJobWithoutExposingReusableCargo(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var be = f.machine(); be.setItem(0, fuel(41)); f.ticks(be, 83); be.activateCharm(CharmAmplifier.DURATION);
            var drops = new ArrayList<ItemEntity>();
            Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> capture = event -> { if (event.getLevel() == f.level && event.getEntity() instanceof ItemEntity item && item.position().distanceTo(f.pos.getCenter()) < 3) drops.add(item); };
            NeoForge.EVENT_BUS.addListener(capture);
            try {
                f.level.destroyBlock(f.pos, true, f.player());
                helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(EnergyDepositionerContent.ITEM.get()), "Native mining spills reusable fuel or double-drops inventory");
                var machine = drops.getFirst().getItem().copy(); drops.getFirst().discard();
                helper.assertTrue(machine.getMaxStackSize() == 1 && !StoneTabletItem.acceptsCargo(machine), "Stateful job can stack or be resealed as recyclable cargo");
                var p = f.player(); p.setItemInHand(InteractionHand.MAIN_HAND, machine);
                var floor = f.pos.below(); f.level.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
                helper.assertTrue(machine.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(floor.getCenter().add(0, .5, 0), Direction.UP, floor, false))).consumesAction(), "Native carried job cannot be placed");
                var placed = (EnergyDepositionerBlockEntity)f.level.getBlockEntity(f.pos);
                helper.assertTrue(placed != null && placed.progress() == 83 && placed.hasJob() && placed.activeAmplifier() == CharmAmplifier.DURATION, "Mining/placement refreshes pending progress or modifier");
                f.ticks(placed, 117);
                helper.assertTrue(Math.abs(placed.energy() - 41) < .002 && !StoneTabletItem.isFilled(placed.getItem(1)), "Mining a drained job duplicates PE/cargo"); helper.succeed();
            } finally { NeoForge.EVENT_BUS.unregister(capture); drops.forEach(ItemEntity::discard); }
        }
    }
    @GameTest(template="empty", batch="deposition_corrupt_job")
    public static void InvalidStoredJobRemainsCarriedWithoutPEReplayOrOverwritingQueuedFuel(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var be = f.machine(); be.setItem(0, fuel(17)); f.ticks(be, 73); be.setItem(0, fuel(5));
            for (boolean badCargo : List.of(false, true)) {
                var tag = be.saveWithFullMetadata(f.level.registryAccess());
                if (badCargo) { var cursed = fuel(17); StoneTabletItem.curse(cursed); tag.put("ProcessingStack", cursed.save(f.level.registryAccess())); }
                else tag.putInt("ProcessingTime", 200);
                var restored = new EnergyDepositionerBlockEntity(f.pos, be.getBlockState()); restored.loadWithComponents(tag, f.level.registryAccess()); restored.setLevel(f.level);
                float before = restored.energy(); f.ticks(restored, 201);
                var after = restored.saveWithFullMetadata(f.level.registryAccess());
                helper.assertTrue(restored.energy() == before && restored.getItem(1).isEmpty() && restored.hasJob()
                        && StoneTabletItem.storedValue(restored.getItem(0)) == 5
                        && after.getCompound("ProcessingStack").equals(tag.getCompound("ProcessingStack")), "Invalid serialized job is silently lost, reset for PE replay or overwrites queued cargo");
            }
            helper.succeed();
        }
    }
    @GameTest(template="empty", batch="deposition_disruption_reset")
    public static void SourceDisruptionReservesThresholdBeforeVetoAndCannotReenterTheEnergyTick(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var be = f.machine(); var tag = be.saveWithFullMetadata(f.level.registryAccess()); tag.putInt("Tolerance", 199); be.loadWithComponents(tag, f.level.registryAccess());
            be.addEnergy(100); var target = f.pos.offset(3, 0, 0); f.level.setBlock(target, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 2);
            var collector = (EnergyCollectorBlockEntity)f.level.getBlockEntity(target); int[] events = {0};
            Consumer<DisruptionEvent> veto = event -> {
                if (event.level() == f.level && event.pos().equals(f.pos)) {
                    helper.assertTrue(be.tolerance() == 0, "Source threshold is not reserved before native event"); events[0]++; event.setCanceled(true); be.tick(f.level, RandomSourceZero.INSTANCE);
                }
            };
            NeoForge.EVENT_BUS.addListener(veto);
            try {
                be.tick(f.level, RandomSourceZero.INSTANCE);
                helper.assertTrue(be.tolerance() == 0 && be.energy() == 85 && collector.energy() == 15, "Protection callback reenters and double-transfers PE");
                // Disruptions may be disabled by the preserved server config; never change it for a test.
                helper.assertTrue(dev.abyssalport.config.MobEcologyConfig.disruptionsDisabled() ? events[0] == 0 : events[0] == 1, "Source disruption dispatch count is wrong"); helper.succeed();
            } finally { NeoForge.EVENT_BUS.unregister(veto); }
        }
    }
    @GameTest(template="empty", batch="deposition_roles_and_charms")
    public static void SourceCollectorCharmsAndCapacityKeepTransfersConservedAndRejectPassiveReceivers(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var be = f.machine(); var target = f.pos.offset(3, 0, 0); f.level.setBlock(target, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 2);
            var collector = (EnergyCollectorBlockEntity)f.level.getBlockEntity(target); collector.addEnergy(990); be.addEnergy(100); be.tick(f.level, RandomSourceZero.INSTANCE);
            helper.assertTrue(collector.energy() == 1000 && be.energy() == 90 && be.tolerance() == 1, "Full collector wastes PE or tolerance grows without transfer");
            collector.consumeEnergy(1000); var p = f.player(); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CharmContent.item(CharmAmplifier.POWER), 2));
            p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(f.pos.getCenter(), Direction.UP, f.pos, false)));
            helper.assertTrue(be.activeAmplifier() == CharmAmplifier.POWER && p.getMainHandItem().getCount() == 1 && be.energyQuanta() == 30, "Actual modifier use is cosmetic or consumes wrong quantity");
            be.tick(f.level, RandomSourceZero.INSTANCE); helper.assertTrue(be.energy() == 60 && collector.energy() == 30 && be.tolerance() == 3, "Source amplified transfer differs from 30 PE / 2 tolerance");
            helper.assertTrue(be.clearCharm() && be.activateCharm(CharmAmplifier.DURATION) && be.transferBound() == 80, "Source Duration factor is wrong");
            helper.assertTrue(be.clearCharm() && be.activateCharm(CharmAmplifier.RANGE), "Range charm cannot activate"); be.refreshCollectors(f.level);
            helper.assertTrue(be.collectorPositions().contains(target) && !be.acceptsExternalEnergy(), "Depositioner incorrectly acts as an externally rechargeable collector");
            f.level.setBlock(target, AbyssalPort.ENERGY_RELAY.get().defaultBlockState(), 2); be.refreshCollectors(f.level); var relay = (EnergyRelayBlockEntity)f.level.getBlockEntity(target);
            be.tick(f.level, RandomSourceZero.INSTANCE); helper.assertTrue(relay.energy() == 0 && be.energy() == 60, "Passive relay is mistaken for a source collector");
            be.consumeEnergy(60); be.addEnergy(9999); be.setItem(0, fuel(5)); f.ticks(be, 200);
            helper.assertTrue(be.energy() == 10000 && !StoneTabletItem.isFilled(be.getItem(1)), "Source 10000 PE cap overflows or returns spent cargo"); helper.succeed();
        }
    }
    @GameTest(template="empty", batch="deposition_emitter_callback")
    public static void MiningEmitterInsideNativeProtectionCallbackCannotCopyItsPEIntoACollector(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var be = f.machine(); be.setItem(0, fuel(37)); f.ticks(be, 199); be.addEnergy(100);
            var target = f.pos.offset(3, 0, 0); f.level.setBlock(target, AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(), 2);
            var collector = (EnergyCollectorBlockEntity)f.level.getBlockEntity(target); var drops = new ArrayList<ItemEntity>(); int[] callbacks = {0};
            Consumer<DepositionEvent> removeEmitter = event -> {
                if (event.level() == f.level && event.machine().equals(f.pos)) { callbacks[0]++; event.setCanceled(true); f.level.destroyBlock(f.pos, true, f.player()); }
            };
            Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> capture = event -> { if (event.getLevel() == f.level && event.getEntity() instanceof ItemEntity item && item.position().distanceTo(f.pos.getCenter()) < 3) drops.add(item); };
            NeoForge.EVENT_BUS.unregister(f.veto); NeoForge.EVENT_BUS.addListener(removeEmitter); NeoForge.EVENT_BUS.addListener(capture);
            try {
                f.ticks(be, 1);
                helper.assertTrue(callbacks[0] == 1 && drops.size() == 1 && collector.energy() == 0, "Removed source instance still transfers its carried energy after a native callback");
                var data = drops.getFirst().getItem().get(DataComponents.CUSTOM_DATA);
                helper.assertTrue(data != null && Math.abs(data.copyTag().getFloat("PotEnergy") - 137) < .002, "Callback mining loses or duplicates the generated/carried PE balance");
                helper.succeed();
            } finally { NeoForge.EVENT_BUS.unregister(removeEmitter); NeoForge.EVENT_BUS.unregister(capture); drops.forEach(ItemEntity::discard); }
        }
    }
    @GameTest(template="empty", batch="deposition_menu_and_handlers")
    public static void NativeSidedHandlersRejectBlankCursedMalformedTabletsAndMenuCannotInsertIntoOutput(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var be = f.machine(); var top = f.level.getCapability(Capabilities.ItemHandler.BLOCK, f.pos, Direction.UP);
            var bottom = f.level.getCapability(Capabilities.ItemHandler.BLOCK, f.pos, Direction.DOWN);
            helper.assertTrue(top != null && bottom != null && top.getSlots() == 1 && bottom.getSlots() == 1
                    && f.level.getCapability(Capabilities.ItemHandler.BLOCK, f.pos, Direction.NORTH) == null
                    && f.level.getCapability(Capabilities.ItemHandler.BLOCK, f.pos, null) == null, "Source automation faces are incorrect");
            var cursed = fuel(12); StoneTabletItem.curse(cursed); var bad = fuel(12);
            var corrupt = bad.get(DataComponents.CUSTOM_DATA).copyTag(); corrupt.putFloat("PotEnergy", Float.NaN); bad.set(DataComponents.CUSTOM_DATA, CustomData.of(corrupt));
            for (var invalid : List.of(new ItemStack(StateTransformerContent.TABLET.get()), cursed, bad))
                helper.assertTrue(ItemStack.matches(top.insertItem(0, invalid.copy(), false), invalid), "Invalid source tablet enters automation");
            var valid = fuel(12); helper.assertTrue(top.insertItem(0, valid.copy(), true).isEmpty() && be.isEmpty(), "Simulated insertion mutates native inventory");
            helper.assertTrue(top.insertItem(0, valid, false).isEmpty() && top.extractItem(0, 1, false).isEmpty(), "Top can extract reusable source fuel");
            var p = f.player(); var menu = new EnergyDepositionerMenu(9, p.getInventory(), be);
            helper.assertTrue(menu.slots.size() == 38 && !menu.getSlot(1).mayPlace(fuel(1)), "Menu output accepts input or wrong inventory size");
            f.ticks(be, 200); helper.assertTrue(!bottom.extractItem(0, 1, true).isEmpty() && !be.getItem(1).isEmpty(), "Simulated output extraction deletes tablet");
            var output = bottom.extractItem(0, 1, false); helper.assertTrue(!StoneTabletItem.isFilled(output) && be.getItem(1).isEmpty(), "Native bottom extraction returns full cargo");
            p.moveTo(f.pos.getCenter().add(30, 0, 0)); helper.assertTrue(!menu.stillValid(p) && menu.quickMoveStack(p, 0).isEmpty(), "Remote stale menu bypasses distance guard"); helper.succeed();
        }
    }
    @GameTest(template="empty", batch="deposition_native_open")
    public static void RealServerPlayerOpensRegisteredMenuAndDataWritesCannotAssignPE(GameTestHelper helper) {
        try (var f = new Fixture(helper)) {
            var be = f.machine(); var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "deposition-menu"), false);
            var p = new ServerPlayer(f.level.getServer(), f.level, cookie.gameProfile(), cookie.clientInformation());
            var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND); var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
            try {
                net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection); f.level.getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
                p.setGameMode(GameType.SURVIVAL); p.setNoGravity(true); p.teleportTo(f.level, f.pos.getX() + .5, f.pos.getY() + .5, f.pos.getZ() - 1.5, 0, 0);
                helper.assertTrue(be.getBlockState().useWithoutItem(f.level, p, new BlockHitResult(f.pos.getCenter(), Direction.NORTH, f.pos, false)).consumesAction()
                        && p.containerMenu instanceof EnergyDepositionerMenu, "Native block click does not open registered screen/menu");
                var menu = (EnergyDepositionerMenu)p.containerMenu; menu.setData(0, 199); menu.setData(1, 10000);
                helper.assertTrue(be.progress() == 0 && be.energy() == 0 && menu.getType() == EnergyDepositionerContent.MENU.get(), "Client DataSlots can assign authoritative energy");
                var client = EnergyDepositionerContent.MENU.get().create(10, p.getInventory()); client.setData(0, 83); client.setData(1, 777);
                helper.assertTrue(client.progress() == 83 && client.energy() == 777 && client.slots.size() == 38, "Client menu fails native energy/progress synchronization"); helper.succeed();
            } finally { p.closeContainer(); f.level.getServer().getPlayerList().remove(p); channel.finishAndReleaseAll(); }
        }
    }
    @GameTest(template="empty", batch="deposition_native_hoppers", timeoutTicks=280)
    public static void NativeRegisteredTickerHoppersConsumeFuelAndReturnOnlyAnEmptyTablet(GameTestHelper helper) { live(helper); }
    @GameTest(template="empty", templateNamespace="abyssalport_worldtest", batch="deposition_normal_hoppers", timeoutTicks=280)
    public static void NormalWorldNativeHoppersRunTheRegisteredSourceFuelPath(GameTestHelper helper) { live(helper); }
    private static void live(GameTestHelper helper) {
        var f = new Fixture(helper);
        try {
            var be = f.machine(); f.level.setBlock(f.pos.above(), Blocks.HOPPER.defaultBlockState(), 2);
            f.level.setBlock(f.pos.below(), Blocks.HOPPER.defaultBlockState(), 2); f.level.setBlock(f.pos.below(2), Blocks.CHEST.defaultBlockState(), 2);
            var input = (Container)f.level.getBlockEntity(f.pos.above()); var bottom = (Container)f.level.getBlockEntity(f.pos.below()); var chest = (Container)f.level.getBlockEntity(f.pos.below(2)); input.setItem(0, fuel(64));
            helper.runAfterDelay(245, () -> {
                try {
                    helper.assertTrue(input.isEmpty() && bottom.isEmpty() && be.isEmpty() && !StoneTabletItem.isFilled(chest.getItem(0))
                            && chest.getItem(0).is(StateTransformerContent.TABLET.get()) && Math.abs(be.energy() - 64) < .005,
                            "Actual hoppers/registered ticker fail to consume exactly one fuel cargo and return one empty tablet"); helper.succeed();
                } finally { f.close(); }
            });
        } catch (RuntimeException failure) { f.close(); throw failure; }
    }
    @GameTest(template="empty", batch="deposition_biome_cells")
    public static void ModernQuartCellsConvertBelowZeroAndAtWorldTopWithoutOutsideBiomeChanges(GameTestHelper helper) { biome(helper, false); }
    @GameTest(template="empty", batch="deposition_native_protection")
    public static void NativePlacementVetoRollsBackWholeCellIncludingItsBiomeAndOtherBlocks(GameTestHelper helper) { biome(helper, true); }
    private static void biome(GameTestHelper helper, boolean denyBlock) {
        var level = helper.getLevel(); var anchor = helper.absolutePos(new BlockPos(3, 25, 3)); int x = anchor.getX() & ~3, z = anchor.getZ() & ~3;
        var chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4); helper.assertTrue(chunk != null, "Native biome fixture chunk is not loaded");
        var oldBlocks = new LinkedHashMap<BlockPos, BlockState>();
        for (var pos : BlockPos.betweenClosed(new BlockPos(x, level.getMinBuildHeight(), z), new BlockPos(x + 3, level.getMaxBuildHeight() - 1, z + 3)))
            if (level.getBlockEntity(pos) == null) oldBlocks.put(pos.immutable(), level.getBlockState(pos));
        var oldBiomes = new ArrayList<Map<BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>>();
        for (var section : chunk.getSections()) {
            var cells = new HashMap<BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>();
            for (int bx = 0; bx < 4; bx++) for (int by = 0; by < 4; by++) for (int bz = 0; bz < 4; bz++) cells.put(new BlockPos(bx, by, bz), section.getNoiseBiome(bx, by, bz)); oldBiomes.add(cells);
        }
        var deep = new BlockPos(x, level.getMinBuildHeight() + 1, z); var top = new BlockPos(x + 1, level.getMaxBuildHeight() - 2, z);
        Consumer<DepositionEvent> cells = event -> { if (event.level() == level && event.machine().equals(anchor) && (event.minimum().getX() != x || event.minimum().getZ() != z)) event.setCanceled(true); };
        Consumer<BlockEvent.EntityPlaceEvent> protection = event -> { if (denyBlock && event.getLevel() == level && event.getPos().equals(top)) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(cells); NeoForge.EVENT_BUS.addListener(protection);
        try {
            helper.assertTrue(level.getBlockEntity(deep) == null && level.getBlockEntity(top) == null, "Vertical fixture intersects inventory");
            level.setBlock(deep, Blocks.STONE.defaultBlockState(), 2); level.setBlock(top, Blocks.COBBLESTONE.defaultBlockState(), 2);
            var wall = Blocks.COBBLESTONE_WALL.defaultBlockState().setValue(WallBlock.NORTH_WALL, WallSide.TALL).setValue(BlockStateProperties.WATERLOGGED, true);
            var stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP).setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT);
            helper.assertTrue(DarklandsDeposition.replacement(wall).getValue(WallBlock.NORTH_WALL) == WallSide.TALL
                    && DarklandsDeposition.replacement(wall).getValue(BlockStateProperties.WATERLOGGED)
                    && DarklandsDeposition.replacement(stairs).getValue(StairBlock.SHAPE) == StairsShape.INNER_LEFT
                    && DarklandsDeposition.replacement(stairs).getValue(StairBlock.HALF) == Half.TOP, "Native shape/waterlogging lost during source block conversion");
            helper.assertTrue(DarklandsDeposition.replacement(Blocks.BIRCH_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X)).is(DarklandsWoodContent.LOG.get())
                    && DarklandsDeposition.replacement(Blocks.SPRUCE_LEAVES.defaultBlockState()).is(DarklandsWoodContent.LEAVES.get())
                    && DarklandsDeposition.replacement(Blocks.SMOOTH_STONE_SLAB.defaultBlockState()).is(EnergyDepositionerContent.STONE_SLAB.get()), "Flattened source log/leaf/slab families are incomplete");
            DarklandsDeposition.apply(level, anchor, RandomSourceZero.INSTANCE);
            if (denyBlock) helper.assertTrue(level.getBlockState(deep).is(Blocks.STONE) && level.getBlockState(top).is(Blocks.COBBLESTONE)
                    && chunk.getNoiseBiome(x >> 2, level.getMinBuildHeight() >> 2, z >> 2).equals(oldBiomes.getFirst().get(new BlockPos((x >> 2) & 3, 0, (z >> 2) & 3))), "Native veto leaves partial blocks or changed biome");
            else helper.assertTrue(level.getBlockState(deep).is(AbyssalPort.DARKSTONE.get()) && level.getBlockState(top).is(AbyssalPort.DARKSTONE_COBBLESTONE.get())
                    && chunk.getNoiseBiome(x >> 2, level.getMinBuildHeight() >> 2, z >> 2).is(DarklandsWorldgen.DARKLANDS)
                    && chunk.getNoiseBiome(x >> 2, (level.getMaxBuildHeight() - 1) >> 2, z >> 2).is(DarklandsWorldgen.DARKLANDS), "Modern conversion ignores negative Y, high Y or biome storage");
            for (int section = 0; section < chunk.getSections().length; section++) for (int bx = 0; bx < 4; bx++) for (int by = 0; by < 4; by++) for (int bz = 0; bz < 4; bz++)
                if (bx != ((x >> 2) & 3) || bz != ((z >> 2) & 3)) helper.assertTrue(chunk.getSections()[section].getNoiseBiome(bx, by, bz).equals(oldBiomes.get(section).get(new BlockPos(bx, by, bz))), "Biome conversion touches a denied or outside cell");
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(cells); NeoForge.EVENT_BUS.unregister(protection);
            oldBlocks.forEach((pos, state) -> { if (level.getBlockState(pos) != state && level.getBlockEntity(pos) == null) level.setBlock(pos, state, 2); });
            for (int section = 0; section < chunk.getSections().length; section++) {
                var saved = oldBiomes.get(section);
                chunk.getSections()[section].fillBiomesFromNoise((bx, by, bz, sampler) -> saved.get(new BlockPos(bx & 3, by & 3, bz & 3)), level.getChunkSource().randomState().sampler(),
                        chunk.getPos().x * 4, (level.getMinBuildHeight() >> 2) + section * 4, chunk.getPos().z * 4);
            }
            chunk.setUnsaved(true);
        }
    }
}
