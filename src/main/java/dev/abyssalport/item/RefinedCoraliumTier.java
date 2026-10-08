package dev.abyssalport.item;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.content.WastelandMineralsContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public enum RefinedCoraliumTier implements Tier {
    INSTANCE;
    @Override public int getUses() { return 1800; }
    @Override public float getSpeed() { return 12; }
    @Override public float getAttackDamageBonus() { return 5; }
    @Override public int getEnchantmentValue() { return 13; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "incorrect_for_refined_coralium_tool"));
    }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(WastelandMineralsContent.INGOT.get()); }
}
