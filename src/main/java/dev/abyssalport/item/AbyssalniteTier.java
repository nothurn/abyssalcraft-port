package dev.abyssalport.item;

import dev.abyssalport.content.AbyssalniteContent;
import dev.abyssalport.AbyssalPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Source-informed first metal tier; see SOURCE-AUDIT for LGPL provenance. */
public enum AbyssalniteTier implements Tier {
    INSTANCE;

    @Override public int getUses() { return 1261; }
    @Override public float getSpeed() { return 10.0F; }
    @Override public float getAttackDamageBonus() { return 4.0F; }
    @Override public int getEnchantmentValue() { return 12; }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        // Original level 4 admits liquified Coralium, but not level-5 pearlescent ore.
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(
                AbyssalPort.MOD_ID, "incorrect_for_abyssalnite_tool"));
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(AbyssalniteContent.INGOT.get());
    }
}
