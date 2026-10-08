package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.block.entity.ShoggothBiomassBlockEntity;
import dev.abyssalport.content.ShoggothLairContent;
import dev.abyssalport.entity.ShoggothEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class ShoggothBiomassBlock extends BaseEntityBlock {
    public static final MapCodec<ShoggothBiomassBlock> CODEC=simpleCodec(ShoggothBiomassBlock::new);
    private static final VoxelShape COLLISION=box(0,0,0,16,13.6,16);
    public ShoggothBiomassBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return COLLISION; }
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(!(entity instanceof ShoggothEntity)) {
            var velocity=entity.getDeltaMovement(); entity.setDeltaMovement(velocity.x*.4,velocity.y,velocity.z*.4);
        }
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new ShoggothBiomassBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide?null:createTickerHelper(type,ShoggothLairContent.BIOMASS_ENTITY.get(),ShoggothBiomassBlockEntity::serverTick);
    }
}
