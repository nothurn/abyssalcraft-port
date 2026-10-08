package dev.abyssalport.content;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.MimicFireBlock;
import dev.abyssalport.entity.*;
import dev.abyssalport.entity.CorruptedAnimalEntity.Kind;
import dev.abyssalport.worldgen.MobEcologySpawns;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.registries.*;

public final class MobEcologyContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> MODIFIERS=DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,AbyssalPort.MOD_ID);
    private static final Map<Kind,DeferredHolder<EntityType<?>,EntityType<CorruptedAnimalEntity>>> EVIL=new EnumMap<>(Kind.class),DEMON=new EnumMap<>(Kind.class);
    private static final List<DeferredHolder<Item,DeferredSpawnEggItem>> EGGS=new ArrayList<>();
    public static final DeferredHolder<Block,MimicFireBlock> MIMIC_FIRE=BLOCKS.register("mimic_fire",()->new MimicFireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FIRE)));
    public static final DeferredHolder<EntityType<?>,EntityType<ShubOffspringEntity>> OFFSPRING=ENTITIES.register("shub_offspring",()->
        EntityType.Builder.of(ShubOffspringEntity::new,MobCategory.MONSTER).sized(1,2.9F).eyeHeight(2.03F).clientTrackingRange(8).build("shub_offspring"));
    static {
        MODIFIERS.register("animal_ecology_spawns",()->MobEcologySpawns.CODEC);
        for(boolean demon:new boolean[]{false,true})for(Kind kind:Kind.values()) {
            String id=name(kind,demon);float width=kind==Kind.CHICKEN?.3F:.9F,height=switch(kind){case COW,SHEEP->1.3F;case PIG->.9F;case CHICKEN->.7F;};
            var holder=ENTITIES.register(id,()->EntityType.Builder.<CorruptedAnimalEntity>of((type,level)->new CorruptedAnimalEntity(type,level,kind,demon),
                MobCategory.MONSTER).sized(width,height).fireImmune().clientTrackingRange(8).build(id));
            (demon?DEMON:EVIL).put(kind,holder);EGGS.add(ITEMS.register(id+"_spawn_egg",()->new DeferredSpawnEggItem(holder,demon?0x432C2D:0x79696A,0xD64935,new Item.Properties())));
        }
        EGGS.add(ITEMS.register("shub_offspring_spawn_egg",()->new DeferredSpawnEggItem(OFFSPRING,0x352B2C,0xCEB19C,new Item.Properties())));
    }
    private MobEcologyContent() { }
    public static String name(Kind kind,boolean demon) { return (demon?"demon_":"evil_")+kind.name().toLowerCase(Locale.ROOT); }
    public static EntityType<CorruptedAnimalEntity> type(Kind kind,boolean demon) { return(demon?DEMON:EVIL).get(kind).get(); }
    public static void register(IEventBus bus) {ENTITIES.register(bus);ITEMS.register(bus);BLOCKS.register(bus);MODIFIERS.register(bus);
        bus.addListener(MobEcologyContent::attributes);bus.addListener(MobEcologyContent::placements);bus.addListener(MobEcologyContent::creative);}
    private static void attributes(EntityAttributeCreationEvent event) {for(boolean demon:new boolean[]{false,true})for(Kind kind:Kind.values())event.put(type(kind,demon),CorruptedAnimalEntity.createAttributes(kind).build());
        event.put(OFFSPRING.get(),ShubOffspringEntity.createAttributes().build());}
    private static void placements(RegisterSpawnPlacementsEvent event) {for(boolean demon:new boolean[]{false,true})for(Kind kind:Kind.values())event.register(type(kind,demon),SpawnPlacementTypes.ON_GROUND,
        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,CorruptedAnimalEntity::checkSpawn,RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(OFFSPRING.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ShubOffspringEntity::checkSpawn,RegisterSpawnPlacementsEvent.Operation.REPLACE);}
    private static void creative(BuildCreativeModeTabContentsEvent event) {if(event.getTabKey()==CreativeModeTabs.SPAWN_EGGS)EGGS.forEach(egg->event.accept(egg.get()));}
}
