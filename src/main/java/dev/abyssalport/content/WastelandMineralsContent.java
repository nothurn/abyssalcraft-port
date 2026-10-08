package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.item.RefinedCoraliumArmorEffects;
import dev.abyssalport.item.RefinedCoraliumTier;
import dev.abyssalport.research.RefinedCoraliumResearch;
import dev.abyssalport.worldgen.RangedOreFeature;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Source-informed Wasteland mining progression. All media is independently drawn. */
public final class WastelandMineralsContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<ArmorMaterial> ARMOR = DeferredRegister.create(Registries.ARMOR_MATERIAL, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Feature<?>, RangedOreFeature> ORE_FEATURE = FEATURES.register("ranged_ore", RangedOreFeature::new);

    public static final DeferredHolder<Block, Block> NITRE_ORE = BLOCKS.register("nitre_ore", () -> ore(3, 6, 0, 2));
    public static final DeferredHolder<Block, Block> ABYSSAL_NITRE_ORE = BLOCKS.register("abyssal_nitre_ore", () -> ore(3, 6, 0, 2));
    public static final DeferredHolder<Block, Block> ABYSSAL_CORALIUM_ORE = BLOCKS.register("abyssal_coralium_ore", () -> ore(3, 6, 0, 2));
    public static final DeferredHolder<Block, Block> ABYSSAL_IRON_ORE = BLOCKS.register("abyssal_iron_ore", () -> ore(3, 6, 0, 0));
    public static final DeferredHolder<Block, Block> ABYSSAL_GOLD_ORE = BLOCKS.register("abyssal_gold_ore", () -> ore(5, 10, 0, 0));
    public static final DeferredHolder<Block, Block> ABYSSAL_DIAMOND_ORE = BLOCKS.register("abyssal_diamond_ore", () -> ore(5, 10, 3, 7));
    public static final DeferredHolder<Block, Block> LIQUIFIED_CORALIUM_ORE = BLOCKS.register("liquified_coralium_ore", () -> ore(10, 12, 0, 0));
    public static final DeferredHolder<Block, Block> PEARLESCENT_CORALIUM_ORE = BLOCKS.register("pearlescent_coralium_ore", () -> ore(8, 10, 2, 5));
    public static final DeferredHolder<Block, Block> STORAGE_BLOCK = BLOCKS.register("refined_coralium_block", () ->
            new Block(BlockBehaviour.Properties.of().strength(5, 10).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredHolder<Item, BlockItem> NITRE_ORE_ITEM = ITEMS.register("nitre_ore", () -> blockItem(NITRE_ORE));
    public static final DeferredHolder<Item, BlockItem> ABYSSAL_NITRE_ORE_ITEM = ITEMS.register("abyssal_nitre_ore", () -> blockItem(ABYSSAL_NITRE_ORE));
    public static final DeferredHolder<Item, BlockItem> ABYSSAL_CORALIUM_ORE_ITEM = ITEMS.register("abyssal_coralium_ore", () -> blockItem(ABYSSAL_CORALIUM_ORE));
    public static final DeferredHolder<Item, BlockItem> ABYSSAL_IRON_ORE_ITEM = ITEMS.register("abyssal_iron_ore", () -> blockItem(ABYSSAL_IRON_ORE));
    public static final DeferredHolder<Item, BlockItem> ABYSSAL_GOLD_ORE_ITEM = ITEMS.register("abyssal_gold_ore", () -> blockItem(ABYSSAL_GOLD_ORE));
    public static final DeferredHolder<Item, BlockItem> ABYSSAL_DIAMOND_ORE_ITEM = ITEMS.register("abyssal_diamond_ore", () -> blockItem(ABYSSAL_DIAMOND_ORE));
    public static final DeferredHolder<Item, BlockItem> LIQUIFIED_CORALIUM_ORE_ITEM = ITEMS.register("liquified_coralium_ore", () -> blockItem(LIQUIFIED_CORALIUM_ORE));
    public static final DeferredHolder<Item, BlockItem> PEARLESCENT_CORALIUM_ORE_ITEM = ITEMS.register("pearlescent_coralium_ore", () -> blockItem(PEARLESCENT_CORALIUM_ORE));
    public static final DeferredHolder<Item, BlockItem> BLOCK_ITEM = ITEMS.register("refined_coralium_block", () -> blockItem(STORAGE_BLOCK));
    public static final DeferredHolder<Item, Item> NITRE = ITEMS.register("nitre", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> INGOT = ITEMS.register("refined_coralium_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NUGGET = ITEMS.register("refined_coralium_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, PickaxeItem> PICKAXE = ITEMS.register("refined_coralium_pickaxe", () ->
            new PickaxeItem(RefinedCoraliumTier.INSTANCE, new Item.Properties().attributes(DiggerItem.createAttributes(RefinedCoraliumTier.INSTANCE, 1, -2.8F))));
    public static final DeferredHolder<Item, AxeItem> AXE = ITEMS.register("refined_coralium_axe", () ->
            new AxeItem(RefinedCoraliumTier.INSTANCE, new Item.Properties().attributes(DiggerItem.createAttributes(RefinedCoraliumTier.INSTANCE, 5, -3.0F))));
    public static final DeferredHolder<Item, ShovelItem> SHOVEL = ITEMS.register("refined_coralium_shovel", () ->
            new ShovelItem(RefinedCoraliumTier.INSTANCE, new Item.Properties().attributes(DiggerItem.createAttributes(RefinedCoraliumTier.INSTANCE, 1.5F, -3.0F))));
    public static final DeferredHolder<Item, SwordItem> SWORD = ITEMS.register("refined_coralium_sword", () ->
            new SwordItem(RefinedCoraliumTier.INSTANCE, new Item.Properties().attributes(SwordItem.createAttributes(RefinedCoraliumTier.INSTANCE, 3, -2.4F))));
    public static final DeferredHolder<Item, HoeItem> HOE = ITEMS.register("refined_coralium_hoe", () ->
            new HoeItem(RefinedCoraliumTier.INSTANCE, new Item.Properties().attributes(DiggerItem.createAttributes(RefinedCoraliumTier.INSTANCE, -5, 0))));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MATERIAL = ARMOR.register("refined_coralium", () ->
            new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8,
                    ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3), 14, SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(INGOT.get()), List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                    AbyssalPort.MOD_ID, "refined_coralium"))), 1, 0));
    public static final DeferredHolder<Item, ArmorItem> HELMET = ITEMS.register("refined_coralium_helmet", () -> armor(ArmorItem.Type.HELMET));
    public static final DeferredHolder<Item, ArmorItem> CHESTPLATE = ITEMS.register("refined_coralium_chestplate", () -> armor(ArmorItem.Type.CHESTPLATE));
    public static final DeferredHolder<Item, ArmorItem> LEGGINGS = ITEMS.register("refined_coralium_leggings", () -> armor(ArmorItem.Type.LEGGINGS));
    public static final DeferredHolder<Item, ArmorItem> BOOTS = ITEMS.register("refined_coralium_boots", () -> armor(ArmorItem.Type.BOOTS));

    private WastelandMineralsContent() { }
    private static Block ore(float hardness, float resistance, int minXp, int maxXp) {
        return new DropExperienceBlock(UniformInt.of(minXp, maxXp), BlockBehaviour.Properties.of()
                .strength(hardness, resistance).requiresCorrectToolForDrops().sound(SoundType.STONE));
    }
    private static BlockItem blockItem(DeferredHolder<Block, Block> block) { return new BlockItem(block.get(), new Item.Properties()); }
    private static ArmorItem armor(ArmorItem.Type type) { return new ArmorItem(MATERIAL, type, new Item.Properties().durability(type.getDurability(37))); }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ARMOR.register(bus); FEATURES.register(bus);
        bus.addListener(WastelandMineralsContent::creativeItems);
        NeoForge.EVENT_BUS.addListener(RefinedCoraliumArmorEffects::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(RefinedCoraliumResearch::onPickup);
        NeoForge.EVENT_BUS.addListener(RefinedCoraliumResearch::onCrafted);
        NeoForge.EVENT_BUS.addListener(RefinedCoraliumResearch::onSmelted);
    }

    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            for (var ore : List.of(NITRE_ORE_ITEM, ABYSSAL_NITRE_ORE_ITEM, ABYSSAL_CORALIUM_ORE_ITEM,
                    ABYSSAL_IRON_ORE_ITEM, ABYSSAL_GOLD_ORE_ITEM, ABYSSAL_DIAMOND_ORE_ITEM,
                    LIQUIFIED_CORALIUM_ORE_ITEM, PEARLESCENT_CORALIUM_ORE_ITEM)) event.accept(ore.get());
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(BLOCK_ITEM.get());
        else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(NITRE.get()); event.accept(INGOT.get()); event.accept(NUGGET.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PICKAXE.get()); event.accept(AXE.get()); event.accept(SHOVEL.get()); event.accept(HOE.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(SWORD.get()); event.accept(HELMET.get()); event.accept(CHESTPLATE.get()); event.accept(LEGGINGS.get()); event.accept(BOOTS.get());
        }
    }
}
