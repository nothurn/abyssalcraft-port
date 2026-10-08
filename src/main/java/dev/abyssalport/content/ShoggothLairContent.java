package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.ShoggothBiomassBlock;
import dev.abyssalport.block.entity.ShoggothBiomassBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

/** Native early-survival lairs; the missing higher ecology is not a decorative substitute. */
public final class ShoggothLairContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block,Block> MONOLITH_STONE=BLOCKS.register("monolith_stone",()->new Block(
            BlockBehaviour.Properties.of().strength(6,24).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Item,BlockItem> MONOLITH_STONE_ITEM=ITEMS.register("monolith_stone",()->new BlockItem(MONOLITH_STONE.get(),new Item.Properties()));
    public static final DeferredHolder<Block,ShoggothBiomassBlock> BIOMASS=BLOCKS.register("shoggoth_biomass",()->
            new ShoggothBiomassBlock(BlockBehaviour.Properties.of().strength(1,18).sound(SoundType.SAND).lightLevel(state->7).noOcclusion()));
    public static final DeferredHolder<Item,BlockItem> BIOMASS_ITEM=ITEMS.register("shoggoth_biomass",()->new BlockItem(BIOMASS.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ShoggothBiomassBlockEntity>> BIOMASS_ENTITY=BLOCK_ENTITIES.register("shoggoth_biomass",()->
            BlockEntityType.Builder.of(ShoggothBiomassBlockEntity::new,BIOMASS.get()).build(null));
    public static final ResourceKey<Structure> LAIR=ResourceKey.create(Registries.STRUCTURE,WastelandContent.id("shoggoth_lair"));
    public static final ResourceKey<Structure> WASTELAND_LAIR=ResourceKey.create(Registries.STRUCTURE,WastelandContent.id("wasteland_shoggoth_lair"));
    public static final ResourceKey<Structure> DREADLANDS_LAIR=ResourceKey.create(Registries.STRUCTURE,WastelandContent.id("dreadlands_shoggoth_lair"));
    private ShoggothLairContent() { }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus); bus.addListener(ShoggothLairContent::creative);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(BIOMASS_ITEM.get());
        if(event.getTabKey()==CreativeModeTabs.BUILDING_BLOCKS) event.accept(MONOLITH_STONE_ITEM.get());
    }
}
