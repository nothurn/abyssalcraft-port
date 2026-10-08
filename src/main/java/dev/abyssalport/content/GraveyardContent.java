package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.TombstoneBlock;
import dev.abyssalport.block.GhoulHeadBlock;
import dev.abyssalport.block.entity.TombstoneBlockEntity;
import dev.abyssalport.entity.GhoulEntity;
import dev.abyssalport.item.GhoulFleshItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.*;

/** Source-informed grave content for the three currently reachable worlds. */
public final class GraveyardContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,AbyssalPort.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<GhoulEntity>> GHOUL=entity("ghoul",GhoulEntity.Kind.NORMAL);
    public static final DeferredHolder<EntityType<?>,EntityType<GhoulEntity>> SHADOW_GHOUL=entity("shadow_ghoul",GhoulEntity.Kind.SHADOW);
    public static final DeferredHolder<EntityType<?>,EntityType<GhoulEntity>> DEPTHS_GHOUL=entity("depths_ghoul",GhoulEntity.Kind.DEPTHS);
    public static final DeferredHolder<EntityType<?>,EntityType<GhoulEntity>> DREADED_GHOUL=entity("dreaded_ghoul",GhoulEntity.Kind.DREADED);
    public static final DeferredHolder<Block,TombstoneBlock> STONE=stone("stone_tombstone");
    public static final DeferredHolder<Block,TombstoneBlock> DARKSTONE=stone("darkstone_tombstone");
    public static final DeferredHolder<Block,TombstoneBlock> ABYSSAL_STONE=stone("abyssal_stone_tombstone");
    public static final DeferredHolder<Block,TombstoneBlock> CORALIUM_STONE=stone("coralium_stone_tombstone");
    public static final DeferredHolder<Block,TombstoneBlock> DREADSTONE=stone("dreadstone_tombstone");
    public static final DeferredHolder<Block,TombstoneBlock> ELYSIAN_STONE=stone("elysian_stone_tombstone");
    public static final DeferredHolder<Block,GhoulHeadBlock> DEPTHS_HEAD=head("depths_ghoul_head");
    public static final DeferredHolder<Block,GhoulHeadBlock> PETE_HEAD=head("pete_head");
    public static final DeferredHolder<Block,GhoulHeadBlock> WILSON_HEAD=head("wilson_head");
    public static final DeferredHolder<Block,GhoulHeadBlock> ORANGE_HEAD=head("orange_head");
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TombstoneBlockEntity>> TOMBSTONE_ENTITY=BLOCK_ENTITIES.register("tombstone",()->
            BlockEntityType.Builder.of(TombstoneBlockEntity::new,STONE.get(),DARKSTONE.get(),ABYSSAL_STONE.get(),CORALIUM_STONE.get(),DREADSTONE.get(),ELYSIAN_STONE.get()).build(null));
    public static final DeferredHolder<Item,GhoulFleshItem> FLESH=flesh("ghoul_flesh",GhoulEntity.Kind.NORMAL);
    public static final DeferredHolder<Item,GhoulFleshItem> SHADOW_FLESH=flesh("shadow_ghoul_flesh",GhoulEntity.Kind.SHADOW);
    public static final DeferredHolder<Item,GhoulFleshItem> DEPTHS_FLESH=flesh("depths_ghoul_flesh",GhoulEntity.Kind.DEPTHS);
    public static final DeferredHolder<Item,GhoulFleshItem> DREADED_FLESH=flesh("dreaded_ghoul_flesh",GhoulEntity.Kind.DREADED);
    public static final DeferredHolder<Item,Item> SHOGGOTH_FLESH=ITEMS.register("overworld_shoggoth_flesh",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Item,Item> ABYSSAL_SHOGGOTH_FLESH=ITEMS.register("abyssal_shoggoth_flesh",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Item,Item> DREADED_SHOGGOTH_FLESH=ITEMS.register("dreaded_shoggoth_flesh",()->new Item(new Item.Properties()));
    public static final ResourceKey<Structure> GRAVEYARD=ResourceKey.create(Registries.STRUCTURE,WastelandContent.id("graveyard"));
    public static final ResourceKey<Structure> DARKLANDS_GRAVEYARD=ResourceKey.create(Registries.STRUCTURE,WastelandContent.id("darklands_graveyard"));
    public static final ResourceKey<Structure> WASTELAND_GRAVEYARD=ResourceKey.create(Registries.STRUCTURE,WastelandContent.id("wasteland_graveyard"));
    public static final ResourceKey<Structure> DREADLANDS_GRAVEYARD=ResourceKey.create(Registries.STRUCTURE,WastelandContent.id("dreadlands_graveyard"));
    private static DeferredHolder<EntityType<?>,EntityType<GhoulEntity>> entity(String name,GhoulEntity.Kind kind) {
        var entity=ENTITIES.register(name,()-> {
            var builder=EntityType.Builder.<GhoulEntity>of((type,level)->new GhoulEntity(type,level,kind),MobCategory.MONSTER).sized(.9F,1.7F).clientTrackingRange(8);
            if(kind==GhoulEntity.Kind.DREADED) builder.fireImmune();
            return builder.build(name);
        });
        ITEMS.register(name+"_spawn_egg",()->new DeferredSpawnEggItem(entity,kind==GhoulEntity.Kind.SHADOW?0x171821:kind==GhoulEntity.Kind.DREADED?0x4d202b:0x6b7253,
                kind==GhoulEntity.Kind.DEPTHS?0x6ea496:0xcecbbb,new Item.Properties()));
        return entity;
    }
    private static DeferredHolder<Block,TombstoneBlock> stone(String name) {
        var stone=BLOCKS.register(name,()->new TombstoneBlock(BlockBehaviour.Properties.of().strength(2.5F,20).sound(SoundType.STONE).noOcclusion()));
        ITEMS.register(name,()->new BlockItem(stone.get(),new Item.Properties()));
        return stone;
    }
    private static DeferredHolder<Block,GhoulHeadBlock> head(String name) {
        var head=BLOCKS.register(name,()->new GhoulHeadBlock(BlockBehaviour.Properties.of().strength(1,6).sound(SoundType.WOOL).noOcclusion()));
        ITEMS.register(name,()->new BlockItem(head.get(),new Item.Properties().stacksTo(1)));
        return head;
    }
    public static EntityType<GhoulEntity> raisedBy(net.minecraft.world.level.block.state.BlockState state) {
        if(state.is(DARKSTONE.get())) return SHADOW_GHOUL.get();
        if(state.is(ABYSSAL_STONE.get())||state.is(CORALIUM_STONE.get())) return DEPTHS_GHOUL.get();
        if(state.is(DREADSTONE.get())||state.is(ELYSIAN_STONE.get())) return DREADED_GHOUL.get();
        return GHOUL.get();
    }
    private static DeferredHolder<Item,GhoulFleshItem> flesh(String name,GhoulEntity.Kind kind) {
        return ITEMS.register(name,()->new GhoulFleshItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(.1F).build()),kind));
    }
    private GraveyardContent() { }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus); BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus);
        bus.addListener(GraveyardContent::attributes); bus.addListener(GraveyardContent::placements); bus.addListener(GraveyardContent::creative);
    }
    private static void attributes(EntityAttributeCreationEvent event) {
        for(var entry:java.util.Map.of(GHOUL,GhoulEntity.Kind.NORMAL,SHADOW_GHOUL,GhoulEntity.Kind.SHADOW,DEPTHS_GHOUL,GhoulEntity.Kind.DEPTHS,DREADED_GHOUL,GhoulEntity.Kind.DREADED).entrySet())
            event.put(entry.getKey().get(),GhoulEntity.createAttributes(entry.getValue()).build());
    }
    private static void placements(RegisterSpawnPlacementsEvent event) {
        for(var entity:java.util.List.of(GHOUL.get(),SHADOW_GHOUL.get(),DEPTHS_GHOUL.get(),DREADED_GHOUL.get())) event.register(entity,SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey()==CreativeModeTabs.FOOD_AND_DRINKS) for(var flesh:java.util.List.of(FLESH,SHADOW_FLESH,DEPTHS_FLESH,DREADED_FLESH)) event.accept(flesh.get());
        if(event.getTabKey()==CreativeModeTabs.INGREDIENTS) for(var flesh:java.util.List.of(SHOGGOTH_FLESH,ABYSSAL_SHOGGOTH_FLESH,DREADED_SHOGGOTH_FLESH)) event.accept(flesh.get());
        if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS) for(var entry:BLOCKS.getEntries()) event.accept(entry.get().asItem());
        if(event.getTabKey()==CreativeModeTabs.SPAWN_EGGS) for(var entry:ITEMS.getEntries()) if(entry.get() instanceof DeferredSpawnEggItem) event.accept(entry.get());
    }
}
