package dev.abyssalport.item;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.DreadlandsContent;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Registry-owned key tier and per-stack selection; keys never teleport directly. */
public final class GatewayKeyItem extends Item {
    private static final String DESTINATION = "GatewayDestination";
    private final int tier;
    public GatewayKeyItem(Properties properties) { this(properties, 0); }
    public GatewayKeyItem(Properties properties, int tier) {
        super(properties);
        if (tier < 0 || tier > 1) throw new IllegalArgumentException("Unsupported gateway key tier");
        this.tier = tier;
    }
    public int tier() { return tier; }
    public boolean permits(ResourceKey<Level> source, ResourceKey<Level> target) {
        return connected(source, target) && (tier >= 1
                || !DreadlandsContent.DIMENSION.equals(source) && !DreadlandsContent.DIMENSION.equals(target));
    }
    public static ResourceKey<Level> destination(ItemStack key) {
        var tag = key.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(DESTINATION)) return Level.OVERWORLD;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(DESTINATION));
        return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
    }
    public static boolean connected(ResourceKey<Level> source, ResourceKey<Level> destination) {
        return source.equals(Level.OVERWORLD) && WastelandContent.DIMENSION.equals(destination)
                || source.equals(WastelandContent.DIMENSION) && Level.OVERWORLD.equals(destination)
                || source.equals(WastelandContent.DIMENSION) && DreadlandsContent.DIMENSION.equals(destination)
                || source.equals(DreadlandsContent.DIMENSION) && WastelandContent.DIMENSION.equals(destination);
    }
    public static void setDestination(ItemStack key, ResourceKey<Level> destination) {
        var tag = key.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(DESTINATION, destination.location().toString());
        key.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    private static Component label(ResourceKey<Level> dimension) {
        return Component.translatable(WastelandContent.DIMENSION.equals(dimension)
                ? "dimension.abyssalport.abyssal_wasteland" : DreadlandsContent.DIMENSION.equals(dimension)
                ? "dimension.abyssalport.dreadlands" : Level.OVERWORLD.equals(dimension)
                ? "dimension.minecraft.overworld" : "message.abyssalport.gateway_unknown");
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack key = player.getItemInHand(hand);
        if (!level.isClientSide) {
            List<ResourceKey<Level>> choices = tier == 0 ? List.of(Level.OVERWORLD, WastelandContent.DIMENSION)
                    : List.of(Level.OVERWORLD, WastelandContent.DIMENSION, DreadlandsContent.DIMENSION);
            int current = choices.indexOf(destination(key));
            var selected = choices.get(Math.floorMod(current + (player.isShiftKeyDown() ? -1 : 1), choices.size()));
            setDestination(key, selected);
            player.displayClientMessage(Component.translatable("message.abyssalport.gateway_destination", label(selected)), true);
        }
        return InteractionResultHolder.sidedSuccess(key, level.isClientSide);
    }
    @Override
    public void appendHoverText(ItemStack key, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.abyssalport.gateway_destination", label(destination(key)))
                .withStyle(tier == 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.abyssalport.gateway_key").withStyle(ChatFormatting.GRAY));
    }
}
