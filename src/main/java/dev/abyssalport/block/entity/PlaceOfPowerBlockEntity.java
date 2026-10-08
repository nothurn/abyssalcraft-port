package dev.abyssalport.block.entity;

import dev.abyssalport.content.PlaceOfPowerContent;
import dev.abyssalport.energy.PlaceOfPowerLayout;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent layout identity, never a persistent authorization to receive structure bonuses. */
public final class PlaceOfPowerBlockEntity extends BlockEntity {
    private PlaceOfPowerLayout layout;
    private Direction orientation=Direction.EAST;
    public PlaceOfPowerBlockEntity(BlockPos pos,BlockState state) { super(PlaceOfPowerContent.ENTITY.get(),pos,state); }
    public PlaceOfPowerLayout layout() { return layout; }
    public Direction orientation() { return orientation; }
    public void configure(PlaceOfPowerLayout layout,Direction orientation) {
        if (orientation.getAxis()==Direction.Axis.Y) throw new IllegalArgumentException("Horizontal orientation required");
        this.layout=layout; this.orientation=orientation; setChanged(); bindStatues();
        if(level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    public boolean hasGeometry() { return level!=null && layout!=null && layout.matches(level,worldPosition,orientation,true); }
    public boolean containsStatue(BlockPos pos) { return layout!=null && layout.containsStatue(worldPosition,orientation,pos); }
    public boolean isValid() {
        if(!hasGeometry()) return false;
        for(var pos:layout.statuePositions(worldPosition,orientation))
            if(!(level.getBlockEntity(pos) instanceof DeityStatueBlockEntity statue) || !statue.canJoinStructure(worldPosition)) return false;
        return true;
    }
    public boolean bindStatues() {
        if(!isValid()) return false;
        for(var pos:layout.statuePositions(worldPosition,orientation)) ((DeityStatueBlockEntity)level.getBlockEntity(pos)).linkStructure(worldPosition);
        return true;
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,PlaceOfPowerBlockEntity base) {
        // Repair/new statue replacement rebinds on the original 100-tick validation cadence.
        // Bonus lookups separately verify geometry immediately, not a five-second old flag.
        if(level.getGameTime()%100==0) base.bindStatues();
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);
        if(layout!=null) tag.putString("Structure",layout.id());
        tag.putString("Orientation",orientation.getSerializedName());
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); layout=PlaceOfPowerLayout.fromId(tag.getString("Structure"));
        Direction loaded=Direction.byName(tag.getString("Orientation"));
        orientation=loaded!=null&&loaded.getAxis()!=Direction.Axis.Y?loaded:Direction.EAST;
    }
}
