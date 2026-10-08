package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.entity.PrototypeEffigyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class PrototypeEffigyBlock extends BaseEntityBlock {
    public static final MapCodec<PrototypeEffigyBlock> CODEC = simpleCodec(PrototypeEffigyBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(box(3, 0, 3, 13, 3, 13), box(5, 3, 5, 11, 16, 11));

    public PrototypeEffigyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrototypeEffigyBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, AbyssalPort.PROTOTYPE_EFFIGY_ENTITY.get(),
                PrototypeEffigyBlockEntity::serverTick);
    }
}
