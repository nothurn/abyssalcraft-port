package dev.abyssalport.block;

import com.mojang.serialization.MapCodec;
import dev.abyssalport.block.entity.PlaceOfPowerBlockEntity;
import dev.abyssalport.content.PlaceOfPowerContent;
import dev.abyssalport.content.ShoggothLairContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class PlaceOfPowerBaseBlock extends BaseEntityBlock {
    public static final MapCodec<PlaceOfPowerBaseBlock> CODEC=simpleCodec(PlaceOfPowerBaseBlock::new);
    public PlaceOfPowerBaseBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new PlaceOfPowerBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide?null:createTickerHelper(type,PlaceOfPowerContent.ENTITY.get(),PlaceOfPowerBlockEntity::serverTick);
    }
    @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state) { return new ItemStack(ShoggothLairContent.MONOLITH_STONE_ITEM.get()); }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof PlaceOfPowerBlockEntity base) {
            player.displayClientMessage(Component.translatable("message.abyssalport.power.status",
                base.layout()==null?Component.translatable("message.abyssalport.power.unknown"):Component.translatable("screen.abyssalport.power."+base.layout().id()),
                Component.translatable(base.isValid()?"message.abyssalport.power.active":"message.abyssalport.power.inactive")),true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
