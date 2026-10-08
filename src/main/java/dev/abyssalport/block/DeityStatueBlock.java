package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.abyssalport.block.entity.DeityStatueBlockEntity;
import dev.abyssalport.block.entity.PrototypeEffigyBlockEntity;
import dev.abyssalport.content.StatueContent;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/** Facing-aware, native one-block sculptures; no reserved source OBJ is used. */
public final class DeityStatueBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final MapCodec<DeityStatueBlock> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
        StatueContent.Deity.CODEC.fieldOf("deity").forGetter(DeityStatueBlock::deity),propertiesCodec())
        .apply(instance,DeityStatueBlock::new));
    private static final VoxelShape SHAPE=box(4,0,4,12,16,12);
    private final StatueContent.Deity deity;
    public DeityStatueBlock(StatueContent.Deity deity,BlockBehaviour.Properties properties) {
        super(properties); this.deity=deity; registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    public StatueContent.Deity deity() { return deity; }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()); }
    @Override protected BlockState rotate(BlockState state,Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state,Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new DeityStatueBlockEntity(pos,state); }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DeityStatueBlockEntity statue)
            player.displayClientMessage(Component.translatable("message.abyssalport.statue_status",
                Component.translatable("amplifier.abyssalport."+statue.activeAmplifier().id()),
                (int)statue.energyQuanta(),statue.chargeInterval(),(int)statue.chargingRange()),true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,StatueContent.ENTITY.get(),PrototypeEffigyBlockEntity::serverTick);
    }
}
