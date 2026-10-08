package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.abyssalport.energy.EnergyDeviceTier;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.entity.EnergyRelayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Six-direction relay from BlockEnergyRelay, Shinoow, LGPL-3.0.
 * Existing horizontal states retain their orientation; the front is the output.
 */
public final class EnergyRelayBlock extends EnergyStorageBlock {
    public static final MapCodec<EnergyRelayBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.xmap(EnergyDeviceTier::valueOf, EnergyDeviceTier::name).fieldOf("tier").forGetter(EnergyRelayBlock::tier),
            propertiesCodec()).apply(instance, (tier, properties) -> new EnergyRelayBlock(properties, tier)));
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape NORTH_SHAPE = box(2, 1, 4, 14, 15, 16);
    private static final VoxelShape EAST_SHAPE = box(0, 1, 2, 12, 15, 14);
    private static final VoxelShape SOUTH_SHAPE = box(2, 1, 0, 14, 15, 12);
    private static final VoxelShape WEST_SHAPE = box(4, 1, 2, 16, 15, 14);
    private static final VoxelShape UP_SHAPE = box(2, 0, 1, 14, 12, 15);
    private static final VoxelShape DOWN_SHAPE = box(2, 4, 1, 14, 16, 15);

    public EnergyRelayBlock(BlockBehaviour.Properties properties) {
        this(properties, EnergyDeviceTier.BASE);
    }
    public EnergyRelayBlock(BlockBehaviour.Properties properties, EnergyDeviceTier tier) {
        super(properties, tier);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!player.mayBuild()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            // A property-only change preserves the native block entity/balance.
            level.setBlock(pos, state.cycle(FACING), 3);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyRelayBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, AbyssalPort.ENERGY_RELAY_ENTITY.get(),
                EnergyRelayBlockEntity::serverTick);
    }
}
