package dev.abyssalport.block.entity;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.energy.EnergyStorageBlockEntity;
import dev.abyssalport.block.EnergyCollectorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** First-tier PE buffer. The original collector has a 1,000 PE capacity. */
public final class EnergyCollectorBlockEntity extends EnergyStorageBlockEntity implements dev.abyssalport.energy.StatueEnergyCollector {
    public static final int CAPACITY = 1_000;

    public EnergyCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(AbyssalPort.ENERGY_COLLECTOR_ENTITY.get(), pos, state,
                ((EnergyCollectorBlock) state.getBlock()).tier().collectorCapacity());
    }
}
