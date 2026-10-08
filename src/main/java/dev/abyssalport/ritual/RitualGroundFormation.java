package dev.abyssalport.ritual;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandProgressionContent;
import dev.abyssalport.content.DreadlandsContent;
import dev.abyssalport.item.NecronomiconItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Tiered cobblestone formations adapted from RitualUtil (Shinoow, LGPL-3.0). */
public final class RitualGroundFormation {
    public enum Result {
        CREATED,
        NOT_COBBLESTONE,
        BOOK_TOO_WEAK,
        WRONG_DIMENSION,
        INCOMPLETE,
        OBSTRUCTED,
        CROSSES_CHUNK,
        UNLOADED,
        PROTECTED
    }

    private RitualGroundFormation() {
    }

    public static Result form(ServerLevel level, BlockPos center, ServerPlayer player, ItemStack book) {
        Result check = inspect(level, center, player, book);
        if (check != Result.CREATED) {
            return check;
        }

        // Inspect every position before changing any block: invalid layouts stay untouched.
        int tier = materialTier(level.getBlockState(center));
        boolean darkstone=level.getBlockState(center).is(AbyssalPort.DARKSTONE_COBBLESTONE.get());
        level.setBlock(center, (darkstone?dev.abyssalport.content.DarklandsStructuresContent.ALTAR.get():RitualRequirements.altar(tier)).defaultBlockState(), 3);
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            level.setBlock(center.offset(offset), (darkstone?dev.abyssalport.content.DarklandsStructuresContent.PEDESTAL.get():RitualRequirements.pedestal(tier)).defaultBlockState(), 3);
        }
        EarlyResearch.unlock(player, EarlyResearch.RITUAL_GROUND);
        return Result.CREATED;
    }

    public static Result inspect(ServerLevel level, BlockPos center, ServerPlayer player, ItemStack book) {
        if (!level.hasChunkAt(center)) {
            return Result.UNLOADED;
        }
        var material = level.getBlockState(center).getBlock();
        if (!isFormationMaterial(level.getBlockState(center))) {
            return Result.NOT_COBBLESTONE;
        }
        int tier = materialTier(level.getBlockState(center));
        if (NecronomiconItem.tier(book) < tier) return Result.BOOK_TOO_WEAK;
        if (tier == 1 && !level.dimension().equals(WastelandContent.DIMENSION)
                || tier == 2 && !level.dimension().equals(DreadlandsContent.DIMENSION)) return Result.WRONG_DIMENSION;

        List<BlockPos> targets = new ArrayList<>();
        targets.add(center);
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            BlockPos pedestal = center.offset(offset);
            if (pedestal.getX() >> 4 != center.getX() >> 4
                    || pedestal.getZ() >> 4 != center.getZ() >> 4) {
                return Result.CROSSES_CHUNK;
            }
            if (!level.hasChunkAt(pedestal)) {
                return Result.UNLOADED;
            }
            if (!level.getBlockState(pedestal).is(material)) {
                return Result.INCOMPLETE;
            }
            targets.add(pedestal);
        }

        for (BlockPos target : targets) {
            if (!level.mayInteract(player, target)
                    || !player.mayUseItemAt(target, Direction.UP, book)) {
                return Result.PROTECTED;
            }
        }

        // As in the original, each pedestal needs clear adjacent space on its level.
        for (int index = 1; index < targets.size(); index++) {
            BlockPos pedestal = targets.get(index);
            for (BlockPos around : BlockPos.betweenClosed(pedestal.offset(-1, 0, -1),
                    pedestal.offset(1, 0, 1))) {
                if (around.equals(pedestal)) {
                    continue;
                }
                if (!level.hasChunkAt(around)) {
                    return Result.UNLOADED;
                }
                if (level.getBlockState(around).isCollisionShapeFullBlock(level, around)) {
                    return Result.OBSTRUCTED;
                }
            }
        }
        return Result.CREATED;
    }

    public static boolean isFormationMaterial(net.minecraft.world.level.block.state.BlockState state) {
        return materialTier(state) >= 0;
    }
    private static int materialTier(net.minecraft.world.level.block.state.BlockState state) {
        if(state.is(Blocks.COBBLESTONE)||state.is(AbyssalPort.DARKSTONE_COBBLESTONE.get())) return 0;
        if(state.is(WastelandContent.COBBLESTONE.get())) return 1;
        return state.is(DreadlandsContent.COBBLESTONE.get()) ? 2 : -1;
    }
}
