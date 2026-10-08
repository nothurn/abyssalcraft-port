package dev.abyssalport.research;

import dev.abyssalport.worldgen.DarklandsWorldgen;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Records a player's first visit to the actual Darklands biome, not fallback soil patches. */
public final class DarklandsResearch {
    private DarklandsResearch() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level().getGameTime() % 20 == 0
                && !EarlyResearch.has(player, EarlyResearch.DARKLANDS)) {
            discover(player, player.level().getBiome(player.blockPosition()));
        }
    }

    public static boolean discover(ServerPlayer player, Holder<Biome> biome) {
        return DarklandsWorldgen.isDarklands(biome)
                && EarlyResearch.unlock(player, EarlyResearch.DARKLANDS);
    }
}
