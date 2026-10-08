package dev.abyssalport.block.entity;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.EnergyRelayBlock;
import dev.abyssalport.energy.EnergyStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Source relay: any eligible adjacent PE store behind, first receiver in front.
 * Derived from TileEntityEnergyRelay/PEUtils, Shinoow, LGPL-3.0.
 */
public final class EnergyRelayBlockEntity extends EnergyStorageBlockEntity {
    public static final int CAPACITY = 500;
    private int activeTicks;

    public EnergyRelayBlockEntity(BlockPos pos, BlockState state) {
        super(AbyssalPort.ENERGY_RELAY_ENTITY.get(), pos, state,
                ((EnergyRelayBlock) state.getBlock()).tier().relayCapacity());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyRelayBlockEntity relay) {
        if (!(level instanceof ServerLevel server) || !(state.getBlock() instanceof EnergyRelayBlock block)
                || server.hasNeighborSignal(pos)) {
            return;
        }
        Direction facing = state.getValue(EnergyRelayBlock.FACING);
        relay.activeTicks = (relay.activeTicks + 1) % 40;
        if (relay.activeTicks % 20 == 0) {
            BlockPos source = pos.relative(facing.getOpposite());
            if (server.hasChunkAt(source)
                    && server.getBlockEntity(source) instanceof EnergyStorageBlockEntity storage) {
                storage.transferTo(relay, block.tier().drain());
            }
        }
        if (relay.activeTicks == 0 && relay.energy() > 0) {
            relay.pushToContainer(server, pos, facing);
        }
        if (relay.activeTicks == 0) {
            relay.setChanged();
        }
    }

    private void pushToContainer(ServerLevel level, BlockPos pos, Direction facing) {
        var tier = ((EnergyRelayBlock) getBlockState().getBlock()).tier();
        for (int distance = 1; distance <= tier.range(); distance++) {
            BlockPos target = pos.relative(facing, distance);
            if (!level.hasChunkAt(target)) {
                return;
            }
            BlockState state = level.getBlockState(target);
            if (Block.isShapeFullBlock(state.getCollisionShape(level, target))) {
                return;
            }
            if (level.getBlockEntity(target) instanceof EnergyStorageBlockEntity storage) {
                // The first full receiver blocks the beam too. Never skip it to
                // another store further away or restrict the role to one class.
                float accepted=transferTo(storage, tier.transfer());
                dev.abyssalport.energy.PEStreams.show(level, pos, target, accepted);
                return;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ActiveTicks", activeTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        activeTicks = Math.clamp(tag.getInt("ActiveTicks"), 0, 39);
    }
}
