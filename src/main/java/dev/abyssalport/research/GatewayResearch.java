package dev.abyssalport.research;

import dev.abyssalport.content.GatewayContent;
import dev.abyssalport.content.WastelandContent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Persisted key discovery and actual dimension entry, independent of the travel method. */
public final class GatewayResearch {
    private GatewayResearch() { }
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getOriginalStack().is(GatewayContent.KEY.get()))
            EarlyResearch.unlock(player, EarlyResearch.GATEWAY);
    }
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getCrafting().is(GatewayContent.KEY.get()))
            EarlyResearch.unlock(player, EarlyResearch.GATEWAY);
    }
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().getGameTime() % 20 == 0) discoverDimension(player);
    }
    public static boolean discoverDimension(ServerPlayer player) {
        if (player.level().dimension().equals(dev.abyssalport.content.DreadlandsContent.DIMENSION))
            return EarlyResearch.unlock(player, EarlyResearch.DREADLANDS);
        return player.level().dimension().equals(WastelandContent.DIMENSION) && EarlyResearch.unlock(player, EarlyResearch.WASTELAND);
    }
    public static void discoverFromInventory(ServerPlayer player) {
        if (player.getInventory().contains(stack -> stack.is(GatewayContent.KEY.get()))) EarlyResearch.unlock(player, EarlyResearch.GATEWAY);
        discoverDimension(player);
    }
}
