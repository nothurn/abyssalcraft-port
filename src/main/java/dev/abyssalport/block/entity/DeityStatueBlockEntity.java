package dev.abyssalport.block.entity;

import dev.abyssalport.block.DeityStatueBlock;
import dev.abyssalport.content.StatueContent;
import dev.abyssalport.content.GatewayContent;
import dev.abyssalport.energy.CharmAmplifier;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.research.EarlyResearch;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Native PE flow adapted from AbyssalCraft PEUtils / TileEntityStatue (Shinoow, LGPL-3.0).
 * Copyright (c) 2012-2026 Shinoow; this NeoForge adaptation remains under LGPL-3.0.
 * Structure/disruption authorization requires real valid geometry.
 */
public final class DeityStatueBlockEntity extends PrototypeEffigyBlockEntity {
    private CharmAmplifier activeAmplifier = CharmAmplifier.NONE;
    private StatueContent.Deity activeDeity;
    private int tolerance;
    private boolean dispatchingDisruption;
    private BlockPos structureBase;
    private Integer chargingStructureAmplifier;
    private final Set<BlockPos> collectors = new LinkedHashSet<>();
    private long collectorScanTick = Long.MIN_VALUE;
    private int collectorScanDepth = -1;
    private int collectorScanRadius = -1;
    public DeityStatueBlockEntity(BlockPos pos,BlockState state) { super(StatueContent.ENTITY.get(),pos,state); }
    public StatueContent.Deity deity() { return ((DeityStatueBlock)getBlockState().getBlock()).deity(); }
    public CharmAmplifier activeAmplifier() { return activeAmplifier; }
    public StatueContent.Deity activeDeity() { return activeDeity; }
    public boolean activateCharm(CharmAmplifier amplifier, StatueContent.Deity deity) {
        if (amplifier == null || amplifier == CharmAmplifier.NONE || activeAmplifier != CharmAmplifier.NONE) return false;
        activeAmplifier = amplifier; activeDeity = deity; changedCharm(); return true;
    }
    public boolean clearCharm() {
        if (activeAmplifier == CharmAmplifier.NONE) return false;
        activeAmplifier = CharmAmplifier.NONE; activeDeity = null; changedCharm(); return true;
    }
    public float amplifierFactor(CharmAmplifier type) {
        return activeAmplifier == type ? type.factor(activeDeity != null && activeDeity == deity()) : 0;
    }
    @Override public float energyQuanta() {
        return activeAmplifier == CharmAmplifier.NONE ? 5 : 10 * Math.max(amplifierFactor(CharmAmplifier.POWER), 1);
    }
    @Override public int chargeInterval() { return (int)(120 / Math.max(amplifierFactor(CharmAmplifier.DURATION), 1)); }
    @Override public double chargingRange() { return 7 + 4 * rangeAmplifiers() + amplifierFactor(CharmAmplifier.RANGE); }
    @Override public int collectorSearchRadius() { return 3 + rangeAmplifiers() + (int)(amplifierFactor(CharmAmplifier.RANGE) / 2); }
    @Override protected int collectorLimit() { return 20; }
    public int tolerance() { return tolerance; }
    public Set<BlockPos> collectorPositions() { return Set.copyOf(collectors); }
    public BlockPos structureBase() { return structureBase; }
    private boolean nearbyBase(BlockPos pos) {
        return Math.abs((long)pos.getX()-worldPosition.getX())<=2 && Math.abs((long)pos.getZ()-worldPosition.getZ())<=2
            && Math.abs((long)pos.getY()-worldPosition.getY())<=3;
    }
    private PlaceOfPowerBlockEntity geometricBase() {
        if(level==null||structureBase==null||!nearbyBase(structureBase)||!level.hasChunkAt(structureBase)) return null;
        return level.getBlockEntity(structureBase) instanceof PlaceOfPowerBlockEntity base
            && base.containsStatue(worldPosition) && base.hasGeometry() ? base : null;
    }
    public boolean canJoinStructure(BlockPos anchor) { return structureBase==null||structureBase.equals(anchor)||geometricBase()==null; }
    public void linkStructure(BlockPos anchor) {
        if(!nearbyBase(anchor)||!canJoinStructure(anchor)||anchor.equals(structureBase)) return;
        structureBase=anchor.immutable(); collectorScanTick=Long.MIN_VALUE; setChanged();
        if(level!=null&&!level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    public int structureRangeAmplifier() {
        if(chargingStructureAmplifier!=null) return chargingStructureAmplifier;
        var base=geometricBase(); return base!=null&&base.isValid()?base.layout().rangeAmplifier():0;
    }
    public int rangeAmplifiers() { return rangePillars()+structureRangeAmplifier(); }
    public int rangePillars() {
        if (level == null || !level.hasChunkAt(worldPosition.below())
                || !level.getBlockState(worldPosition.below()).is(GatewayContent.MONOLITH_PILLAR.get())) return 0;
        return level.hasChunkAt(worldPosition.below(2))
                && level.getBlockState(worldPosition.below(2)).is(GatewayContent.MONOLITH_PILLAR.get()) ? 2 : 1;
    }
    public int independentChargeBound() { return 120 - (int)(20 * amplifierFactor(CharmAmplifier.DURATION)); }

    @Override protected void tickEnergy(ServerLevel server, BlockPos pos) { tickEnergy(server, server.getRandom()); }

    /** Uses the supplied source of entropy; the registered native ticker uses the world's RNG. */
    public void tickEnergy(ServerLevel server, RandomSource random) {
        if(level!=server) return;
        // One immutable geometry result for this synchronous transfer only. UI calls,
        // subsequent transfers and block break checks always inspect the current world.
        chargingStructureAmplifier=structureRangeAmplifier();
        try {
            tickTransfers(server,random);
            if(tolerance>=100&&!dispatchingDisruption) {
                // The real source resets at the threshold, including disabled/protected
                // statues. Reserve before calling native listeners to prevent reentry.
                tolerance=0;dispatchingDisruption=true;setChanged();
                try {dev.abyssalport.energy.StatueDisruptions.trigger(server,this,random);}
                finally {dispatchingDisruption=false;}
            }
        } finally { chargingStructureAmplifier=null; }
    }
    private void tickTransfers(ServerLevel server,RandomSource random) {
        if (!canCharge(server, worldPosition)) return;
        int depth = rangeAmplifiers(), radius = collectorSearchRadius();
        // Initial load and changed range are immediately usable. Otherwise scan at most once
        // per 200 real ticks, also when daylight is frozen. Never force-load a search chunk.
        long tick = server.getGameTime();
        if (collectorScanTick == Long.MIN_VALUE || collectorScanDepth != depth || collectorScanRadius != radius
                || tick < collectorScanTick || tick - collectorScanTick >= 200) refreshCollectors(server);

        var nearest = server.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), chargingRange(), false);
        if (nearest != null && hasInventoryBook(nearest) && (NecronomiconItem.isBook(nearest.getMainHandItem())
                || NecronomiconItem.isBook(nearest.getOffhandItem()))) {
            if (++activeTicks >= chargeInterval()) {
                activeTicks = random.nextInt(10);
                var bounds = new AABB(worldPosition).inflate(chargingRange());
                for (ServerPlayer player : server.players()) if (bounds.intersects(player.getBoundingBox()) && hasInventoryBook(player)) {
                    boolean accepted = chargeBook(player.getMainHandItem()) | chargeBook(player.getOffhandItem());
                    if (accepted) {
                        EarlyResearch.unlock(player, EarlyResearch.MONOLITH);
                        dev.abyssalport.energy.PEStreams.show(server, worldPosition, player.blockPosition(), 1);
                    }
                }
            }
            if (activeTicks % 20 == 0 || activeTicks < 10) setChanged();
        }
        for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition).inflate(chargingRange()),
                entity -> canAcceptBook(entity.getItem()))) {
            if (random.nextInt(independentChargeBound()) == 0) {
                var book = item.getItem().copy();
                if (chargeBook(book)) {
                    item.setItem(book); // A new stack marks native entity data dirty for clients.
                    dev.abyssalport.energy.PEStreams.show(server, worldPosition, item.blockPosition(), 1);
                }
            }
        }
        for (BlockPos pos : collectors) {
            if (!server.hasChunkAt(pos) || !(server.getBlockEntity(pos) instanceof dev.abyssalport.energy.EnergyStorageBlockEntity collector)
                    || !(collector instanceof dev.abyssalport.energy.StatueEnergyCollector) || !collector.acceptsExternalEnergy()
                    || !isolatedCollector(server, pos) || random.nextInt(independentChargeBound()) != 0) continue;
            float accepted = energyQuanta() - collector.addEnergy(energyQuanta());
            if (accepted > 0) {
                addTolerance(activeAmplifier == CharmAmplifier.NONE ? 1 : 2);
                dev.abyssalport.energy.PEStreams.show(server, worldPosition, pos, accepted);
            }
        }
    }

    public void refreshCollectors(ServerLevel server) {
        collectors.clear();
        collectorScanDepth = rangeAmplifiers(); collectorScanRadius = collectorSearchRadius();
        collectorScanTick = server.getGameTime();
        outer: for (int x = -collectorScanRadius; x <= collectorScanRadius; x++)
            for (int y = 0; y <= collectorScanDepth; y++) for (int z = -collectorScanRadius; z <= collectorScanRadius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) < 3) continue;
                var pos = worldPosition.offset(x, -y, z);
                // The source limit applies to found positions, even full or currently non-isolated collectors.
                if (server.hasChunkAt(pos) && server.getBlockEntity(pos) instanceof dev.abyssalport.energy.StatueEnergyCollector) {
                    collectors.add(pos);
                    if (collectors.size() == collectorLimit()) break outer;
                }
            }
    }

    private static boolean isolatedCollector(ServerLevel server, BlockPos pos) {
        for (var direction : Direction.values()) {
            var neighbor = pos.relative(direction);
            if (!server.hasChunkAt(neighbor) || server.getBlockEntity(neighbor) instanceof dev.abyssalport.energy.StatueEnergyCollector) return false;
        }
        return true;
    }
    private static boolean hasInventoryBook(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (NecronomiconItem.isBook(player.getInventory().getItem(slot))) return true;
        return false;
    }
    private static boolean canAcceptBook(ItemStack book) {
        return NecronomiconItem.isBook(book) && NecronomiconItem.getPotentialEnergy(book) < NecronomiconItem.capacity(book);
    }
    private boolean chargeBook(ItemStack book) {
        if (!canAcceptBook(book)) return false;
        float accepted = energyQuanta() - NecronomiconItem.addPotentialEnergy(book, energyQuanta());
        if (accepted <= 0) return false;
        addTolerance(activeAmplifier == CharmAmplifier.NONE ? 2 : 4);
        return true;
    }
    private void addTolerance(int amount) {
        if(structureRangeAmplifier()>0) return;
        tolerance = (int)Math.min(Integer.MAX_VALUE, (long)tolerance + amount);
        setChanged();
    }
    @Override protected boolean canCharge(ServerLevel level, BlockPos pos) {
        if(structureRangeAmplifier()>0) return true;
        // Query the statue cell, as the source does. The opaque roof block itself
        // can have full skylight, so checking pos.above() falsely accepts a roof.
        if (!level.canSeeSky(pos)) return false;
        for (var direction : Direction.values()) {
            var adjacent = pos.relative(direction);
            if (!level.hasChunkAt(adjacent) || level.getBlockEntity(adjacent) instanceof PrototypeEffigyBlockEntity
                    || level.getBlockEntity(adjacent) instanceof EnergyDepositionerBlockEntity) return false;
        }
        for (int offset : new int[]{-2, 2}) {
            var adjacent = pos.offset(0, offset, 0);
            if (!level.hasChunkAt(adjacent) || level.getBlockEntity(adjacent) instanceof PrototypeEffigyBlockEntity
                    || level.getBlockEntity(adjacent) instanceof EnergyDepositionerBlockEntity) return false;
        }
        return true;
    }
    private void changedCharm() {
        collectorScanTick = Long.MIN_VALUE;
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("ActiveAmplifier", activeAmplifier.id());
        tag.putString("ActiveDeity", activeDeity == null ? "" : activeDeity.getSerializedName());
        tag.putInt("Tolerance", tolerance);
        if(structureBase!=null) tag.putLong("PowerBasePos",structureBase.asLong());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        activeAmplifier = CharmAmplifier.fromId(tag.getString("ActiveAmplifier")); activeDeity = null;
        tolerance = Math.max(0, tag.getInt("Tolerance"));
        structureBase=tag.contains("PowerBasePos")?BlockPos.of(tag.getLong("PowerBasePos")):null;
        if(structureBase!=null&&!nearbyBase(structureBase)) structureBase=null;
        collectors.clear(); collectorScanTick = Long.MIN_VALUE;
        if (activeAmplifier != CharmAmplifier.NONE) for (var deity : StatueContent.Deity.values())
            if (deity.getSerializedName().equals(tag.getString("ActiveDeity"))) activeDeity = deity;
    }
}
