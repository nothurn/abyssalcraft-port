package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.item.CrystalBagItem;
import dev.abyssalport.menu.CrystalBagMenu;
import dev.abyssalport.recipe.CrystalBagUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

/** The three bag tiers whose source ingredients are reachable in the current survival path. */
public final class CrystalBagContent {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,AbyssalPort.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,AbyssalPort.MOD_ID);
    public static final DeferredHolder<Item,CrystalBagItem> SMALL=ITEMS.register("small_crystal_bag",()->new CrystalBagItem(new Item.Properties().stacksTo(1),18));
    public static final DeferredHolder<Item,CrystalBagItem> MEDIUM=ITEMS.register("medium_crystal_bag",()->new CrystalBagItem(new Item.Properties().stacksTo(1),36));
    public static final DeferredHolder<Item,CrystalBagItem> LARGE=ITEMS.register("large_crystal_bag",()->new CrystalBagItem(new Item.Properties().stacksTo(1),54));
    public static final DeferredHolder<MenuType<?>,MenuType<CrystalBagMenu>> MENU=MENUS.register("crystal_bag",()->IMenuTypeExtension.create(CrystalBagMenu::new));
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<CrystalBagUpgradeRecipe>> UPGRADE=
            SERIALIZERS.register("crystal_bag_upgrade",CrystalBagUpgradeRecipe.Serializer::new);
    private CrystalBagContent() { }
    public static void register(IEventBus bus) {
        ITEMS.register(bus); MENUS.register(bus); SERIALIZERS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==CreativeModeTabs.TOOLS_AND_UTILITIES) { event.accept(SMALL.get()); event.accept(MEDIUM.get()); event.accept(LARGE.get()); }
        });
    }
}
