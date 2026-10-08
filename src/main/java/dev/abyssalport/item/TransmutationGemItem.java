package dev.abyssalport.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A ten-use crafting catalyst. Each real recipe returns one damaged copy, never a fresh gem. */
public final class TransmutationGemItem extends Item {
    public TransmutationGemItem(Properties properties) { super(properties); }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) { return stack.getDamageValue() + 1 < stack.getMaxDamage(); }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (!hasCraftingRemainingItem(stack)) return ItemStack.EMPTY;
        ItemStack remainder = stack.copyWithCount(1);
        remainder.setDamageValue(stack.getDamageValue() + 1);
        return remainder;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack material) { return false; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.abyssalport.transmutation_gem", stack.getMaxDamage() - stack.getDamageValue()));
    }
}
