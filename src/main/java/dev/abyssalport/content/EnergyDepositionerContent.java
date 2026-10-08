package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.EnergyDepositionerBlock;
import dev.abyssalport.block.entity.EnergyDepositionerBlockEntity;
import dev.abyssalport.menu.EnergyDepositionerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.*;

/** Connected source Energy Depositioner, Shinoow, LGPL-3.0; independent media. */
public final class EnergyDepositionerContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AbyssalPort.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block, EnergyDepositionerBlock> BLOCK = BLOCKS.register("energy_depositioner", () ->
            new EnergyDepositionerBlock(BlockBehaviour.Properties.of().strength(2, 4).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> ITEM = ITEMS.register("energy_depositioner", () -> new BlockItem(BLOCK.get(), new Item.Properties().stacksTo(1)) {
        @Override public boolean canFitInsideContainerItems() { return false; }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
            var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            float pe = data == null ? 0 : data.copyTag().getFloat("PotEnergy");
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.abyssalport.stored_energy",
                    Float.isFinite(pe) ? (int)Math.clamp(pe, 0, 10000) : 0, 10000).withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.abyssalport.energy_depositioner").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
    });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyDepositionerBlockEntity>> ENTITY = ENTITIES.register("energy_depositioner", () ->
            BlockEntityType.Builder.of(EnergyDepositionerBlockEntity::new, BLOCK.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<EnergyDepositionerMenu>> MENU = MENUS.register("energy_depositioner", () ->
            new MenuType<>(EnergyDepositionerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<Block, SlabBlock> STONE_SLAB = BLOCKS.register("darkstone_slab", () -> new SlabBlock(stone()));
    public static final DeferredHolder<Block, SlabBlock> COBBLE_SLAB = BLOCKS.register("darkstone_cobblestone_slab", () -> new SlabBlock(stone()));
    public static final DeferredHolder<Block, StairBlock> COBBLE_STAIRS = BLOCKS.register("darkstone_cobblestone_stairs", () -> new StairBlock(AbyssalPort.DARKSTONE_COBBLESTONE.get().defaultBlockState(), stone()));
    public static final DeferredHolder<Block, WallBlock> COBBLE_WALL = BLOCKS.register("darkstone_cobblestone_wall", () -> new WallBlock(stone()));
    static {
        for (var block : java.util.List.of(STONE_SLAB, COBBLE_SLAB, COBBLE_STAIRS, COBBLE_WALL))
            ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }
    private EnergyDepositionerContent() { }
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.of().strength(1.5F, 10).requiresCorrectToolForDrops(); }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) -> event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, ENTITY.get(), (be, side) -> side == net.minecraft.core.Direction.UP || side == net.minecraft.core.Direction.DOWN ? new SidedInvWrapper(be, side) : null));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(ITEM.get());
            if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
                java.util.List.of(STONE_SLAB, COBBLE_SLAB, COBBLE_STAIRS, COBBLE_WALL).forEach(holder -> event.accept(holder.get()));
        });
    }
}
