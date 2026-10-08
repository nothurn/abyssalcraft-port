package dev.abyssalport;

import dev.abyssalport.block.entity.*;
import dev.abyssalport.content.*;
import dev.abyssalport.energy.CharmAmplifier;
import dev.abyssalport.item.NecronomiconItem;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StatueEnergyGameTests {
    private StatueEnergyGameTests() { }
    /** Controlled entropy for the real transfer routine, never a production timing override. */
    public static final class FixedRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        final int value;
        final List<Integer> bounds = new ArrayList<>();
        public FixedRandom(int value) { super(0); this.value=value; }
        @Override public int nextInt(int bound) { bounds.add(bound); return Math.min(value, bound-1); }
    }
    private static DeityStatueBlockEntity place(GraveyardGameTests.Fixture fixture) {
        fixture.level.setBlock(fixture.node, StatueContent.block(StatueContent.Deity.CTHULHU).defaultBlockState(),2);
        return (DeityStatueBlockEntity)fixture.level.getBlockEntity(fixture.node);
    }
    private static int timer(DeityStatueBlockEntity statue, ServerLevel level) {
        return statue.saveWithFullMetadata(level.registryAccess()).getInt("ActiveTicks");
    }
    private static ServerPlayer nativePlayer(ServerLevel level, BlockPos pos) {
        // The vanilla helper hard-codes isSpectator=false and isCreative=true;
        // it cannot establish the real spectator/nearest-player production gate.
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
            new com.mojang.authlib.GameProfile(UUID.randomUUID(),"statue-pe-audit"),false);
        var player=new ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        player.getInventory().clearContent();player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);
        player.teleportTo(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,0,0);
        return player;
    }
    private static ItemEntity drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,stack);
        item.setNoGravity(true); item.setDeltaMovement(Vec3.ZERO); item.setNeverPickUp();
        if(!level.addFreshEntity(item)) throw new AssertionError("Native dropped-book admission rejected");
        return item;
    }
    private static final class OtherParticipants implements AutoCloseable {
        private final Map<net.minecraft.world.entity.Entity,Vec3> positions=new LinkedHashMap<>();
        private final ServerLevel level;
        OtherParticipants(GraveyardGameTests.Fixture fixture) {
            level=fixture.level;
            for(var player:List.copyOf(level.players()))if(player.position().distanceTo(fixture.node.getCenter())<20) positions.put(player,player.position());
            for(var item:level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(fixture.node).inflate(20),e->NecronomiconItem.isBook(e.getItem()))) positions.put(item,item.position());
            positions.forEach((entity,pos)->move(entity,pos.add(128,0,128)));
        }
        private void move(net.minecraft.world.entity.Entity entity, Vec3 pos) {
            if(entity instanceof ServerPlayer player)player.teleportTo(level,pos.x,pos.y,pos.z,player.getYRot(),player.getXRot());
            else entity.moveTo(pos);
        }
        @Override public void close(){positions.forEach((entity,pos)->{if(!entity.isRemoved())move(entity,pos);});}
    }

    @GameTest(template="empty", batch="statue_independent_transfers")
    public static void DroppedBooksAndCollectorsChargeWithoutPlayersRespectRandomBoundsCapacityAndSavedTolerance(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper);var otherParticipants=new OtherParticipants(fixture)) {
            var statue=place(fixture); var entities=new ArrayList<ItemEntity>();
            try {
                var book=new ItemStack(AbyssalPort.NECRONOMICON.get()); NecronomiconItem.setPotentialEnergy(book,4998);
                var data=book.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag(); data.putInt("PrivateAudit",37);
                book.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
                var dropped=drop(fixture.level,fixture.node.east(),book); entities.add(dropped);
                var device=drop(fixture.level,fixture.node.west(),new ItemStack(AbyssalPort.ENERGY_COLLECTOR.get())); entities.add(device);
                var collectorPos=fixture.node.east(3); fixture.level.setBlock(collectorPos,AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);
                var collector=(EnergyCollectorBlockEntity)fixture.level.getBlockEntity(collectorPos);collector.addEnergy(998);
                var miss=new FixedRandom(1);statue.tickEnergy(fixture.level,miss);
                helper.assertTrue(NecronomiconItem.getPotentialEnergy(dropped.getItem())==4998&&collector.energy()==998&&statue.tolerance()==0
                    &&miss.bounds.equals(List.of(120,120)),"Independent source chances or declined tolerance differ");
                var hit=new FixedRandom(0);statue.tickEnergy(fixture.level,hit);
                helper.assertTrue(NecronomiconItem.getPotentialEnergy(dropped.getItem())==5000&&collector.energy()==1000&&statue.tolerance()==3
                    &&dropped.getItem()!=book&&dropped.getItem().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getInt("PrivateAudit")==37,
                    "Capacity overflow, dropped native stack sync, or transfer tolerance differs");
                statue.tickEnergy(fixture.level,hit);
                helper.assertTrue(statue.tolerance()==3&&timer(statue,fixture.level)==0&&!device.getItem().has(net.minecraft.core.component.DataComponents.CUSTOM_DATA),
                    "Full target spends tolerance, missing player advances held clock, or device item is treated as a book");
                statue.activateCharm(CharmAmplifier.DURATION,null);NecronomiconItem.setPotentialEnergy(dropped.getItem(),0);collector.consumeEnergy(1000);
                hit=new FixedRandom(0);statue.tickEnergy(fixture.level,hit);
                helper.assertTrue(hit.bounds.equals(List.of(80,80))&&NecronomiconItem.getPotentialEnergy(dropped.getItem())==10&&collector.energy()==10&&statue.tolerance()==9,
                    "Generic duration does not independently affect dropped books and collectors");
                var saved=statue.saveWithFullMetadata(fixture.level.registryAccess());
                var loaded=new DeityStatueBlockEntity(fixture.node,statue.getBlockState());loaded.loadWithComponents(saved,fixture.level.registryAccess());
                helper.assertTrue(loaded.tolerance()==9&&loaded.independentChargeBound()==80,"Saved transfer accounting or duration is lost");
                saved.putInt("Tolerance",-1);loaded.loadWithComponents(saved,fixture.level.registryAccess());helper.assertTrue(loaded.tolerance()==0,"Malformed tolerance becomes negative");
                saved.putInt("Tolerance",Integer.MAX_VALUE);statue.loadWithComponents(saved,fixture.level.registryAccess());statue.tickEnergy(fixture.level,new FixedRandom(0));
                helper.assertTrue(statue.tolerance()==0,"Real source threshold no longer clears saved saturated tolerance");
                helper.succeed();
            } finally { entities.forEach(ItemEntity::discard); }
        }
    }

    @GameTest(template="empty", batch="statue_held_source_clock")
    public static void HeldTimerRequiresNearestNonSpectatorHoldingBookChargesBothHandsAndPersistsRestartJitter(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper);var otherParticipants=new OtherParticipants(fixture)) {
            var statue=place(fixture);var player=nativePlayer(fixture.level,fixture.node.east(3));var nearest=nativePlayer(fixture.level,fixture.node.east());
            try {
                player.setGameMode(GameType.CREATIVE);player.moveTo(fixture.node.getCenter().add(3,0,0));
                nearest.setGameMode(GameType.CREATIVE);nearest.moveTo(fixture.node.getCenter().add(1,0,0));
                var main=new ItemStack(AbyssalPort.NECRONOMICON.get());var off=new ItemStack(AbyssalPort.NECRONOMICON.get());
                player.setItemInHand(InteractionHand.MAIN_HAND,main);player.setItemInHand(InteractionHand.OFF_HAND,off);
                var random=new FixedRandom(9);for(int i=0;i<120;i++)statue.tickEnergy(fixture.level,random);
                helper.assertTrue(timer(statue,fixture.level)==0&&NecronomiconItem.getPotentialEnergy(main)==0,"Nearest player without a book fails original held gate: timer="+timer(statue,fixture.level)
                    +", book="+NecronomiconItem.getPotentialEnergy(main)+", nearest="+fixture.level.getNearestPlayer(fixture.node.getX(),fixture.node.getY(),fixture.node.getZ(),7,false));
                nearest.setGameMode(GameType.SPECTATOR);
                for(int i=0;i<119;i++)statue.tickEnergy(fixture.level,random);
                helper.assertTrue(timer(statue,fixture.level)==119&&NecronomiconItem.getPotentialEnergy(main)==0,"Held source clock skips a tick");
                statue.tickEnergy(fixture.level,random);
                helper.assertTrue(timer(statue,fixture.level)==9&&random.bounds.equals(List.of(10))&&NecronomiconItem.getPotentialEnergy(main)==5
                    &&NecronomiconItem.getPotentialEnergy(off)==5&&statue.tolerance()==4,"Held books do not receive real quanta with 0-9 source restart jitter: timer="+timer(statue,fixture.level)
                    +", random="+random.bounds+", main="+NecronomiconItem.getPotentialEnergy(main)+", off="+NecronomiconItem.getPotentialEnergy(off)+", tolerance="+statue.tolerance()
                    +", players="+fixture.level.players().stream().filter(p->p.position().distanceTo(fixture.node.getCenter())<8).map(p->p.getName().getString()+":"+p.position()+":"+p.getMainHandItem()).toList());
                player.setGameMode(GameType.SPECTATOR);statue.tickEnergy(fixture.level,random);
                helper.assertTrue(timer(statue,fixture.level)==9,"Spectator drives the held charging clock");
                player.setGameMode(GameType.CREATIVE);NecronomiconItem.setPotentialEnergy(main,5000);NecronomiconItem.setPotentialEnergy(off,5000);
                for(int i=0;i<111;i++)statue.tickEnergy(fixture.level,random);
                helper.assertTrue(timer(statue,fixture.level)==9&&statue.tolerance()==4,"Full held books wrongly accumulate tolerance or stop the source clock");
                helper.succeed();
            } finally {
                for(var p:List.of(player,nearest)){p.getInventory().clearContent();fixture.level.getServer().getPlayerList().remove(p);}
            }
        }
    }

    @GameTest(template="empty", batch="monolith_pillar_survival")
    public static void NativePillarCraftDropShapeAndTwoConsecutiveBelowStatueAmplifyActualHeldRangeAndCollectorDepth(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper);var otherParticipants=new OtherParticipants(fixture)) {
            var statue=place(fixture);var player=helper.makeMockServerPlayerInLevel();var lower=fixture.node.below(2);var prior=fixture.level.getBlockState(lower);
            helper.assertTrue(fixture.level.getBlockEntity(lower)==null,"Pillar test overlaps real BE below fixture");
            try {
                player.setGameMode(GameType.CREATIVE);player.moveTo(fixture.node.getCenter().add(14,0,0));var book=new ItemStack(AbyssalPort.NECRONOMICON.get());player.setItemInHand(InteractionHand.MAIN_HAND,book);
                var table=fixture.node.west(2);fixture.level.setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),2);
                var craft=new CraftingMenu(12,player.getInventory(),ContainerLevelAccess.create(fixture.level,table));
                for(int slot:List.of(1,2,4,5))craft.getSlot(slot).set(new ItemStack(ShoggothLairContent.MONOLITH_STONE.get()));
                helper.assertTrue(craft.getSlot(0).getItem().is(GatewayContent.MONOLITH_PILLAR.get().asItem())&&craft.getSlot(0).getItem().getCount()==1,"Actual four-stone pillar recipe is absent: result="+craft.getSlot(0).getItem()
                    +", recipe="+fixture.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,"monolith_pillar"))
                    +", inputs="+java.util.stream.IntStream.range(1,10).mapToObj(slot->craft.getSlot(slot).getItem().toString()).toList());
                craft.quickMoveStack(player,0);for(int slot:List.of(1,2,4,5))helper.assertTrue(craft.getSlot(slot).getItem().isEmpty(),"Native pillar crafting does not consume four stones");
                fixture.level.setBlock(lower,GatewayContent.MONOLITH_PILLAR.get().defaultBlockState(),2);
                helper.assertTrue(statue.rangePillars()==0&&statue.chargingRange()==7,"Floating second pillar or full Monolith Stone grants a false bonus");
                fixture.level.setBlock(fixture.node.below(),GatewayContent.MONOLITH_PILLAR.get().defaultBlockState(),2);
                helper.assertTrue(statue.rangePillars()==2&&statue.chargingRange()==15&&statue.collectorSearchRadius()==5,"Two consecutive pillars do not add eight book-range blocks");
                var collectorPos=fixture.node.offset(5,-2,0);var collectorPrior=fixture.level.getBlockState(collectorPos);
                helper.assertTrue(fixture.level.getBlockEntity(collectorPos)==null,"Lower collector fixture overlaps a real BE");
                fixture.level.setBlock(collectorPos,AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);
                var collector=(EnergyCollectorBlockEntity)fixture.level.getBlockEntity(collectorPos);
                try {
                    for(int i=0;i<120;i++)statue.tickEnergy(fixture.level,new FixedRandom(1));
                    statue.tickEnergy(fixture.level,new FixedRandom(0));
                    helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==5&&collector.energy()==5&&statue.collectorPositions().contains(collectorPos),"Real pillar book range or lower search layer is disconnected");
                    fixture.level.setBlock(lower,Blocks.STONE.defaultBlockState(),2);statue.tickEnergy(fixture.level,new FixedRandom(0));
                    helper.assertTrue(statue.rangePillars()==1&&statue.chargingRange()==11&&collector.energy()==5&&!statue.collectorPositions().contains(collectorPos),"Removing lower pillar retains stale collector depth");
                } finally {fixture.level.setBlock(collectorPos,collectorPrior,2);}
                var block=GatewayContent.MONOLITH_PILLAR.get();var shape=block.defaultBlockState().getShape(fixture.level,fixture.node.below()).bounds();
                var drops=Block.getDrops(block.defaultBlockState(),fixture.level,fixture.node.below(),null,player,new ItemStack(Items.IRON_PICKAXE));
                helper.assertTrue(shape.getXsize()==.5&&shape.getZsize()==.5&&shape.getYsize()==1&&block.getExplosionResistance()==24
                    &&drops.size()==1&&drops.getFirst().is(block.asItem()),"Pillar shape/hardness/drop differs from source");
                helper.succeed();
            } finally {fixture.level.setBlock(lower,prior,2);player.getInventory().clearContent();fixture.level.getServer().getPlayerList().remove(player);}
        }
    }

    @GameTest(template="empty", batch="statue_collector_limit")
    public static void CollectorCacheCountsFullAndAdjacentPositionsBeforeTwentyLimitAndRevalidatesLiveTargets(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper);var otherParticipants=new OtherParticipants(fixture)) {
            var statue=place(fixture);statue.activateCharm(CharmAmplifier.RANGE,null);var positions=new ArrayList<BlockPos>();
            outer:for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)if(Math.max(Math.abs(x),Math.abs(z))>=3) {
                var pos=fixture.node.offset(x,0,z);fixture.level.setBlock(pos,AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);
                ((EnergyCollectorBlockEntity)fixture.level.getBlockEntity(pos)).addEnergy(1000);positions.add(pos);if(positions.size()==21)break outer;
            }
            statue.tickEnergy(fixture.level,new FixedRandom(0));
            helper.assertTrue(statue.collectorPositions().size()==20&&statue.collectorPositions().containsAll(positions.subList(0,20))
                &&!statue.collectorPositions().contains(positions.getLast())&&statue.tolerance()==0,"Collector cache limit is applied after full/isolation filtering");
            var targetPos=positions.getFirst();var target=(EnergyCollectorBlockEntity)fixture.level.getBlockEntity(targetPos);target.consumeEnergy(1000);
            statue.tickEnergy(fixture.level,new FixedRandom(0));helper.assertTrue(target.energy()==0,"Cached adjacent collector bypasses live isolation");
            for(var pos:positions.subList(1,positions.size()))fixture.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            statue.tickEnergy(fixture.level,new FixedRandom(0));helper.assertTrue(target.energy()==10&&statue.tolerance()==2,"Live isolated target fails independent cached transfer");
            fixture.level.setBlock(targetPos,Blocks.AIR.defaultBlockState(),2);statue.tickEnergy(fixture.level,new FixedRandom(0));
            helper.assertTrue(statue.tolerance()==2,"Removed cached collector incurs ghost tolerance");helper.succeed();
        }
    }

    @GameTest(template="empty", templateNamespace="abyssalport_worldtest", batch="statue_independent_native_world", timeoutTicks=1000)
    public static void NativeRegisteredStatueChargesDroppedBookAndCollectorOverRealWorldTicksWithNoPlayer(GameTestHelper helper) {
        var level=helper.getLevel();var pos=new BlockPos(580008,300,580008);var saved=new LinkedHashMap<BlockPos,BlockState>();
        var forced=new ArrayList<ChunkPos>();var items=new ArrayList<ItemEntity>();
        Runnable cleanup=()->{items.forEach(ItemEntity::discard);saved.forEach((cell,state)->level.setBlock(cell,state,3));forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false));};
        try {
            var chunk=new ChunkPos(pos);level.getChunk(chunk.x,chunk.z);if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(chunk.x,chunk.z,true);forced.add(chunk);}
            for(var cell:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(4,2,1))){helper.assertTrue(level.getBlockEntity(cell)==null,"Native PE audit intersects existing BE");saved.put(cell.immutable(),level.getBlockState(cell));level.setBlock(cell,Blocks.AIR.defaultBlockState(),3);}
            level.setBlock(pos,StatueContent.block(StatueContent.Deity.HASTUR).defaultBlockState(),3);
            level.setBlock(pos.east(3),AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),3);
            var statue=(DeityStatueBlockEntity)level.getBlockEntity(pos);var collector=(EnergyCollectorBlockEntity)level.getBlockEntity(pos.east(3));
            statue.activateCharm(CharmAmplifier.DURATION,null);
            var item=drop(level,pos.west(),new ItemStack(AbyssalPort.NECRONOMICON.get()));items.add(item);
            helper.runAfterDelay(800,()->{
                try {
                    float book=NecronomiconItem.getPotentialEnergy(item.getItem());
                    helper.assertTrue(level.getNearestPlayer(pos.getX(),pos.getY(),pos.getZ(),7,false)==null&&book>0&&collector.energy()>0&&timer(statue,level)==0,
                        "Actual independent native ticker needs a player or fails: book="+book+", collector="+collector.energy());
                    helper.assertTrue(statue.tolerance()==(int)(book/10)*4+(int)(collector.energy()/10)*2,"Actual independent accepted transfers do not persist exact tolerance");
                    com.mojang.logging.LogUtils.getLogger().info("NATIVE STATUE INDEPENDENT AUDIT: book={} collector={} tolerance={} heldTimer={} after 800 actual ticks, no player",
                        book,collector.energy(),statue.tolerance(),timer(statue,level));helper.succeed();
                } finally {cleanup.run();}
            });
        } catch(RuntimeException|AssertionError error){cleanup.run();throw error;}
    }
}
