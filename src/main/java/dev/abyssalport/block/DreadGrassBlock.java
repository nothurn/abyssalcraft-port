package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.content.DreadlandsContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

/** Source-informed dim-light soil, using native grass bonemeal/plant mechanics. */
public final class DreadGrassBlock extends GrassBlock {
    public static final MapCodec<GrassBlock> CODEC=simpleCodec(DreadGrassBlock::new);
    public DreadGrassBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public MapCodec<GrassBlock> codec() { return CODEC; }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!level.hasChunksAt(pos.offset(-3,-3,-3),pos.offset(3,3,3))) return;
        var above=pos.above(); var cover=level.getBlockState(above);
        if(level.getMaxLocalRawBrightness(above)<4&&cover.getLightBlock(level,above)>2) {
            level.setBlockAndUpdate(pos,DreadlandsContent.DIRT.get().defaultBlockState()); return;
        }
        // The original intentionally spreads without a daylight threshold.
        for(int attempt=0;attempt<4;attempt++) {
            var target=pos.offset(random.nextInt(3)-1,random.nextInt(5)-3,random.nextInt(3)-1);
            var top=level.getBlockState(target.above());
            if(level.getBlockState(target).is(DreadlandsContent.DIRT.get())&&top.getLightBlock(level,target.above())<=2)
                level.setBlockAndUpdate(target,defaultBlockState().setValue(SNOWY,top.is(Blocks.SNOW)));
        }
    }
}
