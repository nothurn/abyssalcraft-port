package dev.abyssalport.block;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.abyssalport.block.entity.SacrificialAltarBlockEntity;
import dev.abyssalport.content.SacrificialAltarContent;
import dev.abyssalport.energy.EnergyDeviceTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Source Block{Tiered}SacrificialAltar, Shinoow, LGPL-3.0; independently owned model. */
public final class SacrificialAltarBlock extends EnergyPedestalBlock {
    public static final MapCodec<SacrificialAltarBlock> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            Codec.STRING.xmap(EnergyDeviceTier::valueOf,EnergyDeviceTier::name).fieldOf("tier").forGetter(SacrificialAltarBlock::tier),
            propertiesCodec()).apply(instance,(tier,properties)->new SacrificialAltarBlock(properties,tier)));
    private static final VoxelShape SHAPE=box(2.4,0,2.4,13.6,14.4,13.6);
    public SacrificialAltarBlock(BlockBehaviour.Properties properties,EnergyDeviceTier tier) {super(properties,tier,false);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {return defaultBlockState();}
    @Override public boolean acceptsItemEnergy() {return tier()!=EnergyDeviceTier.BASE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new SacrificialAltarBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide?null:createTickerHelper(type,SacrificialAltarContent.ENTITY.get(),SacrificialAltarBlockEntity::serverTick);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity player,ItemStack stack) {
        super.setPlacedBy(level,pos,state,player,stack);var data=stack.get(DataComponents.CUSTOM_DATA);
        if(data!=null && level.getBlockEntity(pos) instanceof SacrificialAltarBlockEntity altar)altar.restoreItemCounters(data.copyTag());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(level.getBlockEntity(pos) instanceof SacrificialAltarBlockEntity altar && altar.item().isEmpty() && player instanceof ServerPlayer server) {
            server.displayClientMessage(Component.translatable("message.abyssalport.sacrificial_altar_status",(int)altar.energy(),altar.capacity(),
                    (altar.cooldown()+19)/20,altar.targetCount(),altar.maxTargets()),true);return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state,level,pos,player,hit);
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if(level.getBlockEntity(pos) instanceof SacrificialAltarBlockEntity altar && altar.cooldown()>0)
            level.addParticle(ParticleTypes.LAVA,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0,0);
    }
}
