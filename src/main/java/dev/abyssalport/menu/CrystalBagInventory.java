package dev.abyssalport.menu;

import dev.abyssalport.item.CrystalBagItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Server-only live inventory bound to the original held stack, never an equal replacement bag. */
public final class CrystalBagInventory extends SimpleContainer {
    private final ItemStack bag;
    private final Player owner;
    private final InteractionHand hand;
    private final int lockedSlot;
    private boolean loading=true;
    public CrystalBagInventory(Player owner,ItemStack bag,InteractionHand hand,int lockedSlot) {
        super(CrystalBagItem.capacity(bag));
        if(!CrystalBagItem.hasValidContents(bag)) throw new IllegalArgumentException("Invalid crystal-bag contents");
        this.bag=bag; this.owner=owner; this.hand=hand; this.lockedSlot=lockedSlot;
        var contents=bag.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        for(int slot=0;slot<Math.min(getContainerSize(),contents.getSlots());slot++) super.setItem(slot,contents.getStackInSlot(slot));
        loading=false;
    }
    @Override public boolean stillValid(Player player) {
        return player==owner && !player.isRemoved() && player.isAlive() && player.getItemInHand(hand)==bag
                && (hand!=InteractionHand.MAIN_HAND || player.getInventory().selected==lockedSlot);
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return CrystalBagItem.accepts(stack); }
    @Override public void setItem(int slot,ItemStack stack) {
        if(!stack.isEmpty() && !canPlaceItem(slot,stack)) throw new IllegalArgumentException("Crystal bag rejects this item");
        super.setItem(slot,stack);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        var removed=super.removeItemNoUpdate(slot); if(!removed.isEmpty()) setChanged(); return removed;
    }
    @Override public void setChanged() {
        super.setChanged();
        if(!loading && stillValid(owner)) {
            var items=new java.util.ArrayList<ItemStack>();
            for(int slot=0;slot<getContainerSize();slot++) items.add(getItem(slot));
            bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items));
            owner.getInventory().setChanged();
        }
    }
}
