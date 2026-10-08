package dev.abyssalport;

import dev.abyssalport.block.DeityStatueBlock;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.content.*;
import dev.abyssalport.energy.*;
import dev.abyssalport.item.NecronomiconItem;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlaceOfPowerGameTests {
    private PlaceOfPowerGameTests() { }
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level;
        final BlockPos anchor;
        final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
        final List<ChunkPos> forced=new ArrayList<>();
        final List<ItemEntity> drops=new ArrayList<>();
        final List<ServerPlayer> players=new ArrayList<>();
        Fixture(GameTestHelper helper) { this(helper,helper.absolutePos(new BlockPos(3,14,3))); }
        Fixture(GameTestHelper helper,BlockPos anchor) {
            level=helper.getLevel();this.anchor=anchor;
            for(int x=(anchor.getX()-6)>>4;x<=(anchor.getX()+6)>>4;x++) for(int z=(anchor.getZ()-6)>>4;z<=(anchor.getZ()+6)>>4;z++) {
                level.getChunk(x,z);var chunk=new ChunkPos(x,z);
                if(!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(x,z,true);forced.add(chunk); }
            }
            for(var pos:BlockPos.betweenClosed(anchor.offset(-6,-5,-6),anchor.offset(6,6,6))) {
                helper.assertTrue(level.getBlockEntity(pos)==null,"Place-of-Power fixture intersects an existing BE at "+pos);
                saved.put(pos.immutable(),level.getBlockState(pos));
            }
            clear();
        }
        void clear() { saved.keySet().forEach(pos->level.setBlock(pos,Blocks.AIR.defaultBlockState(),2)); }
        void build(PlaceOfPowerLayout layout,Direction facing) {
            int deity=0;
            for(var piece:layout.pieces()) {
                var state=piece.previewState(facing);
                if(piece.material()==PlaceOfPowerLayout.Material.STATUE) state=StatueContent.block(StatueContent.Deity.values()[deity++%7])
                    .defaultBlockState().setValue(DeityStatueBlock.FACING,PlaceOfPowerLayout.rotation(facing).rotate(piece.facing()));
                level.setBlock(anchor.offset(piece.rotatedOffset(facing)),state,2);
            }
        }
        ServerPlayer player() {
            var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"power-audit"),false);
            var player=new ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            new io.netty.channel.embedded.EmbeddedChannel(connection);
            level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
            player.getInventory().clearContent();player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);
            player.teleportTo(level,anchor.getX()+5.5,anchor.getY()+.5,anchor.getZ()+.5,0,0);players.add(player);return player;
        }
        ItemEntity book(BlockPos pos) {
            var item=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(AbyssalPort.NECRONOMICON.get()));
            item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);item.setNeverPickUp();
            if(!level.addFreshEntity(item)) throw new AssertionError("Dropped book rejected"); drops.add(item);return item;
        }
        @Override public void close() {
            drops.forEach(ItemEntity::discard);players.forEach(player->level.getServer().getPlayerList().remove(player));
            saved.forEach((pos,state)->level.setBlock(pos,state,2));forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false));
        }
    }
    private static void assertFormed(GameTestHelper helper,Fixture fixture,PlaceOfPowerLayout layout,Direction orientation) {
        var base=(PlaceOfPowerBlockEntity)fixture.level.getBlockEntity(fixture.anchor);
        helper.assertTrue(base!=null&&base.layout()==layout&&base.isValid(),"Native base has no complete "+layout+" geometry");
        for(var pos:layout.statuePositions(fixture.anchor,orientation)) {
            var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(pos);
            helper.assertTrue(statue.structureRangeAmplifier()==layout.rangeAmplifier()&&statue.chargingRange()==7+4*layout.rangeAmplifier()
                &&statue.collectorSearchRadius()==3+layout.rangeAmplifier(),"Actual statue does not receive the source range/radius: "+layout);
        }
    }

    @GameTest(template="empty",batch="power_book_geometry")
    public static void AllThreeLayoutsFormThroughNativeBookUseWithFourOrientationsAndPreserveInventory(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var player=fixture.player();var book=new ItemStack(AbyssalPort.NECRONOMICON.get());NecronomiconItem.setPotentialEnergy(book,777);
            player.setItemInHand(InteractionHand.MAIN_HAND,book);
            for(var layout:PlaceOfPowerLayout.values()) for(var orientation:Direction.Plane.HORIZONTAL) {
                fixture.clear();fixture.build(layout,orientation);
                var before=new LinkedHashMap<BlockPos,BlockState>();
                for(var piece:layout.pieces()) { var pos=fixture.anchor.offset(piece.rotatedOffset(orientation));before.put(pos,fixture.level.getBlockState(pos)); }
                var hit=new BlockHitResult(fixture.anchor.getCenter(),Direction.UP,fixture.anchor,false);
                var result=book.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
                helper.assertTrue(result.consumesAction()&&book.getCount()==1&&NecronomiconItem.getPotentialEnergy(book)==777,"Native book formation consumes PE/item or fails");
                assertFormed(helper,fixture,layout,orientation);
                before.forEach((pos,state)->{if(!pos.equals(fixture.anchor)) helper.assertTrue(fixture.level.getBlockState(pos).equals(state),"Formation overwrites a non-anchor material");});
                var picked=PlaceOfPowerContent.BASE.get().getCloneItemStack(fixture.level,fixture.anchor,fixture.level.getBlockState(fixture.anchor));
                var loot=Block.getDrops(fixture.level.getBlockState(fixture.anchor),fixture.level,fixture.anchor,fixture.level.getBlockEntity(fixture.anchor),player,new ItemStack(Items.NETHERITE_PICKAXE));
                helper.assertTrue(picked.is(ShoggothLairContent.MONOLITH_STONE_ITEM.get())&&loot.size()==1&&loot.getFirst().is(ShoggothLairContent.MONOLITH_STONE_ITEM.get()),"Formed base is portable or loses its original stone on mining");
            }
            helper.assertTrue(PlaceOfPowerLayout.BASIC.pieces().stream().filter(piece->piece.material()==PlaceOfPowerLayout.Material.STONE).count()==15
                &&PlaceOfPowerLayout.BASIC.pieces().stream().filter(piece->piece.material()==PlaceOfPowerLayout.Material.PILLAR).count()==4
                &&PlaceOfPowerLayout.ARCHWAY.pieces().size()==18,"Source material counts changed");
            for(var deity:StatueContent.Deity.values()) {
                fixture.clear();fixture.build(PlaceOfPowerLayout.TOTEMPOLE,Direction.EAST);
                for(int y=1;y<=3;y++) fixture.level.setBlock(fixture.anchor.above(y),StatueContent.block(deity).defaultBlockState(),2);
                helper.assertTrue(PlaceOfPowerFormation.form(fixture.level,fixture.anchor,player,book)==PlaceOfPowerFormation.Result.CREATED,"Source deity cannot join a totem: "+deity);
                assertFormed(helper,fixture,PlaceOfPowerLayout.TOTEMPOLE,Direction.EAST);
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="power_complete_archway")
    public static void MissingBlocksWrongStairDirectionsSlabHalvesAndPrototypeStatuesCannotForm(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var player=fixture.player();var book=new ItemStack(AbyssalPort.NECRONOMICON.get());
            for(var layout:PlaceOfPowerLayout.values()) for(var orientation:Direction.Plane.HORIZONTAL) {
                fixture.clear();fixture.build(layout,orientation);
                for(var piece:layout.pieces()) if(!piece.offset().equals(BlockPos.ZERO)) {
                    var pos=fixture.anchor.offset(piece.rotatedOffset(orientation));var before=fixture.level.getBlockState(pos);
                    fixture.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                    helper.assertTrue(PlaceOfPowerFormation.form(fixture.level,fixture.anchor,player,book)==PlaceOfPowerFormation.Result.INCOMPLETE,
                        "Missing required "+layout+" block is accepted: "+piece);
                    helper.assertTrue(fixture.level.getBlockState(fixture.anchor).is(ShoggothLairContent.MONOLITH_STONE.get()),"Incomplete layout mutates its anchor");
                    fixture.level.setBlock(pos,before,2);
                }
            }
            fixture.clear();fixture.build(PlaceOfPowerLayout.ARCHWAY,Direction.EAST);
            for(var piece:PlaceOfPowerLayout.ARCHWAY.pieces()) {
                var pos=fixture.anchor.offset(piece.offset());var state=fixture.level.getBlockState(pos);BlockState invalid=null;
                if(state.getBlock() instanceof StairBlock) invalid=state.setValue(StairBlock.FACING,state.getValue(StairBlock.FACING).getOpposite());
                else if(state.getBlock() instanceof SlabBlock) invalid=state.setValue(SlabBlock.TYPE,SlabType.DOUBLE);
                if(invalid==null) continue;
                fixture.level.setBlock(pos,invalid,2);
                helper.assertTrue(PlaceOfPowerFormation.form(fixture.level,fixture.anchor,player,book)==PlaceOfPowerFormation.Result.INCOMPLETE,"Wrong stair/slab half accepted");
                fixture.level.setBlock(pos,state,2);
            }
            fixture.clear();fixture.build(PlaceOfPowerLayout.TOTEMPOLE,Direction.EAST);
            fixture.level.setBlock(fixture.anchor.above(2),AbyssalPort.PROTOTYPE_EFFIGY.get().defaultBlockState(),2);
            helper.assertTrue(PlaceOfPowerFormation.form(fixture.level,fixture.anchor,player,book)==PlaceOfPowerFormation.Result.INCOMPLETE,"Legacy prototype counts as a deity statue");
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="power_permissions_save")
    public static void NativeAdventurePermissionInvalidSaveLabelsAndBrokenGeometryCannotGrantBonuses(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            fixture.build(PlaceOfPowerLayout.TOTEMPOLE,Direction.EAST);
            var player=fixture.player();var book=new ItemStack(AbyssalPort.NECRONOMICON.get());player.setGameMode(GameType.ADVENTURE);
            helper.assertTrue(PlaceOfPowerFormation.form(fixture.level,fixture.anchor,player,book)==PlaceOfPowerFormation.Result.PROTECTED
                &&fixture.level.getBlockState(fixture.anchor).is(ShoggothLairContent.MONOLITH_STONE.get()),"Native adventure permissions do not prevent formation");
            player.setGameMode(GameType.CREATIVE);helper.assertTrue(PlaceOfPowerFormation.form(fixture.level,fixture.anchor,player,book)==PlaceOfPowerFormation.Result.CREATED,"Valid totem rejected");
            var base=(PlaceOfPowerBlockEntity)fixture.level.getBlockEntity(fixture.anchor);
            var pos=fixture.anchor.above();var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(pos);
            var saved=base.saveWithFullMetadata(fixture.level.registryAccess());
            var copy=new PlaceOfPowerBlockEntity(fixture.anchor,base.getBlockState());copy.setLevel(fixture.level);copy.loadWithComponents(saved,fixture.level.registryAccess());
            helper.assertTrue(copy.layout()==PlaceOfPowerLayout.TOTEMPOLE&&copy.isValid(),"Real layout does not survive native save/load");
            var statueSave=statue.saveWithFullMetadata(fixture.level.registryAccess());statueSave.putInt("Tolerance",42);
            statue.loadWithComponents(statueSave,fixture.level.registryAccess());
            var drop=fixture.book(fixture.anchor.east());statue.tickEnergy(fixture.level,new StatueEnergyGameTests.FixedRandom(0));
            helper.assertTrue(NecronomiconItem.getPotentialEnergy(drop.getItem())==5&&statue.tolerance()==42,"Valid totem cannot charge or wipes historical tolerance");
            var top=fixture.anchor.above(3);var prior=fixture.level.getBlockState(top);fixture.level.setBlock(top,Blocks.AIR.defaultBlockState(),2);
            helper.assertTrue(statue.structureRangeAmplifier()==0&&statue.chargingRange()==7&&!base.isValid(),"Breaking a structure leaves bonuses active until the next ticker");
            float priorEnergy=NecronomiconItem.getPotentialEnergy(drop.getItem());statue.tickEnergy(fixture.level,new StatueEnergyGameTests.FixedRandom(0));
            helper.assertTrue(NecronomiconItem.getPotentialEnergy(drop.getItem())==priorEnergy,"Invalid adjacent-statue totem still charges");
            fixture.level.setBlock(top,prior,2);helper.assertTrue(base.bindStatues(),"Repaired real structure cannot rebind");assertFormed(helper,fixture,PlaceOfPowerLayout.TOTEMPOLE,Direction.EAST);
            saved.putString("Structure","not-a-real-layout");copy.loadWithComponents(saved,fixture.level.registryAccess());
            helper.assertTrue(copy.layout()==null&&!copy.isValid(),"Unknown saved structure supplies authorization");
            statueSave.putLong("PowerBasePos",new BlockPos(30_000_000,300,30_000_000).asLong());statue.loadWithComponents(statueSave,fixture.level.registryAccess());
            helper.assertTrue(statue.structureBase()==null&&statue.structureRangeAmplifier()==0,"Far forged base position can query/load remote chunks");
            statueSave.putLong("PowerBasePos",fixture.anchor.asLong());statue.loadWithComponents(statueSave,fixture.level.registryAccess());
            fixture.level.setBlock(fixture.anchor,ShoggothLairContent.MONOLITH_STONE.get().defaultBlockState(),2);
            helper.assertTrue(statue.structureRangeAmplifier()==0,"Removed base plus a saved link grants bonuses");helper.succeed();
        }
    }

    @GameTest(template="empty",batch="power_collector_layers")
    public static void StructureRangeReachesLowerCollectorsAndDoesNotGrowTolerance(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            fixture.build(PlaceOfPowerLayout.TOTEMPOLE,Direction.EAST);
            var player=fixture.player();helper.assertTrue(PlaceOfPowerFormation.form(fixture.level,fixture.anchor,player,new ItemStack(AbyssalPort.NECRONOMICON.get()))==PlaceOfPowerFormation.Result.CREATED,"Totem does not form");
            var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(fixture.anchor.above());
            var collectorPos=fixture.anchor.above().offset(6,-3,0);
            fixture.level.setBlock(collectorPos,AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);
            var collector=(EnergyCollectorBlockEntity)fixture.level.getBlockEntity(collectorPos);
            statue.tickEnergy(fixture.level,new StatueEnergyGameTests.FixedRandom(0));
            helper.assertTrue(statue.collectorPositions().contains(collectorPos)&&collector.energy()==5&&statue.tolerance()==0,"Real totem collector radius/depth or cost suppression fails");
            statue.activateCharm(CharmAmplifier.RANGE,null);
            helper.assertTrue(statue.chargingRange()==23&&statue.collectorSearchRadius()==8&&statue.energyQuanta()==10,"Charm and structure factors do not compose");
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="power_native_placement_veto")
    public static void NativePlacementVetoRestoresActivationStoneAndLeavesNoAuthorizedSavedLinks(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            fixture.build(PlaceOfPowerLayout.TOTEMPOLE,Direction.EAST);var player=fixture.player();
            var book=new ItemStack(AbyssalPort.NECRONOMICON.get());NecronomiconItem.setPotentialEnergy(book,123);player.setItemInHand(InteractionHand.MAIN_HAND,book);
            var events=new int[]{0};
            java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent> veto=event->{
                if(event.getEntity()==player&&event.getPos().equals(fixture.anchor)) { events[0]++;event.setCanceled(true); }
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(veto);
            try {
                var hit=new BlockHitResult(fixture.anchor.getCenter(),Direction.UP,fixture.anchor,false);
                var result=book.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
                helper.assertTrue(result==InteractionResult.FAIL&&events[0]==1
                    &&fixture.level.getBlockState(fixture.anchor).is(ShoggothLairContent.MONOLITH_STONE.get())
                    &&fixture.level.getBlockEntity(fixture.anchor)==null&&NecronomiconItem.getPotentialEnergy(book)==123,
                    "Native veto misses formation, duplicates its event or does not roll back the activation stone/book");
                for(var pos:PlaceOfPowerLayout.TOTEMPOLE.statuePositions(fixture.anchor,Direction.EAST)) {
                    var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(pos);
                    helper.assertTrue(statue.structureRangeAmplifier()==0&&statue.canJoinStructure(fixture.anchor),"Canceled placement leaves authorized or unrepairable statue links");
                }
            } finally {net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(veto);}
            var hit=new BlockHitResult(fixture.anchor.getCenter(),Direction.UP,fixture.anchor,false);book.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            assertFormed(helper,fixture,PlaceOfPowerLayout.TOTEMPOLE,Direction.EAST);helper.succeed();
        }
    }

    @GameTest(template="empty",batch="power_loaded_chunk_boundary")
    public static void CompleteLoadedStructureCanCrossAChunkBoundaryButANonMemberSavedLinkCannotUseIt(GameTestHelper helper) {
        var origin=helper.absolutePos(new BlockPos(3,14,3));
        var anchor=new BlockPos((origin.getX()&~15)+15,origin.getY(),(origin.getZ()&~15)+15);
        try(var fixture=new Fixture(helper,anchor)) {
            fixture.build(PlaceOfPowerLayout.BASIC,Direction.EAST);var player=fixture.player();
            helper.assertTrue(new ChunkPos(anchor).toLong()!=new ChunkPos(anchor.east()).toLong(),"Boundary fixture does not cross actual native chunks");
            helper.assertTrue(PlaceOfPowerFormation.form(fixture.level,anchor,player,new ItemStack(AbyssalPort.NECRONOMICON.get()))==PlaceOfPowerFormation.Result.CREATED,"Fully loaded cross-chunk structure is rejected");
            assertFormed(helper,fixture,PlaceOfPowerLayout.BASIC,Direction.EAST);
            var pos=anchor.above(2);fixture.level.setBlock(pos,StatueContent.block(StatueContent.Deity.HASTUR).defaultBlockState(),2);
            var stranger=(DeityStatueBlockEntity)fixture.level.getBlockEntity(pos);var tag=stranger.saveWithFullMetadata(fixture.level.registryAccess());
            tag.putLong("PowerBasePos",anchor.asLong());stranger.loadWithComponents(tag,fixture.level.registryAccess());
            helper.assertTrue(stranger.structureRangeAmplifier()==0&&stranger.chargingRange()==7,"Saved pointer to a nearby valid base authorizes a non-member statue");
            helper.succeed();
        }
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="power_native_normal_world",timeoutTicks=1100)
    public static void AllThreeRealStructuresChargeIndoorsViaRegisteredTicksWithoutNewTolerance(GameTestHelper helper) {
        var fixtures=new ArrayList<Fixture>();var books=new ArrayList<ItemEntity>();var statues=new ArrayList<DeityStatueBlockEntity>();
        Runnable cleanup=()->fixtures.forEach(Fixture::close);
        try {
            int placementIndex=0;
            for(var layout:PlaceOfPowerLayout.values()) {
                var fixture=new Fixture(helper,new BlockPos(600008+placementIndex++*48,300,600008));fixtures.add(fixture);
                fixture.build(layout,Direction.EAST);
                var player=fixture.player();helper.assertTrue(PlaceOfPowerFormation.form(fixture.level,fixture.anchor,player,new ItemStack(AbyssalPort.NECRONOMICON.get()))==PlaceOfPowerFormation.Result.CREATED,"Native structure formation fails");
                // Build a real opaque roof. The only charging exception must be valid structure membership.
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)fixture.level.setBlock(fixture.anchor.offset(x,6,z),Blocks.STONE.defaultBlockState(),3);
                var pos=layout.statuePositions(fixture.anchor,Direction.EAST).getFirst();
                var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(pos);statues.add(statue);
                books.add(fixture.book(fixture.anchor.east(3).south(3)));
                player.teleportTo(fixture.level,fixture.anchor.getX()+64.5,301,fixture.anchor.getZ()+.5,0,0);
            }
            helper.runAfterDelay(900,()->{
                try {
                    for(int index=0;index<fixtures.size();index++) {
                        var fixture=fixtures.get(index);var layout=PlaceOfPowerLayout.values()[index];float pe=NecronomiconItem.getPotentialEnergy(books.get(index).getItem());
                        helper.assertTrue(!fixture.level.canSeeSky(statues.get(index).getBlockPos())&&pe>0,"Actual indoor native "+layout+" does not charge: "+pe);
                        for(var pos:layout.statuePositions(fixture.anchor,Direction.EAST)) helper.assertTrue(((DeityStatueBlockEntity)fixture.level.getBlockEntity(pos)).tolerance()==0,"Complete native structure grows tolerance");
                        com.mojang.logging.LogUtils.getLogger().info("NATIVE PLACE OF POWER AUDIT: {} book={} PE, range={}, all tolerances=0, indoor after 900 real ticks",layout.id(),pe,statues.get(index).chargingRange());
                    }
                    helper.succeed();
                } finally {cleanup.run();}
            });
        } catch(RuntimeException|AssertionError error) { cleanup.run();throw error; }
    }
}
