package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.entity.ShoggothEntity;
import dev.abyssalport.entity.ShoggothEntity.Kind;
import dev.abyssalport.entity.ShoggothEntity.Habitat;
import dev.abyssalport.entity.ShoggothAcidProjectile;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.registries.*;

/** Three connected forms in the three reachable worlds; native types preserve habitat through travel. */
public final class ShoggothEcologyContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    // Rendering payload, not a craftable survival ingredient or creative reward.
    public static final DeferredHolder<Item,Item> ACID_GLOB=ITEMS.register("shoggoth_acid_glob",()->new Item(new Item.Properties()));
    public static final DeferredHolder<EntityType<?>,EntityType<ShoggothAcidProjectile>> ACID_PROJECTILE=ENTITIES.register("shoggoth_acid_projectile",()->
            EntityType.Builder.<ShoggothAcidProjectile>of(ShoggothAcidProjectile::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(4).updateInterval(10).build("shoggoth_acid_projectile"));
    private static final Map<Habitat,Map<Kind,DeferredHolder<EntityType<?>,EntityType<ShoggothEntity>>>> TYPES=new EnumMap<>(Habitat.class);
    static {
        for(var habitat:Habitat.values()) {
            var forms=new EnumMap<Kind,DeferredHolder<EntityType<?>,EntityType<ShoggothEntity>>>(Kind.class); TYPES.put(habitat,forms);
            for(var kind:Kind.values()) {
                String name=name(kind,habitat);
                var holder=habitat==Habitat.OVERWORLD&&kind==Kind.LESSER?AbyssalPort.LESSER_SHOGGOTH:ENTITIES.register(name,()->
                        EntityType.Builder.<ShoggothEntity>of((type,level)->new ShoggothEntity(type,level,kind,habitat),MobCategory.MONSTER)
                                .sized(kind.width,kind.height).clientTrackingRange(8).build(name));
                forms.put(kind,holder);
                ITEMS.register(name+"_spawn_egg",()->new DeferredSpawnEggItem(holder,habitat==Habitat.ABYSSAL?0x245b56:habitat==Habitat.DREAD?0x582538:0x303f32,
                        kind==Kind.GREATER?0xddd794:kind==Kind.NORMAL?0xaaa274:0x81875c,new Item.Properties()));
            }
        }
    }
    private ShoggothEcologyContent() { }
    public static String name(Kind kind,Habitat habitat) {
        return (kind==Kind.LESSER?"lesser_":kind==Kind.GREATER?"greater_":"")
                +(habitat==Habitat.ABYSSAL?"abyssal_":habitat==Habitat.DREAD?"dreaded_":"")+"shoggoth";
    }
    public static EntityType<ShoggothEntity> type(Kind kind,Habitat habitat) { return TYPES.get(habitat).get(kind).get(); }
    /** Original dimensional draw: half Lesser, 49.5% normal, 0.5% Greater. */
    public static EntityType<ShoggothEntity> biomassType(ServerLevel level,RandomSource random) {
        Habitat habitat=level.dimension().equals(WastelandContent.DIMENSION)?Habitat.ABYSSAL:
                level.dimension().equals(DreadlandsContent.DIMENSION)?Habitat.DREAD:Habitat.OVERWORLD;
        Kind kind=Kind.LESSER;
        if(habitat!=Habitat.OVERWORLD&&random.nextBoolean()) kind=random.nextInt(100)==0?Kind.GREATER:Kind.NORMAL;
        return type(kind,habitat);
    }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus); ITEMS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event)-> {
            for(var habitat:Habitat.values()) for(var kind:Kind.values()) if(habitat!=Habitat.OVERWORLD||kind!=Kind.LESSER)
                event.put(type(kind,habitat),ShoggothEntity.createAttributes(kind).build());
        });
        bus.addListener((RegisterSpawnPlacementsEvent event)-> {
            for(var habitat:Habitat.values()) for(var kind:Kind.values()) if(habitat!=Habitat.OVERWORLD||kind!=Kind.LESSER)
                event.register(type(kind,habitat),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        Monster::checkMonsterSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event)-> {
            if(event.getTabKey()==CreativeModeTabs.SPAWN_EGGS) for(var entry:ITEMS.getEntries()) if(entry.get() instanceof DeferredSpawnEggItem) event.accept(entry.get());
        });
    }
}
