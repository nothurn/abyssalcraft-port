package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.StateTransformerBlock;
import dev.abyssalport.block.entity.StateTransformerBlockEntity;
import dev.abyssalport.item.StoneTabletItem;
import dev.abyssalport.menu.StateTransformerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.*;

public final class StateTransformerContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AbyssalPort.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block, StateTransformerBlock> BLOCK = BLOCKS.register("state_transformer", () ->
            new StateTransformerBlock(BlockBehaviour.Properties.of().strength(2, 4).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> MACHINE = ITEMS.register("state_transformer", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, StoneTabletItem> TABLET = ITEMS.register("stone_tablet", () -> new StoneTabletItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StateTransformerBlockEntity>> ENTITY = ENTITIES.register("state_transformer", () ->
            BlockEntityType.Builder.of(StateTransformerBlockEntity::new, BLOCK.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<StateTransformerMenu>> MENU = MENUS.register("state_transformer", () ->
            new MenuType<>(StateTransformerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    private StateTransformerContent() { }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) -> event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, ENTITY.get(), (be, side) -> side == null ? null : new SidedInvWrapper(be, side)));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(MACHINE.get());
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(TABLET.get());
        });
    }
}
