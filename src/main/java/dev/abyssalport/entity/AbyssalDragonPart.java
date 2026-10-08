package dev.abyssalport.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;

/** Native multipart hitbox; only the parent is saved and independently tracked. */
public final class AbyssalDragonPart extends PartEntity<AbyssalDragonEntity> {
    private final EntityDimensions dimensions;
    public final String name;
    public AbyssalDragonPart(AbyssalDragonEntity parent, String name, float width, float height) {
        super(parent);
        this.name = name;
        dimensions = EntityDimensions.scalable(width, height);
        refreshDimensions();
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public EntityDimensions getDimensions(Pose pose) { return dimensions; }
    @Override public boolean isPickable() { return getParent().isAlive(); }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean is(Entity entity) { return entity == this || entity == getParent(); }
    @Override public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurtPart(this, source, amount);
    }
}
