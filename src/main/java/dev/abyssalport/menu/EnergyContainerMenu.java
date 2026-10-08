package dev.abyssalport.menu;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.entity.EnergyContainerBlockEntity;
import dev.abyssalport.energy.PotentialEnergyItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Two native PE item slots and lossless 32-bit balance/capacity synchronization. */
public final class EnergyContainerMenu extends AbstractContainerMenu {
    private static final int CONTAINER_SLOTS = 2;
    private final ContainerLevelAccess access;
    private final DataSlot[] balanceData = new DataSlot[4];
    private final Block expectedBlock;

    public EnergyContainerMenu(int id, Inventory inventory) {
        this(id, inventory, new ItemStackHandler(CONTAINER_SLOTS), null, ContainerLevelAccess.NULL);
    }

    public EnergyContainerMenu(int id, Inventory inventory, EnergyContainerBlockEntity container) {
        this(id, inventory, container.items(), container,
                ContainerLevelAccess.create(container.getLevel(), container.getBlockPos()));
    }

    private EnergyContainerMenu(int id, Inventory inventory, ItemStackHandler items, EnergyContainerBlockEntity container,
            ContainerLevelAccess access) {
        super(AbyssalPort.ENERGY_CONTAINER_MENU.get(), id);
        this.access = access;
        expectedBlock = container == null ? AbyssalPort.ENERGY_CONTAINER.get() : container.getBlockState().getBlock();
        for (int index = 0; index < balanceData.length; index++) {
            final int field = index;
            balanceData[index] = container == null ? DataSlot.standalone() : new DataSlot() {
                @Override public int get() {
                    int value = field < 2 ? (int) container.energy() : container.capacity();
                    return (value >>> (field % 2 * 16)) & 0xFFFF;
                }
                @Override public void set(int value) { /* Server balance is authoritative. */ }
            };
            addDataSlot(balanceData[index]);
        }
        if (container == null) balanceData[2].set(EnergyContainerBlockEntity.CAPACITY);
        addSlot(new SlotItemHandler(items, 0, 44, 38));
        addSlot(new SlotItemHandler(items, 1, 116, 38));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 100 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 158));
        }
    }

    public int energy() {
        return join(balanceData[0].get(), balanceData[1].get());
    }

    public int capacity() { return join(balanceData[2].get(), balanceData[3].get()); }
    private static int join(int low, int high) { return (low & 0xFFFF) | ((high & 0xFFFF) << 16); }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, expectedBlock);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = slot.getItem();
        ItemStack moved = original.copy();
        if (index < CONTAINER_SLOTS) {
            if (!moveItemStackTo(original, CONTAINER_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (PotentialEnergyItems.isContainer(original)) {
            int target = PotentialEnergyItems.stored(original) > 0 || !PotentialEnergyItems.canReceive(original) ? 0 : 1;
            if (!moveItemStackTo(original, target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }
}
