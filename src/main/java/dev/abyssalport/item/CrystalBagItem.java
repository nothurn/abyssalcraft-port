package dev.abyssalport.item;

import java.util.List;
import dev.abyssalport.AbyssalPort;
import dev.abyssalport.menu.CrystalBagMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/** Slot count belongs to the item tier; the native container component carries each bag's own contents. */
public final class CrystalBagItem extends Item {
    public static final TagKey<Item> CRYSTALS=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,"crystals"));
    private final int capacity;
    public CrystalBagItem(Properties properties,int capacity) {
        super(properties);
        if(capacity!=18 && capacity!=36 && capacity!=54) throw new IllegalArgumentException("Unsupported crystal-bag tier");
        this.capacity=capacity;
    }
    public static int capacity(ItemStack stack) { return stack.getItem() instanceof CrystalBagItem bag?bag.capacity:0; }
    public static boolean accepts(ItemStack stack) { return !stack.isEmpty() && !(stack.getItem() instanceof CrystalBagItem) && stack.is(CRYSTALS); }
    public static boolean hasValidContents(ItemStack stack) {
        int size=capacity(stack); if(size==0 || stack.getCount()!=1) return false;
        var contents=stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        for(int slot=0;slot<contents.getSlots();slot++) {
            var item=contents.getStackInSlot(slot);
            if(!item.isEmpty() && (slot>=size || !accepts(item) || item.getCount()>item.getMaxStackSize())) return false;
        }
        return true;
    }
    @Override public boolean canFitInsideContainerItems() { return false; }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if(player instanceof ServerPlayer server) {
            if(!hasValidContents(stack)) {
                player.displayClientMessage(Component.translatable("message.abyssalport.crystal_bag.invalid").withStyle(ChatFormatting.RED),true);
                return InteractionResultHolder.fail(stack);
            }
            int lockedSlot=hand==InteractionHand.MAIN_HAND?player.getInventory().selected:40;
            server.openMenu(new SimpleMenuProvider((id,inventory,owner)->new CrystalBagMenu(id,inventory,stack,hand,lockedSlot),stack.getHoverName()),
                    buffer->{ buffer.writeVarInt(capacity); buffer.writeEnum(hand); buffer.writeVarInt(lockedSlot); });
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        long used=stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyStream().count();
        tooltip.add(Component.translatable("tooltip.abyssalport.crystal_bag",used,capacity).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.abyssalport.crystal_bag.open").withStyle(ChatFormatting.DARK_AQUA));
    }
}
