package dev.abyssalport.entity;

import dev.abyssalport.content.ShoggothEcologyContent;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Native throwing physics, own glob sprite, and safely released birth passengers. */
public final class ShoggothAcidProjectile extends ThrowableItemProjectile {
    public static final int MAX_AGE=600;
    private int activeTicks;
    private boolean releasePending;
    private final Map<UUID,Vec3> lastClearPositions=new HashMap<>();
    public ShoggothAcidProjectile(EntityType<? extends ShoggothAcidProjectile> type,Level level) { super(type,level); }
    @Override protected Item getDefaultItem() { return ShoggothEcologyContent.ACID_GLOB.get(); }
    @Override protected boolean canHitEntity(Entity target) {
        return !(target instanceof ShoggothEntity)&&!(target instanceof ShoggothPart)&&super.canHitEntity(target);
    }
    @Override protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if(level() instanceof ServerLevel server&&result.getEntity() instanceof LivingEntity living)
            ShoggothAcid.damage(server,this,living,6,2,12);
        if(!level().isClientSide) finish();
    }
    @Override protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if(level() instanceof ServerLevel server) {
            if(getOwner() instanceof ShoggothEntity owner) ShoggothAcid.corrode(server,result.getBlockPos(),owner);
            // Impact always ends flight, including protected blocks and a
            // disabled griefing rule; do not leak stationary projectiles.
            finish();
        }
    }
    @Override protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        rememberClearPosition(passenger);
    }
    @Override protected void positionRider(Entity passenger,Entity.MoveFunction callback) {
        rememberClearPosition(passenger);
        super.positionRider(passenger,callback);
        rememberClearPosition(passenger);
    }
    private void rememberClearPosition(Entity passenger) {
        if(!level().isClientSide&&safeFor(passenger,passenger.position())) lastClearPositions.put(passenger.getUUID(),passenger.position());
    }
    private boolean safeFor(Entity passenger,Vec3 location) {
        if(location==null||!Double.isFinite(location.x)||!Double.isFinite(location.y)||!Double.isFinite(location.z)) return false;
        var body=passenger.getBoundingBox().move(location.subtract(passenger.position()));
        return body.minY>=level().getMinBuildHeight()&&body.maxY<level().getMaxBuildHeight()
            &&level().getWorldBorder().isWithinBounds(body)
            &&level().hasChunkAt(BlockPos.containing(body.minX,body.minY,body.minZ))
            &&level().hasChunkAt(BlockPos.containing(body.maxX,body.maxY,body.maxZ))
            &&level().hasChunkAt(BlockPos.containing(body.minX,body.minY,body.maxZ))
            &&level().hasChunkAt(BlockPos.containing(body.maxX,body.minY,body.minZ))
            &&level().noCollision(passenger,body);
    }
    private Vec3 safeLanding(Entity passenger) {
        if(safeFor(passenger,position())) return position();
        if(safeFor(passenger,passenger.position())) return passenger.position();
        var previous=lastClearPositions.get(passenger.getUUID());
        // Prefer a nearby remembered clear body over a distant birth position.
        if(previous!=null&&previous.distanceToSqr(position())<=16&&safeFor(passenger,previous)) return previous;
        for(int radius=1;radius<=3;radius++) for(int y=0;y<=3;y++)
            for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
                if(Math.max(Math.abs(x),Math.abs(z))!=radius&&y!=radius) continue;
                var candidate=position().add(x,y,z); if(safeFor(passenger,candidate)) return candidate;
            }
        return safeFor(passenger,previous)?previous:null;
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        var safe=safeLanding(passenger); return safe==null?passenger.position():safe;
    }
    public void finish() {
        if(level().isClientSide||isRemoved()) return;
        releasePending=true;
        for(var passenger:List.copyOf(getPassengers())) {
            // Vanilla's generic projectile dismount point is not collision
            // checked. Never discard a carrier while its newborn is trapped
            // or a native dismount event still vetoes the release.
            if(safeLanding(passenger)==null) continue;
            passenger.stopRiding();
            if(!passenger.isPassenger()) passenger.setDeltaMovement(random.nextGaussian()*.2,.5,random.nextGaussian()*.2);
        }
        if(!getPassengers().isEmpty()) { setDeltaMovement(Vec3.ZERO); return; }
        lastClearPositions.clear(); discard();
    }
    @Override public void tick() {
        if(!level().isClientSide&&releasePending) { finish(); return; }
        super.tick(); if(!level().isClientSide&&!isRemoved()&&++activeTicks>=MAX_AGE) finish();
        if(level().isClientSide) for(int index=0;index<4;index++) level().addParticle(new net.minecraft.core.particles.ItemParticleOption(
                net.minecraft.core.particles.ParticleTypes.ITEM,getItem()),getX()+(random.nextDouble()-.5)*.35,getY(),getZ()+(random.nextDouble()-.5)*.35,0,0,0);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("AcidTicks",activeTicks); tag.putBoolean("ReleasePending",releasePending);
        var saved=new ListTag(); lastClearPositions.forEach((id,pos)-> {
            var entry=new CompoundTag(); entry.putUUID("Passenger",id); entry.putDouble("X",pos.x); entry.putDouble("Y",pos.y); entry.putDouble("Z",pos.z); saved.add(entry);
        }); tag.put("LastClearPassengers",saved);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); activeTicks=Math.clamp(tag.getInt("AcidTicks"),0,MAX_AGE); releasePending=tag.getBoolean("ReleasePending");
        lastClearPositions.clear(); var saved=tag.getList("LastClearPassengers",Tag.TAG_COMPOUND);
        for(int index=0;index<Math.min(16,saved.size());index++) {
            var entry=saved.getCompound(index); var pos=new Vec3(entry.getDouble("X"),entry.getDouble("Y"),entry.getDouble("Z"));
            if(entry.hasUUID("Passenger")&&Double.isFinite(pos.x)&&Double.isFinite(pos.y)&&Double.isFinite(pos.z)) lastClearPositions.put(entry.getUUID("Passenger"),pos);
        }
    }
}
