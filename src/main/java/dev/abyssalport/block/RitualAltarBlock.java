package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class RitualAltarBlock extends BaseEntityBlock {
    public static final MapCodec<RitualAltarBlock> CODEC = simpleCodec(RitualAltarBlock::new);

    public RitualAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!dev.abyssalport.item.NecronomiconItem.isBook(held)) {
            if (level.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
                if (!level.isClientSide && !altar.placeInput(held)) {
                    player.displayClientMessage(Component.translatable("message.abyssalport.altar_occupied"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
            if (!altar.start(server, serverPlayer)) {
                player.displayClientMessage(Component.translatable("message.abyssalport.ritual_requirements"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
            if (!altar.isRunning() && !altar.getInput().isEmpty()) {
                ItemStack returned = altar.removeInput();
                if (!player.addItem(returned)) player.drop(returned, false);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.translatable(altar.isRunning()
                    ? "message.abyssalport.ritual_running" : "message.abyssalport.ritual_hint",
                    altar.remainingTicks()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) {
            ItemStack stack = altar.removeInput();
            if (!stack.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5,
                    pos.getY() + 0.5, pos.getZ() + 0.5, stack));
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, AbyssalPort.RITUAL_ALTAR_ENTITY.get(),
                RitualAltarBlockEntity::serverTick);
    }
}
