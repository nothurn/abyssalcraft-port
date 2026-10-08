package dev.abyssalport.energy;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.item.NecronomiconItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared persisted PE balance for the first-tier collector, relay and container. */
public abstract class EnergyStorageBlockEntity extends BlockEntity {
    public static final int MANUAL_TRANSFER = 100;
    private final int capacity;
    private float energy;

    protected EnergyStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.capacity = capacity;
    }

    public final int capacity() {
        return capacity;
    }

    public final float energy() {
        return energy;
    }
    /** Source canAcceptPE capability, separate from internal generation/capacity. */
    public boolean acceptsExternalEnergy() { return true; }

    /** Returns the amount that did not fit. */
    public final float addEnergy(float amount) {
        PotentialEnergyMath.Transfer transfer = PotentialEnergyMath.add(capacity, energy, amount);
        if (transfer.stored() != energy) {
            energy = transfer.stored();
            setChanged();
        }
        return transfer.remainder();
    }

    /** Returns the amount actually taken. */
    public final float consumeEnergy(float amount) {
        PotentialEnergyMath.Transfer transfer = PotentialEnergyMath.consume(capacity, energy, amount);
        if (transfer.stored() != energy) {
            energy = transfer.stored();
            setChanged();
        }
        return transfer.remainder();
    }

    /** Transfer only what the destination can accept. Both balances remain conserved. */
    public final float transferTo(EnergyStorageBlockEntity target, float amount) {
        if (target == this || !Float.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Invalid PE transfer");
        }
        if (!target.acceptsExternalEnergy()) return 0;
        float offered = Math.min(amount, energy);
        float accepted = offered - target.addEnergy(offered);
        consumeEnergy(accepted);
        return accepted;
    }

    public final float transferToBook(ItemStack book) {
        if (!NecronomiconItem.isBook(book)) {
            return 0;
        }
        float offered = Math.min(MANUAL_TRANSFER, energy);
        float accepted = offered - NecronomiconItem.addPotentialEnergy(book, offered);
        consumeEnergy(accepted);
        return accepted;
    }

    public final float transferFromBook(ItemStack book) {
        if (!NecronomiconItem.isBook(book) || !acceptsExternalEnergy()) {
            return 0;
        }
        float requested = Math.min(MANUAL_TRANSFER, capacity - energy);
        float consumed = NecronomiconItem.consumePotentialEnergy(book, requested);
        addEnergy(consumed);
        return consumed;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("PotEnergy", energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        float stored = tag.getFloat("PotEnergy");
        energy = Float.isFinite(stored) ? Math.clamp(stored, 0, capacity) : 0;
    }
}
