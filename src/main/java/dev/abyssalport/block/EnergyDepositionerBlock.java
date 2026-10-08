package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.block.entity.EnergyDepositionerBlockEntity;
import dev.abyssalport.content.EnergyDepositionerContent;
import dev.abyssalport.item.RitualCharmItem;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** BlockEnergyDepositioner, Shinoow, LGPL-3.0. Loot carries the entire in-flight job. */
public final class EnergyDepositionerBlock extends BaseEntityBlock {
    public static final MapCodec<EnergyDepositionerBlock> CODEC = simpleCodec(EnergyDepositionerBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = box(1.92, 0, 1.92, 14.08, 12, 14.08);
    public EnergyDepositionerBlock(BlockBehaviour.Properties properties) {
        super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EnergyDepositionerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, EnergyDepositionerContent.ENTITY.get(), EnergyDepositionerBlockEntity::serverTick);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack item) {
        super.setPlacedBy(level, pos, state, placer, item);
        var data = item.get(DataComponents.CUSTOM_DATA);
        if (data != null && level.getBlockEntity(pos) instanceof EnergyDepositionerBlockEntity be) be.restoreItemData(data.copyTag());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.getMainHandItem().getItem() instanceof RitualCharmItem) return InteractionResult.PASS;
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof EnergyDepositionerBlockEntity be) {
            if (!player.mayInteract(level, pos) || player.isSpectator()) return InteractionResult.FAIL;
            server.openMenu(be);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EnergyDepositionerBlockEntity be ? (int)(15 * be.energy() / be.capacity()) : 0;
    }
}
