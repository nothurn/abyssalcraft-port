package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.DeityStatueBlock;
import dev.abyssalport.block.entity.DeityStatueBlockEntity;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

/** Seven source deities on independently authored voxel geometry and pixels. */
public final class StatueContent {
    public enum Deity implements StringRepresentable {
        CTHULHU("cthulhu"), HASTUR("hastur"), JZAHAR("jzahar"), AZATHOTH("azathoth"),
        NYARLATHOTEP("nyarlathotep"), YOG_SOTHOTH("yog_sothoth"), SHUB_NIGGURATH("shub_niggurath");
        public static final com.mojang.serialization.Codec<Deity> CODEC=StringRepresentable.fromEnum(Deity::values);
        private final String id;
        Deity(String id) { this.id=id; }
        @Override public String getSerializedName() { return id; }
    }
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,AbyssalPort.MOD_ID);
    private static final EnumMap<Deity,DeferredHolder<Block,DeityStatueBlock>> STATUES=new EnumMap<>(Deity.class);
    private static final EnumMap<Deity,DeferredHolder<Item,BlockItem>> STATUE_ITEMS=new EnumMap<>(Deity.class);
    static {
        for(var deity:Deity.values()) {
            var id=deity.getSerializedName()+"_statue";
            var block=BLOCKS.register(id,()->new DeityStatueBlock(deity,BlockBehaviour.Properties.of()
                .strength(6,12).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion()));
            STATUES.put(deity,block);
            STATUE_ITEMS.put(deity,ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties())));
        }
    }
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<DeityStatueBlockEntity>> ENTITY=ENTITIES.register("deity_statue",()->
        BlockEntityType.Builder.of(DeityStatueBlockEntity::new,blocks().toArray(Block[]::new)).build(null));
    private StatueContent() { }
    public static DeityStatueBlock block(Deity deity) { return STATUES.get(deity).get(); }
    public static List<DeityStatueBlock> blocks() { return java.util.Arrays.stream(Deity.values()).map(StatueContent::block).toList(); }
    public static boolean isStatue(BlockState state) {
        return state.getBlock() instanceof DeityStatueBlock || state.is(AbyssalPort.PROTOTYPE_EFFIGY.get());
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); bus.addListener(StatueContent::creative);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS) STATUE_ITEMS.values().forEach(item->event.accept(item.get()));
    }
}
