package dev.abyssalport.test;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.entity.GatewayAnchorBlockEntity;
import dev.abyssalport.block.entity.StateTransformerBlockEntity;
import dev.abyssalport.content.CrystalBagContent;
import dev.abyssalport.content.CrystallizationContent;
import dev.abyssalport.content.StateTransformerContent;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.item.StoneTabletItem;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.world.GatewayTravel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;

/** Opt-in probe owned by WorldAuditTestRunner, never an automatic product workflow.
 * prepare and verify must run in distinct server JVMs using the same isolated world.
 * PlayerList and normal server shutdown write the real files; no test replaces a
 * block entity or deserializes its own synthetic save to pretend a process restart.
 */
final class RestartAuditProbe implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_KEY = "abyssalport_restart_audit";
    private static final UUID PLAYER_ID = UUID.fromString("4f560bed-9409-42b9-a831-825312084745");
    private static final BlockPos SOURCE = new BlockPos(24576, 200, 24576);
    private static final BlockPos DESTINATION = new BlockPos(24576, 160, 24576);
    private static final BlockPos MACHINE = SOURCE.offset(0, 1, 8);
    private static final BlockPos RESPAWN = SOURCE.offset(0, 1, 3);
    private final MinecraftServer server;
    private final String phase;
    private final ServerLevel overworld, wasteland;
    private State state;
    private ServerPlayer player;
    private EmbeddedChannel channel;
    private int startTick;
    private boolean unpacking;

    RestartAuditProbe(MinecraftServer server, String phase) {
        if (!phase.equals("prepare") && !phase.equals("verify"))
            throw new IllegalArgumentException("Restart phase must be prepare or verify");
        this.server = server; this.phase = phase;
        overworld = server.overworld(); wasteland = server.getLevel(WastelandContent.DIMENSION);
        require(wasteland != null, "Real Wasteland dimension is missing");
    }
    String phase() { return phase; }

    void start() {
        startTick = server.getTickCount();
        server.getPlayerList().setViewDistance(2);
        server.getPlayerList().setSimulationDistance(2);
        state = overworld.getDataStorage().get(State.FACTORY, DATA_KEY);
        LOGGER.info("RESTART AUDIT START: phase={}, JVM={}, world={}", phase,
                ProcessHandle.current().pid(), server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT));
        if (phase.equals("prepare")) prepare(); else verify();
    }

    private void prepare() {
        require(state == null, "Prepare refuses to replace an existing restart audit; use a new isolated world name");
        state = new State(new CompoundTag());
        overworld.getDataStorage().set(DATA_KEY, state);
        platform(overworld, SOURCE); platform(wasteland, DESTINATION);
        for (BlockPos floor : BlockPos.betweenClosed(MACHINE.offset(-1,-1,-1), MACHINE.offset(1,-1,1)))
            overworld.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        var source = GatewayTravel.createAnchor(overworld, SOURCE, WastelandContent.DIMENSION, Direction.Axis.X);
        var destination = GatewayTravel.createAnchor(wasteland, DESTINATION, Level.OVERWORLD, Direction.Axis.X);
        source.pairWith(destination); destination.pairWith(source);
        state.data.putUUID("SourceId", source.portalId()); state.data.putUUID("DestinationId", destination.portalId());
        state.data.putLong("PrepareProcess", ProcessHandle.current().pid());
        overworld.setBlock(MACHINE, StateTransformerContent.BLOCK.get().defaultBlockState(), 3);
        var machine = machine(); machine.setItem(0, tablet());
        var cargo = cargo(); for (int slot = 1; slot < 50; slot++) machine.setItem(slot, cargo.get(slot-1).copy());
        force(overworld, MACHINE, true);
        force(wasteland, DESTINATION, true);
        login();
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setNoGravity(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, book());
        player.getInventory().setItem(7, named(Items.DIAMOND, 3, "Restart diamonds"));
        player.getInventory().setItem(8, bag());
        player.setData(AbyssalPort.EARLY_RESEARCH.get(), EarlyResearch.MONOLITH | EarlyResearch.BASIC_SCROLL);
        player.setRespawnPosition(Level.OVERWORLD, RESPAWN, 37, true, false);
        player.setHealth(18); player.getFoodData().setFoodLevel(10); player.getFoodData().setSaturation(0);
        player.giveExperiencePoints(123);
        player.teleportTo(overworld, SOURCE.getX()+.5, SOURCE.getY()+1, SOURCE.getZ()+.5, 0, 0);
        collide(overworld, SOURCE);
        require(player.level() == wasteland, "Native outbound player transfer failed");
        assertPlayer();
        state.data.putInt("Discoveries", EarlyResearch.discoveries(player));
        state.data.putInt("Experience", player.totalExperience);
        state.setDirty();
    }

    private void verify() {
        require(state != null && state.data.getBoolean("Prepared"), "No completed saved prepare phase exists");
        require(state.data.getLong("PrepareProcess") != ProcessHandle.current().pid(), "Verify must be a different JVM");
        require(!overworld.getChunkSource().hasChunk(SOURCE.getX()>>4, SOURCE.getZ()>>4),
                "Saved return destination is already loaded; this does not test cold-anchor recovery");
        LOGGER.info("RESTART AUDIT COLD RETURN: source chunk absent before native login and return");
        login();
        require(player.level() == wasteland, "Native PlayerList did not reload the saved Wasteland dimension");
        assertPlayer();
        require(EarlyResearch.discoveries(player) == state.data.getInt("Discoveries"), "Saved player discoveries changed");
        require(player.totalExperience == state.data.getInt("Experience"), "Saved XP changed");
        require(!overworld.getChunkSource().hasChunk(SOURCE.getX()>>4, SOURCE.getZ()>>4),
                "Login loaded the remote Overworld return destination before travel");
        wasteland.getChunkAt(DESTINATION);
        var destination = anchor(wasteland, DESTINATION);
        require(destination.portalId().equals(state.data.getUUID("DestinationId"))
                && destination.partnerId().equals(state.data.getUUID("SourceId")), "Saved outbound identity/pair changed");
        collide(wasteland, DESTINATION);
        require(player.level() == overworld && player.position().distanceToSqr(GatewayTravel.arrival(SOURCE)) < .01,
                "Cold saved portal did not return to the exact original destination");
        var source = anchor(overworld, SOURCE);
        require(source.portalId().equals(state.data.getUUID("SourceId"))
                && source.partnerId().equals(state.data.getUUID("DestinationId"))
                && SOURCE.equals(destination.partner()) && DESTINATION.equals(source.partner()),
                "Restart/return recreated or detached one of the saved anchors");
        require(GatewayTravel.safeArrival(overworld, SOURCE, player), "Restored arrival is unsafe");
        var machine = machine();
        require(machine.progress() == state.data.getInt("Progress") && machine.mode() == 0,
                "Native chunk reload lost valid in-flight processing progress");
        require(ItemStack.matches(machine.getItem(0), tablet()), "Native reload changed blank tablet metadata");
        assertCargo(machine);
        force(overworld, MACHINE, true);
        player.teleportTo(overworld, SOURCE.getX()+1.5, SOURCE.getY()+1, SOURCE.getZ()+.5, 0, 0);
        LOGGER.info("RESTART AUDIT RESTORED: exact portal UUIDs/arrival, player items/PE/research/XP/respawn, machine progress={}", machine.progress());
    }

    boolean tick() {
        require(server.getTickCount()-startTick < 1200, "Restart probe timed out waiting for native machine tickers");
        var machine = machine();
        if (phase.equals("prepare")) {
            if (machine.progress() < 73) return false;
            require(machine.progress() < 200 && StoneTabletItem.isBlank(machine.getItem(0)), "Prepare missed active transaction");
            assertCargo(machine); assertPlayer();
            state.data.putInt("Progress", machine.progress());
            state.data.putInt("Discoveries", EarlyResearch.discoveries(player));
            state.data.putInt("Experience", player.totalExperience);
            state.data.putBoolean("Prepared", true); state.setDirty();
            // Persisted force tickets would invalidate the next JVM's unloaded-anchor assertion.
            force(overworld, MACHINE, false); force(wasteland, DESTINATION, false);
            server.getPlayerList().saveAll();
            LOGGER.info("RESTART AUDIT PREPARED: native player save + paired gateways + live machine at {} ticks; normal shutdown will save chunks", machine.progress());
            return true;
        }
        if (!unpacking) {
            if (!StoneTabletItem.isFilled(machine.getItem(0))) return false;
            require(ItemStack.matches(machine.getItem(0), StoneTabletItem.seal(tablet(), cargo()))
                    && machine.getItem(49).isEmpty(), "Resumed pack lost original contents/components or duplicated cargo");
            require(machine.setMode(1), "Completed restored pack cannot choose unpack mode");
            unpacking = true;
            return false;
        }
        if (!StoneTabletItem.isBlank(machine.getItem(0))) return false;
        require(machine.progress() == 0 && ItemStack.matches(machine.getItem(0), tablet()), "Resumed unpack lost the original blank tablet");
        assertCargo(machine); assertPlayer();
        force(overworld, MACHINE, false);
        state.data.putBoolean("Verified", true); state.setDirty();
        LOGGER.info("RESTART AUDIT PASS: two JVMs, cold exact return, native player-file reload, saved {}-tick transaction resumed and full 49-slot native unpack; zero manual machine ticks", state.data.getInt("Progress"));
        return true;
    }

    private void login() {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(PLAYER_ID, "restart-audit"), false);
        player = new ServerPlayer(server, overworld, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND); channel = new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
    }
    private void collide(ServerLevel level, BlockPos pos) {
        player.setPortalCooldown(0);
        player.moveTo(pos.getX()+.5, pos.getY()+1, pos.getZ()+.5);
        level.getBlockState(pos.above()).entityInside(level, pos.above(), player);
        player.baseTick(); // Native PortalProcessor; never synthesize a DimensionTransition.
    }
    private void assertPlayer() {
        require(ItemStack.matches(player.getMainHandItem(), book()), "Player book/name/fractional PE changed");
        require(ItemStack.matches(player.getInventory().getItem(7), named(Items.DIAMOND, 3, "Restart diamonds")), "Player stack/name changed");
        require(ItemStack.matches(player.getInventory().getItem(8), bag()), "Filled native crystal bag contents changed");
        require(player.getRespawnDimension().equals(Level.OVERWORLD) && RESPAWN.equals(player.getRespawnPosition())
                && player.getRespawnAngle() == 37 && player.isRespawnForced(), "Respawn point/angle/forced state changed");
        require(player.getHealth() == 18, "Player health changed during isolated restart");
    }
    private void assertCargo(StateTransformerBlockEntity machine) {
        var expected = cargo();
        for (int slot=1; slot<50; slot++)
            require(ItemStack.matches(machine.getItem(slot), expected.get(slot-1)), "Saved/restored cargo differs in slot " + slot);
    }
    private StateTransformerBlockEntity machine() {
        require(overworld.getBlockEntity(MACHINE) instanceof StateTransformerBlockEntity, "Native machine did not survive world save/load");
        return (StateTransformerBlockEntity)overworld.getBlockEntity(MACHINE);
    }
    private static GatewayAnchorBlockEntity anchor(ServerLevel level, BlockPos pos) {
        require(level.getBlockEntity(pos) instanceof GatewayAnchorBlockEntity, "Native saved gateway missing at " + pos);
        return (GatewayAnchorBlockEntity)level.getBlockEntity(pos);
    }
    private static ItemStack named(Item item, int count, String name) {
        var stack = new ItemStack(item, count); stack.set(DataComponents.CUSTOM_NAME, Component.literal(name)); return stack;
    }
    private static ItemStack tablet() { return named(StateTransformerContent.TABLET.get(), 1, "Restart tablet"); }
    private static ItemStack book() {
        var book = named(AbyssalPort.NECRONOMICON.get(), 1, "Restart Necronomicon");
        NecronomiconItem.setPotentialEnergy(book, 1234.25f); return book;
    }
    private static ItemStack bag() {
        var bag = named(CrystalBagContent.LARGE.get(), 1, "Restart crystals");
        bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(
                new ItemStack(CrystallizationContent.material("carbon").crystal().get(), 64),
                new ItemStack(CrystallizationContent.material("sulfur").clusterItem().get(), 3))));
        return bag;
    }
    private static NonNullList<ItemStack> cargo() {
        var cargo = NonNullList.withSize(49, ItemStack.EMPTY);
        cargo.set(0, named(Items.STONE, 23, "Restart stone"));
        var sword = named(Items.IRON_SWORD, 1, "Restart blade"); sword.setDamageValue(43); cargo.set(17, sword);
        cargo.set(48, named(Items.DIAMOND, 3, "Last restart slot")); return cargo;
    }
    private static void platform(ServerLevel level, BlockPos pos) {
        for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-2,0,-2), pos.offset(2,0,2)))
            level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        for (BlockPos air : BlockPos.betweenClosed(pos.offset(-2,1,-2), pos.offset(2,4,2)))
            level.setBlock(air, Blocks.AIR.defaultBlockState(), 3);
    }
    private static void force(ServerLevel level, BlockPos pos, boolean enabled) {
        level.setChunkForced(pos.getX()>>4, pos.getZ()>>4, enabled);
    }
    private static void require(boolean condition, String detail) {
        if (!condition) throw new IllegalStateException(detail);
    }
    @Override public void close() { if (channel != null) channel.finishAndReleaseAll(); }

    private static final class State extends SavedData {
        static final Factory<State> FACTORY = new Factory<>(() -> new State(new CompoundTag()),
                (data, registries) -> new State(data.copy()));
        final CompoundTag data;
        State(CompoundTag data) { this.data = data; }
        @Override public CompoundTag save(CompoundTag target, HolderLookup.Provider registries) {
            return target.merge(data.copy());
        }
    }
}
