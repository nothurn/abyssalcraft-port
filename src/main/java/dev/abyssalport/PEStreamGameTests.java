package dev.abyssalport;

import dev.abyssalport.block.EnergyRelayBlock;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.content.StatueContent;
import dev.abyssalport.energy.*;
import dev.abyssalport.network.PEStreamPayload;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.*;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PEStreamGameTests {
    private PEStreamGameTests() { }
    /** Real native broadcast/connection, but an embedded channel: no socket/client. */
    private static final class Observer implements AutoCloseable {
        final GraveyardGameTests.Fixture fixture;
        final io.netty.channel.embedded.EmbeddedChannel channel;
        final ServerPlayer player;
        final List<PEStreamPayload> received=new ArrayList<>();
        Observer(GraveyardGameTests.Fixture fixture) {
            this(fixture,true);
        }
        Observer(GraveyardGameTests.Fixture fixture, boolean negotiated) {
            this.fixture=fixture;
            var cookie=CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"pe-stream-observer"),false);
            player=new ServerPlayer(fixture.level.getServer(),fixture.level,cookie.gameProfile(),cookie.clientInformation());
            var connection=new Connection(PacketFlow.SERVERBOUND);
            channel=new io.netty.channel.embedded.EmbeddedChannel(connection);
            if(negotiated) NetworkRegistry.configureMockConnection(connection);
            fixture.level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
            player.setGameMode(GameType.SPECTATOR); player.getInventory().clearContent(); player.setNoGravity(true);
            player.teleportTo(fixture.level,fixture.node.getX()+.5,fixture.node.getY()+.5,fixture.node.getZ()+.5,0,0);
            flush(); received.clear();
        }
        void flush() {
            channel.runPendingTasks(); Object packet;
            while((packet=channel.readOutbound())!=null) {
                if(packet instanceof ClientboundCustomPayloadPacket payload && payload.payload() instanceof PEStreamPayload stream) received.add(stream);
                io.netty.util.ReferenceCountUtil.release(packet);
            }
        }
        @Override public void close() {
            fixture.level.getServer().getPlayerList().remove(player); channel.finishAndReleaseAll();
        }
    }
    @GameTest(template="empty",batch="pe_unnegotiated_stream_observer")
    public static void UnnegotiatedAndDistantObserversCannotBreakOrReceiveAcceptedRelayTransfer(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper);var observer=new Observer(fixture,false);var admitted=new Observer(fixture)) {
            helper.assertTrue(!NetworkRegistry.hasChannel(observer.player.connection,PEStreamPayload.TYPE.id())
                    && NetworkRegistry.hasChannel(admitted.player.connection,PEStreamPayload.TYPE.id()),"Fixture does not exercise both native negotiation states");
            var state=AbyssalPort.ENERGY_RELAY.get().defaultBlockState().setValue(EnergyRelayBlock.FACING,Direction.EAST);
            fixture.level.setBlock(fixture.node,state,2); fixture.level.setBlock(fixture.node.east(4),AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);
            var relay=(EnergyRelayBlockEntity)fixture.level.getBlockEntity(fixture.node);var receiver=(EnergyCollectorBlockEntity)fixture.level.getBlockEntity(fixture.node.east(4));relay.addEnergy(40);
            for(int tick=0;tick<40;tick++)EnergyRelayBlockEntity.serverTick(fixture.level,fixture.node,state,relay);
            observer.flush();admitted.flush();helper.assertTrue(receiver.energy()==20 && relay.energy()==20 && observer.received.isEmpty() && admitted.received.size()==1,
                    "Unnegotiated observer changes PE or receives unavailable payload");
            admitted.received.clear();admitted.player.teleportTo(fixture.level,fixture.node.getX()+100,fixture.node.getY(),fixture.node.getZ(),0,0);
            for(int tick=0;tick<40;tick++)EnergyRelayBlockEntity.serverTick(fixture.level,fixture.node,state,relay);
            observer.flush();admitted.flush();helper.assertTrue(receiver.energy()==40 && relay.energy()==0 && observer.received.isEmpty() && admitted.received.isEmpty(),
                    "Distant observer receives stream or changes actual accepted PE");helper.succeed();
        }
    }
    @GameTest(template="empty",batch="pe_actual_stream_packets")
    public static void NativeAcceptedRelaySendsOneActualPayloadButRedstoneObstructionAndFullReceiverSendNone(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper);var observer=new Observer(fixture)) {
            helper.assertTrue(BuiltInRegistries.PARTICLE_TYPE.getKey(PEStreams.STREAM.get()).toString().equals("abyssalport:pe_stream"),"Owned native particle type is unregistered");
            var state=AbyssalPort.ENERGY_RELAY.get().defaultBlockState().setValue(EnergyRelayBlock.FACING,Direction.EAST);
            fixture.level.setBlock(fixture.node,state,2); fixture.level.setBlock(fixture.node.east(4),AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);
            var relay=(EnergyRelayBlockEntity)fixture.level.getBlockEntity(fixture.node); var receiver=(EnergyCollectorBlockEntity)fixture.level.getBlockEntity(fixture.node.east(4)); relay.addEnergy(100);
            for(int tick=0;tick<40;tick++)EnergyRelayBlockEntity.serverTick(fixture.level,fixture.node,state,relay);
            observer.flush(); helper.assertTrue(receiver.energy()==20 && relay.energy()==80 && observer.received.equals(List.of(new PEStreamPayload(fixture.node,fixture.node.east(4)))),
                    "Accepted native transfer does not broadcast exactly one stream payload: "+observer.received);
            observer.received.clear(); receiver.addEnergy(receiver.capacity());
            for(int tick=0;tick<40;tick++)EnergyRelayBlockEntity.serverTick(fixture.level,fixture.node,state,relay);
            observer.flush(); helper.assertTrue(observer.received.isEmpty() && relay.energy()==80,"Full receiver emits a fake accepted stream");
            receiver.consumeEnergy(receiver.energy());fixture.level.setBlock(fixture.node.east(),Blocks.STONE.defaultBlockState(),2);
            for(int tick=0;tick<40;tick++)EnergyRelayBlockEntity.serverTick(fixture.level,fixture.node,state,relay);
            observer.flush();helper.assertTrue(observer.received.isEmpty() && receiver.energy()==0,"Obstructed transfer emits stream");
            fixture.level.setBlock(fixture.node.east(),Blocks.AIR.defaultBlockState(),2);fixture.level.setBlock(fixture.node.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
            for(int tick=0;tick<40;tick++)EnergyRelayBlockEntity.serverTick(fixture.level,fixture.node,state,relay);
            observer.flush();helper.assertTrue(observer.received.isEmpty() && receiver.energy()==0,"Powered relay emits stream");
            helper.succeed();
        }
    }
    @GameTest(template="empty",batch="pe_stream_statue_and_codec")
    public static void RealStatueAcceptedCollectorAndDroppedBookStreamsRoundTripWithoutLoadingUnknownChunks(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper);var observer=new Observer(fixture)) {
            var from=fixture.node; var to=from.east(3); fixture.level.setBlock(from,StatueContent.block(StatueContent.Deity.CTHULHU).defaultBlockState(),2);
            fixture.level.setBlock(to,AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);
            var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(from); var receiver=(EnergyCollectorBlockEntity)fixture.level.getBlockEntity(to);
            var book=new net.minecraft.world.entity.item.ItemEntity(fixture.level,from.getX()+1.5,from.getY()+.5,from.getZ()+.5,new ItemStack(AbyssalPort.NECRONOMICON.get()));
            book.setNoGravity(true); book.setNeverPickUp(); helper.assertTrue(fixture.level.addFreshEntity(book),"Dropped-book admission failed");
            try {
                statue.tickEnergy(fixture.level,new StatueEnergyGameTests.FixedRandom(0));observer.flush();
                helper.assertTrue(receiver.energy()==5 && dev.abyssalport.item.NecronomiconItem.getPotentialEnergy(book.getItem())==5
                        && observer.received.contains(new PEStreamPayload(from,to)) && observer.received.contains(new PEStreamPayload(from,book.blockPosition())),
                        "Actual statue outputs do not emit admitted native streams: "+observer.received);
                observer.received.clear(); receiver.addEnergy(receiver.capacity()); dev.abyssalport.item.NecronomiconItem.setPotentialEnergy(book.getItem(),5000);
                statue.tickEnergy(fixture.level,new StatueEnergyGameTests.FixedRandom(0));observer.flush(); helper.assertTrue(observer.received.isEmpty(),"Full statue receivers emit visual charge");
                var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),fixture.level.registryAccess());
                try {
                    var payload=new PEStreamPayload(new BlockPos(-17,140,-1),new BlockPos(-13,140,-1)); PEStreamPayload.STREAM_CODEC.encode(buffer,payload);
                    helper.assertTrue(buffer.readableBytes()==16 && PEStreamPayload.STREAM_CODEC.decode(buffer).equals(payload),"Actual stream codec loses signed positions or sends per-particle data");
                } finally { buffer.release(); }
                var forced=Set.copyOf(fixture.level.getForcedChunks()); var unknown=new BlockPos(1_000_000,140,1_000_000);
                helper.assertTrue(!fixture.level.hasChunkAt(unknown) && !PEStreams.show(fixture.level,from,unknown,1)
                        && !fixture.level.hasChunkAt(unknown) && forced.equals(fixture.level.getForcedChunks()),"Visual stream loads/forces distant chunks");
                for(float declined:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY}) helper.assertTrue(!PEStreams.show(fixture.level,from,to,declined),"Declined/nonfinite PE is visualized");
                observer.flush();helper.assertTrue(observer.received.isEmpty(),"Rejected stream path still broadcasts");helper.succeed();
            } finally { book.discard(); }
        }
    }
}
