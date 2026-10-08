package dev.abyssalport.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;

/** Native unsaved hit surfaces; health, loot and effects belong to their parent. */
public final class ShoggothPart extends PartEntity<ShoggothEntity> {
    public enum Surface { BODY,HEAD }
    private final Surface surface;
    private final EntityDimensions size;
    ShoggothPart(ShoggothEntity parent,Surface surface,float width,float height) {
        super(parent); this.surface=surface; size=EntityDimensions.scalable(width,height); refreshDimensions();
    }
    public Surface surface() { return surface; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public EntityDimensions getDimensions(Pose pose) { return size; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return getParent().isAlive()&&!getParent().isRemoved(); }
    @Override public boolean is(Entity entity) { return entity==this||entity==getParent(); }
    @Override public ItemStack getPickResult() { return getParent().getPickResult(); }
    @Override public boolean hurt(DamageSource source,float amount) {
        return isPickable()&&!isInvulnerableTo(source)&&getParent().hurt(source,amount);
    }
}
