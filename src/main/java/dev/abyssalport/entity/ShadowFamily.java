package dev.abyssalport.entity;

import dev.abyssalport.content.GraveyardContent;
import dev.abyssalport.content.ShadowFamilyContent;
import dev.abyssalport.content.WastelandContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.*;

/** Shadow classification/death rules derived from Shinoow's AbyssalCraftEventHooks (LGPL-3.0, 2012-2026). */
public final class ShadowFamily {
    public static final ResourceKey<DamageType> SHADOW = ResourceKey.create(Registries.DAMAGE_TYPE, WastelandContent.id("shadow"));
    private ShadowFamily() { }
    public static boolean isShadow(Entity entity) {
        return entity instanceof ShadowCreatureEntity || entity instanceof GhoulEntity ghoul && ghoul.isShadow();
    }
    public static DamageSource damage(LivingEntity source) {
        return new DamageSource(source.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SHADOW), source);
    }
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (isShadow(event.getEntity()) && (event.getSource().is(SHADOW) || isShadow(event.getSource().getEntity()))) event.setCanceled(true);
    }
    /** Lowest priority, non-cancelled deaths only: keep other native death/conversion vetoes authoritative. */
    public static void onDeath(LivingDeathEvent event) {
        var victim = event.getEntity();
        // Evil animals own a single native Demon death-phase. Do not also admit a shadow replacement.
        if (victim instanceof CorruptedAnimalEntity animal && !animal.demon()
                && dev.abyssalport.config.MobEcologyConfig.demonOnDeath()) return;
        if (victim.level() instanceof ServerLevel && !victim.isRemoved() && !isShadow(victim)
                && (event.getSource().is(SHADOW) || isShadow(event.getSource().getEntity()))
                && (victim instanceof Player || victim.getRandom().nextBoolean())) convertDeath(victim);
    }
    public static EntityType<? extends Mob> deathForm(LivingEntity victim) {
        if (victim instanceof Player) return ShadowFamilyContent.MONSTER.get();
        if (victim instanceof GhoulEntity) return GraveyardContent.SHADOW_GHOUL.get();
        if (victim.getBbHeight() >= 2.2F) return ShadowFamilyContent.BEAST.get();
        if (victim.getBbHeight() >= 1.2F) return ShadowFamilyContent.MONSTER.get();
        return ShadowFamilyContent.type(ShadowCreatureEntity.Kind.CREATURE);
    }
    /** Admit replacement first. Rejected conversion never removes the victim or its ordinary death/drops. */
    public static boolean convertDeath(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel level) || victim.isRemoved() || !victim.isDeadOrDying() || isShadow(victim)) return false;
        var type = deathForm(victim);
        if (!EventHooks.canLivingConvert(victim, type, ignored -> { })) return false;
        var replacement = type.create(level);
        if (replacement == null) return false;
        boolean admitted = false;
        try {
            replacement.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), victim.getXRot());
            // A larger source-form must not be embedded into the dead animal's smaller space.
            for (var pos : net.minecraft.core.BlockPos.betweenClosed(
                    net.minecraft.core.BlockPos.containing(replacement.getBoundingBox().getMinPosition()),
                    net.minecraft.core.BlockPos.containing(replacement.getBoundingBox().getMaxPosition()))) if (!level.hasChunkAt(pos)) return false;
            if (!level.noCollision(replacement) || level.containsAnyLiquid(replacement.getBoundingBox())) return false;
            EventHooks.finalizeMobSpawn(replacement, level, level.getCurrentDifficultyAt(replacement.blockPosition()), MobSpawnType.CONVERSION, null);
            if (replacement.isSpawnCancelled()) return false;
            if (victim instanceof Player) { replacement.setCustomName(victim.getName()); replacement.setPersistenceRequired(); }
            else { if (victim.hasCustomName()) replacement.setCustomName(victim.getCustomName());
                if (victim instanceof Mob mob && mob.isPersistenceRequired()) replacement.setPersistenceRequired(); }
            admitted = level.addFreshEntity(replacement) && replacement.isAddedToLevel() && !replacement.isRemoved();
            if (!admitted) return false;
            // Player death/respawn/inventory stay entirely native. Non-player source transformations do not double-drop.
            if (!(victim instanceof Player)) victim.discard();
            EventHooks.onLivingConvert(victim, replacement);
            if (victim instanceof Player) level.levelEvent(null, 1016, replacement.blockPosition(), 0);
            return true;
        } finally { if (!admitted) replacement.discard(); }
    }
}
