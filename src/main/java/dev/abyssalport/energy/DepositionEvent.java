package dev.abyssalport.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Protection hook before a whole, vertically spanning native biome cell is converted. */
public final class DepositionEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;
    private final BlockPos machine, minimum, maximum;
    public DepositionEvent(ServerLevel level, BlockPos machine, int x, int z) {
        this.level = level; this.machine = machine.immutable();
        minimum = new BlockPos(x, level.getMinBuildHeight(), z);
        maximum = new BlockPos(x + 3, level.getMaxBuildHeight() - 1, z + 3);
    }
    public ServerLevel level() { return level; }
    public BlockPos machine() { return machine; }
    public BlockPos minimum() { return minimum; }
    public BlockPos maximum() { return maximum; }
}
