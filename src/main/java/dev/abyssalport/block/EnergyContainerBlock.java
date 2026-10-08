package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.abyssalport.energy.EnergyDeviceTier;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.entity.EnergyContainerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Persistent 10,000 PE reservoir for the first working transfer chain. */
public final class EnergyContainerBlock extends EnergyStorageBlock {
    public static final MapCodec<EnergyContainerBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.xmap(EnergyDeviceTier::valueOf, EnergyDeviceTier::name).fieldOf("tier").forGetter(EnergyContainerBlock::tier),
            propertiesCodec()).apply(instance, (tier, properties) -> new EnergyContainerBlock(properties, tier)));
    // Source container is 3/4-block high, not a full cube. The owned model's
    // uprights end at the same height; a full shape would block its PE receiver.
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 12, 16);

    public EnergyContainerBlock(BlockBehaviour.Properties properties) {
        this(properties, EnergyDeviceTier.BASE);
    }
    public EnergyContainerBlock(BlockBehaviour.Properties properties, EnergyDeviceTier tier) { super(properties, tier); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyContainerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof EnergyContainerBlockEntity container) {
            serverPlayer.openMenu(container);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, AbyssalPort.ENERGY_CONTAINER_ENTITY.get(),
                EnergyContainerBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof EnergyContainerBlockEntity container) {
            for (int slot = 0; slot < container.items().getSlots(); slot++) {
                var stack = container.items().extractItem(slot, container.items().getSlotLimit(slot), false);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
