package dev.abyssalport.entity;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.content.ShoggothEcologyContent;
import dev.abyssalport.content.ShoggothLairContent;
import dev.abyssalport.content.WastelandContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.EventHooks;

/** Source-informed acid with finite, event-respecting terrain and equipment effects. */
public final class ShoggothAcid {
    public static final ResourceKey<DamageType> DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,WastelandContent.id("shoggoth_acid"));
    private static final TagKey<Item> RESISTANT=TagKey.create(Registries.ITEM,WastelandContent.id("acid_resistant_armor"));
    private ShoggothAcid() { }

    public static boolean damage(ServerLevel level,Entity directionSource,LivingEntity target,float amount,int armorWear,int shieldWear) {
        if(target instanceof ShoggothEntity||target.isSpectator()||!target.isAlive()||amount<=0) return false;
        var type=level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE);
        // Vanilla bypasses_armor is included transitively in bypasses_shield.
        // Use an ordinary blockable type ONLY for the native facing test;
        // the actual accepted hit below remains the absolute acid type.
        var facingType=level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK);
        var directional=new DamageSource(facingType,directionSource,directionSource);
        boolean shield=target.isDamageSourceBlocked(directional);
        // The original acid source is absolute and unattributed. Shield
        // direction is tested separately; a blocked spray still chips one HP.
        // Keep vetoes authoritative before applying any equipment corrosion.
        if(!target.hurt(new DamageSource(type),shield?1:amount)) return false;
        if(shield) {
            var hand=target.getUsedItemHand(); var item=target.getUseItem();
            item.hurtAndBreak(shieldWear,target,hand==InteractionHand.OFF_HAND?EquipmentSlot.OFFHAND:EquipmentSlot.MAINHAND);
        } else for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
            var item=target.getItemBySlot(slot); if(!item.isEmpty()&&!item.is(RESISTANT)) item.hurtAndBreak(armorWear,target,slot);
        }
        return true;
    }

    public static boolean corrode(ServerLevel level,BlockPos pos,ShoggothEntity owner) {
        if(owner==null||!owner.isAlive()||!level.hasChunkAt(pos)||!EventHooks.canEntityGrief(level,owner)) return false;
        var state=level.getBlockState(pos); float hardness=state.getDestroySpeed(level,pos);
        if(state.isAir()||hardness<0||hardness>=3||state.hasBlockEntity()||state.is(AbyssalPort.SHOGGOTH_OOZE.get())
            ||state.is(ShoggothLairContent.MONOLITH_STONE.get())||state.is(ShoggothLairContent.BIOMASS.get())
            ||!state.canEntityDestroy(level,pos,owner)||!EventHooks.onEntityDestroyBlock(owner,pos,state)) return false;
        return level.destroyBlock(pos,false,owner);
    }

    public static boolean splash(ShoggothEntity owner,boolean above) {
        if(!(owner.level() instanceof ServerLevel level)||!owner.isAlive()||!EventHooks.canEntityGrief(level,owner)) return false;
        var bounds=owner.getBoundingBox().inflate(1,0,1); boolean changed=false;
        int wear=owner.kind()==ShoggothEntity.Kind.LESSER?4:owner.kind()==ShoggothEntity.Kind.NORMAL?8:12;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,bounds,entity->entity!=owner&&entity.isAlive()))
            if(owner.hasLineOfSight(target)) changed|=damage(level,owner,target,Math.max(0,4.5F-owner.distanceTo(target)),wear,wear*3/2);
        int layer=above?(int)Math.floor(bounds.maxY):(int)Math.floor(bounds.minY);
        for(int x=(int)Math.floor(bounds.minX);x<=(int)Math.floor(bounds.maxX);x++)
            for(int z=(int)Math.floor(bounds.minZ);z<=(int)Math.floor(bounds.maxZ);z++)
                for(int dy=0;dy<2;dy++) changed|=corrode(level,new BlockPos(x,layer+(above?dy:-dy),z),owner);
        level.broadcastEntityEvent(owner,(byte)23); return changed;
    }
}
