package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.TransmutatorBlock;
import dev.abyssalport.block.entity.TransmutatorBlockEntity;
import dev.abyssalport.menu.TransmutatorMenu;
import dev.abyssalport.recipe.TransmutationRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WastelandProcessingContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,AbyssalPort.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,AbyssalPort.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,AbyssalPort.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block,Block> STONE=BLOCKS.register("coralium_stone",()->new Block(stone()));
    public static final DeferredHolder<Block,Block> COBBLESTONE=BLOCKS.register("coralium_cobblestone",()->new Block(stone()));
    public static final DeferredHolder<Block,TransmutatorBlock> TRANSMUTATOR=BLOCKS.register("transmutator",()->new TransmutatorBlock(
            BlockBehaviour.Properties.of().strength(2.5F,12).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state->state.getValue(TransmutatorBlock.LIT)?14:0)));
    public static final DeferredHolder<Item,BlockItem> STONE_ITEM=ITEMS.register("coralium_stone",()->new BlockItem(STONE.get(),new Item.Properties()));
    public static final DeferredHolder<Item,BlockItem> COBBLESTONE_ITEM=ITEMS.register("coralium_cobblestone",()->new BlockItem(COBBLESTONE.get(),new Item.Properties()));
    public static final DeferredHolder<Item,BlockItem> MACHINE_ITEM=ITEMS.register("transmutator",()->new BlockItem(TRANSMUTATOR.get(),new Item.Properties()));
    public static final DeferredHolder<Item,Item> BRICK=ITEMS.register("coralium_brick",()->new Item(new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<TransmutatorBlockEntity>> ENTITY=ENTITIES.register("transmutator",()->
            BlockEntityType.Builder.of(TransmutatorBlockEntity::new,TRANSMUTATOR.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<TransmutatorMenu>> MENU=MENUS.register("transmutator",()->new MenuType<>(TransmutatorMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>,RecipeType<TransmutationRecipe>> TRANSMUTATION=TYPES.register("transmutation",()->new RecipeType<>() {
        @Override public String toString() { return "abyssalport:transmutation"; }
    });
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<TransmutationRecipe>> SERIALIZER=SERIALIZERS.register("transmutation",TransmutationRecipe.Serializer::new);
    private WastelandProcessingContent() { }
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.of().strength(1.5F,10).requiresCorrectToolForDrops().sound(SoundType.STONE); }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); TYPES.register(bus); SERIALIZERS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event)->event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,ENTITY.get(),(be,side)->
                        side==null?new net.neoforged.neoforge.items.wrapper.InvWrapper(be):new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be,side)));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==CreativeModeTabs.BUILDING_BLOCKS) { event.accept(STONE_ITEM.get()); event.accept(COBBLESTONE_ITEM.get()); }
            if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(MACHINE_ITEM.get());
            if(event.getTabKey()==CreativeModeTabs.INGREDIENTS) event.accept(BRICK.get());
        });
    }
}
