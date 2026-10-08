package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.entity.AbyssalDragonEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WastelandBossContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<AbyssalDragonEntity>> ASORAH = ENTITIES.register("asorah", () ->
            EntityType.Builder.<AbyssalDragonEntity>of((type, level) -> new AbyssalDragonEntity(type, level, true), MobCategory.MONSTER)
                .sized(7.5F, 4.5F).fireImmune().clientTrackingRange(12).updateInterval(1).build("abyssalport:asorah"));
    public static final DeferredHolder<EntityType<?>, EntityType<AbyssalDragonEntity>> SPECTRAL_DRAGON = ENTITIES.register("spectral_dragon", () ->
            EntityType.Builder.<AbyssalDragonEntity>of((type, level) -> new AbyssalDragonEntity(type, level, false), MobCategory.MONSTER)
                .sized(2.5F, 1.5F).fireImmune().clientTrackingRange(10).updateInterval(1).build("abyssalport:spectral_dragon"));
    public static final DeferredHolder<Item, Item> EYE = ITEMS.register("eye_of_the_abyss", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> SPECTRAL_EGG = ITEMS.register("spectral_dragon_spawn_egg", () ->
            new DeferredSpawnEggItem(SPECTRAL_DRAGON, 0x476D74, 0xABD9B2, new Item.Properties()));
    private WastelandBossContent() { }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus); ITEMS.register(bus);
        bus.addListener(WastelandBossContent::attributes); bus.addListener(WastelandBossContent::creative);
        bus.addListener(WastelandBossContent::placements);
    }
    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(ASORAH.get(), AbyssalDragonEntity.attributes(true).build());
        event.put(SPECTRAL_DRAGON.get(), AbyssalDragonEntity.attributes(false).build());
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(EYE.get());
        else if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(SPECTRAL_EGG.get());
    }
    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(SPECTRAL_DRAGON.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getLevel().dimension().equals(WastelandContent.DIMENSION)
                        && level.getDifficulty() != Difficulty.PEACEFUL,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
