package dev.abyssalport.item;

import dev.abyssalport.energy.EnergyDeviceTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Source tier label plus the connected infusion/forging instructions. */
public final class EmbossedRingItem extends Item {
    private final EnergyDeviceTier tier;
    public EmbossedRingItem(Properties properties, EnergyDeviceTier tier) { super(properties); this.tier = tier; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.abyssalport.pe_ring."+tier.ordinal()).withStyle(ChatFormatting.GRAY));
        if (tier != EnergyDeviceTier.BASE) tooltip.add(Component.translatable("tooltip.abyssalport.energy_upgrade").withStyle(ChatFormatting.DARK_GREEN));
    }
}
