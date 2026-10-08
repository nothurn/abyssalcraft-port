package dev.abyssalport.block.entity;

import dev.abyssalport.block.SacrificialAltarBlock;
import dev.abyssalport.content.SacrificialAltarContent;
import dev.abyssalport.entity.ShadowFamily;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** TileEntity{Tiered}SacrificialAltar, Shinoow, copyright 2012-2026, LGPL-3.0.
 * Observe an admitted native death, not zero health or a discarded living mob.
 * Pending deaths stay bounded by the source target cap while cooling/full.
 */
public final class SacrificialAltarBlockEntity extends EnergyPedestalBlockEntity {
    private static final Set<SacrificialAltarBlockEntity> LOADED=Collections.newSetFromMap(new WeakHashMap<>());
    private static final class Target {
        final Mob mob;
        float admittedDeath;
        Target(Mob mob) { this.mob=mob; }
    }
    private final List<Target> targets=new ArrayList<>();
    private int collectionLimit;
    private int cooldown;
    public SacrificialAltarBlockEntity(BlockPos pos, BlockState state) {
        super(SacrificialAltarContent.ENTITY.get(),pos,state,((SacrificialAltarBlock)state.getBlock()).tier().pedestalCapacity());
    }
    private int tier() { return ((SacrificialAltarBlock)getBlockState().getBlock()).tier().ordinal(); }
    public int maxTargets() { return 1+tier(); }
    public int cooldownStart() { return 1200-200*tier(); }
    public int cooldown() { return cooldown; }
    public int collectionLimit() { return collectionLimit; }
    public int targetCount() { return targets.size(); }
    @Override public boolean acceptsExternalEnergy() { return false; }
    @Override public double displayItemHeight() { return .92; }
    @Override public void onLoad() { super.onLoad(); if(level instanceof ServerLevel) LOADED.add(this); }
    @Override public void setRemoved() { LOADED.remove(this);targets.clear();super.setRemoved(); }
    public static void observeAdmittedDeath(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel) || !mob.isDeadOrDying()) return;
        float energy=mob.getMaxHealth(); if(!Float.isFinite(energy) || energy<=0) return;
        for(var altar:List.copyOf(LOADED)) {
            if(altar.isRemoved() || altar.getLevel()!=mob.level()) continue;
            for(var target:altar.targets) if(target.mob==mob && target.admittedDeath==0) target.admittedDeath=energy;
        }
    }
    private static boolean eligible(Mob mob) {
        return mob.isAlive() && !mob.isBaby() && !mob.getType().is(EntityTypeTags.UNDEAD) && !ShadowFamily.isShadow(mob);
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,SacrificialAltarBlockEntity altar) {
        if(!(level instanceof ServerLevel server)) return;
        LOADED.add(altar);
        if(altar.cooldown>0) { altar.cooldown--;altar.setChanged();if(altar.cooldown==0 || altar.cooldown%20==0)altar.sync(); }
        EnergyPedestalBlockEntity.serverTick(level,pos,state,altar);
        altar.targets.removeIf(target->target.admittedDeath==0 && (target.mob.isRemoved() || !eligible(target.mob) || !server.hasChunkAt(target.mob.blockPosition())));
        if(altar.targets.size()<altar.maxTargets()) {
            for(var mob:server.getEntitiesOfClass(Mob.class,new AABB(pos).inflate(8,3,8),SacrificialAltarBlockEntity::eligible)) {
                if(altar.targets.stream().noneMatch(target->target.mob==mob))altar.targets.add(new Target(mob));
                if(altar.targets.size()==altar.maxTargets())break;
            }
        }
        var iterator=altar.targets.iterator();
        while(iterator.hasNext()) {
            var target=iterator.next();
            if(altar.energy()>=altar.capacity()) continue;
            if(target.admittedDeath>0 && altar.cooldown==0) {
                altar.addEnergy(target.admittedDeath);altar.collectionLimit+=target.admittedDeath;iterator.remove();altar.sync();
            } else if(target.admittedDeath==0 && target.mob.isAlive() && server.hasChunkAt(target.mob.blockPosition())) {
                target.mob.addEffect(new MobEffectInstance(MobEffects.GLOWING,20,0,false,false));
            }
        }
        if(altar.collectionLimit>=altar.capacity()/5F) {altar.collectionLimit=0;altar.cooldown=altar.cooldownStart();altar.sync();}
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);tag.putInt("CollectionLimit",collectionLimit);tag.putInt("CoolDown",cooldown);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);
        collectionLimit=Math.clamp(tag.getInt("CollectionLimit"),0,capacity());
        cooldown=Math.clamp(tag.getInt("CoolDown"),0,1200);targets.clear();
    }
    public void restoreItemCounters(CompoundTag tag) {
        collectionLimit=Math.clamp(tag.getInt("CollectionLimit"),0,capacity());
        cooldown=Math.clamp(tag.getInt("CoolDown"),0,1200);sync();
    }
}
