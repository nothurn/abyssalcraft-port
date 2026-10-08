package dev.abyssalport.block.entity;

import dev.abyssalport.block.GatewayAnchorBlock;
import dev.abyssalport.content.GatewayContent;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Anchor identity and reciprocal portal pairing survive world reloads. Unknown destinations fail closed. */
public final class GatewayAnchorBlockEntity extends BlockEntity {
    private ResourceKey<Level> destination;
    private UUID portalId = UUID.randomUUID();
    private UUID partnerId;
    private BlockPos partner;
    public GatewayAnchorBlockEntity(BlockPos pos, BlockState state) { super(GatewayContent.ANCHOR_ENTITY.get(), pos, state); }
    public ResourceKey<Level> destination() { return destination; }
    public UUID portalId() { return portalId; }
    public UUID partnerId() { return partnerId; }
    public BlockPos partner() { return partner; }
    public void configure(ResourceKey<Level> destination) { this.destination = destination; setChanged(); }
    public void pairWith(GatewayAnchorBlockEntity other) {
        partner = other.getBlockPos().immutable(); partnerId = other.portalId(); setChanged();
    }
    public boolean intact() {
        return level != null && getBlockState().hasProperty(GatewayAnchorBlock.ACTIVE)
                && getBlockState().getValue(GatewayAnchorBlock.ACTIVE) && destination != null
                && level.getBlockState(worldPosition.above()).is(GatewayContent.PORTAL.get())
                && level.getBlockState(worldPosition.above(2)).is(GatewayContent.PORTAL.get());
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, GatewayAnchorBlockEntity anchor) {
        if (state.getValue(GatewayAnchorBlock.ACTIVE) && level.getGameTime() % 20 == 0 && !anchor.intact()) {
            for (int y = 1; y <= 2; y++) if (level.getBlockState(pos.above(y)).is(GatewayContent.PORTAL.get())) level.removeBlock(pos.above(y), false);
            level.setBlock(pos, state.setValue(GatewayAnchorBlock.ACTIVE, false), 3);
        }
    }
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("PortalId", portalId);
        if (destination != null) tag.putString("Destination", destination.location().toString());
        if (partner != null && partnerId != null) { tag.put("Partner", NbtUtils.writeBlockPos(partner)); tag.putUUID("PartnerId", partnerId); }
    }
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        portalId = tag.hasUUID("PortalId") ? tag.getUUID("PortalId") : UUID.randomUUID();
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Destination"));
        destination = id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
        partner = NbtUtils.readBlockPos(tag, "Partner").orElse(null);
        partnerId = partner != null && tag.hasUUID("PartnerId") ? tag.getUUID("PartnerId") : null;
    }
}
