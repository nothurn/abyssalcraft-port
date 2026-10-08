package dev.abyssalport.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.abyssalport.content.WastelandProcessingContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Native reloadable processing recipe, separate from ordinary furnace recipes. */
public record TransmutationRecipe(Ingredient ingredient,ItemStack result,float experience) implements Recipe<SingleRecipeInput> {
    public TransmutationRecipe {
        if(result.isEmpty() || result.getCount()>result.getMaxStackSize() || !Float.isFinite(experience) || experience<0 || experience>100)
            throw new IllegalArgumentException("Invalid transmutation output/experience");
        result=result.copy();
    }
    @Override public boolean matches(SingleRecipeInput input,Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=1; }
    @Override public RecipeSerializer<?> getSerializer() { return WastelandProcessingContent.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return WastelandProcessingContent.TRANSMUTATION.get(); }
    @Override public boolean isSpecial() { return true; }
    public static final class Serializer implements RecipeSerializer<TransmutationRecipe> {
        private static final MapCodec<TransmutationRecipe> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(TransmutationRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(TransmutationRecipe::result),
                com.mojang.serialization.Codec.FLOAT.optionalFieldOf("experience",0F).forGetter(TransmutationRecipe::experience)
        ).apply(instance,TransmutationRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf,TransmutationRecipe> STREAM=new StreamCodec<>() {
            @Override public TransmutationRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new TransmutationRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),ItemStack.STREAM_CODEC.decode(buffer),buffer.readFloat());
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer,TransmutationRecipe recipe) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,recipe.ingredient()); ItemStack.STREAM_CODEC.encode(buffer,recipe.result()); buffer.writeFloat(recipe.experience());
            }
        };
        @Override public MapCodec<TransmutationRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,TransmutationRecipe> streamCodec() { return STREAM; }
    }
}
