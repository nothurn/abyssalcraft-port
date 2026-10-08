package dev.abyssalport.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

public final class EnergyStorageItem extends BlockItem {
    private final int capacity;

    public EnergyStorageItem(Block block, Properties properties, int capacity) {
        // A merged charged stack would clone its component balance on placement.
        // Keep stateful device items independent, like books and other machines.
        super(block, properties.stacksTo(1));
        this.capacity = capacity;
    }

    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof EnergyStorageItem storage ? storage.capacity : 0;
    }
    public static boolean acceptsEnergy(ItemStack stack) {
        return stack.getItem() instanceof EnergyStorageItem item
                && (!(item.getBlock() instanceof dev.abyssalport.block.EnergyStorageBlock block) || block.acceptsItemEnergy());
    }

    public static float addEnergy(ItemStack stack, float amount) {
        if (!(stack.getItem() instanceof EnergyStorageItem storage) || !Float.isFinite(amount) || amount < 0) return amount;
        float accepted = Math.min(amount, storage.capacity - storedEnergy(stack));
        if (accepted > 0) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            var tag = data == null ? new net.minecraft.nbt.CompoundTag() : data.copyTag();
            tag.putFloat("PotEnergy", storedEnergy(stack) + accepted);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return amount - accepted;
    }

    public static float storedEnergy(ItemStack stack) {
        if (!(stack.getItem() instanceof EnergyStorageItem storage)) return 0;
        CustomData data=stack.get(DataComponents.CUSTOM_DATA);
        float value=data==null?0:data.copyTag().getFloat("PotEnergy");
        return Float.isFinite(value)?Math.clamp(value,0,storage.capacity):0;
    }

    public static float consumeEnergy(ItemStack stack,float amount) {
        if (!(stack.getItem() instanceof EnergyStorageItem)||!Float.isFinite(amount)||amount<0) return 0;
        float taken=Math.min(storedEnergy(stack),amount);
        if(taken>0) {
            CustomData data=stack.get(DataComponents.CUSTOM_DATA);
            var tag=data==null?new net.minecraft.nbt.CompoundTag():data.copyTag();
            tag.putFloat("PotEnergy",storedEnergy(stack)-taken);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        }
        return taken;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.abyssalport.stored_energy",
                (int)storedEnergy(stack), capacity)
                .withStyle(ChatFormatting.GRAY));
    }
}
