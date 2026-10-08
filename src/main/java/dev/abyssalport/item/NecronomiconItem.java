package dev.abyssalport.item;

import java.util.List;

import dev.abyssalport.ritual.RitualGroundFormation;
import dev.abyssalport.network.BookOpenPayload;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.research.ShadowMaterialResearch;
import dev.abyssalport.research.AbyssalniteResearch;
import dev.abyssalport.research.CoraliumResearch;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import dev.abyssalport.energy.PotentialEnergyMath;
import net.neoforged.neoforge.network.PacketDistributor;

/** Player-held guide and PE vessel. Capacity belongs to the item tier, never to mutable NBT. */
public final class NecronomiconItem extends Item {
    public static final int MAX_POTENTIAL_ENERGY = 5_000;
    // The original 1.12.2 mod uses this exact numeric tag; keep it for future migration.
    private static final String ENERGY_KEY = "PotEnergy";
    private final int tier;
    private final int capacity;

    public NecronomiconItem(Properties properties) {
        this(properties, 0, MAX_POTENTIAL_ENERGY);
    }

    public NecronomiconItem(Properties properties, int tier, int capacity) {
        super(properties);
        if (tier < 0 || capacity < MAX_POTENTIAL_ENERGY) throw new IllegalArgumentException("Invalid book tier");
        this.tier = tier;
        this.capacity = capacity;
    }

    public static boolean isBook(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof NecronomiconItem;
    }

    public static int capacity(ItemStack stack) {
        return isBook(stack) ? ((NecronomiconItem) stack.getItem()).capacity : 0;
    }

    public static int tier(ItemStack stack) {
        return isBook(stack) ? ((NecronomiconItem) stack.getItem()).tier : -1;
    }

    public static float getPotentialEnergy(ItemStack stack) {
        if (!isBook(stack)) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        float stored = data.copyTag().getFloat(ENERGY_KEY);
        return Float.isFinite(stored) ? Math.clamp(stored, 0, capacity(stack)) : 0;
    }

    public static void setPotentialEnergy(ItemStack stack, float amount) {
        if (!isBook(stack)) throw new IllegalArgumentException("PE requires a Necronomicon");
        if (!Float.isFinite(amount)) {
            throw new IllegalArgumentException("Potential energy must be finite");
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putFloat(ENERGY_KEY, Math.clamp(amount, 0, capacity(stack)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static float addPotentialEnergy(ItemStack stack, float amount) {
        PotentialEnergyMath.Transfer transfer = PotentialEnergyMath.add(capacity(stack),
                getPotentialEnergy(stack), amount);
        setPotentialEnergy(stack, transfer.stored());
        return transfer.remainder();
    }

    public static float consumePotentialEnergy(ItemStack stack, float amount) {
        PotentialEnergyMath.Transfer transfer = PotentialEnergyMath.consume(capacity(stack),
                getPotentialEnergy(stack), amount);
        setPotentialEnergy(stack, transfer.stored());
        return transfer.remainder();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            ShadowMaterialResearch.discoverFromInventory(serverPlayer);
            dev.abyssalport.research.GhoulResearch.discoverFromInventory(serverPlayer);
            AbyssalniteResearch.discoverFromInventory(serverPlayer);
            CoraliumResearch.discoverFromInventory(serverPlayer);
            dev.abyssalport.research.GatewayResearch.discoverFromInventory(serverPlayer);
            dev.abyssalport.research.RefinedCoraliumResearch.discoverFromInventory(serverPlayer);
            dev.abyssalport.research.WastelandProgressionResearch.discoverFromInventory(serverPlayer);
            PacketDistributor.sendToPlayer(serverPlayer, new BookOpenPayload(
                    hand == InteractionHand.MAIN_HAND ? 0 : 1, EarlyResearch.discoveries(serverPlayer)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.getBlockState(context.getClickedPos()).is(dev.abyssalport.content.ShoggothLairContent.MONOLITH_STONE.get())) {
            if (level instanceof ServerLevel server && context.getPlayer() instanceof ServerPlayer player) {
                var result=dev.abyssalport.energy.PlaceOfPowerFormation.form(server,context.getClickedPos(),player,context.getItemInHand());
                player.displayClientMessage(Component.translatable("message.abyssalport.power.formation."
                    +result.name().toLowerCase(java.util.Locale.ROOT)),true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!RitualGroundFormation.isFormationMaterial(level.getBlockState(context.getClickedPos()))) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && context.getPlayer() instanceof ServerPlayer player) {
            RitualGroundFormation.Result result = RitualGroundFormation.form(server,
                    context.getClickedPos(), player, context.getItemInHand());
            player.displayClientMessage(Component.translatable("message.abyssalport.formation."
                    + result.name().toLowerCase(java.util.Locale.ROOT)), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.abyssalport.potential_energy",
                (int) getPotentialEnergy(stack), capacity(stack)).withStyle(ChatFormatting.GRAY));
        if (tier > 0) tooltip.add(Component.translatable("tooltip.abyssalport.book_tier", tier).withStyle(ChatFormatting.DARK_AQUA));
    }
}
