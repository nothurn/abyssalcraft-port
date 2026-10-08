package dev.abyssalport.effect;

import dev.abyssalport.content.WastelandContent;
import java.util.Set;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Initial fluid-exposure damage; contagion, transformations and antidote progression remain separate. */
public final class CoraliumPlagueEffect extends MobEffect {
    private static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, WastelandContent.id("coralium_immune"));
    public CoraliumPlagueEffect() { super(MobEffectCategory.HARMFUL, 0x29958B); }
    public static boolean immune(LivingEntity entity) { return entity.getType().is(IMMUNE); }
    @Override public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effect) { }
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && !immune(entity) && entity.tickCount % Math.max(1, 40 >> Math.clamp(amplifier, 0, 5)) == 0)
            entity.hurt(entity.damageSources().magic(), 2);
        return true;
    }
}
