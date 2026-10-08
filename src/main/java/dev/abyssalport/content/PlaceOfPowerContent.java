package dev.abyssalport.content;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.PlaceOfPowerBaseBlock;
import dev.abyssalport.block.entity.PlaceOfPowerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** The formed base is not an item: mining/picking yields its original Monolith Stone. */
public final class PlaceOfPowerContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,AbyssalPort.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,AbyssalPort.MOD_ID);
    public static final DeferredHolder<Block,PlaceOfPowerBaseBlock> BASE=BLOCKS.register("place_of_power_base",()->
        new PlaceOfPowerBaseBlock(BlockBehaviour.Properties.of().strength(6,24).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<PlaceOfPowerBlockEntity>> ENTITY=ENTITIES.register("place_of_power",()->
        BlockEntityType.Builder.of(PlaceOfPowerBlockEntity::new,BASE.get()).build(null));
    private PlaceOfPowerContent() { }
    public static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); }
}
