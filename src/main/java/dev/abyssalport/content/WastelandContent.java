package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.CoraliumLiquidBlock;
import dev.abyssalport.effect.CoraliumPlagueEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** First dimension's native registries. Its structures, full ores and mob families are separate work. */
public final class WastelandContent {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, id("abyssal_wasteland"));
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, AbyssalPort.MOD_ID);
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, AbyssalPort.MOD_ID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block, Block> STONE = BLOCKS.register("abyssal_stone", () -> new Block(
            BlockBehaviour.Properties.of().strength(3, 6).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Block, Block> COBBLESTONE = BLOCKS.register("abyssal_cobblestone", () -> new Block(
            BlockBehaviour.Properties.of().strength(3, 6).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Block, Block> SAND = BLOCKS.register("abyssal_sand", () -> new Block(
            BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.SAND)));
    public static final DeferredHolder<Block, Block> FUSED_SAND = BLOCKS.register("fused_abyssal_sand", () -> new Block(
            BlockBehaviour.Properties.of().strength(0.7F).sound(SoundType.SAND)));
    public static final DeferredHolder<Item, BlockItem> STONE_ITEM = ITEMS.register("abyssal_stone", () -> new BlockItem(STONE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> COBBLESTONE_ITEM = ITEMS.register("abyssal_cobblestone", () -> new BlockItem(COBBLESTONE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> SAND_ITEM = ITEMS.register("abyssal_sand", () -> new BlockItem(SAND.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> FUSED_SAND_ITEM = ITEMS.register("fused_abyssal_sand", () -> new BlockItem(FUSED_SAND.get(), new Item.Properties()));
    public static final DeferredHolder<FluidType, FluidType> CORALIUM_TYPE = FLUID_TYPES.register("coralium", () -> new FluidType(
            FluidType.Properties.create().density(1200).viscosity(1500).canSwim(true).canDrown(true).canHydrate(false)));
    public static final DeferredHolder<Fluid, FlowingFluid> CORALIUM = FLUIDS.register("coralium", () -> new BaseFlowingFluid.Source(fluidProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CORALIUM = FLUIDS.register("flowing_coralium", () -> new BaseFlowingFluid.Flowing(fluidProperties()));
    public static final DeferredHolder<Block, CoraliumLiquidBlock> LIQUID_CORALIUM = BLOCKS.register("liquid_coralium", () ->
            new CoraliumLiquidBlock(CORALIUM.get(), BlockBehaviour.Properties.of().noCollission().strength(100).noLootTable().replaceable().liquid()));
    public static final DeferredHolder<Item, BucketItem> CORALIUM_BUCKET = ITEMS.register("coralium_bucket", () ->
            new BucketItem(CORALIUM.get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
    public static final DeferredHolder<MobEffect, CoraliumPlagueEffect> CORALIUM_PLAGUE = EFFECTS.register("coralium_plague", CoraliumPlagueEffect::new);

    private WastelandContent() { }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, path); }
    private static BaseFlowingFluid.Properties fluidProperties() {
        return new BaseFlowingFluid.Properties(CORALIUM_TYPE, CORALIUM, FLOWING_CORALIUM)
                .block(LIQUID_CORALIUM).bucket(CORALIUM_BUCKET).tickRate(5).slopeFindDistance(4).levelDecreasePerBlock(1);
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); FLUID_TYPES.register(bus); FLUIDS.register(bus); EFFECTS.register(bus);
        bus.addListener(WastelandContent::creativeItems);
    }
    private static void creativeItems(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(STONE_ITEM.get()); event.accept(COBBLESTONE_ITEM.get()); event.accept(FUSED_SAND_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) event.accept(SAND_ITEM.get());
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(CORALIUM_BUCKET.get());
    }
}
