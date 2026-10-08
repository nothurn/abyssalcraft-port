package dev.abyssalport.entity;

import dev.abyssalport.config.MobEcologyConfig;
import dev.abyssalport.content.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.event.EventHooks;

/** Evil/Demon gameplay derived from Shinoow's eight animal classes (LGPL-3.0, 2012-2026). Owned rigs and pixels. */
public final class CorruptedAnimalEntity extends Monster implements IShearable {
    public enum Kind { COW, CHICKEN, PIG, SHEEP }
    private static final EntityDataAccessor<Boolean> BURN=SynchedEntityData.defineId(CorruptedAnimalEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> KILLED=SynchedEntityData.defineId(CorruptedAnimalEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> KILLED_NAME=SynchedEntityData.defineId(CorruptedAnimalEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> KILLED_TEXTURE=SynchedEntityData.defineId(CorruptedAnimalEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> KILLED_SIGNATURE=SynchedEntityData.defineId(CorruptedAnimalEntity.class,EntityDataSerializers.STRING);
    private final Kind kind;private final boolean demon;private EatBlockGoal eatGoal;private int eatTicks;
    private boolean transforming;
    public float flap,flapOld,flapAmount,flapAmountOld;private float flapSpeed=1;
    public CorruptedAnimalEntity(EntityType<? extends CorruptedAnimalEntity> type,Level level,Kind kind,boolean demon){super(type,level);this.kind=kind;this.demon=demon;xpReward=5;
        if(MobEcologyConfig.hardcore()){getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHealth(kind)*2);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);setHealth(getMaxHealth());}
        if(kind==Kind.SHEEP&&!demon){eatGoal=new EatBlockGoal(this);goalSelector.addGoal(3,eatGoal);}}
    public Kind kind(){return kind;}public boolean demon(){return demon;}public boolean canBurn(){return entityData.get(BURN);}public void setCanBurn(boolean value){entityData.set(BURN,value);}
    public int eatTicks(){return eatTicks;}
    public static int baseHealth(Kind kind){return switch(kind){case COW,PIG->15;case CHICKEN->10;case SHEEP->12;};}
    public static AttributeSupplier.Builder createAttributes(Kind kind){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,baseHealth(kind)).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.MOVEMENT_SPEED,.25);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(BURN,false);builder.define(KILLED,Optional.empty());builder.define(KILLED_NAME,"");builder.define(KILLED_TEXTURE,"");builder.define(KILLED_SIGNATURE,"");}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,.35,true));
        goalSelector.addGoal(4,new WaterAvoidingRandomStrollGoal(this,.35));goalSelector.addGoal(5,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(6,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
    public static boolean checkSpawn(EntityType<CorruptedAnimalEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
        boolean demon=type.getDescriptionId().contains("demon_");return MobEcologyConfig.weight(demon?"demon":"evil")>0
            &&(demon||!MobEcologyConfig.newMoonOnly()||!level.getLevel().isDay()&&level.getMoonBrightness()==0)
            &&Monster.checkMonsterSpawnRules(type,level,reason,pos,random);}
    @Override public Component getName(){if(!demon&&!hasCustomName())return Component.translatable("entity.minecraft."+kind.name().toLowerCase(Locale.ROOT));return super.getName();}
    public void recordKilledPlayer(Player player){if(kind==Kind.SHEEP&&!demon){entityData.set(KILLED,Optional.of(player.getUUID()));entityData.set(KILLED_NAME,bounded(player.getGameProfile().getName(),64));
        var property=player.getGameProfile().getProperties().get("textures").stream().findFirst().orElse(null);
        entityData.set(KILLED_TEXTURE,property==null?"":bounded(property.value(),16384));entityData.set(KILLED_SIGNATURE,property==null?"":bounded(property.signature(),4096));}}
    private static String bounded(String value,int maximum){return value==null?"":value.substring(0,Math.min(maximum,value.length()));}
    public Optional<UUID> killedPlayer(){return entityData.get(KILLED);}public String killedPlayerName(){return entityData.get(KILLED_NAME);}
    public com.mojang.authlib.GameProfile killedProfile(){if(killedPlayer().isEmpty()||killedPlayerName().isEmpty())return null;
        var profile=new com.mojang.authlib.GameProfile(killedPlayer().get(),killedPlayerName());var value=entityData.get(KILLED_TEXTURE);
        if(!value.isEmpty())profile.getProperties().put("textures",new com.mojang.authlib.properties.Property("textures",value,entityData.get(KILLED_SIGNATURE).isEmpty()?null:entityData.get(KILLED_SIGNATURE)));return profile;}
    public Item shearingDrop(){return switch(kind){case COW->Items.LEATHER;case CHICKEN->Items.FEATHER;case PIG->Items.PORKCHOP;case SHEEP->killedPlayer().isPresent()&&!killedPlayerName().isEmpty()?Items.ROTTEN_FLESH:Items.WHITE_WOOL;};}
    @Override public boolean isShearable(Player player,ItemStack stack,Level level,BlockPos pos){return !demon&&isAlive()&&!isRemoved()&&!transforming;}
    @Override public List<ItemStack> onSheared(Player player,ItemStack stack,Level level,BlockPos pos){
        if(level.isClientSide||!isShearable(player,stack,level,pos))return List.of();var result=new ItemStack(shearingDrop(),1+random.nextInt(3));
        if(!transform(false))return List.of();playSound(SoundEvents.SHEEP_SHEAR,1,1);playSound(SoundEvents.GHAST_HURT,1,.2F);return List.of(result);}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){var tool=player.getItemInHand(hand);
        if(!demon&&tool.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_SHEAR)) {
            if(level().isClientSide)return InteractionResult.SUCCESS;
            if(player.isSpectator()||!player.mayBuild())return InteractionResult.CONSUME;
            var drops=onSheared(player,tool,level(),blockPosition());
            if(!drops.isEmpty()){drops.forEach(drop->spawnShearedDrop(level(),blockPosition(),drop));tool.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));}
            return InteractionResult.CONSUME; // Also stop the generic item callback after a refused native conversion.
        }
        if(demon&&tool.is(Items.FLINT_AND_STEEL)&&(MobEcologyConfig.overworldFire()||MobEcologyConfig.hardcore())&&!canBurn()) {
            if(!level().isClientSide&&!player.isSpectator()&&player.mayBuild()){setCanBurn(true);playSound(SoundEvents.FLINTANDSTEEL_USE,1,.8F+random.nextFloat()*.4F);tool.hurtAndBreak(1,player,LivingEntity.getSlotForHand(hand));}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }return super.mobInteract(player,hand);}
    /** Admission-before-removal for both shearing and death. Never convert a Demon a second time. */
    public boolean transform(boolean death){if(demon||transforming||isRemoved()||!(level() instanceof ServerLevel server)||death!=isDeadOrDying())return false;
        boolean admitted=false;transforming=true;CorruptedAnimalEntity replacement=null;
        try {var type=MobEcologyContent.type(kind,true);if(!EventHooks.canLivingConvert(this,type,ignored->{}))return false;
            replacement=type.create(server);if(replacement==null)return false;
            replacement.copyPosition(this);replacement.setNoAi(isNoAi());if(hasCustomName()){replacement.setCustomName(getCustomName());replacement.setCustomNameVisible(isCustomNameVisible());}
            if(isPersistenceRequired())replacement.setPersistenceRequired();
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(replacement.getBoundingBox().getMinPosition()),
                BlockPos.containing(replacement.getBoundingBox().getMaxPosition())))if(!server.hasChunkAt(pos))return false;
            if(!server.noCollision(replacement)||server.containsAnyLiquid(replacement.getBoundingBox()))return false;
            EventHooks.finalizeMobSpawn(replacement,server,server.getCurrentDifficultyAt(blockPosition()),MobSpawnType.CONVERSION,null);
            if(replacement.isSpawnCancelled())return false;
            admitted=server.addFreshEntity(replacement)&&replacement.isAddedToLevel()&&!replacement.isRemoved();if(!admitted)return false;
            EventHooks.onLivingConvert(this,replacement);discard();return true;
        }finally{transforming=false;if(replacement!=null&&!admitted)replacement.discard();}}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit){super.dropCustomDeathLoot(level,source,recentlyHit);
        if(!demon&&MobEcologyConfig.demonOnDeath()&&!(source.getEntity() instanceof ShoggothEntity))transform(true);}
    @Override public void aiStep(){super.aiStep();
        if(kind==Kind.CHICKEN){flapOld=flap;flapAmountOld=flapAmount;flapAmount=Math.clamp(flapAmount+(onGround()?-.3F:1.2F),0,1);
            if(!onGround())flapSpeed=Math.max(1,flapSpeed);flapSpeed*=.9F;if(!onGround()&&getDeltaMovement().y<0)setDeltaMovement(getDeltaMovement().multiply(1,.6,1));flap+=flapSpeed*2;}
        if(level().isClientSide){eatTicks=Math.max(0,eatTicks-1);return;}if(eatGoal!=null)eatTicks=eatGoal.getEatAnimationTick();
        if(demon){if((MobEcologyConfig.overworldFire()||MobEcologyConfig.hardcore())&&!canBurn()&&(isInLava()
                ||level().getBlockStatesIfLoaded(getBoundingBox().deflate(1.0E-6)).anyMatch(state->state.is(net.minecraft.tags.BlockTags.FIRE))))setCanBurn(true);
            if(isInWaterRainOrBubble())setCanBurn(false);if(canBurn())DemonFire.trail(this);}}
    @Override public void igniteForTicks(int ticks){if(ticks>0&&demon&&(MobEcologyConfig.overworldFire()||MobEcologyConfig.hardcore()))setCanBurn(true);super.igniteForTicks(ticks);}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return kind!=Kind.CHICKEN&&super.causeFallDamage(distance,multiplier,source);}
    @Override public boolean canBeAffected(MobEffectInstance effect){return !(demon&&effect.getEffect()==DreadlandsContent.PLAGUE)&&super.canBeAffected(effect);}
    @Override public boolean hurt(DamageSource source,float amount){
        if(demon&&(source.is(DreadProcessingContent.DREAD_DAMAGE)||source.getEntity() instanceof LivingEntity living&&dev.abyssalport.effect.DreadPlagueEffect.immune(living)))return false;
        return super.hurt(source,amount);
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData group){var result=super.finalizeSpawn(level,difficulty,reason,group);
        if(demon&&((level.getLevel().dimension().equals(Level.OVERWORLD)&&MobEcologyConfig.overworldFire()&&random.nextInt(3)==0)
            ||level.getLevel().dimension().equals(Level.NETHER)&&random.nextBoolean()))setCanBurn(true);return result;}
    @Override public void handleEntityEvent(byte event){if(event==10)eatTicks=40;else super.handleEntityEvent(event);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("CanBurn",canBurn());
        killedPlayer().ifPresent(uuid->tag.putUUID("PlayerUUID",uuid));if(!killedPlayerName().isEmpty())tag.putString("PlayerName",killedPlayerName());
        if(!entityData.get(KILLED_TEXTURE).isEmpty()){tag.putString("PlayerTexture",entityData.get(KILLED_TEXTURE));tag.putString("PlayerTextureSignature",entityData.get(KILLED_SIGNATURE));}}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setCanBurn(demon&&tag.getBoolean("CanBurn"));
        entityData.set(KILLED,kind==Kind.SHEEP&&!demon&&tag.hasUUID("PlayerUUID")?Optional.of(tag.getUUID("PlayerUUID")):Optional.empty());
        entityData.set(KILLED_NAME,killedPlayer().isPresent()?bounded(tag.getString("PlayerName"),64):"");
        entityData.set(KILLED_TEXTURE,killedPlayer().isPresent()?bounded(tag.getString("PlayerTexture"),16384):"");entityData.set(KILLED_SIGNATURE,killedPlayer().isPresent()?bounded(tag.getString("PlayerTextureSignature"),4096):"");}
    @Override protected SoundEvent getAmbientSound(){return switch(kind){case COW->SoundEvents.COW_AMBIENT;case CHICKEN->SoundEvents.CHICKEN_AMBIENT;case PIG->SoundEvents.PIG_AMBIENT;case SHEEP->SoundEvents.SHEEP_AMBIENT;};}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.GHAST_HURT;}
    @Override protected SoundEvent getDeathSound(){return switch(kind){case COW->SoundEvents.COW_DEATH;case CHICKEN->SoundEvents.CHICKEN_DEATH;case PIG->SoundEvents.PIG_DEATH;case SHEEP->SoundEvents.SHEEP_DEATH;};}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(switch(kind){case COW->SoundEvents.COW_STEP;case CHICKEN->SoundEvents.CHICKEN_STEP;case PIG->SoundEvents.PIG_STEP;case SHEEP->SoundEvents.SHEEP_STEP;},.15F,1);}
    @Override protected float getSoundVolume(){return kind==Kind.COW?.4F:super.getSoundVolume();}
    @Override public float getVoicePitch(){return demon?.2F:super.getVoicePitch();}
}
