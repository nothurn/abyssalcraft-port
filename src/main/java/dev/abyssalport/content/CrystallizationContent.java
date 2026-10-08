package dev.abyssalport.content;

import java.util.*;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.CrystallizerBlock;
import dev.abyssalport.block.entity.CrystallizerBlockEntity;
import dev.abyssalport.menu.CrystallizerMenu;
import dev.abyssalport.recipe.CrystallizationRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class CrystallizationContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,AbyssalPort.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,AbyssalPort.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,AbyssalPort.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,AbyssalPort.MOD_ID);
    public static final List<String> MATERIALS=List.of("blaze","phosphorus","hydrogen","oxygen","nitrate","sulfur","silica","magnesia",
            "silicon","magnesium","nitrogen","calcium","beryl","coralium","abyssalnite","dreadium","iron","gold","redstone","carbon","potassium","copper");
    public record Crystal(DeferredHolder<Item,Item> fragment,DeferredHolder<Item,Item> shard,DeferredHolder<Item,Item> crystal,
                          DeferredHolder<Block,Block> cluster,DeferredHolder<Item,BlockItem> clusterItem) { }
    public static final Map<String,Crystal> CRYSTALS=createCrystals();
    public static final DeferredHolder<Item,Item> CARBON_CLUSTER=ITEMS.register("carbon_cluster",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Item,Item> DENSE_CARBON_CLUSTER=ITEMS.register("dense_carbon_cluster",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Item,Item> SULFUR=ITEMS.register("sulfur",()->new Item(new Item.Properties()));
    private static Map<String,Crystal> createCrystals() {
        var crystals=new LinkedHashMap<String,Crystal>();
        for(String material:MATERIALS) {
            var fragment=ITEMS.register(material+"_crystal_fragment",()->new Item(new Item.Properties()));
            var shard=ITEMS.register(material+"_crystal_shard",()->new Item(new Item.Properties()));
            var crystal=ITEMS.register(material+"_crystal",()->new Item(new Item.Properties()));
            var block=BLOCKS.register(material+"_crystal_cluster",()->new Block(BlockBehaviour.Properties.of().strength(2,6).requiresCorrectToolForDrops().sound(SoundType.AMETHYST)));
            var item=ITEMS.register(material+"_crystal_cluster",()->new BlockItem(block.get(),new Item.Properties()));
            crystals.put(material,new Crystal(fragment,shard,crystal,block,item));
        }
        return Collections.unmodifiableMap(crystals);
    }
    public static final DeferredHolder<Block,CrystallizerBlock> MACHINE=BLOCKS.register("crystallizer",()->new CrystallizerBlock(
            BlockBehaviour.Properties.of().strength(2.5F,12).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .lightLevel(state->state.getValue(CrystallizerBlock.LIT)?14:0)));
    public static final DeferredHolder<Item,BlockItem> MACHINE_ITEM=ITEMS.register("crystallizer",()->new BlockItem(MACHINE.get(),new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CrystallizerBlockEntity>> ENTITY=ENTITIES.register("crystallizer",()->
            BlockEntityType.Builder.of(CrystallizerBlockEntity::new,MACHINE.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<CrystallizerMenu>> MENU=MENUS.register("crystallizer",()->new MenuType<>(CrystallizerMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<RecipeType<?>,RecipeType<CrystallizationRecipe>> TYPE=TYPES.register("crystallization",()->new RecipeType<>() {
        @Override public String toString() { return "abyssalport:crystallization"; }
    });
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<CrystallizationRecipe>> SERIALIZER=SERIALIZERS.register("crystallization",CrystallizationRecipe.Serializer::new);
    private CrystallizationContent() { }
    public static Crystal material(String name) { return Objects.requireNonNull(CRYSTALS.get(name),name); }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); TYPES.register(bus); SERIALIZERS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event)->event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,ENTITY.get(),(be,side)->
                        side==null?new net.neoforged.neoforge.items.wrapper.InvWrapper(be):new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be,side)));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(MACHINE_ITEM.get());
            if(event.getTabKey()==CreativeModeTabs.INGREDIENTS) { event.accept(CARBON_CLUSTER.get()); event.accept(DENSE_CARBON_CLUSTER.get()); event.accept(SULFUR.get()); }
            for(var crystal:CRYSTALS.values()) {
                if(event.getTabKey()==CreativeModeTabs.INGREDIENTS) { event.accept(crystal.fragment.get()); event.accept(crystal.shard.get()); event.accept(crystal.crystal.get()); }
                if(event.getTabKey()==CreativeModeTabs.BUILDING_BLOCKS) event.accept(crystal.clusterItem.get());
            }
        });
    }
}
