package dev.abyssalport.network;

import dev.abyssalport.AbyssalPort;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server-authoritative discovery snapshot sent only when the field guide opens. */
public record BookOpenPayload(int hand, int discoveries) implements CustomPacketPayload {
    public static final Type<BookOpenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "open_field_guide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BookOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BookOpenPayload::hand,
            ByteBufCodecs.VAR_INT, BookOpenPayload::discoveries,
            BookOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
