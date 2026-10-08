package dev.abyssalport.entity;

import dev.abyssalport.config.MobEcologyConfig;
import dev.abyssalport.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;

/** Gameplay adapted from Shinoow's EntityShubOffspring (2012-2026, LGPL-3.0); no original media. */
public final class ShubOffspringEntity extends Monster {
    public ShubOffspringEntity(EntityType<? extends ShubOffspringEntity> type,Level level) {super(type,level);xpReward=5;
        if(MobEcologyConfig.hardcore()){getAttribute(Attributes.MAX_HEALTH).setBaseValue(80);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);setHealth(80);}}
    public static AttributeSupplier.Builder createAttributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.25);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(3,new MeleeAttackGoal(this,.35,true));
        goalSelector.addGoal(4,new MoveTowardsRestrictionGoal(this,.35));goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.35));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    public static boolean moonAllows(Level level,BlockPos pos,RandomSource random){return (!level.isDay()&&pos.getY()>=level.getSeaLevel()
        &&(level.getMoonBrightness()==0||random.nextFloat()+.01F>level.getMoonBrightness()))
        ||((level.dimension().equals(WastelandContent.DIMENSION)||level.dimension().equals(DreadlandsContent.DIMENSION))&&pos.getY()>=50);}
    public static boolean checkSpawn(EntityType<ShubOffspringEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
        return MobEcologyConfig.weight("offspring")>0&&moonAllows(level.getLevel(),pos,random)&&Monster.checkMonsterSpawnRules(type,level,reason,pos,random);}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.SHEEP_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.SHEEP_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.SHEEP_DEATH;}
    @Override protected void playStepSound(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){playSound(SoundEvents.SHEEP_STEP,.15F,1);}
    @Override protected float getSoundVolume(){return 3;}
    @Override public float getVoicePitch(){return (random.nextFloat()-random.nextFloat())*.2F+(random.nextBoolean()?1.5F:.5F);}
}
