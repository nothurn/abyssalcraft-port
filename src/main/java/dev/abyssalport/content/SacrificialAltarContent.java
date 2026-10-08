package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.SacrificialAltarBlock;
import dev.abyssalport.block.entity.SacrificialAltarBlockEntity;
import dev.abyssalport.energy.EnergyDeviceTier;
import dev.abyssalport.item.EnergyStorageItem;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.registries.*;

/** Native survival Sacrificial Altar/tier forgings, Shinoow source, LGPL-3.0. */
public final class SacrificialAltarContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,AbyssalPort.MOD_ID);
    private static final Map<EnergyDeviceTier,DeferredHolder<Block,SacrificialAltarBlock>> ALTARS=new EnumMap<>(EnergyDeviceTier.class);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SacrificialAltarBlockEntity>> ENTITY=ENTITIES.register("sacrificial_altar",()->
            BlockEntityType.Builder.of(SacrificialAltarBlockEntity::new,ALTARS.values().stream().map(DeferredHolder::get).toArray(Block[]::new)).build(null));
    static {
        for(var tier:EnergyDeviceContent.REACHABLE) {
            String id=(tier==EnergyDeviceTier.BASE?"":tier.id()+"_")+"sacrificial_altar";
            var block=BLOCKS.register(id,()->new SacrificialAltarBlock(BlockBehaviour.Properties.of().strength(6,12)
                    .sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops(),tier));ALTARS.put(tier,block);
            ITEMS.register(id,()->new EnergyStorageItem(block.get(),new Item.Properties(),tier.pedestalCapacity()));
        }
    }
    private SacrificialAltarContent() { }
    public static SacrificialAltarBlock block(EnergyDeviceTier tier) {if(!ALTARS.containsKey(tier))throw new IllegalArgumentException("Unconnected altar tier");return ALTARS.get(tier).get();}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);bus.addListener(SacrificialAltarContent::creative);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,true,SacrificialAltarBlockEntity::observeAdmittedDeath);
        NeoForge.EVENT_BUS.addListener(SacrificialAltarContent::forge);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS)ALTARS.values().forEach(block->event.accept(block.get()));
    }
    private static void forge(AnvilUpdateEvent event) {
        var left=event.getLeft();if(!(left.getItem() instanceof EnergyStorageItem item) || !(item.getBlock() instanceof SacrificialAltarBlock source)
                || !ALTARS.containsKey(source.tier()) || source!=block(source.tier()))return;
        var target=EnergyDeviceContent.REACHABLE.stream().filter(tier->tier.ordinal()>source.tier().ordinal() && event.getRight().is(EnergyDeviceContent.ring(tier))).findFirst().orElse(null);
        if(target==null)return;if(left.getCount()!=1){event.setCanceled(true);return;}
        var result=new ItemStack(block(target).asItem().builtInRegistryHolder(),1,left.getComponentsPatch());
        if(event.getName()!=null) {
            if(event.getName().isBlank())result.remove(DataComponents.CUSTOM_NAME);
            else if(!event.getName().equals(left.getHoverName().getString()))result.set(DataComponents.CUSTOM_NAME,Component.literal(event.getName()));
        }
        event.setOutput(result);event.setCost(1);event.setMaterialCost(1);
    }
}
