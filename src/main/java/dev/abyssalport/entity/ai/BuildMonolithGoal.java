package dev.abyssalport.entity.ai;

import dev.abyssalport.entity.ShoggothEntity;
import dev.abyssalport.worldgen.MonolithBuilder;
import dev.abyssalport.worldgen.MonolithSiteRules;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;

/** Cooperative four-Shoggoth construction, using the same safe site rules as worldgen. */
public final class BuildMonolithGoal extends Goal {
    public static final int ASSEMBLY_TICKS = 40;
    private static final double MIN_SITE_DISTANCE_SQUARED = 4.5 * 4.5;
    private final ShoggothEntity leader;
    private final List<ShoggothEntity> assistants = new ArrayList<>(3);
    private BlockPos site;
    private int height;
    private int remainingTicks;
    private int assemblyTicks;
    private int nextSiteSearch;

    public BuildMonolithGoal(ShoggothEntity leader) {
        this.leader = leader;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(leader.level() instanceof ServerLevel level)
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || leader.isBuilding() || leader.isAssisting()
                || leader.monolithTimer() < ShoggothEntity.MONOLITH_COOLDOWN_TICKS
                || leader.getTarget() != null) {
            return false;
        }
        // Mob.serverAiStep evaluates new goals on alternating ticks selected
        // by (tickCount + entityId). A modulo-even gate starves odd-ID mobs
        // forever; use elapsed deadlines, independent of native AI phase.
        if (leader.tickCount < nextSiteSearch) return false;
        nextSiteSearch = leader.tickCount + 20;
        List<ShoggothEntity> nearby = level.getEntitiesOfClass(ShoggothEntity.class,
                leader.getBoundingBox().inflate(12), entity -> entity != leader && entity.isAlive()
                        && entity.distanceToSqr(leader) <= 12 * 12
                        && !entity.isAssisting() && !entity.isBuilding() && entity.getTarget() == null);
        if (nearby.size() < 3) {
            return false;
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos candidate = leader.blockPosition().offset(
                    leader.getRandom().nextInt(13) - 6, leader.getRandom().nextInt(5) - 2,
                    leader.getRandom().nextInt(13) - 6);
            int candidateHeight = 5 + leader.getRandom().nextInt(8);
            double siteX = candidate.getX() + 0.5;
            double siteY = candidate.getY();
            double siteZ = candidate.getZ() + 0.5;
            double leaderDistance = leader.distanceToSqr(siteX, siteY, siteZ);
            if (leaderDistance >= MIN_SITE_DISTANCE_SQUARED && leaderDistance <= 49
                    && nearby.stream().limit(3).allMatch(entity ->
                            entity.distanceToSqr(siteX, siteY, siteZ) >= MIN_SITE_DISTANCE_SQUARED)
                    && level.hasChunkAt(candidate.offset(-1, 0, -1))
                    && level.hasChunkAt(candidate.offset(-1, 0, 1))
                    && level.hasChunkAt(candidate.offset(1, 0, -1))
                    && level.hasChunkAt(candidate.offset(1, 0, 1))
                    && MonolithBuilder.canBuild(level, candidate, candidateHeight)
                    && clearOfLivingEntities(level, candidate, candidateHeight)
                    && MonolithSiteRules.farFromExistingMonoliths(level, candidate)) {
                site = candidate.immutable();
                height = candidateHeight;
                assistants.clear();
                assistants.addAll(nearby.subList(0, 3));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public boolean canContinueToUse() {
        if (!(leader.level() instanceof ServerLevel level) || !leader.isAlive()
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || leader.getTarget() != null || remainingTicks <= 0 || site == null
                || !MonolithBuilder.canBuild(level, site, height)) {
            return false;
        }
        return assistants.stream().allMatch(entity -> entity.isAlive() && entity.isAssisting()
                && entity.getTarget() == null && entity.distanceToSqr(leader) <= 16 * 16);
    }

    @Override
    public void start() {
        leader.setBuilding(true);
        assistants.forEach(entity -> entity.setAssisting(true));
        remainingTicks = 400;
        assemblyTicks = 0;
        leader.getNavigation().stop();
    }

    @Override
    public void tick() {
        remainingTicks--;
        if (site == null || !(leader.level() instanceof ServerLevel level)
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || leader.getTarget() != null) {
            assemblyTicks = 0;
            return;
        }
        if (remainingTicks % 20 == 0) {
            // An assembled helper must wait, not continually path into the
            // leader's body. Crowding pushes the group into walls, where native
            // climbing separates it and can prevent a safe assembly forever.
            assistants.forEach(entity -> {
                if (entity.distanceToSqr(leader) > 6 * 6) {
                    entity.getNavigation().moveTo(leader, 0.9);
                } else {
                    entity.getNavigation().stop();
                }
            });
        }
        boolean assembled = leader.distanceToSqr(site.getX() + 0.5, site.getY(), site.getZ() + 0.5) <= 8 * 8
                && assistants.stream().allMatch(entity -> entity.distanceToSqr(leader) <= 8 * 8)
                && clearOfLivingEntities(level, site, height);
        assemblyTicks = assembled ? assemblyTicks + 1 : 0;
        if (assemblyTicks < ASSEMBLY_TICKS) {
            return;
        }
        if (!MonolithSiteRules.farFromExistingMonoliths(level, site)) {
            remainingTicks = 0;
            return;
        }
        if (MonolithBuilder.build(level, site, height, 3)) {
            leader.finishMonolith();
            assistants.forEach(ShoggothEntity::finishMonolith);
            level.playSound(null, site, SoundEvents.ANVIL_USE, SoundSource.HOSTILE, 1.5F, 0.65F);
            remainingTicks = 0;
        }
    }

    @Override
    public void stop() {
        assistants.forEach(entity -> entity.setAssisting(false));
        assistants.clear();
        leader.setBuilding(false);
        site = null;
        remainingTicks = 0;
        assemblyTicks = 0;
    }

    private static boolean clearOfLivingEntities(ServerLevel level, BlockPos center, int height) {
        AABB volume = new AABB(center.getX() - 1, center.getY(), center.getZ() - 1,
                center.getX() + 2, center.getY() + height + 1, center.getZ() + 2);
        return level.getEntitiesOfClass(LivingEntity.class, volume,
                entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
    }

}
