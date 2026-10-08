package dev.abyssalport.block;

import dev.abyssalport.entity.ShoggothEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Thin, stackable residue left by Shoggoths; bright light slowly dissolves it. */
public final class ShoggothOozeBlock extends Block {
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 8);
    private static final int MIN_DECAY_DELAY = 400;
    private static final int DECAY_JITTER = 401;
    private static final VoxelShape[] SHAPES = new VoxelShape[8];

    static {
        for (int layers = 1; layers <= 8; layers++) {
            SHAPES[layers - 1] = Block.box(0, 0, 0, 16, layers * 2, 16);
        }
    }

    public ShoggothOozeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this) && level instanceof ServerLevel serverLevel) {
            scheduleDecay(serverLevel, pos, serverLevel.getRandom());
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int height = Math.max(0, (state.getValue(LAYERS) - 1) * 2);
        return Block.box(0, 0, 0, 16, height, 16);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof ShoggothEntity)) {
            Vec3 movement = entity.getDeltaMovement();
            entity.setDeltaMovement(movement.x * 0.4, movement.y, movement.z * 0.4);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Existing worlds may contain residue created before scheduled decay was introduced.
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            scheduleDecay(level, pos, random);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) >= 13) {
            int layers = state.getValue(LAYERS);
            level.setBlock(pos, layers == 1 ? Blocks.AIR.defaultBlockState()
                    : state.setValue(LAYERS, layers - 1), 3);
        }
        if (level.getBlockState(pos).is(this)) {
            scheduleDecay(level, pos, random);
        }
    }

    private void scheduleDecay(ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, MIN_DECAY_DELAY + random.nextInt(DECAY_JITTER));
    }
}
