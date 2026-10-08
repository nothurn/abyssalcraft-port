package dev.abyssalport.ritual;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.item.NecronomiconItem;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** First-tier ritual layout adapted from AbyssalCraft's basicScroll offering pattern (LGPL-3.0). */
public final class BasicScrollRitual {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "basic_scroll");
    public static final int ENERGY_COST = 100;
    public static final int DURATION_TICKS = RitualTiming.forCost(NecronomiconItem.MAX_POTENTIAL_ENERGY,
            ENERGY_COST).seconds() * 20;
    private static final List<BlockPos> OFFSETS = List.of(
            new BlockPos(-3, 0, 0), new BlockPos(0, 0, -3),
            new BlockPos(3, 0, 0), new BlockPos(0, 0, 3),
            new BlockPos(-2, 0, 2), new BlockPos(-2, 0, -2),
            new BlockPos(2, 0, 2), new BlockPos(2, 0, -2)
    );

    private BasicScrollRitual() {
    }

    public static List<BlockPos> pedestalOffsets() {
        return OFFSETS;
    }

    /** Fingerprint for rituals saved before definitions became datapack-driven. */
    public static String legacySignature() {
        return new RitualDefinition(ID, ENERGY_COST, DURATION_TICKS,
                Map.of(Items.BOOK, 4), AbyssalPort.BASIC_SCROLL.get(), 1).signature();
    }
}
