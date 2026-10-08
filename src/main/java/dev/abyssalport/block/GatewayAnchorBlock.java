package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.block.entity.GatewayAnchorBlockEntity;
import dev.abyssalport.content.GatewayContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class GatewayAnchorBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final MapCodec<GatewayAnchorBlock> CODEC = simpleCodec(GatewayAnchorBlock::new);
    public GatewayAnchorBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(ACTIVE, false)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ACTIVE); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GatewayAnchorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, GatewayContent.ANCHOR_ENTITY.get(), GatewayAnchorBlockEntity::serverTick);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) for (int y = 1; y <= 2; y++)
            if (level.getBlockState(pos.above(y)).is(GatewayContent.PORTAL.get())) level.removeBlock(pos.above(y), false);
        super.onRemove(state, level, pos, next, moving);
    }
}
