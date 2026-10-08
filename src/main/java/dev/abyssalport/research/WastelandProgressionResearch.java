package dev.abyssalport.research;

import dev.abyssalport.content.WastelandProgressionContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class WastelandProgressionResearch {
    private WastelandProgressionResearch() { }
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
        if(stack.getItem() instanceof dev.abyssalport.item.CrystalBagItem) return EarlyResearch.unlock(player,EarlyResearch.CRYSTAL_BAGS);
        if(stack.is(dev.abyssalport.content.CrystallizationContent.MACHINE_ITEM.get())
                || stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("abyssalport","crystals"))))
            return EarlyResearch.unlock(player,EarlyResearch.CRYSTALLIZATION);
        if (stack.is(dev.abyssalport.content.DreadProcessingContent.SHARD.get()) || stack.is(dev.abyssalport.content.DreadProcessingContent.INGOT.get())
                || stack.is(dev.abyssalport.content.DreadProcessingContent.NUGGET.get()) || stack.is(dev.abyssalport.content.DreadProcessingContent.STORAGE_ITEM.get())
                || stack.getItem() instanceof net.minecraft.world.item.TieredItem tool && tool.getTier()==dev.abyssalport.item.DreadiumTier.INSTANCE
                || stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor && armor.getMaterial().is(dev.abyssalport.content.DreadProcessingContent.MATERIAL))
            return EarlyResearch.unlock(player, EarlyResearch.DREADIUM);
        if (stack.is(dev.abyssalport.content.WastelandProcessingContent.MACHINE_ITEM.get())
                || stack.is(dev.abyssalport.content.WastelandProcessingContent.BRICK.get())
                || stack.is(dev.abyssalport.content.WastelandProcessingContent.STONE_ITEM.get())
                || stack.is(dev.abyssalport.content.WastelandProcessingContent.COBBLESTONE_ITEM.get()))
            return EarlyResearch.unlock(player, EarlyResearch.PROCESSING);
        if (stack.is(dev.abyssalport.content.DreadlandsProgressionContent.BOOK.get())) return EarlyResearch.unlock(player, EarlyResearch.DREADLANDS_BOOK);
        if (stack.is(dev.abyssalport.content.DreadlandsProgressionContent.ESSENCE.get()) || stack.is(dev.abyssalport.content.DreadlandsProgressionContent.SKIN.get()))
            return EarlyResearch.unlock(player, EarlyResearch.DREADLANDS);
        if (stack.is(dev.abyssalport.content.DreadlandsContent.KEY.get())) return EarlyResearch.unlock(player, EarlyResearch.DREADLANDS_KEY);
        if (stack.is(dev.abyssalport.content.WastelandStructuresContent.POWERSTONE_ITEM.get())) return EarlyResearch.unlock(player, EarlyResearch.POWERSTONE);
        if (stack.is(dev.abyssalport.content.WastelandBossContent.EYE.get())) return EarlyResearch.unlock(player, EarlyResearch.ASORAH);
        if (stack.getItem() instanceof dev.abyssalport.item.RendingStaffItem) return EarlyResearch.unlock(player, EarlyResearch.RENDING);
        if (stack.is(WastelandProgressionContent.ESSENCE.get()) || stack.is(WastelandProgressionContent.SKIN.get())) return EarlyResearch.unlock(player, EarlyResearch.WASTELAND);
        return stack.is(WastelandProgressionContent.BOOK.get()) && EarlyResearch.unlock(player, EarlyResearch.WASTELAND_BOOK);
    }
}
