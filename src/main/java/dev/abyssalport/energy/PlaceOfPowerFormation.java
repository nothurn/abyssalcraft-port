package dev.abyssalport.energy;

import dev.abyssalport.block.entity.DeityStatueBlockEntity;
import dev.abyssalport.block.entity.PlaceOfPowerBlockEntity;
import dev.abyssalport.content.PlaceOfPowerContent;
import dev.abyssalport.content.ShoggothLairContent;
import dev.abyssalport.item.NecronomiconItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Native book interaction: inspect the entire layout before replacing just its activation stone. */
public final class PlaceOfPowerFormation {
    public enum Result { CREATED, INCOMPLETE, PROTECTED, ALREADY_LINKED, NOT_STONE, NO_BOOK }
    private PlaceOfPowerFormation() { }
    public static Result form(ServerLevel level,BlockPos anchor,ServerPlayer player,ItemStack book) {
        if(!NecronomiconItem.isBook(book)) return Result.NO_BOOK;
        if(!level.hasChunkAt(anchor)||!level.getBlockState(anchor).is(ShoggothLairContent.MONOLITH_STONE.get())) return Result.NOT_STONE;
        for(var layout:PlaceOfPowerLayout.values()) for(var orientation:Direction.Plane.HORIZONTAL) {
            if(!layout.matches(level,anchor,orientation,false)) continue;
            for(var piece:layout.pieces()) {
                var pos=anchor.offset(piece.rotatedOffset(orientation));
                if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,book)) return Result.PROTECTED;
            }
            for(var pos:layout.statuePositions(anchor,orientation))
                if(!((DeityStatueBlockEntity)level.getBlockEntity(pos)).canJoinStructure(anchor)) return Result.ALREADY_LINKED;
            if(!level.setBlock(anchor,PlaceOfPowerContent.BASE.get().defaultBlockState(),3)) return Result.INCOMPLETE;
            ((PlaceOfPowerBlockEntity)level.getBlockEntity(anchor)).configure(layout,orientation);
            return Result.CREATED;
        }
        return Result.INCOMPLETE;
    }
}
