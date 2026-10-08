package dev.abyssalport.research;

import dev.abyssalport.AbyssalPort;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** The first shadow sample reveals its field-guide page, regardless of how it was obtained. */
public final class ShadowMaterialResearch {
    private ShadowMaterialResearch() {
    }

    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            discover(player, event.getOriginalStack());
        }
    }

    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            discover(player, event.getCrafting());
        }
    }

    /** Covers existing saves where the sample was collected before this discovery existed. */
    public static void discoverFromInventory(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isShadowMaterial(player.getInventory().getItem(slot))) {
                EarlyResearch.unlock(player, EarlyResearch.SHADOW_MATERIALS);
                return;
            }
        }
    }

    private static void discover(ServerPlayer player, ItemStack stack) {
        if (isShadowMaterial(stack)) {
            EarlyResearch.unlock(player, EarlyResearch.SHADOW_MATERIALS);
        }
    }

    private static boolean isShadowMaterial(ItemStack stack) {
        return stack.is(AbyssalPort.SHADOW_FRAGMENT.get()) || stack.is(AbyssalPort.SHADOW_SHARD.get())
                || stack.is(AbyssalPort.SHADOW_GEM.get());
    }
}
