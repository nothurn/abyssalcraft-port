package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.GatewayAnchorBlock;
import dev.abyssalport.block.GatewayPortalBlock;
import dev.abyssalport.block.MonolithPillarBlock;
import dev.abyssalport.block.entity.GatewayAnchorBlockEntity;
import dev.abyssalport.item.GatewayKeyItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GatewayContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block, GatewayAnchorBlock> ANCHOR = BLOCKS.register("gateway_anchor", () -> new GatewayAnchorBlock(
            BlockBehaviour.Properties.of().strength(5, 1200).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Block, GatewayPortalBlock> PORTAL = BLOCKS.register("gateway_portal", () -> new GatewayPortalBlock(
            BlockBehaviour.Properties.of().strength(-1, 3600000).noCollission().noOcclusion().noLootTable().lightLevel(state -> 8)));
    public static final DeferredHolder<Block, MonolithPillarBlock> MONOLITH_PILLAR = BLOCKS.register("monolith_pillar", () -> new MonolithPillarBlock(
            BlockBehaviour.Properties.of().strength(6, 24).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> ANCHOR_ITEM = ITEMS.register("gateway_anchor", () -> new BlockItem(ANCHOR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> PILLAR_ITEM = ITEMS.register("monolith_pillar", () -> new BlockItem(MONOLITH_PILLAR.get(), new Item.Properties()));
    public static final DeferredHolder<Item, GatewayKeyItem> KEY = ITEMS.register("gateway_key", () -> new GatewayKeyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GatewayAnchorBlockEntity>> ANCHOR_ENTITY = BLOCK_ENTITIES.register("gateway_anchor", () ->
            BlockEntityType.Builder.of(GatewayAnchorBlockEntity::new, ANCHOR.get()).build(null));
    private GatewayContent() { }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); BLOCK_ENTITIES.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(KEY.get());
            if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) { event.accept(ANCHOR_ITEM.get()); event.accept(PILLAR_ITEM.get()); }
        });
    }
}
