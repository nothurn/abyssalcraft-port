package dev.abyssalport.entity;

import dev.abyssalport.AbyssalPort;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Native Shadow family. Behavior derived from Shinoow's EntityShadowCreature,
 * EntityShadowMonster and EntityShadowBeast (copyright 2012-2026, LGPL-3.0).
 * Model, pixels and renderer are independently authored; no upstream media.
 */
public final class ShadowCreatureEntity extends Monster {
    public enum Kind { CREATURE, MONSTER, BEAST }
    private static final EntityDataAccessor<Boolean> BREATHING = SynchedEntityData.defineId(
            ShadowCreatureEntity.class, EntityDataSerializers.BOOLEAN);
    private final Kind kind;
    private int breathTimer;
    public ShadowCreatureEntity(EntityType<? extends ShadowCreatureEntity> type, Level level) {
        this(type, level, Kind.CREATURE);
    }
    public ShadowCreatureEntity(EntityType<? extends ShadowCreatureEntity> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        xpReward = 5;
    }
    public Kind kind() { return kind; }
    public int breathTimer() { return breathTimer; }
    public boolean isBreathing() { return entityData.get(BREATHING); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(BREATHING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(Kind.CREATURE);
    }
    public static AttributeSupplier.Builder createAttributes(Kind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, switch(kind) { case CREATURE -> 20; case MONSTER -> 40; case BEAST -> 60; })
                .add(Attributes.ATTACK_DAMAGE, switch(kind) { case CREATURE -> 6; case MONSTER -> 8; case BEAST -> 10; })
                // Preserve the existing native movement adaptation, not a raw 1.12 path-speed multiplier.
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, kind == Kind.CREATURE ? 20 : 42)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind == Kind.BEAST ? .3 : .2);
    }

    public static boolean canSpawnOnSoil(ServerLevelAccessor level, BlockPos pos) {
        return canSpawnOnSurface(level.getBlockState(pos.below()),level.getBiome(pos));
    }
    public static boolean canSpawnOnSurface(net.minecraft.world.level.block.state.BlockState surface,
            net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
        return surface.is(AbyssalPort.DARKLANDS_SOIL.get())
                || surface.is(AbyssalPort.DARKSTONE.get()) && dev.abyssalport.worldgen.DarklandsWorldgen.isDarklands(biome);
    }

    public static boolean checkShadowSpawnRules(EntityType<ShadowCreatureEntity> type,
            ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return canSpawnOnSoil(level, pos) && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(3, new FleeSunGoal(this, 1.0));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }
    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, MobSpawnType reason, net.minecraft.world.entity.SpawnGroupData group) {
        var result=super.finalizeSpawn(level,difficulty,reason,group);
        equipHolidayHead(java.time.LocalDate.now());
        return result;
    }
    /** Same real-date holiday rule as upstream and native Zombie; never replace pre-equipped headgear. */
    public void equipHolidayHead(java.time.LocalDate date) {
        if (date.getMonthValue()!=10 || date.getDayOfMonth()!=31
                || !getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty() || random.nextFloat()>=.25F) return;
        setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(random.nextFloat()<.1F
                ?net.minecraft.world.level.block.Blocks.JACK_O_LANTERN:net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN));
        setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD,0);
    }

    @Override public boolean doHurtTarget(Entity target) {
        swing(InteractionHand.MAIN_HAND);
        if (kind == Kind.BEAST) swing(InteractionHand.OFF_HAND);
        return super.doHurtTarget(target);
    }
    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SMOKE,
                    getRandomX(.6), getRandomY(), getRandomZ(.6), 0, .015, 0);
            return;
        }
        if (kind != Kind.BEAST) return;
        if (getTarget() != null && getTarget().isAlive() && distanceToSqr(getTarget()) <= 64 && breathTimer <= -300)
            breathTimer = 100;
        entityData.set(BREATHING, breathTimer > 0);
        if (isBreathing()) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(.05, 1, .05));
            level().broadcastEntityEvent(this, (byte) 23);
            if (tickCount % 5 == 0) playSound(SoundEvents.GHAST_SHOOT, .5F + random.nextFloat(), .3F + random.nextFloat() * .7F);
            applyBreath();
        }
        // Saturate the completed cooldown rather than eventually overflowing a persistent int.
        breathTimer = Math.max(-300, breathTimer - 1);
    }
    /** Loaded, block-clipped nearest living ray; the old source could hit behind its block intersection. */
    public LivingEntity breathTarget(double range) {
        if (!(level() instanceof ServerLevel server) || !Double.isFinite(range) || range <= 0) return null;
        range = Math.min(range, 12);
        Vec3 start = getEyePosition(), direction = getLookAngle().scale(range), end = start.add(direction);
        // Never let an attack load terrain. The short ray and its collision neighbourhood are bounded.
        for (int i = 0; i <= (int)Math.ceil(range); i++) {
            BlockPos point = BlockPos.containing(start.add(direction.scale(i / Math.ceil(range))));
            if (!server.hasChunkAt(point)) return null;
        }
        var block = server.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        var hit = ProjectileUtil.getEntityHitResult(server, this, start, end,
                getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator() && entity.isPickable(), 0);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
    private void applyBreath() {
        var target = breathTarget(4 + random.nextDouble() * 8);
        if (target == null) return;
        // Direct hit first, so the random splash cannot replace its 200-tick effects with 100 ticks.
        breathHit(target, true);
        for (var nearby : level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2), Entity::isAlive))
            if (nearby != target && nearby != this && random.nextInt(3) == 0) breathHit(nearby, false);
    }
    public boolean breathHit(LivingEntity target, boolean direct) {
        if (kind != Kind.BEAST || !(level() instanceof ServerLevel) || target.level() != level()
                || !target.isAlive() || target.isSpectator() || !hasLineOfSight(target)) return false;
        float amount = 4.5F - distanceTo(target);
        // Source damage turns negative at range: never submit healing/negative damage to native hooks.
        if (amount <= 0 || !target.hurt(ShadowFamily.damage(this), amount)) return false;
        int duration = direct ? 200 : 100;
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration), this);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 1), this);
        return true;
    }
    @Override public void handleEntityEvent(byte event) {
        if (event != 23) { super.handleEntityEvent(event); return; }
        if (!level().isClientSide) return;
        Vec3 direction = getLookAngle();
        for (int i=0;i<15;i++) {
            double spread = 5 + random.nextDouble() * 2.5, velocity = .5 + random.nextDouble() * .5;
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX()+random.nextDouble()-.5,
                    getEyeY()+random.nextDouble()-.5, getZ()+random.nextDouble()-.5,
                    (direction.x+random.nextGaussian()*.0075*spread)*velocity,
                    (direction.y+random.nextGaussian()*.0075*spread)*velocity,
                    (direction.z+random.nextGaussian()*.0075*spread)*velocity);
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("BreathTimer", breathTimer);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        breathTimer = kind == Kind.BEAST ? Math.clamp(tag.getInt("BreathTimer"), -300, 100) : 0;
        entityData.set(BREATHING, breathTimer > 0);
    }
}
