package dev.abyssalport.entity;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.ShoggothOozeBlock;
import dev.abyssalport.entity.ai.BuildMonolithGoal;
import dev.abyssalport.entity.ai.AssistMonolithGoal;
import dev.abyssalport.content.ShoggothEcologyContent;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.DreadlandsContent;
import dev.abyssalport.effect.CoraliumPlagueEffect;
import dev.abyssalport.effect.DreadPlagueEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

/** Source-informed three-form feeding cycle and native hit surfaces on own geometry/pixels. */
public final class ShoggothEntity extends Monster {
    public enum Kind {
        LESSER(.9F,1.3F,25,6),NORMAL(1.2F,1.8F,50,8),GREATER(1.8F,2.6F,100,10);
        public final float width,height; public final double health,attack;
        Kind(float width,float height,double health,double attack) { this.width=width; this.height=height; this.health=health; this.attack=attack; }
    }
    public enum Habitat { OVERWORLD,ABYSSAL,DREAD }
    private static final EntityDataAccessor<Integer> FOOD=SynchedEntityData.defineId(ShoggothEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CLIMBING=SynchedEntityData.defineId(ShoggothEntity.class,EntityDataSerializers.BOOLEAN);
    public static final int MONOLITH_COOLDOWN_TICKS = 1_500;
    private final Kind kind;
    private final Habitat habitat;
    private final ShoggothPart[] parts;
    private int monolithTimer;
    private int acidCooldown;
    private boolean assisting;
    private boolean building;

    public ShoggothEntity(EntityType<? extends ShoggothEntity> type, Level level) {
        this(type,level,Kind.LESSER,Habitat.OVERWORLD);
    }
    public ShoggothEntity(EntityType<? extends ShoggothEntity> type,Level level,Kind kind,Habitat habitat) {
        super(type, level);
        this.kind=kind; this.habitat=habitat;
        parts=new ShoggothPart[]{new ShoggothPart(this,ShoggothPart.Surface.BODY,kind.width,
                kind==Kind.LESSER?1.1F:kind==Kind.NORMAL?1.4F:2.2F),
            new ShoggothPart(this,ShoggothPart.Surface.HEAD,kind==Kind.LESSER?.8F:kind==Kind.NORMAL?1.1F:1.6F,kind==Kind.GREATER?1.2F:.6F)};
        // Client spawn packets carry only the parent ID. Consecutive reserved
        // IDs follow NeoForge's native multipart tracking on both sides.
        setId(ENTITY_COUNTER.getAndAdd(parts.length+1)+1); updateParts();
        xpReward = 10;
    }
    public Kind kind() { return kind; }
    public Habitat habitat() { return habitat; }
    @Override public boolean isMultipartEntity() { return parts!=null; }
    @Override public ShoggothPart[] getParts() { return parts; }
    @Override public void setId(int id) {
        super.setId(id); if(parts!=null) for(int index=0;index<parts.length;index++) parts[index].setId(id+index+1);
    }
    @Override public void setPos(double x,double y,double z) { super.setPos(x,y,z); updateParts(); }
    @Override public void setYRot(float yaw) { super.setYRot(yaw); updateParts(); }
    private void updateParts() {
        if(parts==null) return;
        double radians=Math.toRadians(getYRot()),forwardX=-Math.sin(radians),forwardZ=Math.cos(radians);
        double bodyOffset=kind==Kind.LESSER?.35:kind==Kind.NORMAL?.5:.7;
        double headOffset=kind==Kind.LESSER?.25:.5,headHeight=kind==Kind.LESSER?.75:kind==Kind.NORMAL?1.2:1.5;
        parts[0].setOldPosAndRot(); parts[1].setOldPosAndRot();
        parts[0].setPos(getX()-forwardX*bodyOffset,getY(),getZ()-forwardZ*bodyOffset);
        parts[1].setPos(getX()+forwardX*headOffset,getY()+headHeight,getZ()+forwardZ*headOffset);
    }
    @Override public void tick() { super.tick(); updateParts(); }
    public int foodLevel() { return entityData.get(FOOD); }
    private void foodLevel(int food) { entityData.set(FOOD,Math.clamp(food,0,1_000_000)); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(FOOD,0); builder.define(CLIMBING,false); }
    @Override protected PathNavigation createNavigation(Level level) { return new WallClimberNavigation(this,level); }
    @Override public boolean onClimbable() { return entityData.get(CLIMBING); }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source) { return false; }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(Kind.LESSER);
    }
    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.health)
                .add(Attributes.ATTACK_DAMAGE, kind.attack)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(2, new AssistMonolithGoal(this));
        goalSelector.addGoal(3, new FleeSunGoal(this, 1.0));
        goalSelector.addGoal(4, new BuildMonolithGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,20,true,false,ShoggothEntity::isFood));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Zombie.class,10,true,false,target->!(target instanceof ZombieVillager)));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Skeleton.class,true));
    }
    public static boolean isFood(LivingEntity victim) {
        return victim instanceof Animal||victim instanceof AmbientCreature||victim instanceof WaterAnimal||victim instanceof Spider
                ||victim.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,WastelandContent.id("shoggoth_food")));
    }
    @Override public boolean killedEntity(ServerLevel level,LivingEntity victim) {
        boolean normalDeath=super.killedEntity(level,victim);
        if(victim.isDeadOrDying()&&isFood(victim)) {
            foodLevel(foodLevel()+(int)Math.ceil(victim.getBbHeight()*victim.getBbWidth()/.25F));
            playSound(SoundEvents.SLIME_SQUISH,1,.7F);
        }
        return normalDeath;
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit=super.doHurtTarget(target);
        if(hit&&!level().isClientSide&&target instanceof LivingEntity living) {
            if(habitat==Habitat.ABYSSAL&&!CoraliumPlagueEffect.immune(living)) living.addEffect(new MobEffectInstance(WastelandContent.CORALIUM_PLAGUE,100));
            if(habitat==Habitat.DREAD&&!DreadPlagueEffect.immune(living)) living.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100));
        }
        return hit;
    }
    @Override public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect()!=MobEffects.POISON&&!(habitat==Habitat.ABYSSAL&&effect.getEffect()==WastelandContent.CORALIUM_PLAGUE)
                &&!(habitat==Habitat.DREAD&&effect.getEffect()==DreadlandsContent.PLAGUE)&&super.canBeAffected(effect);
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(source.is(DamageTypes.IN_WALL)) {
            if(!(level() instanceof ServerLevel server)||server.getBlockState(blockPosition()).is(dev.abyssalport.content.ShoggothLairContent.MONOLITH_STONE.get())
                ||!EventHooks.canEntityGrief(server,this)) return false;
            ShoggothAcid.splash(this,true);
        }
        return !source.is(DamageTypeTags.IS_PROJECTILE)&&!source.is(DamageTypes.CACTUS)&&super.hurt(source,amount);
    }

    /** Consume eight points only after finalized, collision-safe native admission. */
    public boolean tryFoodAction() {
        if(!(level() instanceof ServerLevel server)||!isAlive()||!isAddedToLevel()||foodLevel()<8) return false;
        var child=ShoggothEcologyContent.type(kind==Kind.NORMAL?Kind.LESSER:Kind.NORMAL,habitat).create(server);
        if(child==null) return false;
        child.copyPosition(this); child.setNoAi(isNoAi()); child.setNoGravity(isNoGravity());
        if(kind==Kind.LESSER) {
            child.foodLevel(foodLevel()-8); child.monolithTimer=monolithTimer;
            if(hasCustomName()) { child.setCustomName(getCustomName()); child.setCustomNameVisible(isCustomNameVisible()); }
            if(isPersistenceRequired()) child.setPersistenceRequired();
        }
        if(!server.noCollision(child)||server.containsAnyLiquid(child.getBoundingBox())&&!child.canBreatheUnderwater()) { child.discard(); return false; }
        EventHooks.finalizeMobSpawn(child,server,server.getCurrentDifficultyAt(child.blockPosition()),kind==Kind.LESSER?MobSpawnType.CONVERSION:MobSpawnType.BREEDING,null);
        if(child.isSpawnCancelled()||child.isRemoved()||!server.noCollision(child)
                ||!server.addFreshEntity(child)||child.isRemoved()||!child.isAddedToLevel()) { child.discard(); return false; }
        foodLevel(foodLevel()-8);
        playSound(SoundEvents.SLIME_SQUISH,1,.9F);
        if(kind==Kind.LESSER) discard();
        else if(getTarget()!=null&&getTarget().isAlive()&&random.nextInt(3)==0) {
            var projectile=shootAcidAt(getTarget(),.8F,8);
            if(projectile!=null&&!child.startRiding(projectile,true)) projectile.finish();
        }
        return true;
    }

    public int acidCooldown() { return acidCooldown; }
    public ShoggothAcidProjectile shootAcidAt(LivingEntity target,float speed,float spread) {
        if(!(level() instanceof ServerLevel server)||!isAlive()||!target.isAlive()||target.level()!=level()||target instanceof ShoggothEntity) return null;
        var projectile=ShoggothEcologyContent.ACID_PROJECTILE.get().create(server); if(projectile==null) return null;
        projectile.setOwner(this); projectile.moveTo(getX(),getEyeY()-.1,getZ(),getYRot(),getXRot());
        double dx=target.getX()-getX(),dz=target.getZ()-getZ(),dy=target.getEyeY()-1.1-projectile.getY();
        projectile.shoot(dx,dy+Math.sqrt(dx*dx+dz*dz)*.2,dz,speed,spread);
        if(!server.addFreshEntity(projectile)||projectile.isRemoved()||!projectile.isAddedToLevel()) { projectile.discard(); return null; }
        playSound(SoundEvents.SLIME_SQUISH,1,1.1F); return projectile;
    }
    @Override public void handleEntityEvent(byte event) {
        if(event!=23) { super.handleEntityEvent(event); return; }
        if(level().isClientSide) for(int index=0;index<12;index++) level().addParticle(new net.minecraft.core.particles.ItemParticleOption(
            net.minecraft.core.particles.ParticleTypes.ITEM,new net.minecraft.world.item.ItemStack(ShoggothEcologyContent.ACID_GLOB.get())),
            getX()+(random.nextDouble()-.5)*getBbWidth(),getY()+random.nextDouble()*getBbHeight(),getZ()+(random.nextDouble()-.5)*getBbWidth(),0,.04,0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel serverLevel)||!isAlive()) {
            return;
        }
        entityData.set(CLIMBING,horizontalCollision);
        if(tickCount%20==0&&tryFoodAction()&&isRemoved()) return;
        if(acidCooldown>0) acidCooldown--;
        var target=getTarget();
        if(!isNoAi()&&target!=null&&target.isAlive()&&!target.isSpectator()) {
            double distance=distanceToSqr(target);
            if(kind!=Kind.LESSER&&acidCooldown==0&&distance>32&&distance<=24*24&&shootAcidAt(target,1,12)!=null) acidCooldown=120;
            double reach=getBbWidth()*1.5*getBbWidth()*1.5+target.getBbWidth();
            if(tickCount%20==0&&distance>reach&&getDeltaMovement().horizontalDistanceSqr()<.000001) ShoggothAcid.splash(this,target.getY()>=getY());
        }
        if (monolithTimer < MONOLITH_COOLDOWN_TICKS) {
            monolithTimer++;
        }
        if (tickCount % 10 == 0 && serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            spreadOoze(serverLevel);
            if(kind!=Kind.LESSER) {
                spreadOozeAt(serverLevel,blockPosition().offset(-1,0,0));
                spreadOozeAt(serverLevel,blockPosition().offset(0,0,-1));
                spreadOozeAt(serverLevel,blockPosition().offset(-1,0,-1));
            }
        }
    }

    private void spreadOoze(ServerLevel level) {
        spreadOozeAt(level,blockPosition());
    }
    private void spreadOozeAt(ServerLevel level,BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(AbyssalPort.SHOGGOTH_OOZE.get())) {
            int layers = state.getValue(ShoggothOozeBlock.LAYERS);
            if (layers < 8 && random.nextInt(5) == 0) {
                BlockState next=state.setValue(ShoggothOozeBlock.LAYERS,layers+1);
                // Stacking residue must not embed its maker (or another entity)
                // in the new solid layer. Use native snow-like displacement,
                // but only when the raised body still has real headroom.
                double top=pos.getY()+layers*.125;
                AABB added=new AABB(pos.getX(),top-.125,pos.getZ(),pos.getX()+1,top,pos.getZ()+1);
                for(var entity:level.getEntities(null,added)) {
                    double rise=Math.max(0,top-entity.getBoundingBox().minY);
                    if(!level.noCollision(entity,entity.getBoundingBox().move(0,rise,0))) return;
                }
                level.setBlock(pos,Block.pushEntitiesUp(state,next,level,pos),3);
            }
        } else if (state.canBeReplaced() && state.getFluidState().isEmpty()) {
            BlockState ooze = AbyssalPort.SHOGGOTH_OOZE.get().defaultBlockState();
            if (ooze.canSurvive(level, pos)) {
                level.setBlock(pos, ooze, 3);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MonolithTimer", monolithTimer);
        tag.putInt("FoodLevel",foodLevel());
        tag.putInt("AcidCooldown",acidCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        monolithTimer = Math.clamp(tag.getInt("MonolithTimer"), 0, MONOLITH_COOLDOWN_TICKS);
        foodLevel(tag.getInt("FoodLevel"));
        acidCooldown=Math.clamp(tag.getInt("AcidCooldown"),0,120);
    }

    public int monolithTimer() {
        return monolithTimer;
    }

    public boolean isAssisting() {
        return assisting;
    }

    public boolean isBuilding() {
        return building;
    }

    public void setAssisting(boolean assisting) {
        this.assisting = assisting;
    }

    public void setBuilding(boolean building) {
        this.building = building;
    }

    public void finishMonolith() {
        monolithTimer = 0;
    }

}
