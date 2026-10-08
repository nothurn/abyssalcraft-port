package dev.abyssalport.menu;

import dev.abyssalport.content.CrystalBagContent;
import dev.abyssalport.item.CrystalBagItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class CrystalBagMenu extends AbstractContainerMenu {
    private final Container contents;
    private final Inventory playerInventory;
    private final int capacity,lockedSlot;
    public CrystalBagMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer) {
        this(id,inventory,readCapacity(buffer),buffer.readEnum(InteractionHand.class),buffer.readVarInt());
    }
    private static int readCapacity(RegistryFriendlyByteBuf buffer) {
        int capacity=buffer.readVarInt();
        if(capacity!=18 && capacity!=36 && capacity!=54) throw new IllegalArgumentException("Invalid crystal-bag menu size");
        return capacity;
    }
    private CrystalBagMenu(int id,Inventory inventory,int capacity,InteractionHand hand,int lockedSlot) {
        this(id,inventory,new SimpleContainer(capacity),lockedSlot);
        if(hand==InteractionHand.MAIN_HAND && (lockedSlot<0 || lockedSlot>8) || hand==InteractionHand.OFF_HAND && lockedSlot!=40)
            throw new IllegalArgumentException("Invalid crystal-bag hand slot");
    }
    public CrystalBagMenu(int id,Inventory inventory,ItemStack bag,InteractionHand hand,int lockedSlot) {
        this(id,inventory,new CrystalBagInventory(inventory.player,bag,hand,lockedSlot),lockedSlot);
    }
    private CrystalBagMenu(int id,Inventory inventory,Container contents,int lockedSlot) {
        super(CrystalBagContent.MENU.get(),id);
        this.contents=contents; this.capacity=contents.getContainerSize(); this.playerInventory=inventory; this.lockedSlot=lockedSlot;
        for(int slot=0;slot<capacity;slot++) addSlot(new Slot(contents,slot,8+(slot%9)*18,18+(slot/9)*18) {
            @Override public boolean mayPlace(ItemStack stack) { return CrystalBagItem.accepts(stack); }
        });
        int top=capacity/9*18+49;
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(inventory,col+row*9+9,8+col*18,top+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,top+58) {
            @Override public boolean mayPickup(Player player) { return getContainerSlot()!=lockedSlot; }
            @Override public boolean mayPlace(ItemStack stack) { return getContainerSlot()!=lockedSlot; }
        });
    }
    public int rows() { return capacity/9; }
    public int capacity() { return capacity; }
    private boolean locked(int index) {
        return index>=0 && index<slots.size() && getSlot(index).container==playerInventory && getSlot(index).getContainerSlot()==lockedSlot;
    }
    @Override public boolean stillValid(Player player) { return contents.stillValid(player); }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(!stillValid(player) || locked(slot) || type==ClickType.SWAP && button==lockedSlot) return;
        super.clicked(slot,button,type,player);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player) || index<0 || index>=slots.size() || locked(index)) return ItemStack.EMPTY;
        var slot=getSlot(index); if(!slot.hasItem()) return ItemStack.EMPTY;
        var stack=slot.getItem(); var original=stack.copy();
        if(index<capacity) { if(!moveItemStackTo(stack,capacity,slots.size(),true)) return ItemStack.EMPTY; }
        else if(!CrystalBagItem.accepts(stack) || !moveItemStackTo(stack,0,capacity,false)) return ItemStack.EMPTY;
        if(stack.getCount()==original.getCount()) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,original.copyWithCount(original.getCount()-stack.getCount()));
        return original;
    }
}
