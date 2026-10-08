package dev.abyssalport;

import dev.abyssalport.block.DeityStatueBlock;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.content.*;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.worldgen.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StatueGameTests {
    private StatueGameTests() { }
    @GameTest(template="empty",batch="statues_native_placement")
    public static void SevenDeitiesPlaceFaceRotateDropAndUseTheirOwnPersistedNativeTicker(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var player=helper.makeMockServerPlayerInLevel(); var pos=fixture.node;
            try {
                player.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+3.5); player.setYRot(0);
                for(var deity:StatueContent.Deity.values()) {
                    var block=StatueContent.block(deity); var stack=new ItemStack(block.asItem()); player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                    var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.below().getCenter(),Direction.UP,pos.below(),false));
                    helper.assertTrue(stack.useOn(context).consumesAction(),"Native statue placement fails: "+deity);
                    var state=fixture.level.getBlockState(pos); var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(pos);
                    helper.assertTrue(state.is(block)&&state.getValue(DeityStatueBlock.FACING)==Direction.NORTH&&statue.deity()==deity
                        &&statue.getType()==StatueContent.ENTITY.get()&&statue.getType().isValid(state),"Statue identity/BE type/placement facing differ: "+deity);
                    helper.assertTrue(state.getDestroySpeed(fixture.level,pos)==6&&block.getExplosionResistance()==12,"Statue hardness/resistance differs from its source parameters");
                    helper.assertTrue(state.rotate(Rotation.CLOCKWISE_90).getValue(DeityStatueBlock.FACING)==Direction.EAST
                        &&state.mirror(Mirror.LEFT_RIGHT).getValue(DeityStatueBlock.FACING)==Direction.SOUTH,"Native rotation/mirror fails: "+deity);
                    var loot=Block.getDrops(state,fixture.level,pos,statue,player,new ItemStack(Items.IRON_PICKAXE));
                    helper.assertTrue(loot.size()==1&&loot.getFirst().is(block.asItem())&&loot.getFirst().getCount()==1,"Native statue drops wrong deity: "+deity);
                    var book=new ItemStack(AbyssalPort.NECRONOMICON.get()); player.setItemInHand(InteractionHand.MAIN_HAND,book);
                    var tag=statue.saveWithFullMetadata(fixture.level.registryAccess()); tag.putInt("ActiveTicks",90); statue.loadWithComponents(tag,fixture.level.registryAccess());
                    var saved=statue.saveWithFullMetadata(fixture.level.registryAccess());
                    var loaded=new DeityStatueBlockEntity(pos,state); loaded.loadWithComponents(saved,fixture.level.registryAccess());
                    helper.assertTrue(saved.getInt("ActiveTicks")==90&&loaded.deity()==deity,"Native saved statue loses base charge/identity");
                    var ticker=block.getTicker(fixture.level,state,StatueContent.ENTITY.get()); helper.assertTrue(ticker!=null,"Statue has no native server ticker");
                    for(int tick=0;tick<30;tick++) ticker.tick(fixture.level,pos,state,statue);
                    helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==5,"Survival statue does not resume five-PE base charging: "+deity);
                    fixture.level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                }
                helper.succeed();
            } finally { player.getInventory().clearContent(); fixture.level.getServer().getPlayerList().remove(player); }
        }
    }
    @GameTest(template="empty",batch="statues_native_monolith")
    public static void NativeMonolithsUseSevenTrueStatuesThreeByThreeStoneAndLivingBiomassButPreserveLegacySpacing(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(3,14,3));
        var saved=new LinkedHashMap<BlockPos,BlockState>();
        for(var cell:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,13,2))) {
            helper.assertTrue(level.getBlockEntity(cell)==null,"Monolith test intersects an existing BE"); saved.put(cell.immutable(),level.getBlockState(cell));
        }
        try {
            for(var deity:StatueContent.Deity.values()) {
                saved.keySet().forEach(cell->level.setBlock(cell,cell.getY()==pos.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2));
                int height=deity.ordinal()==6?12:5+deity.ordinal(); long seed=0; while(RandomSource.create(seed).nextInt(7)!=deity.ordinal()) seed++;
                helper.assertTrue(MonolithBuilder.build(level,pos,height,2,RandomSource.create(seed)),"Supported native deity monolith cannot be built: "+deity);
                var top=level.getBlockState(pos.above(height));
                helper.assertTrue(top.is(StatueContent.block(deity))&&top.getValue(DeityStatueBlock.FACING).getAxis().isHorizontal(),"Monolith places prototype/invalid facing: "+deity);
                for(int y=0;y<height;y++) for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
                    helper.assertTrue(level.getBlockState(pos.offset(x,y,z)).is(x==0&&y==0&&z==0?ShoggothLairContent.BIOMASS.get():ShoggothLairContent.MONOLITH_STONE.get()),"Native monolith is not 3x3 source material");
                }
                var node=(ShoggothBiomassBlockEntity)level.getBlockEntity(pos);
                helper.assertTrue(node!=null&&node.admittedCount()==0&&node.activeTicks()==0,"Native monolith lacks a fresh finite biomass node");
                helper.assertTrue(!MonolithSiteRules.farFromExistingMonoliths(level,pos),"New true deity monolith is invisible to spacing");
            }
            saved.keySet().forEach(cell->level.setBlock(cell,cell.getY()==pos.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2));
            for(int y=0;y<5;y++) for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) level.setBlock(pos.offset(x,y,z),AbyssalPort.DARKSTONE.get().defaultBlockState(),2);
            level.setBlock(pos.above(5),AbyssalPort.PROTOTYPE_EFFIGY.get().defaultBlockState(),2);
            helper.assertTrue(!MonolithSiteRules.farFromExistingMonoliths(level,pos),"Saved prototype-era monolith loses legacy spacing");
            helper.assertTrue(level.getBlockState(pos).is(AbyssalPort.DARKSTONE.get())&&level.getBlockState(pos.above(5)).is(AbyssalPort.PROTOTYPE_EFFIGY.get()),"Read-only legacy spacing migrates old blocks");
            helper.succeed();
        } finally { saved.forEach((cell,state)->level.setBlock(cell,state,2)); }
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="statues_normal_ticks",timeoutTicks=400)
    public static void EveryDeityChargesBothHandsThroughActualNormalWorldServerBlockEntityTicks(GameTestHelper helper) {
        var level=helper.getLevel(); var saved=new LinkedHashMap<BlockPos,BlockState>();
        var forced=new ArrayList<net.minecraft.world.level.ChunkPos>(); var players=new ArrayList<net.minecraft.server.level.ServerPlayer>();
        var books=new ArrayList<ItemStack>(); var statues=new ArrayList<DeityStatueBlockEntity>();
        Runnable cleanup=()->{
            players.forEach(player->{ player.getInventory().clearContent(); level.getServer().getPlayerList().remove(player); });
            saved.forEach((pos,state)->level.setBlock(pos,state,2)); forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false));
        };
        try {
            for(var deity:StatueContent.Deity.values()) {
                var pos=new BlockPos(520008+deity.ordinal()*16,300,520008); var chunk=new net.minecraft.world.level.ChunkPos(pos);
                level.getChunk(chunk.x,chunk.z);
                if(!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x,chunk.z,true); forced.add(chunk); }
                for(var cell:BlockPos.betweenClosed(pos.below(),pos.above())) {
                    helper.assertTrue(level.getBlockEntity(cell)==null,"Normal statue audit overlaps an existing BE"); saved.put(cell.immutable(),level.getBlockState(cell));
                }
                level.setBlock(pos.below(),ShoggothLairContent.MONOLITH_STONE.get().defaultBlockState(),2);
                level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2); level.setBlock(pos,StatueContent.block(deity).defaultBlockState(),2);
                helper.assertTrue(level.canSeeSky(pos.above()),"Isolated native statue has no sky access");
                var statue=(DeityStatueBlockEntity)level.getBlockEntity(pos); statues.add(statue);
                var player=helper.makeMockServerPlayerInLevel(); players.add(player); player.setGameMode(net.minecraft.world.level.GameType.CREATIVE); player.setNoGravity(true);
                player.moveTo(pos.getCenter().add(2,0,0));
                for(var hand:InteractionHand.values()) {
                    var book=new ItemStack(AbyssalPort.NECRONOMICON.get()); player.setItemInHand(hand,book); books.add(book);
                }
            }
            helper.runAfterDelay(180,()-> {
                try {
                    helper.assertTrue(books.size()==14&&books.stream().allMatch(book->NecronomiconItem.getPotentialEnergy(book)==5),
                        "Seven actual native statues do not independently charge both held books: "+books.stream().map(NecronomiconItem::getPotentialEnergy).toList());
                    helper.assertTrue(statues.stream().allMatch(statue->statue.saveWithFullMetadata(level.registryAccess()).getInt("ActiveTicks")>0),
                        "Some registered statue BEs did not continue actual native ticking after charging");
                    helper.succeed();
                } finally { cleanup.run(); }
            });
        } catch(RuntimeException|AssertionError error) { cleanup.run(); throw error; }
    }
}
