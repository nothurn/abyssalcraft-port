package dev.abyssalport.research;

import dev.abyssalport.content.AbyssalniteContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class AbyssalniteResearch {
    private AbyssalniteResearch() { }

    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) discover(player, event.getOriginalStack());
    }

    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) discover(player, event.getCrafting());
    }

    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) discover(player, event.getSmelting());
    }

    public static void discoverFromInventory(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            discover(player, player.getInventory().getItem(slot));
        }
    }

    public static boolean discover(ServerPlayer player, ItemStack stack) {
        return (stack.is(AbyssalniteContent.ORE_ITEM.get()) || stack.is(AbyssalniteContent.INGOT.get())
                || stack.is(AbyssalniteContent.NUGGET.get()) || stack.is(AbyssalniteContent.BLOCK_ITEM.get()))
                && EarlyResearch.unlock(player, EarlyResearch.ABYSSALNITE);
    }

}
