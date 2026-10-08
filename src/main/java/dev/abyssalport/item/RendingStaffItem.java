package dev.abyssalport.item;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandProgressionContent;
import dev.abyssalport.research.WastelandProgressionResearch;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Source-informed rending, with server-owned targeting rather than a trusted client entity ID. */
public final class RendingStaffItem extends Item {
    public static final int RANGE = 50;
    public static final int COOLDOWN = 10;
    private final int tier;
    public enum Channel {
        ABYSSAL("energyAbyssal", "abyssal", 100, "abyssal_rending_targets"),
        DREAD("energyDread", "dread", 100, "dread_rending_targets"),
        SHADOW("energyShadow", "shadow", 200, "shadow_rending_targets");
        final String dataKey;
        final String translation;
        public final int threshold;
        final TagKey<EntityType<?>> targets;
        Channel(String dataKey, String translation, int threshold, String tag) {
            this.dataKey = dataKey; this.translation = translation; this.threshold = threshold;
            targets = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, tag));
        }
        boolean accepts(Mob target) {
            return target.getType().is(targets)
                    && !(target instanceof dev.abyssalport.entity.AbyssalDragonEntity)
                    && !(target instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
                    && switch(this) {
                        case ABYSSAL -> target.level().dimension().equals(WastelandContent.DIMENSION);
                        case DREAD -> target.level().dimension().equals(dev.abyssalport.content.DreadlandsContent.DIMENSION);
                        case SHADOW -> true;
                    };
        }
        Item output() { return switch(this) {
            case ABYSSAL -> WastelandProgressionContent.ESSENCE.get();
            case DREAD -> dev.abyssalport.content.DreadlandsProgressionContent.ESSENCE.get();
            case SHADOW -> AbyssalPort.SHADOW_GEM.get();
        }; }
    }

    public RendingStaffItem(Properties properties) { this(properties, 0); }
    public RendingStaffItem(Properties properties, int tier) {
        super(properties);
        if (tier < 0 || tier > 2) throw new IllegalArgumentException("Unsupported rending tier");
        this.tier = tier;
    }
    public static int tier(ItemStack stack) { return stack.getItem() instanceof RendingStaffItem staff ? staff.tier : -1; }
    public int tier() { return tier; }

    public static int energy(ItemStack stack, Channel channel) {
        if (!(stack.getItem() instanceof RendingStaffItem)) return 0;
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(channel.dataKey), 0, 1_000_000);
    }

    private static void storeEnergy(ItemStack stack, Channel channel, int amount) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putInt(channel.dataKey, Math.clamp(amount, 0, 1_000_000));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    /** Exchange only after inventory insertion or a real drop succeeds; full inventories never eat progress. */
    public static boolean redeem(ItemStack stack, Player player) {
        if (player.level().isClientSide || !(stack.getItem() instanceof RendingStaffItem)) return false;
        boolean redeemed = false;
        for (Channel channel : Channel.values()) if (energy(stack, channel) >= channel.threshold) {
            var output = new ItemStack(channel.output());
            boolean delivered = player.getInventory().add(output);
            if (!delivered && !output.isEmpty()) {
                var dropped = player.drop(output, false);
                // Admission and tracking are different: a real drop may still be in a hidden chunk.
                // Conversely ServerPlayer.drop returns an entity even when a join hook vetoes it.
                delivered = dropped != null && dropped.isAddedToLevel() && !dropped.isRemoved();
            }
            if (delivered) {
                storeEnergy(stack, channel, energy(stack, channel) - channel.threshold); redeemed = true;
                if (player instanceof ServerPlayer serverPlayer) WastelandProgressionResearch.discover(serverPlayer, new ItemStack(channel.output()));
            }
        }
        return redeemed;
    }

    public static boolean drain(ItemStack stack, Player player, LivingEntity target) {
        if (player.level().isClientSide || !(stack.getItem() instanceof RendingStaffItem)
                || !(target instanceof Mob mob) || !mob.isAlive() || mob.isRemoved() || mob.level() != player.level()
                || player.getCooldowns().isOnCooldown(stack.getItem()) || player.isSpectator()
                || player.getEyePosition().distanceToSqr(target.getBoundingBox().getCenter()) > RANGE * RANGE
                || !player.hasLineOfSight(target)) return false;
        var channels = java.util.Arrays.stream(Channel.values()).filter(channel -> channel.accepts(mob)
                && energy(stack, channel) < channel.threshold).toList();
        int amount = tier(stack) + 1;
        if (channels.isEmpty() || !mob.hurt(player.damageSources().playerAttack(player), amount)) return false;
        channels.forEach(channel -> storeEnergy(stack, channel, energy(stack, channel) + amount));
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN);
        return true;
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            redeem(stack, player);
            if (!player.isSpectator()) {
                Vec3 start = player.getEyePosition(), direction = player.getViewVector(1.0F).scale(RANGE);
                var block = level.clip(new ClipContext(start, start.add(direction), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                Vec3 end = block.getType() == HitResult.Type.MISS ? start.add(direction) : block.getLocation();
                var hit = ProjectileUtil.getEntityHitResult(level, player, start, end,
                        player.getBoundingBox().expandTowards(direction).inflate(1), entity -> entity instanceof LivingEntity
                        && entity.isAlive() && !entity.isSpectator() && entity.isPickable(), 0);
                if (hit != null && hit.getEntity() instanceof LivingEntity target) drain(stack, player, target);
            }
            redeem(stack, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide) { redeem(stack, player); drain(stack, player, target); redeem(stack, player); }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.abyssalport.rending_staff").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.abyssalport.staff_tier", tier).withStyle(ChatFormatting.GRAY));
        for (Channel channel : Channel.values()) tooltip.add(Component.translatable("tooltip.abyssalport.rending."+channel.translation,
                energy(stack, channel), channel.threshold).withStyle(ChatFormatting.DARK_AQUA));
    }
}
