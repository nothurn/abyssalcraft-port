package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** One native log item; the original's all-bark branch state is preserved separately. */
public final class DarklandsLogBlock extends RotatedPillarBlock {
    public static final BooleanProperty BARK = BooleanProperty.create("bark");
    private static final MapCodec<DarklandsLogBlock> CODEC = simpleCodec(DarklandsLogBlock::new);
    public DarklandsLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BARK, false));
    }
    @Override public MapCodec<DarklandsLogBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BARK);
    }
}
