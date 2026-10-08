package dev.abyssalport.entity;

import dev.abyssalport.content.DreadlandsContent;
import dev.abyssalport.content.DreadEcologyContent;
import dev.abyssalport.effect.DreadPlagueEffect;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Independent native implementation of the original three-form population cycle. */
public final class DreadSpawnEntity extends Monster implements RangedAttackMob {
    public enum Kind { SMALL, GREATER, BEAST }
    private static final EntityDataAccessor<Boolean> CLIMBING=SynchedEntityData.defineId(DreadSpawnEntity.class,EntityDataSerializers.BOOLEAN);
    private final Kind kind;
    private int smallTimer,greaterTimer=10000;
    private boolean mergeReserved,deathChildrenEmitted;
    public DreadSpawnEntity(EntityType<? extends DreadSpawnEntity> type,Level level,Kind kind) {
        super(type,level); this.kind=kind; smallTimer=kind==Kind.BEAST?400:2000; xpReward=kind==Kind.BEAST?10:5;
        // Mob's constructor invokes registerGoals before subtype fields are assigned.
        installCombatGoals();
    }
    public Kind kind() { return kind; }
    public static AttributeSupplier.Builder attributes(Kind kind) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,kind==Kind.SMALL?20:kind==Kind.GREATER?50:100)
                .add(Attributes.ATTACK_DAMAGE,kind==Kind.SMALL?6:kind==Kind.GREATER?12:18)
                .add(Attributes.MOVEMENT_SPEED,kind==Kind.SMALL?.3:kind==Kind.GREATER?.4:.35)
                .add(Attributes.FOLLOW_RANGE,kind==Kind.SMALL?16:42).add(Attributes.KNOCKBACK_RESISTANCE,kind==Kind.SMALL?0:kind==Kind.GREATER?.2:.4);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(CLIMBING,false); }
    @Override protected PathNavigation createNavigation(Level level) { return new WallClimberNavigation(this,level); }
    @Override public boolean onClimbable() { return entityData.get(CLIMBING); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(3,new MoveTowardsRestrictionGoal(this,.35));
        goalSelector.addGoal(4,new WaterAvoidingRandomStrollGoal(this,.35));
        goalSelector.addGoal(5,new RandomLookAroundGoal(this));
        goalSelector.addGoal(5,new LookAtPlayerGoal(this,Player.class,8));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    private boolean useRanged(LivingEntity target) {
        return kind!=Kind.SMALL && target!=null && (distanceToSqr(target)>15 || target instanceof FlyingMob || target.getY()>getY()+4);
    }
    private void installCombatGoals() {
        goalSelector.addGoal(2,new MeleeAttackGoal(this,.35,true) {
            @Override public boolean canUse() { return !useRanged(getTarget())&&super.canUse(); }
            @Override public boolean canContinueToUse() { return !useRanged(getTarget())&&super.canContinueToUse(); }
        });
        if(kind!=Kind.SMALL) goalSelector.addGoal(2,new RangedAttackGoal(this,.4,20,kind==Kind.GREATER?8:15) {
            @Override public boolean canUse() { return useRanged(getTarget())&&super.canUse(); }
            @Override public boolean canContinueToUse() { return useRanged(getTarget())&&super.canContinueToUse(); }
        });
    }
    public static boolean checkSpawnRules(EntityType<DreadSpawnEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return level.getLevel().dimension().equals(DreadlandsContent.DIMENSION)&&level.getDifficulty()!=Difficulty.PEACEFUL
                &&level.getLevel().getEntitiesOfClass(DreadSpawnEntity.class,new AABB(pos).inflate(32),mob->mob.getType()==type&&mob.isAlive()).size()<4
                &&Monster.checkMonsterSpawnRules(type,level,reason,pos,random);
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return effect.getEffect()!=DreadlandsContent.PLAGUE&&super.canBeAffected(effect); }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit=super.doHurtTarget(target);
        if(hit&&!level().isClientSide&&target instanceof LivingEntity living&&!DreadPlagueEffect.immune(living))
            living.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100));
        return hit;
    }
    @Override public void performRangedAttack(LivingEntity target,float distance) {
        if(kind==Kind.SMALL||level().isClientSide||!isAlive()||!target.isAlive()||DreadPlagueEffect.immune(target)||!hasLineOfSight(target)) return;
        var slug=new DreadSlugEntity(DreadEcologyContent.SLUG.get(),this,level());
        double dx=target.getX()-getX(),dz=target.getZ()-getZ();
        double dy=target.getEyeY()-1.1-slug.getY()+Math.sqrt(dx*dx+dz*dz)*.2;
        slug.shoot(dx,dy,dz,1.6F,12);
        if(level().addFreshEntity(slug)) playSound(SoundEvents.SKELETON_SHOOT,1,1/(random.nextFloat()*.4F+.8F));
    }
    @Override public void aiStep() {
        super.aiStep();
        if(level().isClientSide||!isAlive()) return;
        entityData.set(CLIMBING,horizontalCollision);
        if(isNoAi()) return;
        if(tickCount%20==0 && tryMerge()) return;
        if(kind!=Kind.SMALL && --smallTimer<=0) { spawnChild(Kind.SMALL,20); smallTimer=kind==Kind.BEAST?400:2000; }
        if(kind==Kind.BEAST && --greaterTimer<=0) { spawnChild(Kind.GREATER,10); greaterTimer=10000; }
    }
    private List<DreadSpawnEntity> population(Kind match,double radius) {
        return level().getEntitiesOfClass(DreadSpawnEntity.class,getBoundingBox().inflate(radius),mob->mob.kind==match&&mob.isAlive()&&!mob.isRemoved());
    }
    /** Exactly five reserved native entities disappear only after the replacement is admitted. */
    public boolean tryMerge() {
        if(!(level() instanceof ServerLevel server)||kind==Kind.BEAST||mergeReserved||!isAlive()||!isAddedToLevel()) return false;
        Kind next=kind==Kind.SMALL?Kind.GREATER:Kind.BEAST;
        if(population(next,32).size()>=(kind==Kind.SMALL?10:4)) return false;
        var nearby=population(kind,kind==Kind.SMALL?2:5).stream().filter(mob->!mob.mergeReserved)
                .sorted(Comparator.comparingInt(Entity::getId)).toList();
        if(nearby.size()<5||nearby.getFirst()!=this) return false;
        var members=nearby.subList(0,5); var replacement=DreadEcologyContent.type(next).create(server);
        if(replacement==null) return false;
        replacement.moveTo(getX(),getY(),getZ(),getYRot(),getXRot()); replacement.setPersistenceRequired();
        members.forEach(mob->mob.mergeReserved=true);
        try {
            if(!server.addFreshEntity(replacement)||!replacement.isAddedToLevel()) return false;
            if(members.stream().anyMatch(mob->!mob.isAlive()||!mob.isAddedToLevel())) { replacement.discard(); return false; }
            members.forEach(Entity::discard); return true;
        } finally { members.forEach(mob->mob.mergeReserved=false); }
    }
    private boolean spawnChild(Kind child,int cap) {
        if(!(level() instanceof ServerLevel server)||population(child,32).size()>=cap) return false;
        var spawn=DreadEcologyContent.type(child).create(server); if(spawn==null) return false;
        spawn.moveTo(getX(),getY(),getZ(),getYRot(),getXRot());
        return server.addFreshEntity(spawn);
    }
    @Override protected void dropAllDeathLoot(ServerLevel server,DamageSource source) {
        super.dropAllDeathLoot(server,source);
        // Called only after native LivingDeathEvent acceptance, unlike the old pre-super onDeath.
        if(kind!=Kind.SMALL&&!deathChildrenEmitted) {
            deathChildrenEmitted=true;
            for(int count=0;count<2;count++) spawnChild(kind==Kind.GREATER?Kind.SMALL:Kind.GREATER,Integer.MAX_VALUE);
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("SmallSpawnTimer",smallTimer); tag.putInt("GreaterSpawnTimer",greaterTimer); tag.putBoolean("DeathChildrenEmitted",deathChildrenEmitted);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("SmallSpawnTimer")) smallTimer=Math.clamp(tag.getInt("SmallSpawnTimer"),1,kind==Kind.BEAST?400:2000);
        if(tag.contains("GreaterSpawnTimer")) greaterTimer=Math.clamp(tag.getInt("GreaterSpawnTimer"),1,10000);
        deathChildrenEmitted=tag.getBoolean("DeathChildrenEmitted");
    }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SPIDER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SPIDER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SPIDER_DEATH; }
    @Override protected void playStepSound(BlockPos pos,BlockState block) { playSound(SoundEvents.SPIDER_STEP,.15F,kind==Kind.SMALL?1.2F:kind==Kind.GREATER?.8F:.5F); }
}
