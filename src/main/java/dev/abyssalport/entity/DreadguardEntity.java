package dev.abyssalport.entity;

import dev.abyssalport.content.*;
import dev.abyssalport.effect.DreadPlagueEffect;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.util.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Source-informed armoured Dread elite; modern ray casting never sprays through solid blocks. */
public final class DreadguardEntity extends Monster {
    private static final EntityDataAccessor<Integer> SPRAY_TIMER=SynchedEntityData.defineId(DreadguardEntity.class,EntityDataSerializers.INT);
    public DreadguardEntity(EntityType<? extends DreadguardEntity> type,Level level) { super(type,level); xpReward=10; }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,60).add(Attributes.ATTACK_DAMAGE,10)
                .add(Attributes.ARMOR,20).add(Attributes.ARMOR_TOUGHNESS,4).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,42);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(SPRAY_TIMER,0); }
    public int sprayTimer() { return entityData.get(SPRAY_TIMER); }
    public void setSprayTimer(int value) { entityData.set(SPRAY_TIMER,Math.clamp(value,-200,60)); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
            @Override public boolean canUse() { return sprayTimer()>0; }
            @Override public void tick() { getNavigation().stop(); }
        });
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1,true));
        goalSelector.addGoal(3,new MoveTowardsRestrictionGoal(this,1));
        goalSelector.addGoal(4,new WaterAvoidingRandomStrollGoal(this,1));
        goalSelector.addGoal(5,new RandomLookAroundGoal(this));
        goalSelector.addGoal(5,new LookAtPlayerGoal(this,Player.class,8));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public static boolean checkSpawnRules(EntityType<DreadguardEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return level.getLevel().dimension().equals(DreadlandsContent.DIMENSION) && level.getDifficulty()!=Difficulty.PEACEFUL
                && Monster.checkMonsterSpawnRules(type,level,reason,pos,random);
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return effect.getEffect()!=DreadlandsContent.PLAGUE && super.canBeAffected(effect); }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit=super.doHurtTarget(target);
        if(hit && !level().isClientSide && target instanceof LivingEntity living && !DreadPlagueEffect.immune(living))
            living.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100));
        return hit;
    }
    @Override public int getArmorValue() { return Math.min(20,super.getArmorValue()+2); }
    @Override public void aiStep() {
        super.aiStep();
        if(level().isClientSide || !isAlive()) return;
        int timer=sprayTimer(); var target=getTarget();
        if(target!=null && target.isAlive() && distanceToSqr(target)<=64 && timer<=-200) timer=60;
        if(timer>0) {
            setSprayTimer(timer);
            getNavigation().stop(); var velocity=getDeltaMovement(); setDeltaMovement(0,velocity.y,0);
            level().broadcastEntityEvent(this,(byte)23);
            if(tickCount%5==0 && timer>30) playSound(SoundEvents.ZOMBIE_VILLAGER_CURE,.7F+random.nextFloat(),.2F+random.nextFloat()*.5F);
            sprayAlongLook();
        }
        setSprayTimer(Math.max(-200,timer-1));
    }
    /** No client target ID is accepted; solid geometry clips the native nearest living hit. */
    public boolean sprayAlongLook() {
        if(level().isClientSide || !isAlive() || sprayTimer()<=0) return false;
        var start=getEyePosition(); var direction=getViewVector(1).scale(4+random.nextDouble()*8);
        var block=level().clip(new ClipContext(start,start.add(direction),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        var end=block.getType()==HitResult.Type.MISS?start.add(direction):block.getLocation();
        var ray=ProjectileUtil.getEntityHitResult(level(),this,start,end,getBoundingBox().expandTowards(direction).inflate(1),
                e->e instanceof LivingEntity && e.isAlive() && e.isPickable() && !e.isSpectator(),0);
        if(ray==null || !(ray.getEntity() instanceof LivingEntity hit)) return false;
        boolean damaged=sprayDamage(hit);
        for(var entity:level().getEntitiesOfClass(LivingEntity.class,hit.getBoundingBox().inflate(2),e->e!=this && e!=hit && e.isAlive()))
            if(random.nextInt(3)==0 && hasLineOfSight(entity)) damaged|=sprayDamage(entity);
        return damaged;
    }
    private boolean sprayDamage(LivingEntity target) {
        float amount=Math.max(0,4.5F-distanceTo(target));
        if(amount<=0 || target.isSpectator() || DreadPlagueEffect.immune(target)) return false;
        var type=level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DreadProcessingContent.DREAD_DAMAGE);
        boolean hit=target.hurt(new DamageSource(type,this),amount);
        if(hit) target.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100));
        return hit;
    }
    @Override public void handleEntityEvent(byte event) {
        if(event!=23) { super.handleEntityEvent(event); return; }
        if(!level().isClientSide) return;
        var direction=getViewVector(1);
        for(int index=0;index<12;index++) level().addParticle(new net.minecraft.core.particles.ItemParticleOption(
                net.minecraft.core.particles.ParticleTypes.ITEM,new net.minecraft.world.item.ItemStack(DreadProcessingContent.SHARD.get())),
                getX()+(random.nextDouble()-.5)*.3,getEyeY()-.12,getZ()+(random.nextDouble()-.5)*.3,
                direction.x*.6+random.nextGaussian()*.02,direction.y*.6+random.nextGaussian()*.02,direction.z*.6+random.nextGaussian()*.02);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("SprayTimer",sprayTimer()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); setSprayTimer(tag.getInt("SprayTimer")); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ZOMBIE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }
    @Override protected void playStepSound(BlockPos pos,BlockState block) { playSound(SoundEvents.ZOMBIE_STEP,.15F,.7F); }
}
