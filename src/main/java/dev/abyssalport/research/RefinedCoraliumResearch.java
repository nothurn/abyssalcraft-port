package dev.abyssalport.research;

import dev.abyssalport.content.WastelandMineralsContent;
import dev.abyssalport.item.RefinedCoraliumTier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class RefinedCoraliumResearch {
    private RefinedCoraliumResearch() { }
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
    public static boolean discover(ServerPlayer player, ItemStack stack) {
        boolean refined = stack.is(WastelandMineralsContent.INGOT.get()) || stack.is(WastelandMineralsContent.NUGGET.get())
                || stack.is(WastelandMineralsContent.BLOCK_ITEM.get())
                || stack.getItem() instanceof TieredItem tool && tool.getTier() == RefinedCoraliumTier.INSTANCE
                || stack.is(WastelandMineralsContent.HELMET.get()) || stack.is(WastelandMineralsContent.CHESTPLATE.get())
                || stack.is(WastelandMineralsContent.LEGGINGS.get()) || stack.is(WastelandMineralsContent.BOOTS.get());
        return refined && EarlyResearch.unlock(player, EarlyResearch.REFINED_CORALIUM);
    }
}
