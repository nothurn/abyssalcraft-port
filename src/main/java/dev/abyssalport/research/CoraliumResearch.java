package dev.abyssalport.research;

import dev.abyssalport.content.CoraliumContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class CoraliumResearch {
    private CoraliumResearch() { }
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
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) discover(player, player.getInventory().getItem(slot));
    }
    public static void discover(ServerPlayer player, ItemStack stack) {
        if (stack.is(CoraliumContent.GEM.get()) || stack.is(CoraliumContent.PEARL.get())
                || stack.is(CoraliumContent.ORE_ITEM.get()) || stack.is(CoraliumContent.INFUSED_STONE_ITEM.get())
                || stack.getItem() == CoraliumContent.CLUSTER_2.get() || stack.is(CoraliumContent.CLUSTER_3.get())
                || stack.is(CoraliumContent.CLUSTER_4.get()) || stack.is(CoraliumContent.CLUSTER_5.get())
                || stack.is(CoraliumContent.CLUSTER_6.get()) || stack.is(CoraliumContent.CLUSTER_7.get())
                || stack.is(CoraliumContent.CLUSTER_8.get()) || stack.is(CoraliumContent.CLUSTER_9.get())) {
            EarlyResearch.unlock(player, EarlyResearch.CORALIUM);
        }
        if (stack.is(CoraliumContent.TRANSMUTATION_GEM.get()) || stack.is(CoraliumContent.OBLIVION_SHARD.get())) {
            EarlyResearch.unlock(player, EarlyResearch.TRANSMUTATION);
        }
        if (stack.is(CoraliumContent.OBLIVION_CATALYST.get())) EarlyResearch.unlock(player, EarlyResearch.OBLIVION);
    }
}
