package dev.abyssalport.item;

import dev.abyssalport.content.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Source-informed level-six tool values; later Ethaxium blocks remain disallowed by their own tags. */
public enum DreadiumTier implements Tier {
    INSTANCE;
    @Override public int getUses() { return 2300; }
    @Override public float getSpeed() { return 14; }
    @Override public float getAttackDamageBonus() { return 6; }
    @Override public int getEnchantmentValue() { return 14; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return TagKey.create(Registries.BLOCK,WastelandContent.id("incorrect_for_dreadium_tool")); }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(DreadProcessingContent.INGOT.get()); }
}
