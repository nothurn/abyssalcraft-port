package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.*;
import dev.abyssalport.energy.EnergyDeviceTier;
import dev.abyssalport.item.EnergyStorageItem;
import dev.abyssalport.item.EmbossedRingItem;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

/** Reachable ring/PE upgrades from AbyssalCrafting/AnvilForging, Shinoow, LGPL-3.0.
 * Shared native BEs preserve existing base IDs, statue discovery and inventory roles.
 */
public final class EnergyDeviceContent {
    public enum Kind { COLLECTOR, RELAY, CONTAINER, PEDESTAL }
    public static final List<EnergyDeviceTier> REACHABLE = List.of(EnergyDeviceTier.BASE,
            EnergyDeviceTier.OVERWORLD, EnergyDeviceTier.ABYSSAL_WASTELAND, EnergyDeviceTier.DREADLANDS);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AbyssalPort.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<dev.abyssalport.block.entity.EnergyPedestalBlockEntity>> PEDESTAL_ENTITY =
            ENTITIES.register("energy_pedestal", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    dev.abyssalport.block.entity.EnergyPedestalBlockEntity::new, blocks(Kind.PEDESTAL)).build(null));
    private static final Map<Kind, Map<EnergyDeviceTier, DeferredHolder<Block, ? extends EnergyStorageBlock>>> DEVICES = new EnumMap<>(Kind.class);
    private static final Map<Kind, Map<EnergyDeviceTier, DeferredHolder<Item, EnergyStorageItem>>> DEVICE_ITEMS = new EnumMap<>(Kind.class);
    private static final Map<EnergyDeviceTier, DeferredHolder<Item, Item>> RINGS = new EnumMap<>(EnergyDeviceTier.class);
    static {
        for (var kind : Kind.values()) { DEVICES.put(kind, new EnumMap<>(EnergyDeviceTier.class)); DEVICE_ITEMS.put(kind, new EnumMap<>(EnergyDeviceTier.class)); }
        for (var tier : REACHABLE) {
            RINGS.put(tier, ITEMS.register(ringId(tier), () -> new EmbossedRingItem(new Item.Properties(), tier)));
            for (var kind : Kind.values()) {
                if (tier == EnergyDeviceTier.BASE && kind != Kind.PEDESTAL) continue;
                String id = (tier == EnergyDeviceTier.BASE ? "" : tier.id() + "_") + "energy_" + kind.name().toLowerCase(Locale.ROOT);
                var block = BLOCKS.register(id, () -> switch (kind) {
                    case COLLECTOR -> new EnergyCollectorBlock(properties(), tier);
                    case RELAY -> new EnergyRelayBlock(properties(), tier);
                    case CONTAINER -> new EnergyContainerBlock(properties(), tier);
                    case PEDESTAL -> new EnergyPedestalBlock(properties().strength(6, 12), tier);
                });
                DEVICES.get(kind).put(tier, block);
                DEVICE_ITEMS.get(kind).put(tier, ITEMS.register(id, () -> new EnergyStorageItem(block.get(), new Item.Properties(), capacity(kind, tier))));
            }
        }
    }
    private EnergyDeviceContent() { }
    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().strength(3, 6).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).noOcclusion();
    }
    public static String ringId(EnergyDeviceTier tier) { return tier == EnergyDeviceTier.BASE ? "embossed_ring" : tier.id() + "_embossed_ring"; }
    public static Item ring(EnergyDeviceTier tier) {
        if (!REACHABLE.contains(tier)) throw new IllegalArgumentException("Unconnected ring progression: " + tier);
        return RINGS.get(tier).get();
    }
    public static int capacity(Kind kind, EnergyDeviceTier tier) {
        return switch (kind) { case COLLECTOR -> tier.collectorCapacity(); case RELAY -> tier.relayCapacity(); case CONTAINER -> tier.containerCapacity(); case PEDESTAL -> tier.pedestalCapacity(); };
    }
    public static EnergyStorageBlock block(Kind kind, EnergyDeviceTier tier) {
        if (!REACHABLE.contains(tier)) throw new IllegalArgumentException("Unconnected device progression: " + tier);
        if (tier != EnergyDeviceTier.BASE || kind == Kind.PEDESTAL) return DEVICES.get(kind).get(tier).get();
        return switch (kind) { case COLLECTOR -> AbyssalPort.ENERGY_COLLECTOR.get(); case RELAY -> AbyssalPort.ENERGY_RELAY.get(); case CONTAINER -> AbyssalPort.ENERGY_CONTAINER.get(); case PEDESTAL -> throw new IllegalStateException(); };
    }
    public static EnergyStorageItem item(Kind kind, EnergyDeviceTier tier) { return (EnergyStorageItem) block(kind, tier).asItem(); }
    public static Block[] blocks(Kind kind) { return REACHABLE.stream().map(tier -> block(kind, tier)).toArray(Block[]::new); }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); bus.addListener(EnergyDeviceContent::creative);
        NeoForge.EVENT_BUS.addListener(EnergyDeviceContent::forge);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) RINGS.values().forEach(ring -> event.accept(ring.get()));
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
            DEVICE_ITEMS.values().forEach(items -> items.values().forEach(item -> event.accept(item.get())));
    }
    private static void forge(AnvilUpdateEvent event) {
        var left = event.getLeft();
        if (!(left.getItem() instanceof EnergyStorageItem device) || !(device.getBlock() instanceof EnergyStorageBlock block)) return;
        Kind kind = block instanceof EnergyCollectorBlock ? Kind.COLLECTOR : block instanceof EnergyRelayBlock ? Kind.RELAY
                : block instanceof EnergyContainerBlock ? Kind.CONTAINER : block instanceof EnergyPedestalBlock ? Kind.PEDESTAL : null;
        if (kind == null || !REACHABLE.contains(block.tier())) return;
        if (block != block(kind, block.tier())) return;
        EnergyDeviceTier target = REACHABLE.stream().filter(tier -> tier != EnergyDeviceTier.BASE && event.getRight().is(ring(tier))).findFirst().orElse(null);
        if (target == null || target.ordinal() <= block.tier().ordinal()) return;
        if (left.getCount() != 1) { event.setCanceled(true); return; }
        var result = new ItemStack(item(kind, target).builtInRegistryHolder(), 1, left.getComponentsPatch());
        if (event.getName() != null) {
            if (event.getName().isBlank()) result.remove(DataComponents.CUSTOM_NAME);
            else if (!event.getName().equals(left.getHoverName().getString())) result.set(DataComponents.CUSTOM_NAME, Component.literal(event.getName()));
        }
        event.setOutput(result); event.setCost(1); event.setMaterialCost(1);
    }
}
