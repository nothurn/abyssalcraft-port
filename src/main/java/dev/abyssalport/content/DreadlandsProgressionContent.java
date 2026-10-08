package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.RitualAltarBlock;
import dev.abyssalport.block.RitualPedestalBlock;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.item.RendingStaffItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The second reachable book/ritual tier; no creative-only higher-tier shortcut. */
public final class DreadlandsProgressionContent {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    public static final DeferredHolder<Item,Item> ESSENCE=ITEMS.register("dreadlands_essence",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Item,Item> SKIN=ITEMS.register("skin_of_the_dreadlands",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Item,NecronomiconItem> BOOK=ITEMS.register("dreadlands_necronomicon",()->
            new NecronomiconItem(new Item.Properties().stacksTo(1),2,20_000));
    public static final DeferredHolder<Item,RendingStaffItem> STAFF=ITEMS.register("dreadlands_staff_of_rending",()->
            new RendingStaffItem(new Item.Properties().stacksTo(1),2));
    public static final DeferredHolder<Block,RitualAltarBlock> ALTAR=BLOCKS.register("dreadlands_ritual_altar",()->
            new RitualAltarBlock(BlockBehaviour.Properties.of().strength(6,12).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Block,RitualPedestalBlock> PEDESTAL=BLOCKS.register("dreadlands_ritual_pedestal",()->
            new RitualPedestalBlock(BlockBehaviour.Properties.of().strength(6,12).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> ALTAR_ITEM=ITEMS.register("dreadlands_ritual_altar",()->new BlockItem(ALTAR.get(),new Item.Properties()));
    public static final DeferredHolder<Item,BlockItem> PEDESTAL_ITEM=ITEMS.register("dreadlands_ritual_pedestal",()->new BlockItem(PEDESTAL.get(),new Item.Properties()));
    private DreadlandsProgressionContent() { }
    public static void register(IEventBus bus) {
        ITEMS.register(bus); BLOCKS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==CreativeModeTabs.TOOLS_AND_UTILITIES) {
                event.accept(BOOK.get()); event.accept(STAFF.get()); event.accept(ALTAR_ITEM.get()); event.accept(PEDESTAL_ITEM.get());
            } else if(event.getTabKey()==CreativeModeTabs.INGREDIENTS) { event.accept(ESSENCE.get()); event.accept(SKIN.get()); }
        });
    }
}
