package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.energy.CharmAmplifier;
import dev.abyssalport.item.RitualCharmItem;
import java.util.EnumMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

/** Reachable generic charms. Deity infusion remains gated by the later tier-four book. */
public final class CharmContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final EnumMap<CharmAmplifier, DeferredHolder<Item, RitualCharmItem>> CHARMS = new EnumMap<>(CharmAmplifier.class);
    static {
        for (var amplifier : CharmAmplifier.values()) {
            String id = amplifier == CharmAmplifier.NONE ? "ritual_charm" : amplifier.id() + "_ritual_charm";
            CHARMS.put(amplifier, ITEMS.register(id, () -> new RitualCharmItem(new Item.Properties(), amplifier)));
        }
    }
    private CharmContent() { }
    public static RitualCharmItem item(CharmAmplifier amplifier) { return CHARMS.get(amplifier).get(); }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(CharmContent::creative);
        NeoForge.EVENT_BUS.addListener(CharmContent::forge);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) CHARMS.values().forEach(item -> event.accept(item.get()));
    }

    private static void forge(AnvilUpdateEvent event) {
        var left = event.getLeft();
        if (!left.is(item(CharmAmplifier.NONE))) return;
        var right = event.getRight();
        var amplifier = right.is(Items.ARROW) ? CharmAmplifier.RANGE
                : right.is(Items.GUNPOWDER) ? CharmAmplifier.POWER
                : right.is(Items.REDSTONE) ? CharmAmplifier.DURATION : CharmAmplifier.NONE;
        if (amplifier == CharmAmplifier.NONE) return;
        // Native AnvilMenu clears the entire left slot. Reject a stack rather than
        // silently destroying extra charms for the source's single-item output.
        if (left.getCount() != 1) { event.setCanceled(true); return; }
        var result = new ItemStack(item(amplifier).builtInRegistryHolder(), 1, left.getComponentsPatch());
        if (event.getName() != null) {
            if (event.getName().isBlank()) result.remove(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
            else if (!event.getName().equals(left.getHoverName().getString())) result.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    net.minecraft.network.chat.Component.literal(event.getName()));
        }
        event.setOutput(result);
        event.setCost(1);
        event.setMaterialCost(1);
    }
}
