package dev.abyssalport.energy;

import dev.abyssalport.content.StatueContent.Deity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Source DisruptionEvent adapted to the native NeoForge bus (Shinoow, LGPL-3.0). */
public final class DisruptionEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;
    private final BlockPos pos;
    private final Deity deity;
    private final DisruptionCatalogue effect;
    private final List<ServerPlayer> players;
    public DisruptionEvent(ServerLevel level,BlockPos pos,Deity deity,DisruptionCatalogue effect,List<ServerPlayer> players) {
        this.level=level;this.pos=pos.immutable();this.deity=deity;this.effect=effect;this.players=List.copyOf(players);
    }
    public ServerLevel level() { return level; }
    public BlockPos pos() { return pos; }
    public Deity deity() { return deity; }
    public DisruptionCatalogue effect() { return effect; }
    public List<ServerPlayer> players() { return players; }
}
