package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.item.AbyssalniteTier;
import dev.abyssalport.item.AbyssalniteArmorEffects;
import dev.abyssalport.research.AbyssalniteResearch;
import dev.abyssalport.worldgen.AbyssalniteOreFeature;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The complete source-informed Abyssalnite equipment chain, with independent assets. */
public final class AbyssalniteContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AbyssalPort.MOD_ID);
    private static final DeferredRegister<ArmorMaterial> ARMOR =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, AbyssalPort.MOD_ID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, AbyssalPort.MOD_ID);
    public static final DeferredHolder<Feature<?>, AbyssalniteOreFeature> ORE_FEATURE =
            FEATURES.register("abyssalnite_ore", AbyssalniteOreFeature::new);

    public static final DeferredHolder<Block, Block> ORE = BLOCKS.register("abyssalnite_ore",
            () -> new Block(BlockBehaviour.Properties.of().strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Block, Block> STORAGE_BLOCK = BLOCKS.register("abyssalnite_block",
            () -> new Block(BlockBehaviour.Properties.of().strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredHolder<Item, BlockItem> ORE_ITEM = ITEMS.register("abyssalnite_ore",
            () -> new BlockItem(ORE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> BLOCK_ITEM = ITEMS.register("abyssalnite_block",
            () -> new BlockItem(STORAGE_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> INGOT = ITEMS.register("abyssalnite_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NUGGET = ITEMS.register("abyssalnite_nugget",
            () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, PickaxeItem> PICKAXE = ITEMS.register("abyssalnite_pickaxe",
            () -> new PickaxeItem(AbyssalniteTier.INSTANCE, new Item.Properties().attributes(
                    DiggerItem.createAttributes(AbyssalniteTier.INSTANCE, 1.0F, -2.8F))));
    public static final DeferredHolder<Item, AxeItem> AXE = ITEMS.register("abyssalnite_axe",
            () -> new AxeItem(AbyssalniteTier.INSTANCE, new Item.Properties().attributes(
                    DiggerItem.createAttributes(AbyssalniteTier.INSTANCE, 5.0F, -3.0F))));
    public static final DeferredHolder<Item, ShovelItem> SHOVEL = ITEMS.register("abyssalnite_shovel",
            () -> new ShovelItem(AbyssalniteTier.INSTANCE, new Item.Properties().attributes(
                    DiggerItem.createAttributes(AbyssalniteTier.INSTANCE, 1.5F, -3.0F))));
    public static final DeferredHolder<Item, SwordItem> SWORD = ITEMS.register("abyssalnite_sword",
            () -> new SwordItem(AbyssalniteTier.INSTANCE, new Item.Properties().attributes(
                    SwordItem.createAttributes(AbyssalniteTier.INSTANCE, 3, -2.4F))));
    public static final DeferredHolder<Item, HoeItem> HOE = ITEMS.register("abyssalnite_hoe",
            () -> new HoeItem(AbyssalniteTier.INSTANCE, new Item.Properties().attributes(
                    HoeItem.createAttributes(AbyssalniteTier.INSTANCE, -4.0F, 0.0F))));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MATERIAL = ARMOR.register("abyssalnite",
            () -> new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8,
                    ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3), 13,
                    SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(INGOT.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                            AbyssalPort.MOD_ID, "abyssalnite"))), 1.0F, 0.0F));
    public static final DeferredHolder<Item, ArmorItem> HELMET = ITEMS.register("abyssalnite_helmet",
            () -> armor(ArmorItem.Type.HELMET));
    public static final DeferredHolder<Item, ArmorItem> CHESTPLATE = ITEMS.register("abyssalnite_chestplate",
            () -> armor(ArmorItem.Type.CHESTPLATE));
    public static final DeferredHolder<Item, ArmorItem> LEGGINGS = ITEMS.register("abyssalnite_leggings",
            () -> armor(ArmorItem.Type.LEGGINGS));
    public static final DeferredHolder<Item, ArmorItem> BOOTS = ITEMS.register("abyssalnite_boots",
            () -> armor(ArmorItem.Type.BOOTS));

    private AbyssalniteContent() { }

    private static ArmorItem armor(ArmorItem.Type type) {
        return new ArmorItem(MATERIAL, type, new Item.Properties().durability(type.getDurability(35)));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ARMOR.register(bus);
        FEATURES.register(bus);
        bus.addListener(AbyssalniteContent::creativeItems);
        NeoForge.EVENT_BUS.addListener(AbyssalniteResearch::onPickup);
        NeoForge.EVENT_BUS.addListener(AbyssalniteResearch::onCrafted);
        NeoForge.EVENT_BUS.addListener(AbyssalniteResearch::onSmelted);
        NeoForge.EVENT_BUS.addListener(AbyssalniteArmorEffects::onPlayerTick);
    }

    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ORE_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(BLOCK_ITEM.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(INGOT.get());
            event.accept(NUGGET.get());
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PICKAXE.get());
            event.accept(AXE.get());
            event.accept(SHOVEL.get());
            event.accept(HOE.get());
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(SWORD.get());
            event.accept(HELMET.get());
            event.accept(CHESTPLATE.get());
            event.accept(LEGGINGS.get());
            event.accept(BOOTS.get());
        }
    }
}
