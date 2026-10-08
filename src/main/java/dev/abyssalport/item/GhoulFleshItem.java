package dev.abyssalport.item;

import dev.abyssalport.entity.GhoulEntity;
import dev.abyssalport.content.*;
import dev.abyssalport.effect.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class GhoulFleshItem extends Item {
    private final GhoulEntity.Kind kind;
    public GhoulFleshItem(Properties properties,GhoulEntity.Kind kind) { super(properties); this.kind=kind; }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity consumer) {
        var remainder=super.finishUsingItem(stack,level,consumer);
        if(!level.isClientSide) {
            consumer.addEffect(new MobEffectInstance(MobEffects.HUNGER,600,1));
            consumer.addEffect(new MobEffectInstance(MobEffects.CONFUSION,600));
            if(kind==GhoulEntity.Kind.SHADOW) {
                consumer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,600));
                consumer.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,600));
            }
            if(kind==GhoulEntity.Kind.DEPTHS&&!CoraliumPlagueEffect.immune(consumer)) consumer.addEffect(new MobEffectInstance(WastelandContent.CORALIUM_PLAGUE,600));
            if(kind==GhoulEntity.Kind.DREADED&&!DreadPlagueEffect.immune(consumer)) consumer.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,600));
        }
        return remainder;
    }
}
