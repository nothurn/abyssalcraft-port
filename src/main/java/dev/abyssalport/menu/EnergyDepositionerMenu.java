package dev.abyssalport.menu;

import dev.abyssalport.block.entity.EnergyDepositionerBlockEntity;
import dev.abyssalport.content.EnergyDepositionerContent;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** ContainerEnergyDepositioner, Shinoow, LGPL-3.0. Native authoritative 2+36-slot menu. */
public final class EnergyDepositionerMenu extends AbstractContainerMenu {
    private final Container machine;
    private final ContainerData data;
    public EnergyDepositionerMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(2), new SimpleContainerData(2)); }
    public EnergyDepositionerMenu(int id, Inventory inventory, EnergyDepositionerBlockEntity be) {
        this(id, inventory, be, new ContainerData() {
            @Override public int get(int index) { return index == 0 ? be.progress() : (int)be.energy(); }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 2; }
        });
    }
    private EnergyDepositionerMenu(int id, Inventory inventory, Container machine, ContainerData data) {
        super(EnergyDepositionerContent.MENU.get(), id); this.machine = machine; this.data = data; addDataSlots(data);
        addSlot(new Slot(machine, 0, 44, 38) { @Override public boolean mayPlace(ItemStack stack) { return EnergyDepositionerBlockEntity.validFuel(stack); } });
        addSlot(new Slot(machine, 1, 116, 38) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + 18 * column, 84 + 18 * row));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + 18 * column, 142));
    }
    public int progress() { return data.get(0); }
    public int energy() { return data.get(1); }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player) && player.isAlive() && !player.isSpectator(); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) { if (!stillValid(player)) { resetQuickCraft(); return; } super.clicked(slot, button, type, player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (index < 2) { if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY; }
        else if (EnergyDepositionerBlockEntity.validFuel(stack)) { if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY; }
        else if (index < 29) { if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, original.copyWithCount(original.getCount() - stack.getCount())); return original;
    }
}
