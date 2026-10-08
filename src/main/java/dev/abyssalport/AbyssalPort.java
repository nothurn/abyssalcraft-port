package dev.abyssalport;

import dev.abyssalport.block.PrototypeEffigyBlock;
import dev.abyssalport.block.EnergyCollectorBlock;
import dev.abyssalport.block.EnergyRelayBlock;
import dev.abyssalport.block.EnergyContainerBlock;
import dev.abyssalport.block.RitualAltarBlock;
import dev.abyssalport.block.RitualPedestalBlock;
import dev.abyssalport.block.ShoggothOozeBlock;
import dev.abyssalport.block.entity.PrototypeEffigyBlockEntity;
import dev.abyssalport.block.entity.EnergyCollectorBlockEntity;
import dev.abyssalport.block.entity.EnergyRelayBlockEntity;
import dev.abyssalport.block.entity.EnergyContainerBlockEntity;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.client.AbyssalPortClient;
import dev.abyssalport.content.AbyssalniteContent;
import dev.abyssalport.content.CoraliumContent;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandMineralsContent;
import dev.abyssalport.content.WastelandMobsContent;
import dev.abyssalport.content.GatewayContent;
import dev.abyssalport.entity.ShoggothEntity;
import dev.abyssalport.entity.ShadowCreatureEntity;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.item.EnergyStorageItem;
import dev.abyssalport.menu.EnergyContainerMenu;
import dev.abyssalport.network.BookOpenPayload;
import dev.abyssalport.ritual.RitualCatalog;
import dev.abyssalport.research.ShadowMaterialResearch;
import dev.abyssalport.research.DarklandsResearch;
import dev.abyssalport.worldgen.AbandonedMonolithFeature;
import dev.abyssalport.worldgen.DarklandsWorldgen;
import dev.abyssalport.worldgen.ShadowGroveFeature;
import net.minecraft.core.registries.Registries;
import com.mojang.serialization.Codec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(AbyssalPort.MOD_ID)
public final class AbyssalPort {
    public static final String MOD_ID = "abyssalport";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> EARLY_RESEARCH =
            ATTACHMENTS.register("early_research", () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT).copyOnDeath().build());

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyContainerMenu>> ENERGY_CONTAINER_MENU =
            MENUS.register("energy_container", () -> new MenuType<>(EnergyContainerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<EntityType<?>, EntityType<ShoggothEntity>> LESSER_SHOGGOTH =
            ENTITIES.register("lesser_shoggoth", () -> EntityType.Builder.<ShoggothEntity>of(ShoggothEntity::new, MobCategory.MONSTER)
                    .sized(.9F, 1.3F).clientTrackingRange(8).build("lesser_shoggoth"));
    public static final DeferredHolder<EntityType<?>, EntityType<ShadowCreatureEntity>> SHADOW_CREATURE =
            ENTITIES.register("shadow_creature", () -> EntityType.Builder.<ShadowCreatureEntity>of(ShadowCreatureEntity::new, MobCategory.MONSTER)
                    .sized(0.5F, 1.0F).fireImmune().clientTrackingRange(8).build("shadow_creature"));

    public static final DeferredHolder<Feature<?>, AbandonedMonolithFeature> ABANDONED_MONOLITH =
            FEATURES.register("abandoned_monolith", AbandonedMonolithFeature::new);
    public static final DeferredHolder<Feature<?>, ShadowGroveFeature> SHADOW_GROVE =
            FEATURES.register("shadow_grove", ShadowGroveFeature::new);

    public static final DeferredHolder<Block, Block> DARKSTONE = BLOCKS.register("darkstone",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredHolder<Block, Block> DARKSTONE_COBBLESTONE = BLOCKS.register("darkstone_cobblestone",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.2F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredHolder<Block, Block> DARKSTONE_BRICKS = BLOCKS.register("darkstone_bricks",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)));
    public static final DeferredHolder<Block, Block> CRACKED_DARKSTONE_BRICKS = BLOCKS.register("cracked_darkstone_bricks",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)));
    public static final DeferredHolder<Block, SlabBlock> DARKSTONE_BRICK_SLAB = BLOCKS.register("darkstone_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.of().strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)));
    public static final DeferredHolder<Block, StairBlock> DARKSTONE_BRICK_STAIRS = BLOCKS.register("darkstone_brick_stairs",
            () -> new StairBlock(DARKSTONE_BRICKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.of().strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)));
    public static final DeferredHolder<Block, Block> CHISELED_DARKSTONE_BRICKS = BLOCKS.register("chiseled_darkstone_bricks",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)));
    public static final DeferredHolder<Block, Block> DARKLANDS_SOIL = BLOCKS.register("darklands_soil",
            () -> new Block(BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.GRAVEL)));
    public static final DeferredHolder<Block, ShoggothOozeBlock> SHOGGOTH_OOZE = BLOCKS.register("shoggoth_ooze",
            () -> new ShoggothOozeBlock(BlockBehaviour.Properties.of().strength(1.0F)
                    .sound(SoundType.SLIME_BLOCK).noOcclusion().randomTicks()));
    public static final DeferredHolder<Item, BlockItem> DARKSTONE_ITEM = ITEMS.register("darkstone",
            () -> new BlockItem(DARKSTONE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> DARKSTONE_COBBLESTONE_ITEM = ITEMS.register("darkstone_cobblestone",
            () -> new BlockItem(DARKSTONE_COBBLESTONE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> DARKSTONE_BRICKS_ITEM = ITEMS.register("darkstone_bricks",
            () -> new BlockItem(DARKSTONE_BRICKS.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> CRACKED_DARKSTONE_BRICKS_ITEM = ITEMS.register("cracked_darkstone_bricks",
            () -> new BlockItem(CRACKED_DARKSTONE_BRICKS.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> DARKSTONE_BRICK_SLAB_ITEM = ITEMS.register("darkstone_brick_slab",
            () -> new BlockItem(DARKSTONE_BRICK_SLAB.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> DARKSTONE_BRICK_STAIRS_ITEM = ITEMS.register("darkstone_brick_stairs",
            () -> new BlockItem(DARKSTONE_BRICK_STAIRS.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> CHISELED_DARKSTONE_BRICKS_ITEM = ITEMS.register("chiseled_darkstone_bricks",
            () -> new BlockItem(CHISELED_DARKSTONE_BRICKS.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> DARKLANDS_SOIL_ITEM = ITEMS.register("darklands_soil",
            () -> new BlockItem(DARKLANDS_SOIL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, NecronomiconItem> NECRONOMICON = ITEMS.register("necronomicon",
            () -> new NecronomiconItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Block, PrototypeEffigyBlock> PROTOTYPE_EFFIGY = BLOCKS.register("prototype_effigy",
            () -> new PrototypeEffigyBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).noOcclusion()));
    public static final DeferredHolder<Item, BlockItem> PROTOTYPE_EFFIGY_ITEM = ITEMS.register("prototype_effigy",
            () -> new BlockItem(PROTOTYPE_EFFIGY.get(), new Item.Properties()));
    public static final DeferredHolder<Block, EnergyCollectorBlock> ENERGY_COLLECTOR = BLOCKS.register("energy_collector",
            () -> new EnergyCollectorBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).noOcclusion()));
    public static final DeferredHolder<Item, EnergyStorageItem> ENERGY_COLLECTOR_ITEM = ITEMS.register("energy_collector",
            () -> new EnergyStorageItem(ENERGY_COLLECTOR.get(), new Item.Properties(),
                    EnergyCollectorBlockEntity.CAPACITY));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCollectorBlockEntity>> ENERGY_COLLECTOR_ENTITY =
            BLOCK_ENTITIES.register("energy_collector", () -> BlockEntityType.Builder
                    .of(EnergyCollectorBlockEntity::new, dev.abyssalport.content.EnergyDeviceContent.blocks(dev.abyssalport.content.EnergyDeviceContent.Kind.COLLECTOR)).build(null));
    public static final DeferredHolder<Block, EnergyRelayBlock> ENERGY_RELAY = BLOCKS.register("energy_relay",
            () -> new EnergyRelayBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).noOcclusion()));
    public static final DeferredHolder<Item, EnergyStorageItem> ENERGY_RELAY_ITEM = ITEMS.register("energy_relay",
            () -> new EnergyStorageItem(ENERGY_RELAY.get(), new Item.Properties(),
                    EnergyRelayBlockEntity.CAPACITY));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyRelayBlockEntity>> ENERGY_RELAY_ENTITY =
            BLOCK_ENTITIES.register("energy_relay", () -> BlockEntityType.Builder
                    .of(EnergyRelayBlockEntity::new, dev.abyssalport.content.EnergyDeviceContent.blocks(dev.abyssalport.content.EnergyDeviceContent.Kind.RELAY)).build(null));
    public static final DeferredHolder<Block, EnergyContainerBlock> ENERGY_CONTAINER = BLOCKS.register("energy_container",
            () -> new EnergyContainerBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).noOcclusion()));
    public static final DeferredHolder<Item, EnergyStorageItem> ENERGY_CONTAINER_ITEM = ITEMS.register("energy_container",
            () -> new EnergyStorageItem(ENERGY_CONTAINER.get(), new Item.Properties(),
                    EnergyContainerBlockEntity.CAPACITY));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyContainerBlockEntity>> ENERGY_CONTAINER_ENTITY =
            BLOCK_ENTITIES.register("energy_container", () -> BlockEntityType.Builder
                    .of(EnergyContainerBlockEntity::new, dev.abyssalport.content.EnergyDeviceContent.blocks(dev.abyssalport.content.EnergyDeviceContent.Kind.CONTAINER)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PrototypeEffigyBlockEntity>> PROTOTYPE_EFFIGY_ENTITY =
            BLOCK_ENTITIES.register("prototype_effigy", () -> BlockEntityType.Builder
                    .of(PrototypeEffigyBlockEntity::new, PROTOTYPE_EFFIGY.get()).build(null));
    public static final DeferredHolder<Block, RitualAltarBlock> RITUAL_ALTAR = BLOCKS.register("ritual_altar",
            () -> new RitualAltarBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredHolder<Item, BlockItem> RITUAL_ALTAR_ITEM = ITEMS.register("ritual_altar",
            () -> new BlockItem(RITUAL_ALTAR.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualAltarBlockEntity>> RITUAL_ALTAR_ENTITY =
            BLOCK_ENTITIES.register("ritual_altar", () -> BlockEntityType.Builder
                    .of(RitualAltarBlockEntity::new, RITUAL_ALTAR.get(), dev.abyssalport.content.WastelandProgressionContent.ALTAR.get(),
                            dev.abyssalport.content.DreadlandsProgressionContent.ALTAR.get(),
                            dev.abyssalport.content.DarklandsStructuresContent.ALTAR.get()).build(null));
    public static final DeferredHolder<Block, RitualPedestalBlock> RITUAL_PEDESTAL = BLOCKS.register("ritual_pedestal",
            () -> new RitualPedestalBlock(BlockBehaviour.Properties.of().strength(2.5F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final DeferredHolder<Item, BlockItem> RITUAL_PEDESTAL_ITEM = ITEMS.register("ritual_pedestal",
            () -> new BlockItem(RITUAL_PEDESTAL.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RitualPedestalBlockEntity>> RITUAL_PEDESTAL_ENTITY =
            BLOCK_ENTITIES.register("ritual_pedestal", () -> BlockEntityType.Builder
                    .of(RitualPedestalBlockEntity::new, RITUAL_PEDESTAL.get(), dev.abyssalport.content.WastelandProgressionContent.PEDESTAL.get(),
                            dev.abyssalport.content.DreadlandsProgressionContent.PEDESTAL.get(),
                            dev.abyssalport.content.DarklandsStructuresContent.PEDESTAL.get()).build(null));
    public static final DeferredHolder<Item, Item> BASIC_SCROLL = ITEMS.register("basic_scroll",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> SHADOW_FRAGMENT = ITEMS.register("shadow_fragment",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SHADOW_SHARD = ITEMS.register("shadow_shard",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SHADOW_GEM = ITEMS.register("shadow_gem",
            () -> new Item(new Item.Properties()));

    public AbyssalPort(IEventBus modBus, net.neoforged.fml.ModContainer modContainer) {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON,dev.abyssalport.config.MobEcologyConfig.SPEC);
        AbyssalniteContent.register(modBus);
        CoraliumContent.register(modBus);
        WastelandContent.register(modBus);
        WastelandMineralsContent.register(modBus);
        WastelandMobsContent.register(modBus);
        dev.abyssalport.content.WastelandProgressionContent.register(modBus);
        dev.abyssalport.content.WastelandBossContent.register(modBus);
        dev.abyssalport.content.WastelandStructuresContent.register(modBus);
        dev.abyssalport.content.DreadlandsContent.register(modBus);
        dev.abyssalport.content.DreadlandsProgressionContent.register(modBus);
        dev.abyssalport.content.WastelandProcessingContent.register(modBus);
        dev.abyssalport.content.DreadProcessingContent.register(modBus);
        dev.abyssalport.content.CrystallizationContent.register(modBus);
        dev.abyssalport.content.CrystalBagContent.register(modBus);
        dev.abyssalport.content.StateTransformerContent.register(modBus);
        dev.abyssalport.content.DarklandsWoodContent.register(modBus);
        dev.abyssalport.content.DreadEcologyContent.register(modBus);
        dev.abyssalport.content.DarklandsStructuresContent.register(modBus);
        dev.abyssalport.content.GraveyardContent.register(modBus);
        dev.abyssalport.content.ShoggothLairContent.register(modBus);
        dev.abyssalport.content.ShoggothEcologyContent.register(modBus);
        dev.abyssalport.content.StatueContent.register(modBus);
        dev.abyssalport.content.CharmContent.register(modBus);
        dev.abyssalport.content.EnergyDeviceContent.register(modBus);
        dev.abyssalport.energy.PEStreams.register(modBus);
        dev.abyssalport.content.SacrificialAltarContent.register(modBus);
        dev.abyssalport.content.PlaceOfPowerContent.register(modBus);
        dev.abyssalport.content.ShadowFamilyContent.register(modBus);
        dev.abyssalport.content.MobEcologyContent.register(modBus);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, dev.abyssalport.entity.DemonFire::onPlayerDeath);
        NeoForge.EVENT_BUS.addListener(dev.abyssalport.entity.ShadowFamily::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, dev.abyssalport.entity.ShadowFamily::onDeath);
        GatewayContent.register(modBus);
        NeoForge.EVENT_BUS.addListener(this::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener(ShadowMaterialResearch::onPickup);
        NeoForge.EVENT_BUS.addListener(dev.abyssalport.research.GhoulResearch::onPickup);
        NeoForge.EVENT_BUS.addListener(ShadowMaterialResearch::onCrafted);
        NeoForge.EVENT_BUS.addListener(DarklandsResearch::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(dev.abyssalport.research.GatewayResearch::onPickup);
        NeoForge.EVENT_BUS.addListener(dev.abyssalport.research.GatewayResearch::onCrafted);
        NeoForge.EVENT_BUS.addListener(dev.abyssalport.research.GatewayResearch::onPlayerTick);
        BLOCKS.register(modBus);
        // Its Darkstone stairs need the actual base Darkstone registry entry first.
        dev.abyssalport.content.EnergyDepositionerContent.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        FEATURES.register(modBus);
        ENTITIES.register(modBus);
        MENUS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(this::addCreativeItems);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::registerSpawnPlacements);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(DarklandsWorldgen::onCommonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(AbyssalPortClient::registerRenderers);
            modBus.addListener(AbyssalPortClient::registerLayers);
            modBus.addListener(AbyssalPortClient::registerScreens);
            modBus.addListener(AbyssalPortClient::registerParticles);
            modBus.addListener(AbyssalPortClient::registerFluidExtensions);
            modBus.addListener(AbyssalPortClient::setupCutouts);
        }
    }

    private void registerReloadListeners(AddReloadListenerEvent event) {
        event.addListener(RitualCatalog.INSTANCE);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LESSER_SHOGGOTH.get(), ShoggothEntity.createAttributes().build());
        event.put(SHADOW_CREATURE.get(), ShadowCreatureEntity.createAttributes().build());
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            event.registrar("1").playToClient(BookOpenPayload.TYPE, BookOpenPayload.STREAM_CODEC,
                    AbyssalPortClient::receiveBookOpen);
            event.registrar("1").optional().playToClient(dev.abyssalport.network.PEStreamPayload.TYPE, dev.abyssalport.network.PEStreamPayload.STREAM_CODEC,
                    AbyssalPortClient::receivePEStream);
        } else {
            event.registrar("1").playToClient(BookOpenPayload.TYPE, BookOpenPayload.STREAM_CODEC,
                    (payload, context) -> { });
            event.registrar("1").optional().playToClient(dev.abyssalport.network.PEStreamPayload.TYPE, dev.abyssalport.network.PEStreamPayload.STREAM_CODEC,
                    (payload, context) -> { });
        }
    }

    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(LESSER_SHOGGOTH.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SHADOW_CREATURE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ShadowCreatureEntity::checkShadowSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(DARKSTONE_ITEM.get());
            event.accept(DARKSTONE_COBBLESTONE_ITEM.get());
            event.accept(DARKSTONE_BRICKS_ITEM.get());
            event.accept(CRACKED_DARKSTONE_BRICKS_ITEM.get());
            event.accept(DARKSTONE_BRICK_SLAB_ITEM.get());
            event.accept(DARKSTONE_BRICK_STAIRS_ITEM.get());
            event.accept(CHISELED_DARKSTONE_BRICKS_ITEM.get());
            event.accept(DARKLANDS_SOIL_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(NECRONOMICON.get());
            event.accept(PROTOTYPE_EFFIGY_ITEM.get());
            event.accept(ENERGY_COLLECTOR_ITEM.get());
            event.accept(ENERGY_RELAY_ITEM.get());
            event.accept(ENERGY_CONTAINER_ITEM.get());
            event.accept(RITUAL_ALTAR_ITEM.get());
            event.accept(RITUAL_PEDESTAL_ITEM.get());
            event.accept(BASIC_SCROLL.get());
            event.accept(SHADOW_FRAGMENT.get());
            event.accept(SHADOW_SHARD.get());
            event.accept(SHADOW_GEM.get());
        }
    }
}
