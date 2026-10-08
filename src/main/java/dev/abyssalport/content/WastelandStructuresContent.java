package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.DreadlandsPowerstoneBlock;
import dev.abyssalport.item.PowerstoneTrackerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native datapack stronghold and its Survival building/mining materials. */
public final class WastelandStructuresContent {
    public static final ResourceKey<Structure> STRONGHOLD = ResourceKey.create(Registries.STRUCTURE, WastelandContent.id("wasteland_stronghold"));
    public static final TagKey<Structure> TRACKER_TARGETS = TagKey.create(Registries.STRUCTURE, WastelandContent.id("powerstone_tracker_located"));
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block, Block> BRICKS = BLOCKS.register("abyssal_stone_bricks", () -> new Block(stone()));
    public static final DeferredHolder<Block, Block> CRACKED_BRICKS = BLOCKS.register("cracked_abyssal_stone_bricks", () -> new Block(stone()));
    public static final DeferredHolder<Block, Block> CHISELED_BRICKS = BLOCKS.register("chiseled_abyssal_stone_bricks", () -> new Block(stone()));
    public static final DeferredHolder<Block, SlabBlock> SLAB = BLOCKS.register("abyssal_stone_brick_slab", () -> new SlabBlock(stone()));
    public static final DeferredHolder<Block, StairBlock> STAIRS = BLOCKS.register("abyssal_stone_brick_stairs", () -> new StairBlock(BRICKS.get().defaultBlockState(), stone()));
    public static final DeferredHolder<Block, FenceBlock> FENCE = BLOCKS.register("abyssal_stone_brick_fence", () -> new FenceBlock(stone()));
    public static final DeferredHolder<Block, DreadlandsPowerstoneBlock> POWERSTONE = BLOCKS.register("dreadlands_infused_powerstone", () -> new DreadlandsPowerstoneBlock(
            BlockBehaviour.Properties.of().strength(50, 3000).requiresCorrectToolForDrops().noOcclusion().sound(SoundType.STONE)));
    public static final DeferredHolder<Item, BlockItem> BRICKS_ITEM = item("abyssal_stone_bricks", BRICKS);
    public static final DeferredHolder<Item, BlockItem> CRACKED_ITEM = item("cracked_abyssal_stone_bricks", CRACKED_BRICKS);
    public static final DeferredHolder<Item, BlockItem> CHISELED_ITEM = item("chiseled_abyssal_stone_bricks", CHISELED_BRICKS);
    public static final DeferredHolder<Item, BlockItem> SLAB_ITEM = item("abyssal_stone_brick_slab", SLAB);
    public static final DeferredHolder<Item, BlockItem> STAIRS_ITEM = item("abyssal_stone_brick_stairs", STAIRS);
    public static final DeferredHolder<Item, BlockItem> FENCE_ITEM = item("abyssal_stone_brick_fence", FENCE);
    public static final DeferredHolder<Item, BlockItem> POWERSTONE_ITEM = item("dreadlands_infused_powerstone", POWERSTONE);
    public static final DeferredHolder<Item, PowerstoneTrackerItem> TRACKER = ITEMS.register("powerstone_tracker", () -> new PowerstoneTrackerItem(new Item.Properties()));
    private WastelandStructuresContent() { }
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.of().strength(3, 6).requiresCorrectToolForDrops().sound(SoundType.STONE); }
    private static DeferredHolder<Item, BlockItem> item(String id, DeferredHolder<Block, ? extends Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); bus.addListener(WastelandStructuresContent::creativeItems);
    }
    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (var item : java.util.List.of(BRICKS_ITEM, CRACKED_ITEM, CHISELED_ITEM, SLAB_ITEM, STAIRS_ITEM, FENCE_ITEM)) event.accept(item.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(TRACKER.get());
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(POWERSTONE_ITEM.get());
    }
}
