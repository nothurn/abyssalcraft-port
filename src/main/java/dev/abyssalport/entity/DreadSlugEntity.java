package dev.abyssalport.entity;

import dev.abyssalport.content.DreadlandsContent;
import dev.abyssalport.effect.DreadPlagueEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Native collision, owner protection, accepted-hit plague and bounded persisted lifetime. */
public final class DreadSlugEntity extends ThrowableProjectile {
    private int age;
    public DreadSlugEntity(EntityType<? extends DreadSlugEntity> type,Level level) { super(type,level); }
    public DreadSlugEntity(EntityType<? extends DreadSlugEntity> type,LivingEntity owner,Level level) { super(type,owner,level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override protected boolean canHitEntity(Entity entity) {
        return age>5&&!(entity instanceof LivingEntity living&&DreadPlagueEffect.immune(living))&&super.canHitEntity(entity);
    }
    @Override public void tick() { age++; super.tick(); if(!level().isClientSide&&age>=100) discard(); }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if(level().isClientSide||age<=5||hit.getEntity()==getOwner()||!(hit.getEntity() instanceof LivingEntity living)||DreadPlagueEffect.immune(living)) return;
        if(living.hurt(damageSources().thrown(this,getOwner()),6)) living.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100));
        discard();
    }
    @Override protected void onHitBlock(BlockHitResult hit) { super.onHitBlock(hit); if(!level().isClientSide) discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putInt("SlugAge",age); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); age=Math.clamp(tag.getInt("SlugAge"),0,100); }
}
