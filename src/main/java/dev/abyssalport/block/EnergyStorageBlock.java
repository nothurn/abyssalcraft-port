package dev.abyssalport.block;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.energy.EnergyStorageBlockEntity;
import dev.abyssalport.energy.EnergyDeviceTier;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.research.EarlyResearch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Consistent item persistence and book interaction for first-tier PE devices. */
public abstract class EnergyStorageBlock extends BaseEntityBlock {
    private final EnergyDeviceTier tier;
    protected EnergyStorageBlock(BlockBehaviour.Properties properties) {
        this(properties, EnergyDeviceTier.BASE);
    }

    protected EnergyStorageBlock(BlockBehaviour.Properties properties, EnergyDeviceTier tier) {
        super(properties); this.tier = java.util.Objects.requireNonNull(tier);
    }
    public final EnergyDeviceTier tier() { return tier; }
    public boolean acceptsItemEnergy() { return true; }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && level.getBlockEntity(pos) instanceof EnergyStorageBlockEntity storage) {
            float stored = data.copyTag().getFloat("PotEnergy");
            if (Float.isFinite(stored) && stored > 0) {
                storage.addEnergy(Math.min(stored, storage.capacity()));
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!NecronomiconItem.isBook(held)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof EnergyStorageBlockEntity storage) {
            if (state.getBlock() instanceof EnergyCollectorBlock) {
                EarlyResearch.unlock(serverPlayer, EarlyResearch.COLLECTOR);
            } else if (state.getBlock() instanceof EnergyRelayBlock || state.getBlock() instanceof EnergyContainerBlock) {
                EarlyResearch.unlock(serverPlayer, EarlyResearch.NETWORK);
            }
            float transferred = player.isShiftKeyDown()
                    ? storage.transferFromBook(held) : storage.transferToBook(held);
            serverPlayer.displayClientMessage(Component.translatable("message.abyssalport.energy_transfer",
                    (int) transferred, state.getBlock().getName(), (int) storage.energy(), storage.capacity()), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof EnergyStorageBlockEntity storage) {
            serverPlayer.displayClientMessage(Component.translatable("message.abyssalport.energy_status",
                    state.getBlock().getName(), (int) storage.energy(), storage.capacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
