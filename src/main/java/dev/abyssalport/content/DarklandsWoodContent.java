package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.DarklandsLogBlock;
import dev.abyssalport.worldgen.DarklandsOakFeature;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Renewable source-informed Darklands Oak; independent pixels and native wood behaviours. */
public final class DarklandsWoodContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, AbyssalPort.MOD_ID);
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREE_KEY = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "darklands_oak"));
    private static final TreeGrower GROWER = new TreeGrower("abyssalport:darklands_oak", Optional.empty(), Optional.of(TREE_KEY), Optional.empty());
    public static final DeferredHolder<Feature<?>, DarklandsOakFeature> TREE = FEATURES.register("darklands_oak", DarklandsOakFeature::new);
    public static final DeferredHolder<Feature<?>, dev.abyssalport.worldgen.SparseDarklandsOakFeature> SPARSE_TREE =
            FEATURES.register("sparse_darklands_oak", dev.abyssalport.worldgen.SparseDarklandsOakFeature::new);
    public static final Map<String, DeferredHolder<Block, ? extends Block>> WOOD = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> BLOCK_ITEMS = new LinkedHashMap<>();
    public static final DeferredHolder<Block, DarklandsLogBlock> LOG = block("log", () -> new DarklandsLogBlock(logProperties()));
    public static final DeferredHolder<Block, DarklandsLogBlock> HOLLOW_LOG = block("hollow_log", () -> new DarklandsLogBlock(logProperties()));
    public static final DeferredHolder<Block, Block> PLANKS = block("planks", () -> new Block(woodProperties()));
    public static final DeferredHolder<Block, LeavesBlock> LEAVES = block("leaves", () -> new LeavesBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    public static final DeferredHolder<Block, SaplingBlock> SAPLING = block("sapling", () -> new SaplingBlock(GROWER,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    public static final DeferredHolder<Block, StairBlock> STAIRS = block("stairs", () -> new StairBlock(PLANKS.get().defaultBlockState(), woodProperties()));
    public static final DeferredHolder<Block, SlabBlock> SLAB = block("slab", () -> new SlabBlock(woodProperties()));
    public static final DeferredHolder<Block, FenceBlock> FENCE = block("fence", () -> new FenceBlock(woodProperties()));
    public static final DeferredHolder<Block, FenceGateBlock> GATE = block("fence_gate", () -> new FenceGateBlock(WoodType.OAK, woodProperties()));
    public static final DeferredHolder<Block, DoorBlock> DOOR = block("door", () -> new DoorBlock(BlockSetType.OAK,
            woodProperties().strength(3.0F, 15.0F).noOcclusion()));
    public static final DeferredHolder<Block, ButtonBlock> BUTTON = block("button", () -> new ButtonBlock(BlockSetType.OAK, 30,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)));
    public static final DeferredHolder<Block, PressurePlateBlock> PLATE = block("pressure_plate", () -> new PressurePlateBlock(BlockSetType.OAK,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)));

    private DarklandsWoodContent() { }
    private static BlockBehaviour.Properties logProperties() { return BlockBehaviour.Properties.of().strength(2.0F, 1.0F).sound(SoundType.WOOD); }
    private static BlockBehaviour.Properties woodProperties() { return BlockBehaviour.Properties.of().strength(2.0F, 5.0F).sound(SoundType.WOOD); }
    private static <T extends Block> DeferredHolder<Block, T> block(String suffix, Supplier<T> factory) {
        String name = "darklands_oak_" + suffix;
        var block = BLOCKS.register(name, factory);
        WOOD.put(suffix, block);
        BLOCK_ITEMS.put(suffix, ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
        return block;
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); FEATURES.register(bus);
        bus.addListener(DarklandsWoodContent::setup);
        bus.addListener(DarklandsWoodContent::creative);
    }
    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var fire = (FireBlock) Blocks.FIRE;
            for (var entry : WOOD.entrySet()) {
                String name = entry.getKey();
                if (name.equals("leaves")) fire.setFlammable(entry.getValue().get(), 30, 60);
                else if (name.endsWith("log")) fire.setFlammable(entry.getValue().get(), 5, 5);
                else if (!name.equals("sapling") && !name.equals("button") && !name.equals("pressure_plate") && !name.equals("door"))
                    fire.setFlammable(entry.getValue().get(), 5, 20);
            }
        });
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            WOOD.forEach((name, holder) -> { if (!name.equals("leaves") && !name.equals("sapling")) event.accept(holder.get()); });
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(LOG.get()); event.accept(HOLLOW_LOG.get()); event.accept(LEAVES.get()); event.accept(SAPLING.get());
        }
    }
}
