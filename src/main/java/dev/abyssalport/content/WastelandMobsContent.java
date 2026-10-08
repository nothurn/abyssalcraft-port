package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.entity.AbyssalZombieEntity;
import dev.abyssalport.item.CoraliumFleshItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WastelandMobsContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<AbyssalZombieEntity>> ABYSSAL_ZOMBIE = ENTITIES.register("abyssal_zombie", () ->
            EntityType.Builder.of(AbyssalZombieEntity::new, MobCategory.MONSTER).sized(.6F, 1.8F).clientTrackingRange(8).build("abyssal_zombie"));
    public static final DeferredHolder<Item, CoraliumFleshItem> PLAGUED_FLESH = ITEMS.register("coralium_plagued_flesh", () ->
            new CoraliumFleshItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(.1F).build())));
    public static final DeferredHolder<Item, Item> CORALIUM_CHUNK = ITEMS.register("coralium_chunk", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> ZOMBIE_EGG = ITEMS.register("abyssal_zombie_spawn_egg", () ->
            new DeferredSpawnEggItem(ABYSSAL_ZOMBIE, 0x285A46, 0x8CB091, new Item.Properties()));
    private WastelandMobsContent() { }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus); ITEMS.register(bus);
        bus.addListener(WastelandMobsContent::attributes); bus.addListener(WastelandMobsContent::placements); bus.addListener(WastelandMobsContent::creative);
    }
    private static void attributes(EntityAttributeCreationEvent event) { event.put(ABYSSAL_ZOMBIE.get(), AbyssalZombieEntity.createAttributes().build()); }
    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(ABYSSAL_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) event.accept(PLAGUED_FLESH.get());
        else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(CORALIUM_CHUNK.get());
        else if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(ZOMBIE_EGG.get());
    }
}
