package dev.abyssalport.block.entity;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.energy.EnergyStorageBlockEntity;
import dev.abyssalport.block.EnergyContainerBlock;
import dev.abyssalport.energy.PotentialEnergyItems;
import dev.abyssalport.menu.EnergyContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** First-tier PE reservoir. The original container holds 10,000 PE. */
public final class EnergyContainerBlockEntity extends EnergyStorageBlockEntity implements MenuProvider {
    public static final int CAPACITY = 10_000;
    public static final int ITEM_TRANSFER_PER_TICK = 20;
    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return PotentialEnergyItems.isContainer(stack);
        }

        @Override public int getSlotLimit(int slot) { return 1; }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public EnergyContainerBlockEntity(BlockPos pos, BlockState state) {
        super(AbyssalPort.ENERGY_CONTAINER_ENTITY.get(), pos, state,
                ((EnergyContainerBlock) state.getBlock()).tier().containerCapacity());
    }

    public ItemStackHandler items() {
        return items;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyContainerBlockEntity container) {
        ItemStack input = container.items.getStackInSlot(0);
        if (PotentialEnergyItems.isContainer(input)) {
            float amount = Math.min(ITEM_TRANSFER_PER_TICK,
                    Math.min(container.capacity() - container.energy(),
                            PotentialEnergyItems.stored(input)));
            if (amount > 0) {
                float taken = PotentialEnergyItems.consume(input, amount);
                if (taken > 0) {
                    container.addEnergy(taken);
                    container.items.setStackInSlot(0, input);
                }
            }
        }

        ItemStack output = container.items.getStackInSlot(1);
        if (PotentialEnergyItems.canReceive(output) && output != input) {
            float amount = Math.min(ITEM_TRANSFER_PER_TICK,
                    Math.min(container.energy(), PotentialEnergyItems.capacity(output)
                            - PotentialEnergyItems.stored(output)));
            if (amount > 0) {
                float accepted = amount - PotentialEnergyItems.add(output, amount);
                if (accepted > 0) {
                    container.consumeEnergy(accepted);
                    container.items.setStackInSlot(1, output);
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EnergyContainerMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Items"));
    }
}
