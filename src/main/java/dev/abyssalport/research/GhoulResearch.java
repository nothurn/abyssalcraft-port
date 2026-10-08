package dev.abyssalport.research;

import dev.abyssalport.content.GraveyardContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

public final class GhoulResearch {
    private GhoulResearch() { }
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if(event.getPlayer() instanceof ServerPlayer player) discover(player,event.getOriginalStack());
    }
    public static boolean discover(ServerPlayer player,ItemStack stack) {
        return (stack.is(GraveyardContent.FLESH.get())||stack.is(GraveyardContent.SHADOW_FLESH.get())
                ||stack.is(GraveyardContent.DEPTHS_FLESH.get())||stack.is(GraveyardContent.DREADED_FLESH.get()))&&EarlyResearch.unlock(player,EarlyResearch.GHOULS);
    }
    public static void discoverFromInventory(ServerPlayer player) {
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++) discover(player,player.getInventory().getItem(slot));
    }
}
