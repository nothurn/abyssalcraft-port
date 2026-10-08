package dev.abyssalport.block.entity;

import dev.abyssalport.block.EnergyDepositionerBlock;
import dev.abyssalport.content.*;
import dev.abyssalport.energy.*;
import dev.abyssalport.item.StoneTabletItem;
import dev.abyssalport.menu.EnergyDepositionerMenu;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** TileEntityEnergyDepositioner / PEUtils, copyright 2012-2026 Shinoow, LGPL-3.0.
 * Fuel becomes an inaccessible job before any PE is generated. Mining carries
 * that same job, not a reusable full-value tablet plus its already drained PE.
 */
public final class EnergyDepositionerBlockEntity extends EnergyStorageBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int PROCESS_TICKS = 200, CAPACITY = 10000;
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private ItemStack processing = ItemStack.EMPTY;
    private int progress, tolerance;
    private CharmAmplifier amplifier = CharmAmplifier.NONE;
    private final Set<BlockPos> collectors = new LinkedHashSet<>();
    private long scanTick = Long.MIN_VALUE;
    private int scanDepth = -1, scanRadius = -1;
    private boolean inTick, invalidJob;
    public EnergyDepositionerBlockEntity(BlockPos pos, BlockState state) { super(EnergyDepositionerContent.ENTITY.get(), pos, state, CAPACITY); }
    public int progress() { return progress; }
    public int tolerance() { return tolerance; }
    public CharmAmplifier activeAmplifier() { return amplifier; }
    public boolean hasJob() { return !processing.isEmpty(); }
    @Override public boolean acceptsExternalEnergy() { return false; }
    public boolean activateCharm(CharmAmplifier requested) {
        if (requested == null || requested == CharmAmplifier.NONE || amplifier != CharmAmplifier.NONE) return false;
        amplifier = requested; scanTick = Long.MIN_VALUE; setChanged(); return true;
    }
    public boolean clearCharm() {
        if (amplifier == CharmAmplifier.NONE) return false;
        amplifier = CharmAmplifier.NONE; scanTick = Long.MIN_VALUE; setChanged(); return true;
    }
    public float amplifierFactor(CharmAmplifier type) { return amplifier == type ? type.factor(false) : 0; }
    public float energyQuanta() { return amplifier == CharmAmplifier.NONE ? 15 : 20 * Math.max(1, amplifierFactor(CharmAmplifier.POWER)); }
    public int transferBound() { return 120 - (int)(20 * amplifierFactor(CharmAmplifier.DURATION)); }
    public static boolean validFuel(ItemStack stack) { return StoneTabletItem.isFilled(stack) && !StoneTabletItem.isCursed(stack) && StoneTabletItem.storedValue(stack) > 0; }

    public static void serverTick(Level world, BlockPos pos, BlockState state, EnergyDepositionerBlockEntity be) {
        if (world instanceof ServerLevel server) be.tick(server, server.getRandom());
    }
    /** Native ticker passes the server RNG; deterministic fixtures supply their entropy. */
    public void tick(ServerLevel server, RandomSource random) {
        if (level != server || isRemoved() || inTick) return;
        inTick = true;
        try { tickInternal(server, random); } finally { inTick = false; }
    }
    private void tickInternal(ServerLevel server, RandomSource random) {
        if (items.get(1).isEmpty() && !invalidJob) {
            if (processing.isEmpty() && validFuel(items.getFirst())) {
                processing = items.getFirst(); items.set(0, ItemStack.EMPTY); progress = 0; setChanged();
            }
            if (!processing.isEmpty()) {
                float value = StoneTabletItem.storedValue(processing);
                // Cumulative differences give exactly one complete cargo value over 200 steps.
                float before = value * progress / PROCESS_TICKS, after = value * (progress + 1) / PROCESS_TICKS;
                addEnergy(after - before); progress++; setChanged();
                if (progress == PROCESS_TICKS) {
                    // Remove the fuel from the transaction before protection listeners run.
                    var fuel = processing; processing = ItemStack.EMPTY; progress = 0;
                    var output = StoneTabletItem.empty(fuel); items.set(1, output); setChanged();
                    float chance = DarklandsDeposition.apply(server, worldPosition, random);
                    if (random.nextFloat() < chance) StoneTabletItem.curse(output);
                    setChanged();
                }
            }
        }
        // A native biome/protection listener may have mined the machine and
        // captured its saved energy in the drop. The removed instance must not
        // transfer that same balance a second time afterwards.
        if (isRemoved() || server.getBlockEntity(worldPosition) != this) return;
        tickTransfers(server, random);
        if (tolerance >= 200) {
            tolerance = 0; setChanged();
            var deities = StatueContent.Deity.values();
            StatueDisruptions.triggerAt(server, worldPosition, deities[random.nextInt(deities.length)], random);
        }
        var current = server.getBlockState(worldPosition); boolean active = hasJob();
        if (current.is(EnergyDepositionerContent.BLOCK.get()) && current.getValue(EnergyDepositionerBlock.ACTIVE) != active)
            server.setBlock(worldPosition, current.setValue(EnergyDepositionerBlock.ACTIVE, active), 2);
    }
    private int pillars() {
        if (level == null || !level.hasChunkAt(worldPosition.below()) || !level.getBlockState(worldPosition.below()).is(GatewayContent.MONOLITH_PILLAR.get())) return 0;
        return level.hasChunkAt(worldPosition.below(2)) && level.getBlockState(worldPosition.below(2)).is(GatewayContent.MONOLITH_PILLAR.get()) ? 2 : 1;
    }
    private static boolean manipulator(net.minecraft.world.level.block.entity.BlockEntity be) {
        return be instanceof PrototypeEffigyBlockEntity || be instanceof EnergyDepositionerBlockEntity;
    }
    private boolean isolated(ServerLevel server) {
        for (var direction : Direction.values()) {
            var pos = worldPosition.relative(direction);
            if (!server.hasChunkAt(pos) || manipulator(server.getBlockEntity(pos))) return false;
        }
        for (int dy : new int[]{-2, 2}) if (!server.hasChunkAt(worldPosition.offset(0, dy, 0)) || manipulator(server.getBlockEntity(worldPosition.offset(0, dy, 0)))) return false;
        return true;
    }
    public void refreshCollectors(ServerLevel server) {
        collectors.clear(); scanTick = server.getGameTime();
        int depth = pillars(), radius = 3 + depth + (int)(amplifierFactor(CharmAmplifier.RANGE) / 2);
        scanDepth = depth; scanRadius = radius;
        outer: for (int x = -radius; x <= radius; x++) for (int y = 0; y <= depth; y++) for (int z = -radius; z <= radius; z++) {
            if (Math.max(Math.abs(x), Math.abs(z)) < 3) continue;
            var pos = worldPosition.offset(x, -y, z);
            if (server.hasChunkAt(pos) && server.getBlockEntity(pos) instanceof StatueEnergyCollector) {
                collectors.add(pos); if (collectors.size() == 20) break outer;
            }
        }
    }
    public Set<BlockPos> collectorPositions() { return Set.copyOf(collectors); }
    private void tickTransfers(ServerLevel server, RandomSource random) {
        if (energy() <= 0 || !isolated(server)) return;
        long tick = server.getGameTime();
        int depth = pillars(), radius = 3 + depth + (int)(amplifierFactor(CharmAmplifier.RANGE) / 2);
        if (scanTick == Long.MIN_VALUE || tick < scanTick || tick - scanTick >= 200 || scanDepth != depth || scanRadius != radius) refreshCollectors(server);
        for (var pos : collectors) {
            if (!server.hasChunkAt(pos) || !(server.getBlockEntity(pos) instanceof EnergyStorageBlockEntity target)
                    || !(target instanceof StatueEnergyCollector) || !target.acceptsExternalEnergy() || random.nextInt(transferBound()) != 0) continue;
            boolean clear = true;
            for (var direction : Direction.values()) {
                var adjacent = pos.relative(direction);
                if (!server.hasChunkAt(adjacent) || server.getBlockEntity(adjacent) instanceof StatueEnergyCollector) { clear = false; break; }
            }
            if (clear) {
                float accepted = transferTo(target, energyQuanta());
                if (accepted > 0) { tolerance = (int)Math.min(Integer.MAX_VALUE, (long)tolerance + (amplifier == CharmAmplifier.NONE ? 1 : 2)); setChanged(); PEStreams.show(server, worldPosition, pos, accepted); }
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("ProcessingTime", progress); tag.put("ProcessingStack", processing.isEmpty() ? new CompoundTag() : processing.save(registries));
        tag.putInt("Tolerance", tolerance); tag.putString("ActiveAmplifier", amplifier.id());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); items.clear(); ContainerHelper.loadAllItems(tag, items, registries);
        processing = ItemStack.parseOptional(registries, tag.getCompound("ProcessingStack"));
        progress = tag.getInt("ProcessingTime");
        // Corrupt counters/cargo are kept in the carried job, but cannot become
        // a reset, fully redeemable fuel or overwrite an occupied input slot.
        invalidJob = !processing.isEmpty() && (!validFuel(processing) || progress < 0 || progress >= PROCESS_TICKS);
        if (processing.isEmpty()) progress = 0;
        tolerance = Math.max(0, tag.getInt("Tolerance")); amplifier = CharmAmplifier.fromId(tag.getString("ActiveAmplifier")); collectors.clear(); scanTick = Long.MIN_VALUE;
    }
    public void restoreItemData(CompoundTag tag) { loadWithComponents(tag, level.registryAccess()); setChanged(); }
    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty) && processing.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { var result = ContainerHelper.removeItem(items, slot, count); if (!result.isEmpty()) setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { var result = ContainerHelper.takeItem(items, slot); if (!result.isEmpty()) setChanged(); return result; }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void clearContent() { items.clear(); processing = ItemStack.EMPTY; progress = 0; invalidJob = false; setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && validFuel(stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public int[] getSlotsForFace(Direction face) { return face == Direction.UP ? new int[]{0} : face == Direction.DOWN ? new int[]{1} : new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return face == Direction.UP && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return slot == 1 && face == Direction.DOWN; }
    @Override public Component getDisplayName() { return Component.translatable("block.abyssalport.energy_depositioner"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new EnergyDepositionerMenu(id, inventory, this); }
}
