package dev.abyssalport.entity;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Source-informed Asorah/Spectral Dragon core, with bounded flight around the actual summon site. */
public final class AbyssalDragonEntity extends FlyingMob implements Enemy {
    private final boolean boss;
    private final AbyssalDragonPart[] parts;
    private final ServerBossEvent bossEvent;
    private BlockPos home;
    private int flightTicks;
    private Vec3 destination;
    private int hitCooldown;
    private @Nullable java.util.UUID healer;
    private @Nullable DamageSource pendingDeathSource;
    private @Nullable java.util.UUID deathAttacker;
    private ResourceLocation deathDamageType;
    private boolean deathRewardPending;
    private boolean deathRewardPaid;
    private boolean deathByPlayer;

    public AbyssalDragonEntity(EntityType<? extends AbyssalDragonEntity> type, Level level, boolean boss) {
        super(type, level);
        this.boss = boss;
        float scale = boss ? 1 : 1F/3;
        parts = new AbyssalDragonPart[]{new AbyssalDragonPart(this, "head", 4.5F*scale, 4.5F*scale),
            new AbyssalDragonPart(this, "body", 7.5F*scale, 4.5F*scale),
            new AbyssalDragonPart(this, "tail", 3*scale, 3*scale),
            new AbyssalDragonPart(this, "left_wing", 6*scale, 3*scale),
            new AbyssalDragonPart(this, "right_wing", 6*scale, 3*scale)};
        // Reserve consecutive part IDs, the same contract as NeoForge's patched native dragon.
        setId(ENTITY_COUNTER.getAndAdd(parts.length+1)+1);
        bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        bossEvent.setDarkenScreen(boss);
        setNoGravity(true); xpReward = boss ? 5000 : 15;
        if (boss) setPersistenceRequired();
    }
    public boolean isBoss() { return boss; }
    public void setHome(BlockPos pos) { home = pos.immutable(); }
    public BlockPos home() { return home; }
    public @Nullable java.util.UUID healer() { return healer; }
    public ServerBossEvent bossEvent() { return bossEvent; }
    public static AttributeSupplier.Builder attributes(boolean boss) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, boss ? 400 : 30)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.FOLLOW_RANGE, 96)
                .add(Attributes.ATTACK_DAMAGE, 10).add(Attributes.MOVEMENT_SPEED, .3);
    }
    @Override public boolean isMultipartEntity() { return true; }
    @Override public AbyssalDragonPart[] getParts() { return parts; }
    @Override public void setId(int id) {
        super.setId(id);
        if (parts != null) for (int i = 0; i < parts.length; i++) parts[i].setId(id+i+1);
    }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(boss ? 8 : 3); }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return !boss; }
    @Override protected boolean shouldDespawnInPeaceful() { return true; }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); if (boss) bossEvent.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); if (boss) bossEvent.removePlayer(player); }
    @Override public void setCustomName(@Nullable Component name) {
        super.setCustomName(name); if (bossEvent != null) bossEvent.setName(getDisplayName());
    }
    @Override public void tick() {
        super.tick();
        positionParts();
        if (!level().isClientSide && boss) {
            bossEvent.setProgress(Math.clamp(getHealth()/getMaxHealth(), 0, 1));
            bossEvent.setColor(getHealth() < getMaxHealth()/2 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.BLUE);
            bossEvent.setVisible(isAlive());
        }
    }
    @Override protected void customServerAiStep() {
        if (!(level() instanceof ServerLevel server) || !isAlive()) return;
        if (home == null) home = blockPosition();
        if (hitCooldown > 0) hitCooldown--;
        if (flightTicks < Integer.MAX_VALUE) flightTicks++;
        if (boss && flightTicks % 10 == 0) healFromSpectralDragon(server);
        var target = getTarget();
        if (!(target instanceof Player player) || !player.isAlive() || player.isCreative() || player.isSpectator()
                || player.level() != level() || distanceToSqr(player) > 96*96) {
            target = server.getNearestPlayer(getX(), getY(), getZ(), 96, entity -> entity instanceof Player candidate
                    && candidate.isAlive() && !candidate.isCreative() && !candidate.isSpectator());
            setTarget(target);
        }
        // Long approaches and bounded sweep/retreat phases give the player repeated melee opportunities.
        int phase = flightTicks % 240;
        if (target != null && phase >= 120 && phase < 180) destination = target.position().add(0, .5, 0);
        else if (destination == null || distanceToSqr(destination) < 9 || flightTicks % 40 == 0 || horizontalCollision) {
            double angle = flightTicks*.025 + getId()*.5;
            double radius = target == null ? 18 : 14;
            var center = target == null ? home.getCenter() : target.position();
            destination = center.add(Math.cos(angle)*radius, 8+Math.sin(angle*.7)*3, Math.sin(angle)*radius);
            double terrain = server.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(destination.x), Mth.floor(destination.z))+4;
            destination = new Vec3(destination.x, Math.min(server.getMaxBuildHeight()-10, Math.max(destination.y, terrain)), destination.z);
        }
        Vec3 delta = destination.subtract(position());
        double speed = boss ? .55 : .38;
        Vec3 desired = delta.lengthSqr() > .01 ? delta.normalize().scale(speed) : Vec3.ZERO;
        if (horizontalCollision || verticalCollision) desired = desired.add(0, .25, 0);
        setDeltaMovement(getDeltaMovement().scale(.8).add(desired.scale(.2)));
        if (desired.horizontalDistanceSqr() > .0001) {
            float yaw = (float)(Mth.atan2(desired.z, desired.x)*Mth.RAD_TO_DEG)-90;
            setYRot(Mth.rotLerp(.18F, getYRot(), yaw)); setYHeadRot(getYRot()); yBodyRot = getYRot();
        }
        if (flightTicks % 30 == 0) playSound(SoundEvents.ENDER_DRAGON_FLAP, boss ? 2 : .7F, boss ? .8F : 1.2F);
        if (target != null && hitCooldown == 0 && parts[0].getBoundingBox().inflate(.6).intersects(target.getBoundingBox()) && hasLineOfSight(target)) {
            if (target.hurt(damageSources().mobAttack(this), 10)) hitCooldown = 20;
        }
        if (flightTicks % 10 == 0) for (int wing = 3; wing <= 4; wing++) {
            for (Player player : server.getEntitiesOfClass(Player.class, parts[wing].getBoundingBox(),
                    candidate -> candidate.isAlive() && !candidate.isCreative() && !candidate.isSpectator())) {
                Vec3 away = player.position().subtract(position());
                player.knockback(.7, -away.x, -away.z);
            }
        }
    }
    private void healFromSpectralDragon(ServerLevel server) {
        var donor = server.getEntitiesOfClass(AbyssalDragonEntity.class, getBoundingBox().inflate(32),
                entity -> entity != this && !entity.isBoss() && entity.isAlive() && distanceToSqr(entity) <= 32*32)
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        healer = donor == null ? null : donor.getUUID();
        if (donor != null && getHealth() < getMaxHealth()) {
            float transferred = Math.min(1, Math.min(getMaxHealth()-getHealth(), donor.getHealth()));
            heal(transferred); donor.setHealth(donor.getHealth()-transferred);
            if (donor.getHealth() <= 0) donor.die(damageSources().magic());
        }
    }
    private void positionParts() {
        if (parts == null) return;
        double scale = boss ? 1 : 1.0/3, yaw = getYRot()*Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 center = position().add(0, .5*scale, 0);
        Vec3[] positions = {center.add(forward.scale(4.5*scale)), center,
                center.subtract(forward.scale(4.5*scale)), center.add(right.scale(5*scale)), center.subtract(right.scale(5*scale))};
        for (int i = 0; i < parts.length; i++) {
            var part = parts[i]; var old = part.position(); part.setPos(positions[i]); part.xo = old.x; part.yo = old.y; part.zo = old.z;
        }
    }
    public boolean hurtPart(AbyssalDragonPart part, DamageSource source, float amount) {
        if (level().isClientSide || !isAlive() || part.getParent() != this || isInvulnerableTo(source)) return false;
        boolean admin = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (boss && !admin && !(source.getEntity() instanceof Player) && !source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        if (!admin) {
            if (!part.name.equals("head")) amount = amount/4+1;
            if (boss && amount > 30) amount = 10+random.nextInt(10);
        }
        return super.hurt(source, amount);
    }
    @Override public boolean hurt(DamageSource source, float amount) { return parts != null && hurtPart(parts[1], source, amount); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENDER_DRAGON_GROWL; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENDER_DRAGON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }
    @Override protected boolean isAlwaysExperienceDropper() { return boss; }
    @Override protected void dropExperience(@Nullable Entity killer) {
        if (!boss || level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) super.dropExperience(killer);
    }
    @Override protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (!boss) { super.dropAllDeathLoot(level, source); return; }
        if (deathRewardPaid || deathRewardPending) return;
        // Native LivingDeathEvent still controls death; native loot/drop/XP events run at the end of the animation.
        pendingDeathSource = source;
        deathDamageType = source.typeHolder().unwrapKey().orElseThrow().location();
        deathAttacker = source.getEntity() == null ? null : source.getEntity().getUUID();
        deathByPlayer = lastHurtByPlayerTime > 0;
        deathRewardPending = true;
    }
    @Override protected void tickDeath() {
        if (!boss) { super.tickDeath(); return; }
        deathTime++;
        setDeltaMovement(Vec3.ZERO);
        move(MoverType.SELF, new Vec3(0, .1, 0));
        setYRot(getYRot()+3); yBodyRot = getYRot();
        if (level() instanceof ServerLevel server) {
            if (deathTime >= 180) server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY()+2, getZ(), 1, 2, 1, 2, 0);
            if (deathTime >= 200 && !isRemoved()) {
                if (deathRewardPending && !deathRewardPaid) {
                    // Mark before emitting rewards so a subsequent save cannot pay the same death twice.
                    deathRewardPaid = true; deathRewardPending = false;
                    Entity attacker = deathAttacker == null ? null : server.getEntity(deathAttacker);
                    if (pendingDeathSource == null) {
                        var key = ResourceKey.create(Registries.DAMAGE_TYPE, deathDamageType == null
                                ? ResourceLocation.withDefaultNamespace("generic") : deathDamageType);
                        var holder = server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(key)
                                .<net.minecraft.core.Holder<net.minecraft.world.damagesource.DamageType>>map(value -> value)
                                .orElse(server.damageSources().generic().typeHolder());
                        pendingDeathSource = new DamageSource(holder, attacker);
                    }
                    if (deathByPlayer && attacker instanceof Player player) { lastHurtByPlayer = player; lastHurtByPlayerTime = 1; }
                    super.dropAllDeathLoot(server, pendingDeathSource);
                }
                server.broadcastEntityEvent(this, (byte)60);
                remove(RemovalReason.KILLED);
            }
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.putLong("SummonHome", home.asLong());
        tag.putInt("FlightTicks", flightTicks); tag.putInt("ContactCooldown", hitCooldown);
        tag.putBoolean("DeathRewardPending", deathRewardPending); tag.putBoolean("DeathRewardPaid", deathRewardPaid);
        tag.putBoolean("DeathByPlayer", deathByPlayer);
        if (deathAttacker != null) tag.putUUID("DeathAttacker", deathAttacker);
        if (deathDamageType != null) tag.putString("DeathDamageType", deathDamageType.toString());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        home = tag.contains("SummonHome") ? BlockPos.of(tag.getLong("SummonHome")) : null;
        flightTicks = Math.clamp(tag.getInt("FlightTicks"), 0, Integer.MAX_VALUE);
        hitCooldown = Math.clamp(tag.getInt("ContactCooldown"), 0, 20); destination = null; healer = null;
        deathRewardPaid = tag.getBoolean("DeathRewardPaid");
        deathRewardPending = tag.getBoolean("DeathRewardPending") && !deathRewardPaid;
        deathByPlayer = tag.getBoolean("DeathByPlayer");
        deathAttacker = tag.hasUUID("DeathAttacker") ? tag.getUUID("DeathAttacker") : null;
        deathDamageType = ResourceLocation.tryParse(tag.getString("DeathDamageType")); pendingDeathSource = null;
        if (getHealth() <= 0) setPose(Pose.DYING);
    }
}
