package dev.abyssalport.block.entity;

import dev.abyssalport.AbyssalPort;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class RitualPedestalBlockEntity extends BlockEntity {
    private ItemStack offering = ItemStack.EMPTY;

    public RitualPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(AbyssalPort.RITUAL_PEDESTAL_ENTITY.get(), pos, state);
    }

    public ItemStack getOffering() {
        return offering;
    }

    public boolean placeOne(ItemStack held) {
        if (!offering.isEmpty() || held.isEmpty()) {
            return false;
        }
        offering = held.split(1);
        sync();
        return true;
    }

    public ItemStack removeOne() {
        if (offering.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = offering;
        offering = ItemStack.EMPTY;
        sync();
        return removed;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!offering.isEmpty()) {
            tag.put("Offering", offering.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        offering = tag.contains("Offering")
                ? ItemStack.parseOptional(registries, tag.getCompound("Offering")) : ItemStack.EMPTY;
    }
}
