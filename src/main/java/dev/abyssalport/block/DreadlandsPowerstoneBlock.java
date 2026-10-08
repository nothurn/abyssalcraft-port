package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandStructuresContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class DreadlandsPowerstoneBlock extends Block {
    public static final MapCodec<DreadlandsPowerstoneBlock> CODEC = simpleCodec(DreadlandsPowerstoneBlock::new);
    private static final VoxelShape SHAPE = box(3.2, 0, 3.2, 12.8, 16, 12.8);
    public DreadlandsPowerstoneBlock(Properties properties) { super(properties); }
    @Override public MapCodec<DreadlandsPowerstoneBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX()+.25+random.nextDouble()*.5,
                pos.getY()+1.1, pos.getZ()+.25+random.nextDouble()*.5, 0, 0, 0);
    }
    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, entity, tool);
        markExplored(level,pos);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        // A stone broken with the wrong tool is gone too. Do not send future
        // trackers back to an empty pedestal; native harvest gating owns its loot.
        if (!replacement.is(this)) markExplored(level,pos);
        super.onRemove(state,level,pos,replacement,moving);
    }
    private static void markExplored(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server && server.dimension().equals(WastelandContent.DIMENSION)) {
            var structure = server.registryAccess().registryOrThrow(Registries.STRUCTURE).get(WastelandStructuresContent.STRONGHOLD);
            if (structure != null) {
                var start = server.structureManager().getStructureAt(pos, structure);
                if (start.isValid() && start.canBeReferenced()) {
                    server.structureManager().addReference(start);
                    server.getChunk(start.getChunkPos().x, start.getChunkPos().z).setUnsaved(true);
                }
            }
        }
    }
}
