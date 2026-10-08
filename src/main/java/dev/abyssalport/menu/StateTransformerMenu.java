package dev.abyssalport.menu;

import dev.abyssalport.block.entity.StateTransformerBlockEntity;
import dev.abyssalport.content.StateTransformerContent;
import dev.abyssalport.item.StoneTabletItem;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Native 50+36-slot menu; modes and processing counters are server-owned.
 * Source-informed ContainerStateTransformer, Shinoow, LGPL-3.0.
 */
public final class StateTransformerMenu extends AbstractContainerMenu {
    private final Container machine;
    private final ContainerData data;
    public StateTransformerMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(50), new SimpleContainerData(2)); }
    public StateTransformerMenu(int id, Inventory inventory, StateTransformerBlockEntity be) {
        this(id, inventory, be, new ContainerData() {
            @Override public int get(int index) { return index == 0 ? be.progress() : be.mode(); }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 2; }
        });
    }
    private StateTransformerMenu(int id, Inventory inventory, Container machine, ContainerData data) {
        super(StateTransformerContent.MENU.get(), id); this.machine = machine; this.data = data;
        checkContainerSize(machine, 50); checkContainerDataCount(data, 2); addDataSlots(data);
        addMachineSlot(0, 15, 71);
        for (int row = 0; row < 7; row++) for (int column = 0; column < 7; column++) addMachineSlot(column + row * 7 + 1, 44 + 18 * column, 17 + 18 * row);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + 18 * column, 156 + 18 * row));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + 18 * column, 214));
    }
    private void addMachineSlot(int index, int x, int y) {
        addSlot(new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return progress() == 0 && (index == 0 ? StoneTabletItem.isTablet(stack) && !StoneTabletItem.isCursed(stack)
                        && (StoneTabletItem.isBlank(stack) || StoneTabletItem.isFilled(stack)) : StoneTabletItem.acceptsCargo(stack));
            }
            @Override public boolean mayPickup(Player player) { return progress() == 0; }
        });
    }
    public int progress() { return data.get(0); }
    public int mode() { return data.get(1); }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player) && player.isAlive() && !player.isSpectator(); }
    @Override public boolean clickMenuButton(Player player, int requested) {
        if (!stillValid(player) || player.containerMenu != this || !(machine instanceof StateTransformerBlockEntity be)
                || !player.mayInteract(player.level(), be.getBlockPos())) return false;
        return be.setMode(requested);
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || progress() > 0 && slot >= 0 && slot < 50) { resetQuickCraft(); return; }
        super.clicked(slot, button, type, player);
    }
    @Override public boolean canDragTo(Slot slot) { return slot.container != machine || progress() == 0; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.container != machine || progress() == 0; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || progress() > 0 || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (index < 50) { if (!moveItemStackTo(stack, 50, 86, true)) return ItemStack.EMPTY; }
        else if (StoneTabletItem.isTablet(stack) && !StoneTabletItem.isCursed(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (StoneTabletItem.acceptsCargo(stack)) {
            if (!moveItemStackTo(stack, 1, 50, false)) return ItemStack.EMPTY;
        } else if (index < 77) { if (!moveItemStackTo(stack, 77, 86, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 50, 77, false)) return ItemStack.EMPTY;
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount())); return original;
    }
}
