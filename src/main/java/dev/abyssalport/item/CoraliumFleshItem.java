package dev.abyssalport.item;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.effect.CoraliumPlagueEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CoraliumFleshItem extends Item {
    public CoraliumFleshItem(Properties properties) { super(properties); }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        var remainder = super.finishUsingItem(stack, level, consumer);
        if (!level.isClientSide) {
            consumer.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 1));
            consumer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600));
            if (!CoraliumPlagueEffect.immune(consumer)) consumer.addEffect(new MobEffectInstance(WastelandContent.CORALIUM_PLAGUE, 600));
        }
        return remainder;
    }
}
