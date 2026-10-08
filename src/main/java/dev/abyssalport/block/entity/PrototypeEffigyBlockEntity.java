package dev.abyssalport.block.entity;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.research.EarlyResearch;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A temporary PE source, following the original statue's 120-tick / 5-PE base rate. */
public class PrototypeEffigyBlockEntity extends BlockEntity {
    private static final int CHARGE_INTERVAL = 120;
    private static final float ENERGY_PER_CHARGE = 5;
    private static final double RANGE_SQUARED = 7 * 7;
    protected int activeTicks;

    public PrototypeEffigyBlockEntity(BlockPos pos, BlockState state) {
        this(AbyssalPort.PROTOTYPE_EFFIGY_ENTITY.get(),pos,state);
    }

    protected PrototypeEffigyBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type,pos,state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PrototypeEffigyBlockEntity effigy) {
        if (level instanceof ServerLevel server) effigy.tickEnergy(server, pos);
    }

    protected void tickEnergy(ServerLevel server, BlockPos pos) {
        PrototypeEffigyBlockEntity effigy = this;
        if (!effigy.canCharge(server, pos)) {
            return;
        }
        List<ServerPlayer> nearby = server.players().stream()
                .filter(player -> player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                        <= effigy.chargingRange() * effigy.chargingRange())
                .filter(PrototypeEffigyBlockEntity::hasChargeableBook)
                .toList();
        List<EnergyCollectorBlockEntity> collectors = nearbyCollectors(server, pos, effigy.collectorSearchRadius(), effigy.collectorLimit());
        if (nearby.isEmpty() && collectors.isEmpty()) {
            return;
        }
        if (++effigy.activeTicks < effigy.chargeInterval()) {
            if (effigy.activeTicks % 20 == 0) {
                effigy.setChanged();
            }
            return;
        }
        effigy.activeTicks = 0;
        for (ServerPlayer player : nearby) {
            chargeHeldBook(player, effigy.energyQuanta());
        }
        for (EnergyCollectorBlockEntity collector : collectors) {
            collector.addEnergy(effigy.energyQuanta());
        }
        effigy.setChanged();
    }

    /** Base-tier collector positions follow the original horizontal ring, without loading chunks. */
    protected float energyQuanta() { return ENERGY_PER_CHARGE; }
    protected int chargeInterval() { return CHARGE_INTERVAL; }
    protected double chargingRange() { return Math.sqrt(RANGE_SQUARED); }
    protected int collectorSearchRadius() { return 3; }
    protected int collectorLimit() { return Integer.MAX_VALUE; }
    protected boolean canCharge(ServerLevel level, BlockPos pos) { return level.canSeeSky(pos.above()); }

    private static List<EnergyCollectorBlockEntity> nearbyCollectors(ServerLevel level, BlockPos pos, int radius, int limit) {
        List<EnergyCollectorBlockEntity> collectors = new java.util.ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) < 3) {
                    continue;
                }
                BlockPos candidate = pos.offset(x, 0, z);
                if (level.hasChunkAt(candidate)
                        && level.getBlockEntity(candidate) instanceof EnergyCollectorBlockEntity collector
                        && collector.energy() < EnergyCollectorBlockEntity.CAPACITY
                        && isIsolatedCollector(level, candidate)) {
                    collectors.add(collector);
                    if (collectors.size() == limit) return collectors;
                }
            }
        }
        return collectors;
    }

    private static boolean isIsolatedCollector(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (!level.hasChunkAt(neighbor)
                    || level.getBlockEntity(neighbor) instanceof EnergyCollectorBlockEntity) {
                return false;
            }
        }
        return true;
    }

    public static boolean chargeHeldBook(ServerPlayer player) {
        return chargeHeldBook(player, ENERGY_PER_CHARGE);
    }

    private static boolean chargeHeldBook(ServerPlayer player, float amount) {
        boolean charged = false;
        for (ItemStack book : heldBooks(player)) {
            if (NecronomiconItem.isBook(book)
                    && NecronomiconItem.getPotentialEnergy(book) < NecronomiconItem.capacity(book)) {
                NecronomiconItem.addPotentialEnergy(book, amount);
                charged = true;
            }
        }
        if (charged) {
            EarlyResearch.unlock(player, EarlyResearch.MONOLITH);
        }
        return charged;
    }

    private static boolean hasChargeableBook(ServerPlayer player) {
        return heldBooks(player).stream().anyMatch(book -> NecronomiconItem.isBook(book)
                && NecronomiconItem.getPotentialEnergy(book) < NecronomiconItem.capacity(book));
    }

    private static List<ItemStack> heldBooks(ServerPlayer player) {
        return List.of(player.getMainHandItem(), player.getOffhandItem());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ActiveTicks", activeTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        activeTicks = Math.clamp(tag.getInt("ActiveTicks"), 0, CHARGE_INTERVAL - 1);
    }
}
