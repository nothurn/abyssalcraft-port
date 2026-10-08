package dev.abyssalport.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Source-informed ItemStoneTablet / SlotNoInventory, Shinoow, LGPL-3.0.
 * A sealed item inventory is not a rechargeable PE container. Its potential
 * value is derived from the cargo, never supplied by a book or statue.
 */
public final class StoneTabletItem extends Item {
    public static final int CARGO_SLOTS = 49;
    private static final String ENERGY = "PotEnergy";
    private static final String CURSE = "Cursed";

    public StoneTabletItem(Properties properties) { super(properties); }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static boolean isTablet(ItemStack stack) {
        return stack.getCount() == 1 && stack.getItem() instanceof StoneTabletItem;
    }

    // The source checks key presence, including a stored false value.
    public static boolean isCursed(ItemStack stack) {
        return stack.getItem() instanceof StoneTabletItem && data(stack).contains(CURSE);
    }

    public static boolean isBlank(ItemStack stack) {
        return isTablet(stack) && !isCursed(stack) && !stack.has(DataComponents.CONTAINER)
                && !data(stack).contains(ENERGY);
    }

    public static boolean acceptsCargo(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.getCount() > Math.min(64, stack.getMaxStackSize())
                || stack.getItem() instanceof StoneTabletItem || !stack.canFitInsideContainerItems()
                || stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock
                || stack.is(Items.BUNDLE) || stack.has(DataComponents.CONTAINER)
                || stack.has(DataComponents.CONTAINER_LOOT) || stack.has(DataComponents.BUNDLE_CONTENTS)) return false;
        var blockData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockData != null && (blockData.copyTag().contains("Items") || blockData.copyTag().contains("LootTable"))) return false;
        return stack.getCapability(Capabilities.ItemHandler.ITEM) == null;
    }

    public static float value(List<ItemStack> cargo) {
        float energy = 0;
        for (var stack : cargo) if (!stack.isEmpty())
            energy += stack.getCount() * (64 / stack.getMaxStackSize());
        return energy;
    }

    public static boolean isFilled(ItemStack stack) {
        if (!isTablet(stack) || !stack.has(DataComponents.CONTAINER)) return false;
        var tag = data(stack);
        if (!tag.contains(ENERGY, Tag.TAG_ANY_NUMERIC)) return false;
        float energy = tag.getFloat(ENERGY);
        var contents = stack.get(DataComponents.CONTAINER);
        if (!Float.isFinite(energy) || energy < 0 || contents.getSlots() > CARGO_SLOTS) return false;
        float actual = 0;
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            var cargo = contents.getStackInSlot(slot);
            if (!acceptsCargo(cargo)) return false;
            if (!cargo.isEmpty()) actual += cargo.getCount() * (64 / cargo.getMaxStackSize());
        }
        return energy == actual;
    }

    public static float storedValue(ItemStack stack) { return isFilled(stack) ? data(stack).getFloat(ENERGY) : 0; }

    public static NonNullList<ItemStack> cargo(ItemStack stack) {
        if (!isFilled(stack)) throw new IllegalArgumentException("Invalid sealed tablet");
        var items = NonNullList.withSize(CARGO_SLOTS, ItemStack.EMPTY);
        stack.get(DataComponents.CONTAINER).copyInto(items);
        return items;
    }

    public static ItemStack seal(ItemStack original, List<ItemStack> cargo) {
        if (!isBlank(original) || cargo.size() != CARGO_SLOTS || cargo.stream().anyMatch(stack -> !acceptsCargo(stack)))
            throw new IllegalArgumentException("Invalid tablet cargo");
        var output = original.copy();
        output.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(cargo));
        var tag = data(output); tag.putFloat(ENERGY, value(cargo));
        output.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return output;
    }

    public static ItemStack empty(ItemStack original) {
        if (!isFilled(original)) throw new IllegalArgumentException("Invalid sealed tablet");
        var output = original.copy(); output.remove(DataComponents.CONTAINER);
        var tag = data(output); tag.remove(ENERGY);
        if (tag.isEmpty()) output.remove(DataComponents.CUSTOM_DATA);
        else output.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return output;
    }

    public static void curse(ItemStack stack) {
        if (!isTablet(stack)) throw new IllegalArgumentException("Not a singleton tablet");
        var tag = data(stack); tag.putBoolean(CURSE, true); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override public boolean canFitInsideContainerItems() { return false; }
    @Override public boolean isFoil(ItemStack stack) { return isFilled(stack); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isCursed(stack)) tooltip.add(Component.translatable("tooltip.abyssalport.stone_tablet.cursed").withStyle(ChatFormatting.DARK_PURPLE));
        if (isFilled(stack)) {
            tooltip.add(Component.translatable("tooltip.abyssalport.stone_tablet.value", (int)storedValue(stack)).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.abyssalport.stone_tablet.contents",
                    stack.get(DataComponents.CONTAINER).nonEmptyStream().count(), CARGO_SLOTS).withStyle(ChatFormatting.GRAY));
        } else if (isBlank(stack)) tooltip.add(Component.translatable("tooltip.abyssalport.stone_tablet.blank").withStyle(ChatFormatting.GRAY));
        else if (!isCursed(stack)) tooltip.add(Component.translatable("tooltip.abyssalport.stone_tablet.invalid").withStyle(ChatFormatting.RED));
    }
}
