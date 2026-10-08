package dev.abyssalport.menu;

import dev.abyssalport.block.entity.TransmutatorBlockEntity;
import dev.abyssalport.content.WastelandProcessingContent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Native synchronized slots; clients cannot set processing or fuel counters. */
public final class TransmutatorMenu extends AbstractContainerMenu {
    private final Container machine;
    private final ContainerData data;
    public TransmutatorMenu(int id,Inventory inventory) { this(id,inventory,new SimpleContainer(3),new SimpleContainerData(3)); }
    public TransmutatorMenu(int id,Inventory inventory,TransmutatorBlockEntity be) {
        this(id,inventory,be,new ContainerData() {
            @Override public int get(int index) { return switch(index) { case 0->be.progress(); case 1->be.burnTime(); case 2->be.totalBurnTime(); default->0; }; }
            @Override public void set(int index,int value) { }
            @Override public int getCount() { return 3; }
        });
    }
    private TransmutatorMenu(int id,Inventory inventory,Container machine,ContainerData data) {
        super(WastelandProcessingContent.MENU.get(),id); this.machine=machine; this.data=data;
        checkContainerSize(machine,3); checkContainerDataCount(data,3); addDataSlots(data);
        addSlot(new Slot(machine,0,44,32));
        addSlot(new Slot(machine,1,44,64) { @Override public boolean mayPlace(ItemStack stack) { return TransmutatorBlockEntity.fuelTicks(stack)>0; } });
        addSlot(new Slot(machine,2,116,48) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player,ItemStack stack) {
                if(machine instanceof TransmutatorBlockEntity be) be.payExperience(); super.onTake(player,stack);
            }
        });
        for(int row=0;row<3;row++) for(int column=0;column<9;column++) addSlot(new Slot(inventory,column+row*9+9,8+18*column,110+18*row));
        for(int column=0;column<9;column++) addSlot(new Slot(inventory,column,8+18*column,168));
    }
    public int progress() { return data.get(0); }
    public int burnTime() { return data.get(1); }
    public int totalBurnTime() { return data.get(2); }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        var slot=slots.get(index); if(!slot.hasItem()) return ItemStack.EMPTY;
        var stack=slot.getItem(); var original=stack.copy();
        if(index<3) { if(!moveItemStackTo(stack,3,slots.size(),true)) return ItemStack.EMPTY; }
        else {
            int target=TransmutatorBlockEntity.fuelTicks(stack)>0?1:0;
            if(!moveItemStackTo(stack,target,target+1,false)) return ItemStack.EMPTY;
        }
        if(stack.getCount()==original.getCount()) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,original.copyWithCount(original.getCount()-stack.getCount()));
        return original;
    }
}
