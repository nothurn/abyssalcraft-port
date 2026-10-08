package dev.abyssalport.recipe;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.abyssalport.content.CrystallizationContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Two-output native recipe; the water flag additionally requires actual water potion contents. */
public record CrystallizationRecipe(Ingredient ingredient,ItemStack result,ItemStack secondary,float experience,boolean waterOnly)
        implements Recipe<SingleRecipeInput> {
    public CrystallizationRecipe {
        if(result.isEmpty() || result.getCount()>result.getMaxStackSize() || (!secondary.isEmpty() && secondary.getCount()>secondary.getMaxStackSize())
                || !Float.isFinite(experience) || experience<0 || experience>100)
            throw new IllegalArgumentException("Invalid crystallization output/experience");
        result=result.copy(); secondary=secondary.copy();
    }
    @Override public boolean matches(SingleRecipeInput input,Level level) {
        return ingredient.test(input.item()) && (!waterOnly || input.item().is(Items.POTION)
                && input.item().getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).is(Potions.WATER));
    }
    @Override public ItemStack assemble(SingleRecipeInput input,HolderLookup.Provider registries) { return result.copy(); }
    public ItemStack secondaryResult() { return secondary.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=1; }
    @Override public RecipeSerializer<?> getSerializer() { return CrystallizationContent.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return CrystallizationContent.TYPE.get(); }
    @Override public boolean isSpecial() { return true; }
    public static final class Serializer implements RecipeSerializer<CrystallizationRecipe> {
        private static final MapCodec<CrystallizationRecipe> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CrystallizationRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrystallizationRecipe::result),
                ItemStack.STRICT_CODEC.optionalFieldOf("secondary",ItemStack.EMPTY).forGetter(CrystallizationRecipe::secondary),
                Codec.FLOAT.optionalFieldOf("experience",0F).forGetter(CrystallizationRecipe::experience),
                Codec.BOOL.optionalFieldOf("water_only",false).forGetter(CrystallizationRecipe::waterOnly)
        ).apply(instance,CrystallizationRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf,CrystallizationRecipe> STREAM=new StreamCodec<>() {
            @Override public CrystallizationRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new CrystallizationRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),ItemStack.STREAM_CODEC.decode(buffer),
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),buffer.readFloat(),buffer.readBoolean());
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer,CrystallizationRecipe recipe) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,recipe.ingredient()); ItemStack.STREAM_CODEC.encode(buffer,recipe.result());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer,recipe.secondary()); buffer.writeFloat(recipe.experience()); buffer.writeBoolean(recipe.waterOnly());
            }
        };
        @Override public MapCodec<CrystallizationRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,CrystallizationRecipe> streamCodec() { return STREAM; }
    }
}
