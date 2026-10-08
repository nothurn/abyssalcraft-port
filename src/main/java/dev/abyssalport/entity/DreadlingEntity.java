package dev.abyssalport.entity;

import dev.abyssalport.content.DreadlandsContent;
import dev.abyssalport.effect.DreadPlagueEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** Independent 1.21 entity integrating the source's small undead melee creature. */
public final class DreadlingEntity extends Monster {
    public DreadlingEntity(EntityType<? extends DreadlingEntity> type, Level level) { super(type, level); xpReward = 5; }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, .35).add(Attributes.FOLLOW_RANGE, 16);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, true));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 1));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    public static boolean checkSpawnRules(EntityType<DreadlingEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                         BlockPos pos, RandomSource random) {
        return level.getLevel().dimension().equals(DreadlandsContent.DIMENSION) && level.getDifficulty() != Difficulty.PEACEFUL
                && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != DreadlandsContent.PLAGUE && super.canBeAffected(effect);
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !level().isClientSide && target instanceof LivingEntity living && !DreadPlagueEffect.immune(living))
            living.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE, 100));
        return hit;
    }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ZOMBIE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }
    @Override protected void playStepSound(BlockPos pos, BlockState block) { playSound(SoundEvents.ZOMBIE_STEP, .15F, 1); }
}
