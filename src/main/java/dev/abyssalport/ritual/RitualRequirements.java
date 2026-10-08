package dev.abyssalport.ritual;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.content.WastelandProgressionContent;
import dev.abyssalport.content.DreadlandsProgressionContent;
import dev.abyssalport.item.NecronomiconItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Immutable access requirements; mutable item data never grants a higher book tier. */
public record RitualRequirements(int bookTier, int altarTier, @Nullable ResourceKey<Level> dimension) {
    public static final RitualRequirements BASIC = new RitualRequirements(0, 0, null);
    public RitualRequirements {
        if (bookTier < 0 || bookTier > 2 || altarTier < 0 || altarTier > 2 || bookTier < altarTier)
            throw new IllegalArgumentException("Ritual requires an unsupported book/altar tier");
    }
    public boolean acceptsBook(ItemStack book) { return NecronomiconItem.tier(book) >= bookTier; }
    public boolean matchesGround(ServerLevel level, BlockPos pos) {
        if (dimension != null && !dimension.equals(level.dimension())) return false;
        // Two source tier-zero materials, but never a mixed pedestal ring.
        boolean darkstone=altarTier==0&&level.getBlockState(pos).is(dev.abyssalport.content.DarklandsStructuresContent.ALTAR.get());
        var altar = darkstone?dev.abyssalport.content.DarklandsStructuresContent.ALTAR.get():altar(altarTier);
        var pedestal = darkstone?dev.abyssalport.content.DarklandsStructuresContent.PEDESTAL.get():pedestal(altarTier);
        if (!level.getBlockState(pos).is(altar)) return false;
        for (var offset : BasicScrollRitual.pedestalOffsets()) if (!level.getBlockState(pos.offset(offset)).is(pedestal)) return false;
        return true;
    }
    public static net.minecraft.world.level.block.Block altar(int tier) { return switch(tier) {
        case 0 -> AbyssalPort.RITUAL_ALTAR.get(); case 1 -> WastelandProgressionContent.ALTAR.get();
        case 2 -> DreadlandsProgressionContent.ALTAR.get(); default -> throw new IllegalArgumentException("Unsupported altar tier");
    }; }
    public static net.minecraft.world.level.block.Block pedestal(int tier) { return switch(tier) {
        case 0 -> AbyssalPort.RITUAL_PEDESTAL.get(); case 1 -> WastelandProgressionContent.PEDESTAL.get();
        case 2 -> DreadlandsProgressionContent.PEDESTAL.get(); default -> throw new IllegalArgumentException("Unsupported pedestal tier");
    }; }
    public int bookCapacity() { return switch(bookTier) { case 0 -> 5000; case 1 -> 10_000; case 2 -> 20_000; default -> throw new IllegalStateException(); }; }
}
