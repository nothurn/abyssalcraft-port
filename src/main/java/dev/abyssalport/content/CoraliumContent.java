package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.item.TransmutationGemItem;
import dev.abyssalport.research.CoraliumResearch;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Source-informed early ocean/swamp mineral progression, with independently drawn media. */
public final class CoraliumContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block, Block> ORE = BLOCKS.register("coralium_ore", () ->
            new DropExperienceBlock(UniformInt.of(0, 2), properties()));
    public static final DeferredHolder<Block, Block> INFUSED_STONE = BLOCKS.register("coralium_infused_stone", () ->
            new DropExperienceBlock(UniformInt.of(2, 5), properties()));
    public static final DeferredHolder<Item, BlockItem> ORE_ITEM = ITEMS.register("coralium_ore", () ->
            new BlockItem(ORE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> INFUSED_STONE_ITEM = ITEMS.register("coralium_infused_stone", () ->
            new BlockItem(INFUSED_STONE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> GEM = ITEMS.register("coralium_gem", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PEARL = ITEMS.register("coralium_pearl", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLUSTER_2 = ITEMS.register("coralium_gem_cluster_2", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLUSTER_3 = ITEMS.register("coralium_gem_cluster_3", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLUSTER_4 = ITEMS.register("coralium_gem_cluster_4", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLUSTER_5 = ITEMS.register("coralium_gem_cluster_5", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLUSTER_6 = ITEMS.register("coralium_gem_cluster_6", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLUSTER_7 = ITEMS.register("coralium_gem_cluster_7", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLUSTER_8 = ITEMS.register("coralium_gem_cluster_8", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLUSTER_9 = ITEMS.register("coralium_gem_cluster_9", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, TransmutationGemItem> TRANSMUTATION_GEM = ITEMS.register("transmutation_gem", () ->
            new TransmutationGemItem(new Item.Properties().durability(10)));
    public static final DeferredHolder<Item, Item> OBLIVION_SHARD = ITEMS.register("oblivion_shard", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> OBLIVION_CATALYST = ITEMS.register("oblivion_catalyst", () -> new Item(new Item.Properties()));

    private CoraliumContent() { }
    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(CoraliumContent::creativeItems);
        NeoForge.EVENT_BUS.addListener(CoraliumResearch::onPickup);
        NeoForge.EVENT_BUS.addListener(CoraliumResearch::onCrafted);
        NeoForge.EVENT_BUS.addListener(CoraliumResearch::onSmelted);
    }

    private static void creativeItems(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) event.accept(ORE_ITEM.get());
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(INFUSED_STONE_ITEM.get());
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            for (var item : new Item[]{GEM.get(), PEARL.get(), CLUSTER_2.get(), CLUSTER_3.get(), CLUSTER_4.get(),
                    CLUSTER_5.get(), CLUSTER_6.get(), CLUSTER_7.get(), CLUSTER_8.get(), CLUSTER_9.get(), OBLIVION_SHARD.get(), OBLIVION_CATALYST.get()}) event.accept(item);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(TRANSMUTATION_GEM.get());
    }
}
