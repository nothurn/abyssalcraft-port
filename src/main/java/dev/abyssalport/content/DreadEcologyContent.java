package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.entity.DreadSpawnEntity;
import dev.abyssalport.entity.DreadSlugEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.*;

/** Connected small -> greater -> beast population with original normal-mode costs/limits. */
public final class DreadEcologyContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<DreadSpawnEntity>> SMALL=creature("dread_spawn",DreadSpawnEntity.Kind.SMALL,.6F);
    public static final DeferredHolder<EntityType<?>,EntityType<DreadSpawnEntity>> GREATER=creature("greater_dread_spawn",DreadSpawnEntity.Kind.GREATER,1.2F);
    public static final DeferredHolder<EntityType<?>,EntityType<DreadSpawnEntity>> BEAST=creature("lesser_dreadbeast",DreadSpawnEntity.Kind.BEAST,1.8F);
    public static final DeferredHolder<EntityType<?>,EntityType<DreadSlugEntity>> SLUG=ENTITIES.register("dread_slug",()->
            EntityType.Builder.<DreadSlugEntity>of(DreadSlugEntity::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(4).updateInterval(10).build("dread_slug"));
    private DreadEcologyContent() { }
    private static DeferredHolder<EntityType<?>,EntityType<DreadSpawnEntity>> creature(String name,DreadSpawnEntity.Kind kind,float size) {
        var holder=ENTITIES.register(name,()->EntityType.Builder.<DreadSpawnEntity>of((type,level)->new DreadSpawnEntity(type,level,kind),MobCategory.MONSTER)
                .sized(size,size).fireImmune().clientTrackingRange(8).build(name));
        ITEMS.register(name+"_spawn_egg",()->new DeferredSpawnEggItem(holder,0x492735,kind==DreadSpawnEntity.Kind.SMALL?0xB57469:kind==DreadSpawnEntity.Kind.GREATER?0xCF795C:0xE6A275,new Item.Properties()));
        return holder;
    }
    public static EntityType<DreadSpawnEntity> type(DreadSpawnEntity.Kind kind) {
        return switch(kind) { case SMALL->SMALL.get(); case GREATER->GREATER.get(); case BEAST->BEAST.get(); };
    }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus); ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event)->{
            for(var kind:DreadSpawnEntity.Kind.values()) event.put(type(kind),DreadSpawnEntity.attributes(kind).build());
        });
        bus.addListener((RegisterSpawnPlacementsEvent event)->{
            for(var kind:DreadSpawnEntity.Kind.values()) event.register(type(kind),SpawnPlacementTypes.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,DreadSpawnEntity::checkSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey()==CreativeModeTabs.SPAWN_EGGS) for(var egg:ITEMS.getEntries()) event.accept(egg.get());
        });
    }
}
