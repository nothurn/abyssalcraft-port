package dev.abyssalport.network;

import dev.abyssalport.AbyssalPort;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** One native payload per accepted transfer, not one packet per particle. */
public record PEStreamPayload(BlockPos from, BlockPos to) implements CustomPacketPayload {
    public static final Type<PEStreamPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "pe_stream"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PEStreamPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PEStreamPayload::from, BlockPos.STREAM_CODEC, PEStreamPayload::to, PEStreamPayload::new);
    public PEStreamPayload { from=from.immutable(); to=to.immutable(); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
