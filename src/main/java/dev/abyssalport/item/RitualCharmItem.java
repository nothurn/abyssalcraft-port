package dev.abyssalport.item;

import dev.abyssalport.block.entity.DeityStatueBlockEntity;
import dev.abyssalport.energy.CharmAmplifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** A consumed modifier or a reusable blank reset charm, with server-authoritative use. */
public final class RitualCharmItem extends Item {
    private final CharmAmplifier amplifier;
    public RitualCharmItem(Properties properties, CharmAmplifier amplifier) {
        super(properties); this.amplifier = amplifier;
    }
    public CharmAmplifier amplifier() { return amplifier; }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel(); var pos = context.getClickedPos(); var player = context.getPlayer();
        var entity = level.getBlockEntity(pos);
        if (!(entity instanceof DeityStatueBlockEntity) && !(entity instanceof dev.abyssalport.block.entity.EnergyDepositionerBlockEntity)) return InteractionResult.PASS;
        if (player == null || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            boolean changed;
            CharmAmplifier active;
            if (entity instanceof DeityStatueBlockEntity statue) {
                changed = amplifier == CharmAmplifier.NONE ? statue.clearCharm() : statue.activateCharm(amplifier, null);
                active = statue.activeAmplifier();
            } else {
                var depositioner = (dev.abyssalport.block.entity.EnergyDepositionerBlockEntity) entity;
                changed = amplifier == CharmAmplifier.NONE ? depositioner.clearCharm() : depositioner.activateCharm(amplifier);
                active = depositioner.activeAmplifier();
            }
            if (changed) {
                if (amplifier != CharmAmplifier.NONE) context.getItemInHand().shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .5F, 1F);
            }
            player.displayClientMessage(Component.translatable("message.abyssalport.statue_charm",
                    Component.translatable("amplifier.abyssalport." + active.id())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.abyssalport.charm_amplifier",
                Component.translatable("amplifier.abyssalport." + amplifier.id())).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(amplifier == CharmAmplifier.NONE
                ? "tooltip.abyssalport.charm_reset" : "tooltip.abyssalport.charm_use").withStyle(ChatFormatting.GRAY));
    }
}
