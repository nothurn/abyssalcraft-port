package dev.abyssalport.energy;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.entity.DeityStatueBlockEntity;
import dev.abyssalport.block.entity.EnergyCollectorBlockEntity;
import dev.abyssalport.content.*;
import dev.abyssalport.entity.CorruptedAnimalEntity.Kind;
import dev.abyssalport.item.EnergyStorageItem;
import dev.abyssalport.item.NecronomiconItem;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.common.util.BlockSnapshot;

/** All actually registered source disruption effects; native server implementation.
 * Adapted from AbyssalCraft, Copyright (c) 2012-2026 Shinoow, LGPL-3.0.
 * Loaded/collision/admission/event guards intentionally replace unsafe reference fallbacks.
 */
public final class DisruptionEffects {
    private DisruptionEffects() { }
    public static void apply(DisruptionEvent context,RandomSource random) {
        var level=context.level();var pos=context.pos();var players=context.players();
        switch(context.effect()) {
            case LIGHTNING -> {for(var player:players)if(random.nextInt(10)==0)lightning(level,player.position(),false);}
            case FIRE -> players.forEach(player->player.igniteForSeconds(20));
            case SHOGGOTH -> spawn(level,pos,AbyssalPort.LESSER_SHOGGOTH.get(),1,random);
            case POISON -> potion(level,pos,MobEffects.POISON);
            case SLOWNESS -> potion(level,pos,MobEffects.MOVEMENT_SLOWDOWN);
            case WEAKNESS -> potion(level,pos,MobEffects.WEAKNESS);
            case WITHER -> potion(level,pos,MobEffects.WITHER);
            case CORALIUM -> potion(level,pos,WastelandContent.CORALIUM_PLAGUE);
            case POTENTIAL_ENERGY -> {for(var player:players)for(var stack:player.getInventory().items)if(energy(stack)>0){consume(stack,energy(stack)/10);player.hurt(player.damageSources().magic(),2);break;}}
            case FREEZE -> players.forEach(player->player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,14)));
            case SHADOW_SWARM -> {spawn(level,pos,AbyssalPort.SHADOW_CREATURE.get(),4,random);spawn(level,pos,ShadowFamilyContent.MONSTER.get(),4,random);spawn(level,pos,ShadowFamilyContent.BEAST.get(),4,random);}
            case FIRE_RAIN -> fireRain(level,pos,random);
            case DISPLACE -> displace(level,pos,random);
            case RANDOM_TELEPORT -> players.forEach(player->teleportRandomly(level,player,random));
            case DRAIN_PE -> drain(level,pos,players,random);
            case SHEEP_SWARM -> {spawn(level,pos,MobEcologyContent.type(Kind.SHEEP,false),4,random);spawn(level,pos,EntityType.SHEEP,4,random);}
            case ANIMAL_CORRUPTION -> corruptAnimals(level,pos,players,random);
            case OOZE -> ooze(level,pos,random);
            case RANDOM_SWARM -> randomSpawn(level,pos,4,random);
            case RANDOM_SPAWN -> randomSpawn(level,pos,1,random);
            case OFFSPRING -> spawn(level,pos,MobEcologyContent.OFFSPRING.get(),1,random);
            case INVISIBLE_HASTUR,INVISIBLE_NYARLATHOTEP -> invisible(level,pos,random);
            case SACRIFICE_JZAHAR,SACRIFICE_YOG -> {
                for(var target:level.getEntitiesOfClass(Mob.class,new AABB(pos).inflate(64),mob->mob.isAlive()&&(mob instanceof Villager||dev.abyssalport.entity.ShoggothEntity.isFood(mob))))
                    DisruptionEntities.convert(level,target,GraveyardContent.DEPTHS_GHOUL.get(),ghoul->ghoul.setBaby(target.isBaby()));
            }
            case FAMINE_AZATHOTH,FAMINE_SHUB -> players.forEach(player->player.getFoodData().setFoodLevel(0));
        }
    }
    public static void lightning(ServerLevel level,Vec3 at,boolean visualOnly) {
        var bolt=EntityType.LIGHTNING_BOLT.create(level);if(bolt==null)return;
        bolt.moveTo(at);bolt.setVisualOnly(visualOnly);
        if(!DisruptionEntities.loaded(level,bolt.getBoundingBox())||!level.addFreshEntity(bolt))bolt.discard();
    }
    private static void potion(ServerLevel level,BlockPos pos,Holder<MobEffect> effect) {
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(16),LivingEntity::isAlive))
            entity.addEffect(new MobEffectInstance(effect,600)); // Native immunity and effect events remain authoritative.
    }
    private static void spawn(ServerLevel level,BlockPos pos,EntityType<?> type,int count,RandomSource random) {
        for(int index=0;index<count;index++)DisruptionEntities.spawn(level,pos,type,random,mob->{});
    }
    private static void randomSpawn(ServerLevel level,BlockPos pos,int count,RandomSource random) {
        if(!level.hasChunkAt(pos))return;
        var entries=level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.MONSTER);
        for(int index=0;index<count;index++)for(int reroll=0;reroll<=10;reroll++) {
            var entry=entries.getRandom(random);if(entry.isEmpty())break;
            if(entry.get().type==DreadEcologyContent.BEAST.get())continue;
            spawn(level,pos,entry.get().type,1,random);break;
        }
    }
    private static void invisible(ServerLevel level,BlockPos pos,RandomSource random) {
        for(int index=0,count=random.nextInt(4)+2;index<count;index++)
            DisruptionEntities.spawn(level,pos,EntityType.ENDERMAN,random,mob->{
                mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,12000,1,false,false));
                var saved=mob.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                saved.putString("DeathLootTable",BuiltInLootTables.EMPTY.location().toString());
                mob.readAdditionalSaveData(saved);
                for(var slot:EquipmentSlot.values())mob.setDropChance(slot,0);
            });
    }
    private static void fireRain(ServerLevel level,BlockPos pos,RandomSource random) {
        for(double[] offset:new double[][]{{2.5,2.5},{-2.5,2.5},{2.5,-2.5},{-2.5,-2.5},{2.5,0},{-2.5,0},{0,2.5},{0,-2.5}})if(random.nextBoolean()) {
            var fireball=new LargeFireball(EntityType.FIREBALL,level);
            fireball.moveTo(pos.getX()+offset[0],pos.getY()+10,pos.getZ()+offset[1]);
            fireball.setDeltaMovement(0,-.1,0);fireball.accelerationPower=.1;
            if(!DisruptionEntities.loaded(level,fireball.getBoundingBox())||!level.addFreshEntity(fireball))fireball.discard();
        } // Native projectile-hit/explosion events and MOB explosion grief rules remain in place.
    }
    private static float energy(ItemStack stack) {return NecronomiconItem.isBook(stack)?NecronomiconItem.getPotentialEnergy(stack):EnergyStorageItem.storedEnergy(stack);}
    private static void consume(ItemStack stack,float amount) {if(NecronomiconItem.isBook(stack))NecronomiconItem.consumePotentialEnergy(stack,amount);else EnergyStorageItem.consumeEnergy(stack,amount);}
    private static void smoke(ServerLevel level,Vec3 pos) {level.sendParticles(ParticleTypes.LARGE_SMOKE,pos.x,pos.y,pos.z,3,.15,.2,.15,0);}
    private static void drain(ServerLevel level,BlockPos anchor,List<ServerPlayer> players,RandomSource random) {
        var statue=level.getBlockEntity(anchor) instanceof DeityStatueBlockEntity found?found:null;
        int depth=statue==null?2:statue.rangeAmplifiers(),radius=statue==null?7:statue.collectorSearchRadius(),found=0;
        for(var pos:BlockPos.betweenClosed(anchor.offset(-radius,-depth,-radius),anchor.offset(radius,0,radius)))
            if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof EnergyCollectorBlockEntity collector) {
                collector.consumeEnergy(collector.energy()/(random.nextInt(4)==0?1:statue==null?2:4));found++;smoke(level,pos.getCenter());
            }
        if(found==0)for(var player:players)for(var stack:player.getInventory().items)if(energy(stack)>0) {
            consume(stack,energy(stack)/(random.nextInt(4)==0?2:10));
            if(NecronomiconItem.isBook(stack))player.hurt(player.damageSources().magic(),2);
            smoke(level,player.position().add(0,.8,0));
        }
    }
    private static void corruptAnimals(ServerLevel level,BlockPos pos,List<ServerPlayer> players,RandomSource random) {
        for(var animal:level.getEntitiesOfClass(Animal.class,new AABB(pos).inflate(64),Animal::isAlive)) {
            Kind kind=animal instanceof Cow?Kind.COW:animal instanceof Chicken?Kind.CHICKEN:animal instanceof Pig?Kind.PIG:animal instanceof Sheep?Kind.SHEEP:null;
            if(kind!=null){DisruptionEntities.convert(level,animal,MobEcologyContent.type(kind,false),mob->{});continue;}
            if(animal instanceof AbstractHorse horse&&!horse.isInvertedHealAndHarm()) {
                DisruptionEntities.convert(level,horse,random.nextBoolean()?EntityType.ZOMBIE_HORSE:EntityType.SKELETON_HORSE,mob->((AbstractHorse)mob).setAge(horse.getAge()));
            } else if(animal instanceof Wolf wolf&&wolf.isTame()&&!players.isEmpty()) {
                var target=players.get(random.nextInt(players.size()));wolf.setTame(false,true);wolf.setOwnerUUID(null);wolf.setOrderedToSit(false);
                wolf.startPersistentAngerTimer();wolf.setPersistentAngerTarget(target.getUUID());wolf.setTarget(target);
            } else if(animal instanceof Cat cat&&cat.isTame()) {
                // Tamed cats were Ocelots before the native species split. Preserve
                // modern variant instead of inventing a nonexistent "wild" skin ID.
                cat.setTame(false,true);cat.setOwnerUUID(null);cat.setOrderedToSit(false);
            } else if(animal instanceof Rabbit rabbit&&rabbit.getVariant()!=Rabbit.Variant.EVIL)rabbit.setVariant(Rabbit.Variant.EVIL);
        }
    }
    private static void ooze(ServerLevel level,BlockPos anchor,RandomSource random) {
        int radius=random.nextInt(4)+2;
        for(var mutable:BlockPos.betweenClosed(anchor.offset(-radius,-radius,-radius),anchor.offset(radius,radius,radius))) {
            var pos=mutable.immutable();boolean central=Math.abs(pos.getX()-anchor.getX())<2&&Math.abs(pos.getY()-anchor.getY())<2&&Math.abs(pos.getZ()-anchor.getZ())<2;
            if(!(central&&random.nextBoolean()||random.nextInt(3)==0)||!level.hasChunkAt(pos)||!level.hasChunkAt(pos.below()))continue;
            var state=level.getBlockState(pos);var ooze=AbyssalPort.SHOGGOTH_OOZE.get().defaultBlockState();
            if(!state.getFluidState().isEmpty()||level.getBlockEntity(pos)!=null)continue;
            if(state.is(AbyssalPort.SHOGGOTH_OOZE.get()))ooze=state.setValue(dev.abyssalport.block.ShoggothOozeBlock.LAYERS,Math.min(8,state.getValue(dev.abyssalport.block.ShoggothOozeBlock.LAYERS)+1));
            else if(state.canBeReplaced())ooze=ooze.setValue(dev.abyssalport.block.ShoggothOozeBlock.LAYERS,1+random.nextInt(5));else continue;
            if(!ooze.canSurvive(level,pos))continue;
            protectedPlace(level,pos,ooze);
        }
    }
    private static void protectedPlace(ServerLevel level,BlockPos pos,BlockState state) {
        var snapshot=BlockSnapshot.create(level.dimension(),level,pos);
        // Native non-player EntityPlaceEvent reports snapshot.state, not currentState.
        // Its notification snapshot must describe the placed block; rollback keeps old state.
        if(level.setBlock(pos,state,3)&&NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(
                BlockSnapshot.create(level.dimension(),level,pos),level.getBlockState(pos.below()),null)).isCanceled())snapshot.restore();
    }
    public static boolean teleport(ServerLevel level,LivingEntity entity,Vec3 requested) {
        var event=new EntityTeleportEvent.EnderEntity(entity,requested.x,requested.y,requested.z);
        if(NeoForge.EVENT_BUS.post(event).isCanceled())return false;
        var at=event.getTarget();if(!finite(at)||!entity.isAlive()||entity.level()!=level||entity.isPassenger()||entity.isVehicle()
                ||at.y>=level.getMaxBuildHeight())return false;
        // Resolve ground AFTER the native event, so a redirected target receives
        // the same ground/liquid/bounds checks as the original random candidate.
        var ground=BlockPos.containing(at);if(!level.hasChunkAt(ground))return false;
        while(ground.getY()>level.getMinBuildHeight()&&!level.getBlockState(ground.below()).blocksMotion()) {
            ground=ground.below();at=at.add(0,-1,0);
        }
        if(ground.getY()<=level.getMinBuildHeight())return false;
        var box=entity.getBoundingBox().move(at.subtract(entity.position()));
        if(!DisruptionEntities.clear(level,entity,box))return false;
        var old=entity.position();move(level,entity,at);
        level.sendParticles(ParticleTypes.PORTAL,at.x,at.y+.7,at.z,32,.5,.6,.5,.05);
        level.playSound(null,old.x,old.y,old.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);
        level.playSound(null,at.x,at.y,at.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1,1);return true;
    }
    private static boolean finite(Vec3 pos) {return Double.isFinite(pos.x)&&Double.isFinite(pos.y)&&Double.isFinite(pos.z);}
    private static void move(ServerLevel level,LivingEntity entity,Vec3 at) {
        if(entity instanceof ServerPlayer player)player.teleportTo(level,at.x,at.y,at.z,entity.getYRot(),entity.getXRot());
        else entity.teleportTo(at.x,at.y,at.z);
        entity.fallDistance=0;
    }
    private static void teleportRandomly(ServerLevel level,ServerPlayer player,RandomSource random) {
        var at=player.position().add((random.nextDouble()-.5)*64,random.nextInt(64)-32,(random.nextDouble()-.5)*64);
        teleport(level,player,at);
    }
    private static void displace(ServerLevel level,BlockPos pos,RandomSource random) {
        var entities=level.getEntitiesOfClass(LivingEntity.class,new AABB(pos).inflate(16),LivingEntity::isAlive);
        for(var entity:entities) {
            var other=entities.get(random.nextInt(entities.size()));if(entity==other||entity.isPassenger()||entity.isVehicle()||other.isPassenger()||other.isVehicle())continue;
            Vec3 entityBefore=entity.position(),otherBefore=other.position();
            var first=new EntityTeleportEvent.EnderEntity(entity,other.getX(),other.getY(),other.getZ());
            var second=new EntityTeleportEvent.EnderEntity(other,entity.getX(),entity.getY(),entity.getZ());
            if(NeoForge.EVENT_BUS.post(first).isCanceled()||NeoForge.EVENT_BUS.post(second).isCanceled())continue;
            if(!entity.isAlive()||!other.isAlive()||entity.level()!=level||other.level()!=level
                    ||!entity.position().equals(entityBefore)||!other.position().equals(otherBefore))continue;
            if(!swapSpace(level,entity,other,first.getTarget())||!swapSpace(level,other,entity,second.getTarget()))continue;
            if(entity.getBoundingBox().move(first.getTarget().subtract(entityBefore)).intersects(
                    other.getBoundingBox().move(second.getTarget().subtract(otherBefore))))continue;
            // Both native vetos and destinations were checked before either position changes.
            move(level,entity,first.getTarget());move(level,other,second.getTarget());
        }
    }
    private static boolean swapSpace(ServerLevel level,LivingEntity entity,LivingEntity other,Vec3 at) {
        if(!finite(at))return false;var box=entity.getBoundingBox().move(at.subtract(entity.position()));
        if(!DisruptionEntities.loaded(level,box)||level.containsAnyLiquid(box))return false;
        for(var shape:level.getBlockCollisions(entity,box))if(!shape.isEmpty())return false;
        return level.getEntities(entity,box,candidate->candidate!=other&&candidate instanceof LivingEntity living&&living.isAlive()).isEmpty();
    }
}
