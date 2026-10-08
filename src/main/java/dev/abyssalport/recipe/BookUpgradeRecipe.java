package dev.abyssalport.recipe;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.content.WastelandProgressionContent;
import dev.abyssalport.item.NecronomiconItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Native shaped matching/book display, with component-preserving output on both logical sides. */
public record BookUpgradeRecipe(ShapedRecipe base) implements CraftingRecipe {
    @Override public boolean matches(CraftingInput input, Level level) { return base.matches(input, level); }
    @Override public boolean canCraftInDimensions(int width, int height) { return base.canCraftInDimensions(width, height); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return base.getResultItem(registries); }
    @Override public NonNullList<Ingredient> getIngredients() { return base.getIngredients(); }
    @Override public String getGroup() { return base.getGroup(); }
    @Override public CraftingBookCategory category() { return base.category(); }
    @Override public boolean showNotification() { return base.showNotification(); }
    @Override public RecipeSerializer<?> getSerializer() { return WastelandProgressionContent.BOOK_UPGRADE.get(); }

    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack source = ItemStack.EMPTY;
        for (ItemStack stack : input.items()) if (NecronomiconItem.isBook(stack)) {
            if (!source.isEmpty()) return ItemStack.EMPTY;
            source = stack;
        }
        ItemStack output = base.assemble(input, registries);
        if (source.isEmpty() || !NecronomiconItem.isBook(output)
                || NecronomiconItem.tier(output) <= NecronomiconItem.tier(source)) return ItemStack.EMPTY;
        output.applyComponents(source.getComponentsPatch());
        NecronomiconItem.setPotentialEnergy(output, NecronomiconItem.getPotentialEnergy(source));
        return output;
    }

    public static final class Serializer implements RecipeSerializer<BookUpgradeRecipe> {
        private static final MapCodec<BookUpgradeRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(BookUpgradeRecipe::new, BookUpgradeRecipe::base);
        private static final StreamCodec<RegistryFriendlyByteBuf, BookUpgradeRecipe> STREAM =
                ShapedRecipe.Serializer.STREAM_CODEC.map(BookUpgradeRecipe::new, BookUpgradeRecipe::base);
        @Override public MapCodec<BookUpgradeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, BookUpgradeRecipe> streamCodec() { return STREAM; }
    }
}
