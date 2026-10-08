package dev.abyssalport.energy;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.block.DeityStatueBlock;
import dev.abyssalport.block.entity.DeityStatueBlockEntity;
import dev.abyssalport.content.GatewayContent;
import dev.abyssalport.content.PlaceOfPowerContent;
import dev.abyssalport.content.ShoggothLairContent;
import dev.abyssalport.content.StatueContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Bauformen adapted from BasicStructure, TotemPoleStructure and ArchwayStructure.
 * Copyright (c) 2012-2026 Shinoow; this NeoForge adaptation is LGPL-3.0.
 * The guide and validation share one geometry; no upstream model or texture is used.
 */
public enum PlaceOfPowerLayout {
    BASIC("basic", 2, basic()), TOTEMPOLE("totempole", 3, totem()), ARCHWAY("archway", 1, archway());

    public enum Material { STONE, PILLAR, STATUE, WALL, LOWER_STAIR, UPPER_STAIR, LOWER_SLAB, UPPER_SLAB }
    public record Piece(BlockPos offset, Material material, Direction facing) {
        public BlockPos rotatedOffset(Direction orientation) { return offset.rotate(rotation(orientation)); }
        public BlockState previewState(Direction orientation) {
            BlockState state = switch (material) {
                case STONE -> ShoggothLairContent.MONOLITH_STONE.get().defaultBlockState();
                case PILLAR -> GatewayContent.MONOLITH_PILLAR.get().defaultBlockState();
                case STATUE -> StatueContent.block(StatueContent.Deity.CTHULHU).defaultBlockState().setValue(DeityStatueBlock.FACING,facing);
                case WALL -> Blocks.COBBLESTONE_WALL.defaultBlockState();
                case LOWER_STAIR, UPPER_STAIR -> AbyssalPort.DARKSTONE_BRICK_STAIRS.get().defaultBlockState()
                    .setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, material == Material.UPPER_STAIR ? Half.TOP : Half.BOTTOM);
                case LOWER_SLAB, UPPER_SLAB -> AbyssalPort.DARKSTONE_BRICK_SLAB.get().defaultBlockState()
                    .setValue(SlabBlock.TYPE, material == Material.UPPER_SLAB ? SlabType.TOP : SlabType.BOTTOM);
            };
            return state.rotate(rotation(orientation));
        }
        private boolean matches(BlockState state, Direction orientation) {
            return switch (material) {
                case STONE -> state.is(ShoggothLairContent.MONOLITH_STONE.get());
                case PILLAR -> state.is(GatewayContent.MONOLITH_PILLAR.get());
                case STATUE -> state.getBlock() instanceof DeityStatueBlock;
                case WALL -> state.getBlock() instanceof WallBlock;
                case LOWER_STAIR, UPPER_STAIR -> state.getBlock() instanceof StairBlock
                    && state.getValue(StairBlock.FACING) == rotation(orientation).rotate(facing)
                    && state.getValue(StairBlock.HALF) == (material == Material.UPPER_STAIR ? Half.TOP : Half.BOTTOM);
                case LOWER_SLAB, UPPER_SLAB -> state.getBlock() instanceof SlabBlock
                    && state.getValue(SlabBlock.TYPE) == (material == Material.UPPER_SLAB ? SlabType.TOP : SlabType.BOTTOM);
            };
        }
    }

    private final String id;
    private final int range;
    private final List<Piece> pieces;
    PlaceOfPowerLayout(String id, int range, List<Piece> pieces) { this.id=id; this.range=range; this.pieces=List.copyOf(pieces); }
    public String id() { return id; }
    public int rangeAmplifier() { return range; }
    public List<Piece> pieces() { return pieces; }
    public int bottomLayer() { return pieces.stream().mapToInt(piece -> piece.offset().getY()).min().orElse(0); }
    public int topLayer() { return pieces.stream().mapToInt(piece -> piece.offset().getY()).max().orElse(0); }
    public List<BlockPos> statuePositions(BlockPos anchor, Direction orientation) {
        return pieces.stream().filter(piece -> piece.material() == Material.STATUE)
            .map(piece -> anchor.offset(piece.rotatedOffset(orientation))).toList();
    }
    public boolean containsStatue(BlockPos anchor, Direction orientation, BlockPos statue) {
        return statuePositions(anchor, orientation).contains(statue);
    }
    public boolean matches(Level level, BlockPos anchor, Direction orientation, boolean formed) {
        if (orientation.getAxis() == Direction.Axis.Y) return false;
        for (Piece piece : pieces) {
            BlockPos pos = anchor.offset(piece.rotatedOffset(orientation));
            // A saved label cannot force-load a chunk or stand in for actual materials.
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
            var state = level.getBlockState(pos);
            if (piece.offset().equals(BlockPos.ZERO) && formed) {
                if (!state.is(PlaceOfPowerContent.BASE.get())) return false;
            } else if (!piece.matches(state, orientation)) return false;
            if (piece.material() == Material.STATUE && !(level.getBlockEntity(pos) instanceof DeityStatueBlockEntity)) return false;
        }
        return true;
    }
    public static PlaceOfPowerLayout fromId(String id) {
        for (var layout : values()) if (layout.id.equals(id)) return layout;
        return null;
    }
    public static Rotation rotation(Direction direction) {
        return switch (direction) {
            case SOUTH -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_180;
            case NORTH -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
    private static void add(List<Piece> pieces, int x, int y, int z, Material material) {
        pieces.add(new Piece(new BlockPos(x,y,z), material, Direction.EAST));
    }
    private static List<Piece> basic() {
        var pieces = new ArrayList<Piece>();
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            add(pieces,x,-2,z,Material.STONE);
            add(pieces,x,-1,z,Math.abs(x)+Math.abs(z)==2 ? Material.PILLAR : Material.STONE);
        }
        add(pieces,0,0,0,Material.STONE);
        for (var direction : Direction.Plane.HORIZONTAL) pieces.add(new Piece(BlockPos.ZERO.relative(direction),Material.STATUE,direction));
        return pieces;
    }
    private static List<Piece> totem() {
        var pieces=new ArrayList<Piece>(); add(pieces,0,0,0,Material.STONE);
        for(int y=1;y<=3;y++) add(pieces,0,y,0,Material.STATUE);
        return pieces;
    }
    private static List<Piece> archway() {
        var pieces=new ArrayList<Piece>(); add(pieces,0,0,0,Material.STONE); add(pieces,0,1,0,Material.STATUE);
        for(int y=0;y<3;y++) { add(pieces,-2,y,0,Material.WALL); add(pieces,2,y,0,Material.WALL); }
        pieces.add(new Piece(new BlockPos(-2,3,0),Material.LOWER_STAIR,Direction.EAST));
        pieces.add(new Piece(new BlockPos(-1,3,0),Material.UPPER_STAIR,Direction.WEST));
        add(pieces,0,3,0,Material.UPPER_SLAB);
        pieces.add(new Piece(new BlockPos(1,3,0),Material.UPPER_STAIR,Direction.EAST));
        pieces.add(new Piece(new BlockPos(2,3,0),Material.LOWER_STAIR,Direction.WEST));
        for(int x=-2;x<=2;x++) add(pieces,x,4,0,Material.LOWER_SLAB);
        return pieces;
    }
}
