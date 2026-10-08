package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.abyssalport.energy.EnergyDeviceTier;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.entity.EnergyCollectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class EnergyCollectorBlock extends EnergyStorageBlock {
    public static final MapCodec<EnergyCollectorBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.xmap(EnergyDeviceTier::valueOf, EnergyDeviceTier::name).fieldOf("tier").forGetter(EnergyCollectorBlock::tier),
            propertiesCodec()).apply(instance, (tier, properties) -> new EnergyCollectorBlock(properties, tier)));
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 13, 13);

    public EnergyCollectorBlock(BlockBehaviour.Properties properties) {
        this(properties, EnergyDeviceTier.BASE);
    }
    public EnergyCollectorBlock(BlockBehaviour.Properties properties, EnergyDeviceTier tier) { super(properties, tier); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCollectorBlockEntity(pos, state);
    }
}
