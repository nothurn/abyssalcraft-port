package dev.abyssalport.recipe;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.content.CrystalBagContent;
import dev.abyssalport.item.CrystalBagItem;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Upgrade the original bag, preserving contents and names instead of crafting an empty replacement. */
public record CrystalBagUpgradeRecipe(ShapedRecipe base) implements CraftingRecipe {
    private static ItemStack source(CraftingInput input) {
        var found=ItemStack.EMPTY;
        for(var stack:input.items()) if(CrystalBagItem.capacity(stack)>0) {
            if(!found.isEmpty()) return ItemStack.EMPTY; found=stack;
        }
        return found;
    }
    @Override public boolean matches(CraftingInput input,Level level) {
        var bag=source(input);
        return base.matches(input,level) && CrystalBagItem.hasValidContents(bag)
                && CrystalBagItem.capacity(base.getResultItem(level.registryAccess()))==CrystalBagItem.capacity(bag)+18;
    }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries) {
        var bag=source(input); var output=base.assemble(input,registries);
        if(!CrystalBagItem.hasValidContents(bag) || CrystalBagItem.capacity(output)!=CrystalBagItem.capacity(bag)+18) return ItemStack.EMPTY;
        output.applyComponents(bag.getComponentsPatch()); return output;
    }
    @Override public boolean canCraftInDimensions(int width,int height) { return base.canCraftInDimensions(width,height); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return base.getResultItem(registries); }
    @Override public NonNullList<Ingredient> getIngredients() { return base.getIngredients(); }
    @Override public String getGroup() { return base.getGroup(); }
    @Override public CraftingBookCategory category() { return base.category(); }
    @Override public boolean showNotification() { return base.showNotification(); }
    @Override public RecipeSerializer<?> getSerializer() { return CrystalBagContent.UPGRADE.get(); }
    public static final class Serializer implements RecipeSerializer<CrystalBagUpgradeRecipe> {
        private static final MapCodec<CrystalBagUpgradeRecipe> CODEC=ShapedRecipe.Serializer.CODEC.xmap(CrystalBagUpgradeRecipe::new,CrystalBagUpgradeRecipe::base);
        private static final StreamCodec<RegistryFriendlyByteBuf,CrystalBagUpgradeRecipe> STREAM=ShapedRecipe.Serializer.STREAM_CODEC.map(CrystalBagUpgradeRecipe::new,CrystalBagUpgradeRecipe::base);
        @Override public MapCodec<CrystalBagUpgradeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,CrystalBagUpgradeRecipe> streamCodec() { return STREAM; }
    }
}
