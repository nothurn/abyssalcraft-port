package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.RitualAltarBlock;
import dev.abyssalport.block.RitualPedestalBlock;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.item.RendingStaffItem;
import dev.abyssalport.recipe.BookUpgradeRecipe;
import dev.abyssalport.research.WastelandProgressionResearch;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Reachable original-inspired material -> skin -> first dimension book progression. */
public final class WastelandProgressionContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Item, RendingStaffItem> RENDING_STAFF = ITEMS.register("staff_of_rending", () ->
            new RendingStaffItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, RendingStaffItem> WASTELAND_STAFF = ITEMS.register("abyssal_wasteland_staff_of_rending", () ->
            new RendingStaffItem(new Item.Properties().stacksTo(1), 1));
    public static final DeferredHolder<Block, RitualAltarBlock> ALTAR = BLOCKS.register("abyssal_ritual_altar", () ->
            new RitualAltarBlock(BlockBehaviour.Properties.of().strength(6, 12).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Block, RitualPedestalBlock> PEDESTAL = BLOCKS.register("abyssal_ritual_pedestal", () ->
            new RitualPedestalBlock(BlockBehaviour.Properties.of().strength(6, 12).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> ALTAR_ITEM = ITEMS.register("abyssal_ritual_altar", () -> new BlockItem(ALTAR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> PEDESTAL_ITEM = ITEMS.register("abyssal_ritual_pedestal", () -> new BlockItem(PEDESTAL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> ESSENCE = ITEMS.register("abyssal_wasteland_essence", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SKIN = ITEMS.register("skin_of_the_abyssal_wasteland", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, NecronomiconItem> BOOK = ITEMS.register("abyssal_wasteland_necronomicon", () ->
            new NecronomiconItem(new Item.Properties().stacksTo(1), 1, 10_000));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BookUpgradeRecipe>> BOOK_UPGRADE =
            SERIALIZERS.register("book_upgrade", BookUpgradeRecipe.Serializer::new);

    private WastelandProgressionContent() { }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); SERIALIZERS.register(bus);
        bus.addListener(WastelandProgressionContent::creative);
        NeoForge.EVENT_BUS.addListener(WastelandProgressionResearch::onPickup);
        NeoForge.EVENT_BUS.addListener(WastelandProgressionResearch::onCrafted);
        NeoForge.EVENT_BUS.addListener(WastelandProgressionResearch::onSmelted);
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RENDING_STAFF.get()); event.accept(WASTELAND_STAFF.get()); event.accept(BOOK.get());
            event.accept(ALTAR_ITEM.get()); event.accept(PEDESTAL_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ESSENCE.get()); event.accept(SKIN.get());
        }
    }
}
