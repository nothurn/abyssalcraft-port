package dev.abyssalport.block.entity;

import java.util.stream.IntStream;
import dev.abyssalport.block.StateTransformerBlock;
import dev.abyssalport.content.StateTransformerContent;
import dev.abyssalport.item.StoneTabletItem;
import dev.abyssalport.menu.StateTransformerMenu;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Source-informed TileEntityStateTransformer, Shinoow, LGPL-3.0.
 * Inventories remain live until an atomic completion. A persisted snapshot
 * invalidates progress when any external mutation changes the transaction.
 */
public final class StateTransformerBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int PROCESS_TICKS = 200;
    public static final int SIZE = StoneTabletItem.CARGO_SLOTS + 1;
    private static final int[] TABLET = {0};
    private static final int[] ALL = IntStream.range(0, SIZE).toArray();
    private static final int[] NONE = {};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final NonNullList<ItemStack> active = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int mode, progress;

    public StateTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(StateTransformerContent.ENTITY.get(), pos, state);
    }
    public int mode() { return mode; }
    public int progress() { return progress; }
    public boolean processing() { return progress > 0; }
    public boolean setMode(int requested) {
        if (requested < 0 || requested > 1 || processing()) return false;
        if (requested != mode) { mode = requested; invalidateCapabilities(); setChanged(); }
        return true;
    }

    private boolean ready() {
        var tablet = items.getFirst();
        if (StoneTabletItem.isCursed(tablet)) return false;
        if (mode == 0) {
            if (!StoneTabletItem.isBlank(tablet)) return false;
            boolean found = false;
            for (int slot = 1; slot < SIZE; slot++) {
                var stack = items.get(slot);
                if (!StoneTabletItem.acceptsCargo(stack)) return false;
                found |= !stack.isEmpty();
            }
            return found;
        }
        return StoneTabletItem.isFilled(tablet) && items.subList(1, SIZE).stream().allMatch(ItemStack::isEmpty);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StateTransformerBlockEntity be) {
        if (level.isClientSide) return;
        if (be.progress > 0 && !ItemStack.listMatches(be.items, be.active)) be.cancel();
        else if (be.ready()) {
            if (be.progress == 0) for (int slot = 0; slot < SIZE; slot++) be.active.set(slot, be.items.get(slot).copy());
            if (++be.progress == PROCESS_TICKS) {
                if (be.mode == 0) {
                    var output = StoneTabletItem.seal(be.items.getFirst(), be.items.subList(1, SIZE));
                    for (int slot = 1; slot < SIZE; slot++) be.items.set(slot, ItemStack.EMPTY);
                    be.items.set(0, output);
                } else {
                    var cargo = StoneTabletItem.cargo(be.items.getFirst());
                    var blank = StoneTabletItem.empty(be.items.getFirst());
                    for (int slot = 1; slot < SIZE; slot++) be.items.set(slot, cargo.get(slot - 1));
                    be.items.set(0, blank);
                }
                be.cancel();
            }
            be.setChanged();
        } else if (be.progress != 0) be.cancel();
        boolean present = !be.items.getFirst().isEmpty();
        var current = level.getBlockState(pos);
        if (current.is(StateTransformerContent.BLOCK.get()) && current.getValue(StateTransformerBlock.TABLET) != present)
            level.setBlock(pos, current.setValue(StateTransformerBlock.TABLET, present), 3);
    }

    private void cancel() { progress = 0; active.clear(); setChanged(); }
    public int comparatorSignal() {
        if (processing()) return 0;
        float fraction = 0; int occupied = 0;
        for (int slot = 1; slot < SIZE; slot++) if (!items.get(slot).isEmpty()) {
            fraction += (float)items.get(slot).getCount() / Math.min(64, items.get(slot).getMaxStackSize()); occupied++;
        }
        return Math.clamp((int)Math.floor(fraction / SIZE * 14) + (occupied > 0 ? 1 : 0), 0, 15);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Mode", mode); tag.putInt("ProcessingTime", progress);
        if (processing()) {
            var snapshot = new CompoundTag(); ContainerHelper.saveAllItems(snapshot, active, registries); tag.put("ActiveItems", snapshot);
        }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); items.clear(); active.clear(); ContainerHelper.loadAllItems(tag, items, registries);
        mode = tag.getInt("Mode") == 1 ? 1 : 0;
        progress = Math.clamp(tag.getInt("ProcessingTime"), 0, PROCESS_TICKS - 1);
        if (progress > 0 && tag.contains("ActiveItems")) ContainerHelper.loadAllItems(tag.getCompound("ActiveItems"), active, registries);
        if (!ItemStack.listMatches(items, active) || !ready()) { progress = 0; active.clear(); }
    }
    @Override public int getContainerSize() { return SIZE; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        var result = ContainerHelper.removeItem(items, slot, count); if (!result.isEmpty()) setChanged(); return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        var result = ContainerHelper.takeItem(items, slot); if (!result.isEmpty()) setChanged(); return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack); setChanged(); // Never truncate externally supplied cargo: validate before processing.
    }
    @Override public void clearContent() { items.clear(); cancel(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return !processing() && slot >= 0 && slot < SIZE && (slot == 0
                ? StoneTabletItem.isTablet(stack) && !StoneTabletItem.isCursed(stack)
                    && (StoneTabletItem.isBlank(stack) || StoneTabletItem.isFilled(stack))
                : StoneTabletItem.acceptsCargo(stack));
    }
    @Override public int[] getSlotsForFace(Direction face) {
        return mode == 0 ? (face == Direction.UP ? ALL.clone() : TABLET.clone())
                : face == Direction.UP ? TABLET.clone() : face == Direction.DOWN ? ALL.clone() : NONE;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (!canPlaceItem(slot, stack)) return false;
        return mode == 0 ? slot > 0 && face == Direction.UP
                || slot == 0 && face.getAxis().isHorizontal() && StoneTabletItem.isBlank(stack)
                : slot == 0 && face == Direction.UP && StoneTabletItem.isFilled(stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        if (processing() || face != Direction.DOWN) return false;
        return mode == 0 ? slot == 0 && StoneTabletItem.isFilled(stack)
                : slot > 0 || slot == 0 && StoneTabletItem.isBlank(stack);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.abyssalport.state_transformer"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StateTransformerMenu(id, inventory, this);
    }
}
