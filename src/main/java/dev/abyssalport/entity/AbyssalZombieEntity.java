package dev.abyssalport.entity;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandMobsContent;
import dev.abyssalport.effect.CoraliumPlagueEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/** Independent modern implementation of the source-informed Abyssal Zombie core. */
public final class AbyssalZombieEntity extends Monster {
    private static final EntityDataAccessor<Boolean> BABY = SynchedEntityData.defineId(AbyssalZombieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation BABY_SPEED = WastelandContent.id("abyssal_zombie_baby_speed");
    public AbyssalZombieEntity(EntityType<? extends AbyssalZombieEntity> type, Level level) { super(type, level); xpReward = 5; }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 25).add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, .23).add(Attributes.FOLLOW_RANGE, 42).add(Attributes.ARMOR, 2);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(BABY, false); }
    @Override public boolean isBaby() { return entityData.get(BABY); }
    @Override public void setBaby(boolean baby) {
        entityData.set(BABY, baby);
        if (!level().isClientSide && getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            var speed = getAttribute(Attributes.MOVEMENT_SPEED); speed.removeModifier(BABY_SPEED);
            if (baby) speed.addTransientModifier(new AttributeModifier(BABY_SPEED, .5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        refreshDimensions();
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key); if (BABY.equals(key)) refreshDimensions();
    }
    // LivingEntity already applies getAgeScale() to its dimensions; do not scale twice.
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1, true));
        goalSelector.addGoal(4, new FleeSunGoal(this, 1));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }
    @Override public void aiStep() {
        if (!level().isClientSide && isAlive() && isSunBurnTick() && getItemBySlot(EquipmentSlot.HEAD).isEmpty()) igniteForSeconds(8);
        super.aiStep();
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != WastelandContent.CORALIUM_PLAGUE && super.canBeAffected(effect);
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && !level().isClientSide && target instanceof LivingEntity living) {
            if (level().dimension().equals(WastelandContent.DIMENSION) && !CoraliumPlagueEffect.immune(living))
                living.addEffect(new MobEffectInstance(WastelandContent.CORALIUM_PLAGUE, 100));
            if (getMainHandItem().isEmpty() && isOnFire() && random.nextFloat() < level().getDifficulty().getId()*.3F)
                target.igniteForSeconds(2*level().getDifficulty().getId());
        }
        return hit;
    }
    @Override public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean normalDeath = super.killedEntity(level, victim);
        boolean difficultyAllows = level.getDifficulty() == Difficulty.HARD || level.getDifficulty() == Difficulty.NORMAL && !random.nextBoolean();
        if (!difficultyAllows || !victim.isDeadOrDying() || !(victim instanceof Zombie || victim instanceof Villager || victim instanceof Player)
                || !EventHooks.canLivingConvert(victim, WastelandMobsContent.ABYSSAL_ZOMBIE.get(), timer -> { })) return normalDeath;
        if (victim instanceof Mob mob) {
            var converted = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level);
            if (converted == null) return normalDeath;
            converted.copyPosition(victim); converted.setNoAi(mob.isNoAi());
            if (mob.hasCustomName()) { converted.setCustomName(mob.getCustomName()); converted.setCustomNameVisible(mob.isCustomNameVisible()); }
            if (mob.isPersistenceRequired()) converted.setPersistenceRequired();
            EventHooks.finalizeMobSpawn(converted, level, level.getCurrentDifficultyAt(converted.blockPosition()), MobSpawnType.CONVERSION, null);
            converted.setBaby(victim.isBaby());
            if (!level.addFreshEntity(converted)) return normalDeath;
            mob.discard(); EventHooks.onLivingConvert(victim, converted);
            level.levelEvent(null, 1026, converted.blockPosition(), 0);
            return false; // Replaced mob must not also emit its former death loot.
        }
        // Never discard a player or suppress their normal death/inventory handling.
        var converted = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level);
        if (converted != null) {
            converted.copyPosition(victim);
            EventHooks.finalizeMobSpawn(converted, level, level.getCurrentDifficultyAt(converted.blockPosition()), MobSpawnType.CONVERSION, null);
            if (level.addFreshEntity(converted)) {
                EventHooks.onLivingConvert(victim, converted); level.levelEvent(null, 1026, converted.blockPosition(), 0);
            }
        }
        return normalDeath;
    }
    private record Group(boolean baby) implements SpawnGroupData { }
    @Override @Nullable public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData group) {
        super.finalizeSpawn(level, difficulty, reason, group);
        var selected = group instanceof Group previous ? previous : new Group(random.nextFloat() < .05F);
        setBaby(selected.baby()); setCanPickUpLoot(random.nextFloat() < .55F*difficulty.getSpecialMultiplier());
        populateDefaultEquipmentSlots(random, difficulty); populateDefaultEquipmentEnchantments(level, random, difficulty);
        return selected;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("IsBaby", isBaby()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); setBaby(tag.getBoolean("IsBaby")); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ZOMBIE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }
}
