package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.effect.DreadPlagueEffect;
import dev.abyssalport.entity.DreadlingEntity;
import dev.abyssalport.item.GatewayKeyItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Connected entry-stage registries. Later bosses, forests and machinery remain separate work. */
public final class DreadlandsContent {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, WastelandContent.id("dreadlands"));
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, AbyssalPort.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block, Block> STONE = block("dreadstone");
    public static final DeferredHolder<Block, Block> COBBLESTONE = block("dreadstone_cobblestone");
    public static final DeferredHolder<Block, Block> ELYSIAN_STONE = block("elysian_stone");
    public static final DeferredHolder<Block, Block> ELYSIAN_COBBLESTONE = block("elysian_cobblestone");
    public static final DeferredHolder<Block,Block> DIRT=BLOCKS.register("dreadlands_dirt",()->new Block(BlockBehaviour.Properties.of()
            .mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED).strength(.4F,2).sound(SoundType.GRAVEL)));
    public static final DeferredHolder<Block,dev.abyssalport.block.DreadGrassBlock> GRASS=BLOCKS.register("dreadlands_grass",()->new dev.abyssalport.block.DreadGrassBlock(BlockBehaviour.Properties.of()
            .mapColor(net.minecraft.world.level.material.MapColor.COLOR_RED).strength(.4F,2).sound(SoundType.GRASS).randomTicks()));
    public static final DeferredHolder<Item,BlockItem> DIRT_ITEM=ITEMS.register("dreadlands_dirt",()->new BlockItem(DIRT.get(),new Item.Properties()));
    public static final DeferredHolder<Item,BlockItem> GRASS_ITEM=ITEMS.register("dreadlands_grass",()->new BlockItem(GRASS.get(),new Item.Properties()));
    public static final DeferredHolder<Block, Block> DREADED_ORE = block("dreaded_abyssalnite_ore");
    public static final DeferredHolder<Block, Block> ABYSSALNITE_ORE = block("dreadlands_abyssalnite_ore");
    public static final DeferredHolder<Item, GatewayKeyItem> KEY = ITEMS.register("dreadlands_infused_gateway_key",
            () -> new GatewayKeyItem(new Item.Properties().stacksTo(1), 1));
    public static final DeferredHolder<Item, Item> FRAGMENT = ITEMS.register("dread_fragment", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<MobEffect, DreadPlagueEffect> PLAGUE = EFFECTS.register("dread_plague", DreadPlagueEffect::new);
    public static final DeferredHolder<EntityType<?>, EntityType<DreadlingEntity>> DREADLING = ENTITIES.register("dreadling", () ->
            EntityType.Builder.of(DreadlingEntity::new, MobCategory.MONSTER).sized(.8F, 1.5F).fireImmune().clientTrackingRange(8).build("dreadling"));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> EGG = ITEMS.register("dreadling_spawn_egg", () ->
            new DeferredSpawnEggItem(DREADLING, 0x61272C, 0xC2796B, new Item.Properties()));
    private DreadlandsContent() { }
    private static DeferredHolder<Block, Block> block(String name) {
        var holder = BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of().strength(2.5F, 20)
                .requiresCorrectToolForDrops().sound(SoundType.STONE)));
        ITEMS.register(name, () -> new BlockItem(holder.get(), new Item.Properties()));
        return holder;
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); EFFECTS.register(bus); ENTITIES.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(DREADLING.get(), DreadlingEntity.createAttributes().build()));
        bus.addListener((RegisterSpawnPlacementsEvent event) -> event.register(DREADLING.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DreadlingEntity::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS)
                for (var block : BLOCKS.getEntries()) event.accept(block.get());
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(KEY.get());
            if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(FRAGMENT.get());
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(EGG.get());
        });
    }
}
