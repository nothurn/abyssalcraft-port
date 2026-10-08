package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.entity.DreadguardEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;
import java.util.Map;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.registries.*;

/** Reachable elite-drop -> native Transmutator -> Dreadium material progression. */
public final class DreadProcessingContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<ArmorMaterial> ARMOR=DeferredRegister.create(Registries.ARMOR_MATERIAL,AbyssalPort.MOD_ID);
    public static final ResourceKey<DamageType> DREAD_DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,WastelandContent.id("dread"));
    public static final DeferredHolder<EntityType<?>,EntityType<DreadguardEntity>> GUARD=ENTITIES.register("dreadguard",()->
            EntityType.Builder.of(DreadguardEntity::new,MobCategory.MONSTER).sized(1,3).fireImmune().clientTrackingRange(8).build("dreadguard"));
    public static final DeferredHolder<Item,Item> SHARD=ITEMS.register("dreaded_shard",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Item,Item> INGOT=ITEMS.register("dreadium_ingot",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Item,Item> NUGGET=ITEMS.register("dreadium_nugget",()->new Item(new Item.Properties()));
    public static final DeferredHolder<Block,Block> STORAGE=BLOCKS.register("dreadium_block",()->new Block(properties(4,12).sound(SoundType.METAL)));
    public static final DeferredHolder<Block,Block> BRICKS=BLOCKS.register("dreadstone_bricks",()->new Block(properties(2.5F,20)));
    public static final DeferredHolder<Item,BlockItem> STORAGE_ITEM=ITEMS.register("dreadium_block",()->new BlockItem(STORAGE.get(),new Item.Properties()));
    public static final DeferredHolder<Item,BlockItem> BRICKS_ITEM=ITEMS.register("dreadstone_bricks",()->new BlockItem(BRICKS.get(),new Item.Properties()));
    public static final DeferredHolder<Item,DeferredSpawnEggItem> EGG=ITEMS.register("dreadguard_spawn_egg",()->new DeferredSpawnEggItem(GUARD,0x31252B,0xCB514A,new Item.Properties()));
    public static final DeferredHolder<Item,PickaxeItem> PICKAXE=ITEMS.register("dreadium_pickaxe",()->new PickaxeItem(dev.abyssalport.item.DreadiumTier.INSTANCE,
            new Item.Properties().attributes(DiggerItem.createAttributes(dev.abyssalport.item.DreadiumTier.INSTANCE,1,-2.8F))));
    public static final DeferredHolder<Item,AxeItem> AXE=ITEMS.register("dreadium_axe",()->new AxeItem(dev.abyssalport.item.DreadiumTier.INSTANCE,
            new Item.Properties().attributes(DiggerItem.createAttributes(dev.abyssalport.item.DreadiumTier.INSTANCE,5,-3))));
    public static final DeferredHolder<Item,ShovelItem> SHOVEL=ITEMS.register("dreadium_shovel",()->new ShovelItem(dev.abyssalport.item.DreadiumTier.INSTANCE,
            new Item.Properties().attributes(DiggerItem.createAttributes(dev.abyssalport.item.DreadiumTier.INSTANCE,1.5F,-3))));
    public static final DeferredHolder<Item,SwordItem> SWORD=ITEMS.register("dreadium_sword",()->new SwordItem(dev.abyssalport.item.DreadiumTier.INSTANCE,
            new Item.Properties().attributes(SwordItem.createAttributes(dev.abyssalport.item.DreadiumTier.INSTANCE,3,-2.4F))));
    public static final DeferredHolder<Item,HoeItem> HOE=ITEMS.register("dreadium_hoe",()->new HoeItem(dev.abyssalport.item.DreadiumTier.INSTANCE,
            new Item.Properties().attributes(DiggerItem.createAttributes(dev.abyssalport.item.DreadiumTier.INSTANCE,-6,0))));
    public static final DeferredHolder<ArmorMaterial,ArmorMaterial> MATERIAL=ARMOR.register("dreadium",()->new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET,3,ArmorItem.Type.CHESTPLATE,8,ArmorItem.Type.LEGGINGS,6,ArmorItem.Type.BOOTS,3),15,
            SoundEvents.ARMOR_EQUIP_IRON,()->Ingredient.of(INGOT.get()),List.of(new ArmorMaterial.Layer(WastelandContent.id("dreadium"))),1,0));
    public static final DeferredHolder<Item,ArmorItem> HELMET=ITEMS.register("dreadium_helmet",()->armor(ArmorItem.Type.HELMET));
    public static final DeferredHolder<Item,ArmorItem> CHESTPLATE=ITEMS.register("dreadium_chestplate",()->armor(ArmorItem.Type.CHESTPLATE));
    public static final DeferredHolder<Item,ArmorItem> LEGGINGS=ITEMS.register("dreadium_leggings",()->armor(ArmorItem.Type.LEGGINGS));
    public static final DeferredHolder<Item,ArmorItem> BOOTS=ITEMS.register("dreadium_boots",()->armor(ArmorItem.Type.BOOTS));
    private DreadProcessingContent() { }
    private static ArmorItem armor(ArmorItem.Type type) { return new ArmorItem(MATERIAL,type,new Item.Properties().durability(type.getDurability(40))); }
    private static BlockBehaviour.Properties properties(float strength,float resistance) { return BlockBehaviour.Properties.of().strength(strength,resistance).requiresCorrectToolForDrops().sound(SoundType.STONE); }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus); ITEMS.register(bus); BLOCKS.register(bus); ARMOR.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(dev.abyssalport.item.DreadiumArmorEffects::onPlayerTick);
        bus.addListener((EntityAttributeCreationEvent event)->event.put(GUARD.get(),DreadguardEntity.createAttributes().build()));
        bus.addListener((RegisterSpawnPlacementsEvent event)->event.register(GUARD.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                DreadguardEntity::checkSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE));
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==CreativeModeTabs.INGREDIENTS) { event.accept(SHARD.get()); event.accept(INGOT.get()); event.accept(NUGGET.get()); }
            if(event.getTabKey()==CreativeModeTabs.BUILDING_BLOCKS) { event.accept(STORAGE_ITEM.get()); event.accept(BRICKS_ITEM.get()); }
            if(event.getTabKey()==CreativeModeTabs.SPAWN_EGGS) event.accept(EGG.get());
            if(event.getTabKey()==CreativeModeTabs.TOOLS_AND_UTILITIES) { event.accept(PICKAXE.get()); event.accept(AXE.get()); event.accept(SHOVEL.get()); event.accept(HOE.get()); }
            if(event.getTabKey()==CreativeModeTabs.COMBAT) { event.accept(SWORD.get()); event.accept(AXE.get());
                event.accept(HELMET.get()); event.accept(CHESTPLATE.get()); event.accept(LEGGINGS.get()); event.accept(BOOTS.get()); }
        });
    }
}
