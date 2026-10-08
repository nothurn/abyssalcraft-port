package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.world.GatewayTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Uses the native 1.21 PortalProcessor, travel hooks and cooldown, not a per-player teleport shortcut. */
public final class GatewayPortalBlock extends Block implements Portal {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final MapCodec<GatewayPortalBlock> CODEC = simpleCodec(GatewayPortalBlock::new);
    private static final VoxelShape X_SHAPE = box(0, 0, 6, 16, 16, 10), Z_SHAPE = box(6, 0, 0, 10, 16, 16);
    public GatewayPortalBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X)); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false) && !entity.isVehicle() && !(entity instanceof EnderDragon) && !(entity instanceof WitherBoss)
                && Shapes.joinIsNotEmpty(Shapes.create(entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ())),
                    state.getShape(level, pos), BooleanOp.AND)) entity.setAsInsidePortal(this, pos);
    }
    @Override public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return GatewayTravel.transition(level, entity, pos);
    }
}
