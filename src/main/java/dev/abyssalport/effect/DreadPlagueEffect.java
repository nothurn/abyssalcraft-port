package dev.abyssalport.effect;

import dev.abyssalport.content.WastelandContent;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.EffectCure;

/** Source-informed damage/exhaustion core; contagion and conversions are not claimed complete. */
public final class DreadPlagueEffect extends MobEffect {
    private static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, WastelandContent.id("dread_immune"));
    public DreadPlagueEffect() { super(MobEffectCategory.HARMFUL, 0xA13741); }
    public static boolean immune(LivingEntity entity) { return entity.getType().is(IMMUNE); }
    @Override public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effect) { }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }
    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || immune(entity)) return true;
        int potency = Math.clamp(amplifier, 0, 5);
        // The source's unparenthesized modulo/shift is not reproduced as a burst of damage.
        if (entity.tickCount % Math.max(1, 25 >> potency) == 0) entity.hurt(entity.damageSources().magic(), 1);
        if (entity instanceof Player player) player.causeFoodExhaustion(.025F * (potency + 2));
        return true;
    }
}
