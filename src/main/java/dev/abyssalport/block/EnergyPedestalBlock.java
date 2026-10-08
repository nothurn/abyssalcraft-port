package dev.abyssalport.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.abyssalport.block.entity.EnergyPedestalBlockEntity;
import dev.abyssalport.content.EnergyDeviceContent;
import dev.abyssalport.energy.EnergyDeviceTier;
import dev.abyssalport.research.EarlyResearch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** BlockEnergyPedestal / BlockTiltablePedestal, Shinoow, LGPL-3.0.
 * Keeps the singleton interaction separate from ritual-offering pedestals.
 */
public class EnergyPedestalBlock extends EnergyStorageBlock {
    public static final BooleanProperty TILTED = BooleanProperty.create("tilted");
    public static final MapCodec<EnergyPedestalBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.xmap(EnergyDeviceTier::valueOf, EnergyDeviceTier::name).fieldOf("tier").forGetter(EnergyPedestalBlock::tier),
            propertiesCodec()).apply(instance, (tier, properties) -> new EnergyPedestalBlock(properties, tier)));
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 16, 12);
    public EnergyPedestalBlock(BlockBehaviour.Properties properties, EnergyDeviceTier tier) {
        this(properties,tier,true);
    }
    protected EnergyPedestalBlock(BlockBehaviour.Properties properties, EnergyDeviceTier tier, boolean tiltable) {
        super(properties, tier); registerDefaultState(tiltable?stateDefinition.any().setValue(TILTED, false):stateDefinition.any());
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(TILTED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        int angle = net.minecraft.util.Mth.floor(context.getRotation() * 16 / 360 + .5) & 15;
        return defaultBlockState().setValue(TILTED, angle % 4 != 0);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new EnergyPedestalBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, EnergyDeviceContent.PEDESTAL_ENTITY.get(), EnergyPedestalBlockEntity::serverTick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(level, pos, player, held).consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player, ItemStack.EMPTY);
    }
    private InteractionResult interact(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!player.mayBuild() || !level.mayInteract(player, pos) || !(level.getBlockEntity(pos) instanceof EnergyPedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (!pedestal.item().isEmpty()) {
                // Source singleton semantics: no room means leave it on top.
                var returned = pedestal.item().copy();
                if (!player.addItem(returned)) return InteractionResult.PASS;
                pedestal.removeItem();
            } else if (!held.isEmpty()) pedestal.placeOne(held);
            else return super.useWithoutItem(level.getBlockState(pos), level, pos, player, new BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false));
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, .5F, 1);
            if (player instanceof ServerPlayer server) EarlyResearch.unlock(server, EarlyResearch.NETWORK);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof EnergyPedestalBlockEntity pedestal) {
            var held = pedestal.removeItem(); if (!held.isEmpty()) popResource(level, pos, held);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
