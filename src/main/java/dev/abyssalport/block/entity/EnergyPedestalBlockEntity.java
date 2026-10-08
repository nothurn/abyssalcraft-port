package dev.abyssalport.block.entity;

import dev.abyssalport.block.EnergyPedestalBlock;
import dev.abyssalport.content.EnergyDeviceContent;
import dev.abyssalport.energy.EnergyStorageBlockEntity;
import dev.abyssalport.energy.PotentialEnergyItems;
import dev.abyssalport.energy.StatueEnergyCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** TileEntityEnergyPedestal / TieredEnergyPedestal, Shinoow, LGPL-3.0.
 * A statue collector charges its single displayed item from its own PE balance.
 */
public class EnergyPedestalBlockEntity extends EnergyStorageBlockEntity implements StatueEnergyCollector {
    private ItemStack item = ItemStack.EMPTY;
    private float lastSyncedEnergy;
    private ItemStack lastSyncedItem = ItemStack.EMPTY;

    public EnergyPedestalBlockEntity(BlockPos pos, BlockState state) {
        this(EnergyDeviceContent.PEDESTAL_ENTITY.get(), pos, state,
                ((EnergyPedestalBlock) state.getBlock()).tier().pedestalCapacity());
    }
    protected EnergyPedestalBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type,pos,state,capacity);
    }
    public double displayItemHeight() { return 1.02; }
    public ItemStack item() { return item; }
    public boolean placeOne(ItemStack held) {
        if (!item.isEmpty() || held.isEmpty() || held.getCount() != 1 && PotentialEnergyItems.isContainer(held.copyWithCount(1))) return false;
        item = held.split(1); sync(); return true;
    }
    public ItemStack removeItem() {
        var result = item; item = ItemStack.EMPTY;
        if (!result.isEmpty()) sync();
        return result;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyPedestalBlockEntity pedestal) {
        var held = pedestal.item;
        float accepted = 0;
        if (PotentialEnergyItems.canReceive(held)) {
            float amount = Math.min(20, Math.min(pedestal.energy(), PotentialEnergyItems.capacity(held) - PotentialEnergyItems.stored(held)));
            if (amount > 0) {
                accepted = amount - PotentialEnergyItems.add(held, amount);
                if (accepted > 0) pedestal.consumeEnergy(accepted);
            }
        }
        // Coalesce component/energy updates, but synchronize a completed charge immediately.
        if ((level.getGameTime() % 10 == 0 || accepted > 0 && (pedestal.energy() == 0 || PotentialEnergyItems.stored(held) == PotentialEnergyItems.capacity(held)))
                && (pedestal.lastSyncedEnergy != pedestal.energy() || !ItemStack.matches(held, pedestal.lastSyncedItem))) pedestal.sync();
    }
    protected final void sync() {
        setChanged(); lastSyncedEnergy = energy(); lastSyncedItem = item.copy();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!item.isEmpty()) tag.put("Item", item.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        item = tag.contains("Item") ? ItemStack.parseOptional(registries, tag.getCompound("Item")) : ItemStack.EMPTY;
        lastSyncedEnergy = energy(); lastSyncedItem = item.copy();
    }
}
