package dev.abyssalport.ritual;

import dev.abyssalport.content.WastelandBossContent;
import dev.abyssalport.entity.AbyssalDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/** Create and validate the real outcome before charging an altar's book or offerings. */
public final class SummoningRitual {
    private SummoningRitual() { }
    public static boolean supported(EntityType<?> type) { return type == WastelandBossContent.ASORAH.get() || type == WastelandBossContent.SPECTRAL_DRAGON.get(); }
    public static @Nullable AbyssalDragonEntity spawn(ServerLevel level, BlockPos pos, RitualDefinition definition) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !(definition.summonType().create(level) instanceof AbyssalDragonEntity dragon)) return null;
        dragon.moveTo(pos.getX()+.5, pos.getY()+1, pos.getZ()+.5, 0, 0); dragon.setHome(pos);
        EventHooks.finalizeMobSpawn(dragon, level, level.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null);
        var bounds = dragon.getBoundingBox();
        // The source summons above the user. Validate solid world geometry, not the
        // summoner's temporary overlapping hitbox; this flying creature is not a solid obstacle.
        if (!level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ), BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ))
                || !level.getWorldBorder().isWithinBounds(bounds) || java.util.stream.StreamSupport
                    .stream(level.getBlockCollisions(dragon, bounds).spliterator(), false).anyMatch(shape -> !shape.isEmpty())
                || !dragon.isAlive() || dragon.isRemoved() || !level.addFreshEntity(dragon)) { dragon.discard(); return null; }
        return dragon;
    }
}
