package dev.abyssalport.entity;

import dev.abyssalport.content.*;
import dev.abyssalport.effect.CoraliumPlagueEffect;
import dev.abyssalport.effect.DreadPlagueEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;

/** Shared native Ghoul core; variant-specific rules do not require duplicate mob trees. */
public final class GhoulEntity extends Monster {
    public enum Kind { NORMAL, SHADOW, DEPTHS, DREADED }
    private static final EntityDataAccessor<Boolean> BABY=SynchedEntityData.defineId(GhoulEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FLAVOUR=SynchedEntityData.defineId(GhoulEntity.class,EntityDataSerializers.INT);
    private static final net.minecraft.resources.ResourceLocation BABY_SPEED=WastelandContent.id("depths_ghoul_baby_speed");
    private final Kind kind;
    public GhoulEntity(EntityType<? extends GhoulEntity> type,Level level,boolean shadow) {
        this(type,level,shadow?Kind.SHADOW:Kind.NORMAL);
    }
    public GhoulEntity(EntityType<? extends GhoulEntity> type,Level level,Kind kind) {
        super(type,level); this.kind=kind; xpReward=5;
        // Monster's constructor invokes registerGoals before our material is set.
        if(kind==Kind.SHADOW||kind==Kind.DREADED) {
            goalSelector.addGoal(3,new FleeSunGoal(this,1));
        }
        if(kind==Kind.SHADOW) {
            goalSelector.addGoal(7,new LookAtPlayerGoal(this,ShadowCreatureEntity.class,8));
        } else if(kind==Kind.DEPTHS) {
            goalSelector.addGoal(7,new LookAtPlayerGoal(this,AbyssalZombieEntity.class,8));
        } else if(kind==Kind.DREADED) {
            goalSelector.addGoal(7,new LookAtPlayerGoal(this,DreadlingEntity.class,8));
            goalSelector.addGoal(7,new LookAtPlayerGoal(this,DreadSpawnEntity.class,8));
            goalSelector.addGoal(7,new LookAtPlayerGoal(this,DreadguardEntity.class,8));
        }
        if(kind==Kind.DEPTHS||kind==Kind.DREADED) {
            targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,GhoulEntity.class,10,true,false,
                    candidate->candidate instanceof GhoulEntity rival&&(rival.kind()==Kind.NORMAL
                            ||rival.kind()==(kind==Kind.DEPTHS?Kind.DREADED:Kind.DEPTHS))));
        }
    }
    public Kind kind() { return kind; }
    public boolean isShadow() { return kind==Kind.SHADOW; }
    public static AttributeSupplier.Builder createAttributes(boolean shadow) {
        return createAttributes(shadow?Kind.SHADOW:Kind.NORMAL);
    }
    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,kind==Kind.NORMAL?30:35)
                .add(Attributes.ATTACK_DAMAGE,kind==Kind.NORMAL?5:6).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,42);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(BABY,false); builder.define(FLAVOUR,0);
    }
    @Override public boolean isBaby() { return kind==Kind.DEPTHS&&entityData.get(BABY); }
    @Override public void setBaby(boolean baby) {
        baby=kind==Kind.DEPTHS&&baby; entityData.set(BABY,baby);
        if(!level().isClientSide&&getAttribute(Attributes.MOVEMENT_SPEED)!=null) {
            var speed=getAttribute(Attributes.MOVEMENT_SPEED); speed.removeModifier(BABY_SPEED);
            if(baby) speed.addTransientModifier(new AttributeModifier(BABY_SPEED,.5,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        refreshDimensions(); // Native LivingEntity applies age scaling once.
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key); if(BABY.equals(key)) refreshDimensions();
    }
    public int getGhoulType() { return entityData.get(FLAVOUR); }
    public void setGhoulType(int flavour) { entityData.set(FLAVOUR,kind==Kind.DEPTHS?Math.clamp(flavour,0,3):0); }
    @Override public Component getName() {
        if(hasCustomName()||kind!=Kind.DEPTHS||getGhoulType()==0) return super.getName();
        return Component.literal(switch(getGhoulType()) { case 1->"Pete"; case 2->"Mr. Wilson"; default->"Dr. Orange"; });
    }
    @Override protected ResourceKey<LootTable> getDefaultLootTable() {
        if(kind!=Kind.DEPTHS||getGhoulType()==0) return super.getDefaultLootTable();
        return ResourceKey.create(Registries.LOOT_TABLE,WastelandContent.id("entities/depths_ghoul_"+getGhoulType()));
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putBoolean("IsBaby",isBaby()); tag.putInt("GhoulType",getGhoulType());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); setBaby(tag.getBoolean("IsBaby")); setGhoulType(tag.getInt("GhoulType"));
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1,false));
        goalSelector.addGoal(4,new MoveTowardsRestrictionGoal(this,1));
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,1));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,GhoulEntity.class,8));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,net.minecraft.world.entity.monster.Zombie.class,8));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,net.minecraft.world.entity.monster.Skeleton.class,8));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,ShoggothEntity.class,8));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    @Override public boolean doHurtTarget(Entity target) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        boolean hit=super.doHurtTarget(target);
        if(hit&&!level().isClientSide&&target instanceof LivingEntity living) {
            if(kind==Kind.DEPTHS&&level().dimension().equals(WastelandContent.DIMENSION)&&!CoraliumPlagueEffect.immune(living))
                living.addEffect(new MobEffectInstance(WastelandContent.CORALIUM_PLAGUE,100));
            if(kind==Kind.DREADED&&!DreadPlagueEffect.immune(living)) living.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100));
            if(kind==Kind.DEPTHS&&getMainHandItem().isEmpty()&&isOnFire()&&random.nextFloat()<level().getDifficulty().getId()*.3F)
                target.igniteForSeconds(2*level().getDifficulty().getId());
        }
        return hit;
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) {
        // Shadow is not undead: only the source's explicit poison immunity.
        return !(isShadow()&&effect.getEffect()==MobEffects.POISON)
                &&!(kind==Kind.DEPTHS&&effect.getEffect()==WastelandContent.CORALIUM_PLAGUE)
                &&!(kind==Kind.DREADED&&effect.getEffect()==DreadlandsContent.PLAGUE)&&super.canBeAffected(effect);
    }
    @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group) {
        var data=super.finalizeSpawn(level,difficulty,reason,group);
        if(kind==Kind.DEPTHS) {
            var selected=group instanceof DepthsGroup previous?previous:new DepthsGroup(random.nextFloat()<.05F);
            setBaby(selected.baby()); setGhoulType(random.nextFloat()<.2F?random.nextInt(4):0); data=selected;
        }
        setCanPickUpLoot(random.nextFloat()<.55F*difficulty.getSpecialMultiplier());
        populateDefaultEquipmentSlots(random,difficulty); populateDefaultEquipmentEnchantments(level,random,difficulty);
        return data;
    }
    private record DepthsGroup(boolean baby) implements SpawnGroupData { }
    // Default source config ghouls_burn=false: do not ignite either variant.
    @Override protected SoundEvent getAmbientSound() { return isShadow()?null:SoundEvents.ZOMBIE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource damage) { return SoundEvents.ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }
}
