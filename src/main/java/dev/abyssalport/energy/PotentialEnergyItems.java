package dev.abyssalport.energy;

import dev.abyssalport.item.EnergyStorageItem;
import dev.abyssalport.item.NecronomiconItem;
import net.minecraft.world.item.ItemStack;

/** Native container-item roles from PEUtils, Shinoow, LGPL-3.0.
 * One component-bearing item owns one balance; stacked malformed inputs are not
 * repeatedly drained/charged as if each unit held a copy of that same balance.
 */
public final class PotentialEnergyItems {
    private PotentialEnergyItems() { }
    public static boolean isContainer(ItemStack stack) {
        return stack.getCount() == 1 && (NecronomiconItem.isBook(stack) || stack.getItem() instanceof EnergyStorageItem);
    }
    public static int capacity(ItemStack stack) {
        if (!isContainer(stack)) return 0;
        return NecronomiconItem.isBook(stack) ? NecronomiconItem.capacity(stack) : EnergyStorageItem.capacity(stack);
    }
    public static float stored(ItemStack stack) {
        if (!isContainer(stack)) return 0;
        return NecronomiconItem.isBook(stack) ? NecronomiconItem.getPotentialEnergy(stack) : EnergyStorageItem.storedEnergy(stack);
    }
    public static float add(ItemStack stack, float amount) {
        if (!canReceive(stack)) return amount;
        return NecronomiconItem.isBook(stack) ? NecronomiconItem.addPotentialEnergy(stack, amount) : EnergyStorageItem.addEnergy(stack, amount);
    }
    public static boolean canReceive(ItemStack stack) {
        return isContainer(stack) && (NecronomiconItem.isBook(stack) || EnergyStorageItem.acceptsEnergy(stack));
    }
    public static float consume(ItemStack stack, float amount) {
        if (!isContainer(stack)) return 0;
        return NecronomiconItem.isBook(stack) ? NecronomiconItem.consumePotentialEnergy(stack, amount) : EnergyStorageItem.consumeEnergy(stack, amount);
    }
}
