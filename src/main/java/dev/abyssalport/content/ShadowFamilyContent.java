package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.entity.ShadowCreatureEntity;
import dev.abyssalport.entity.ShadowCreatureEntity.Kind;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.registries.*;

/** Existing Creature registry identity remains in AbyssalPort, including old saves. */
public final class ShadowFamilyContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<ShadowCreatureEntity>> MONSTER = entity("shadow_monster", Kind.MONSTER, .6F, 1.8F);
    public static final DeferredHolder<EntityType<?>, EntityType<ShadowCreatureEntity>> BEAST = entity("shadow_beast", Kind.BEAST, 1, 2.8F);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> CREATURE_EGG = egg("shadow_creature", AbyssalPort.SHADOW_CREATURE);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> MONSTER_EGG = egg("shadow_monster", MONSTER);
    public static final DeferredHolder<Item, DeferredSpawnEggItem> BEAST_EGG = egg("shadow_beast", BEAST);
    private ShadowFamilyContent() { }
    private static DeferredHolder<EntityType<?>, EntityType<ShadowCreatureEntity>> entity(String name, Kind kind, float width, float height) {
        return ENTITIES.register(name, () -> EntityType.Builder.<ShadowCreatureEntity>of((type, level) ->
                new ShadowCreatureEntity(type, level, kind), MobCategory.MONSTER).sized(width, height)
                .fireImmune().clientTrackingRange(8).build(name));
    }
    private static DeferredHolder<Item, DeferredSpawnEggItem> egg(String name, java.util.function.Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type) {
        return ITEMS.register(name+"_spawn_egg", () -> new DeferredSpawnEggItem(type, 0x24232B, 0xD6B789, new Item.Properties()));
    }
    public static EntityType<ShadowCreatureEntity> type(Kind kind) {
        return switch(kind) { case CREATURE -> AbyssalPort.SHADOW_CREATURE.get(); case MONSTER -> MONSTER.get(); case BEAST -> BEAST.get(); };
    }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus); ITEMS.register(bus);
        bus.addListener(ShadowFamilyContent::attributes); bus.addListener(ShadowFamilyContent::placements); bus.addListener(ShadowFamilyContent::creative);
    }
    private static void attributes(EntityAttributeCreationEvent event) {
        for (Kind kind : new Kind[]{Kind.MONSTER, Kind.BEAST}) event.put(type(kind), ShadowCreatureEntity.createAttributes(kind).build());
    }
    private static void placements(RegisterSpawnPlacementsEvent event) {
        for (Kind kind : new Kind[]{Kind.MONSTER, Kind.BEAST}) event.register(type(kind), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ShadowCreatureEntity::checkShadowSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(CREATURE_EGG.get()); event.accept(MONSTER_EGG.get()); event.accept(BEAST_EGG.get());
        }
    }
}
