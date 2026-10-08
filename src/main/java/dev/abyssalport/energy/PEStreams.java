package dev.abyssalport.energy;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.network.PEStreamPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.registries.*;

/** Visual notification only after admitted PE; never changes balances or loads chunks.
 * Original PEUtils/PEStreamMessage behaviour, Shinoow, LGPL-3.0; own media.
 */
public final class PEStreams {
    private static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, AbyssalPort.MOD_ID);
    public static final DeferredHolder<net.minecraft.core.particles.ParticleType<?>, SimpleParticleType> STREAM = PARTICLES.register("pe_stream", () -> new SimpleParticleType(false));
    private PEStreams() { }
    public static void register(IEventBus bus) { PARTICLES.register(bus); }
    public static boolean show(ServerLevel level, BlockPos from, BlockPos to, float accepted) {
        if (!Float.isFinite(accepted) || accepted<=0 || from.equals(to) || !level.hasChunkAt(from) || !level.hasChunkAt(to)
                || from.distSqr(to)>PEStreamGeometry.MAX_LENGTH*PEStreamGeometry.MAX_LENGTH) return false;
        double x=(from.getX()+to.getX())*.5+.5, y=(from.getY()+to.getY())*.5+.5, z=(from.getZ()+to.getZ())*.5+.5;
        var payload=new PEStreamPayload(from,to);
        // Visuals must never reject an admitted energy transfer for a player without this optional channel.
        for (var player : level.players()) {
            if (player.connection != null && player.distanceToSqr(x,y,z)<=32*32
                    && NetworkRegistry.hasChannel(player.connection, PEStreamPayload.TYPE.id())) PacketDistributor.sendToPlayer(player,payload);
        }
        return true;
    }
}
