package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.RitualAltarBlock;
import dev.abyssalport.block.RitualPedestalBlock;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Source-informed Darklands building materials; geometry/media are independently authored. */
public final class DarklandsStructuresContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block,Block> GLOWING_BRICKS=BLOCKS.register("glowing_darkstone_bricks",
            ()->new Block(BlockBehaviour.Properties.of().strength(55,3000).requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(state->15)));
    public static final DeferredHolder<Block,RitualAltarBlock> ALTAR=BLOCKS.register("darkstone_ritual_altar",()->new RitualAltarBlock(stone()));
    public static final DeferredHolder<Block,RitualPedestalBlock> PEDESTAL=BLOCKS.register("darkstone_ritual_pedestal",()->new RitualPedestalBlock(stone().noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> GLOWING_ITEM=item("glowing_darkstone_bricks",GLOWING_BRICKS);
    public static final DeferredHolder<Item,BlockItem> ALTAR_ITEM=item("darkstone_ritual_altar",ALTAR);
    public static final DeferredHolder<Item,BlockItem> PEDESTAL_ITEM=item("darkstone_ritual_pedestal",PEDESTAL);
    public static final List<String> PLANS=List.of("house_small","house_large","ritual_ground","ritual_ground_columns",
            "circular_shrine","circular_shrine_columns","elevated_shrine","elevated_shrine_large","memorial_cross","memorial_arch");
    public static final ResourceKey<Structure> RUINS=ResourceKey.create(Registries.STRUCTURE,WastelandContent.id("darklands_ruins"));
    private DarklandsStructuresContent() { }
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.of().strength(3,6).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE); }
    private static DeferredHolder<Item,BlockItem> item(String name,DeferredHolder<Block,? extends Block> block) {
        return ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()));
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)-> {
            if(event.getTabKey()==CreativeModeTabs.BUILDING_BLOCKS) event.accept(GLOWING_ITEM.get());
            if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS) { event.accept(ALTAR_ITEM.get()); event.accept(PEDESTAL_ITEM.get()); }
        });
    }
}
