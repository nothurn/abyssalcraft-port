package dev.abyssalport;

import dev.abyssalport.block.entity.ShoggothBiomassBlockEntity;
import dev.abyssalport.content.*;
import dev.abyssalport.entity.ShoggothEntity;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShoggothLairGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private ShoggothLairGameTests() { }
    private static final class Fixture implements AutoCloseable {
        final GraveyardGameTests.Fixture ground; final ServerPlayer player;
        final net.minecraft.world.phys.Vec3 previous; final List<ShoggothEntity> mobs=new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> capture;
        Fixture(GameTestHelper helper) {
            ground=new GraveyardGameTests.Fixture(helper); player=helper.makeMockServerPlayerInLevel(); previous=player.position();
            player.moveTo(ground.node.getCenter().add(3,2,3)); player.setNoGravity(true);
            capture=event->{ if(event.getLevel()==ground.level&&event.getEntity() instanceof ShoggothEntity mob&&mob.position().distanceTo(ground.node.getCenter())<8) {
                mob.setNoAi(true); mobs.add(mob);
            } };
            NeoForge.EVENT_BUS.addListener(capture);
        }
        ShoggothBiomassBlockEntity place(BlockPos pos) {
            ground.level.setBlock(pos,ShoggothLairContent.BIOMASS.get().defaultBlockState(),2);
            return (ShoggothBiomassBlockEntity)ground.level.getBlockEntity(pos);
        }
        void advance(ShoggothBiomassBlockEntity node,int ticks) {
            for(int tick=0;tick<ticks;tick++) ShoggothBiomassBlockEntity.serverTick(ground.level,node.getBlockPos(),node.getBlockState(),node);
        }
        long admitted() { return mobs.stream().filter(mob->mob.isAddedToLevel()&&!mob.isRemoved()).count(); }
        void clearMobs() { mobs.forEach(Entity::discard); mobs.clear(); }
        @Override public void close() {
            NeoForge.EVENT_BUS.unregister(capture); clearMobs(); player.moveTo(previous); player.setNoGravity(false); ground.close();
        }
    }

    @GameTest(template="empty",batch="shoggoth_lair_templates")
    public static void PackedLairsHaveRegisteredPalettesRealSavedNodesAndClearDescendingEntrances(GameTestHelper helper) {
        for(String name:List.of("arched","split","alcoves")) {
            String path="/data/abyssalport/structure/shoggoth_lair/"+name+".nbt";
            try(var input=ShoggothLairGameTests.class.getResourceAsStream(path)) {
                helper.assertTrue(input!=null,"Native lair is not packed"); var tag=NbtIo.readCompressed(input,NbtAccounter.create(1_000_000));
                for(var value:tag.getList("palette",Tag.TAG_COMPOUND)) {
                    var id=net.minecraft.resources.ResourceLocation.parse(((CompoundTag)value).getString("Name"));
                    helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id),"Lair palette would silently load AIR: "+id);
                }
            } catch(java.io.IOException error) { throw new GameTestAssertException("Unreadable lair: "+error); }
            var template=helper.getLevel().getStructureManager().getOrCreate(WastelandContent.id("shoggoth_lair/"+name));
            helper.assertTrue(template.getSize().equals(new Vec3i(17,14,27)),"Native lair dimensions missing");
            var nodes=template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),ShoggothLairContent.BIOMASS.get());
            helper.assertTrue(nodes.size()==switch(name){case "arched"->3;case "split"->2;default->4;},"Native lair lost functional nodes");
            nodes.forEach(node->helper.assertTrue(node.nbt().getInt("Cooldown")<100&&node.nbt().getInt("SpawnedShoggoths")==0,"Fresh lair node is exhausted"));
            for(var editor:List.of(Blocks.STRUCTURE_BLOCK,Blocks.JIGSAW,Blocks.STRUCTURE_VOID))
                helper.assertTrue(template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),editor).isEmpty(),"Lair exposes editor markers");
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="shoggoth_biomass_timer")
    public static void NativeSavedTimerPausesForPeacefulAndMobRuleButNotDaylightAndRequiresNearbyPlayer(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var node=fixture.place(fixture.ground.node); fixture.player.moveTo(fixture.previous.add(1000,0,1000)); fixture.advance(node,400);
            helper.assertTrue(node.admittedCount()==0&&fixture.mobs.isEmpty()&&node.activeTicks()<10,"No-player node creates a mob or loses its source cooldown reset");
            fixture.advance(node,83); int timer=node.activeTicks(); var tag=node.saveWithFullMetadata(fixture.ground.level.registryAccess());
            var copy=new ShoggothBiomassBlockEntity(fixture.ground.node,node.getBlockState()); copy.loadWithComponents(tag,fixture.ground.level.registryAccess());
            helper.assertTrue(copy.activeTicks()==timer&&copy.admittedCount()==0,"Native biomass save loses timers");
            fixture.ground.level.getServer().setDifficulty(Difficulty.PEACEFUL,true); fixture.advance(node,400);
            helper.assertTrue(node.activeTicks()==timer,"Peaceful timer keeps running");
            fixture.ground.level.getServer().setDifficulty(Difficulty.NORMAL,true);
            fixture.ground.level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,fixture.ground.level.getServer()); fixture.advance(node,400);
            helper.assertTrue(node.activeTicks()==timer,"Disabled spawning timer keeps running");
            fixture.ground.level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,fixture.ground.level.getServer());
            fixture.ground.level.setDayTime(6000); fixture.ground.level.updateSkyBrightness(); fixture.advance(node,20);
            helper.assertTrue(node.activeTicks()==timer+20,"Biomass acquired an invented night-only requirement");
            tag.putInt("Cooldown",Integer.MAX_VALUE); tag.putInt("SpawnedShoggoths",-99); copy.loadWithComponents(tag,fixture.ground.level.registryAccess());
            helper.assertTrue(copy.activeTicks()==399&&copy.admittedCount()==0,"Malformed persisted counts are unbounded"); helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_biomass_veto")
    public static void FinalizeAndAdmissionVetosAndBlockedTerrainNeverCountOrSpendTheNode(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var node=fixture.place(fixture.ground.node);
            Consumer<FinalizeSpawnEvent> veto=event->{ if(event.getEntity() instanceof ShoggothEntity) event.setSpawnCancelled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
            try { fixture.advance(node,400); helper.assertTrue(fixture.mobs.isEmpty()&&node.admittedCount()==0,"Cancelled finalization consumes biomass"); }
            finally { NeoForge.EVENT_BUS.unregister(veto); }
            Consumer<EntityJoinLevelEvent> reject=event->{ if(event.getEntity() instanceof ShoggothEntity) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(reject);
            try { fixture.advance(node,400); helper.assertTrue(fixture.admitted()==0&&node.admittedCount()==0,"Rejected entity counted as an accepted spawn"); }
            finally { NeoForge.EVENT_BUS.unregister(reject); fixture.clearMobs(); }
            for(var pos:BlockPos.betweenClosed(fixture.ground.node.offset(-4,0,-4),fixture.ground.node.offset(4,3,4)))
                if(!pos.equals(fixture.ground.node)) fixture.ground.level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            fixture.advance(node,400); helper.assertTrue(fixture.mobs.isEmpty()&&node.admittedCount()==0
                &&fixture.ground.level.getBlockState(fixture.ground.node).is(ShoggothLairContent.BIOMASS.get()),"Blocked lair uses solid teleport fallback or spends a node"); helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_biomass_neighbours")
    public static void NeighbourCooldownResetHasOnlyTwoLoadedHopsAndBiomassCannotBeSilkMoved(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var level=fixture.ground.level; var centre=fixture.ground.node;
            var nodes=new ArrayList<ShoggothBiomassBlockEntity>();
            for(int dx=0;dx<=3;dx++) { var node=fixture.place(centre.offset(dx,0,0)); fixture.advance(node,180); nodes.add(node); }
            fixture.player.moveTo(fixture.previous.add(1000,0,1000)); fixture.advance(nodes.getFirst(),220);
            helper.assertTrue(nodes.get(1).activeTicks()<30&&nodes.get(2).activeTicks()<30&&nodes.get(3).activeTicks()==180,"Cooldown propagation is recursive/unbounded or does not reach exactly two connected hops");
            var state=ShoggothLairContent.BIOMASS.get().defaultBlockState();
            var tool=new ItemStack(Items.DIAMOND_SHOVEL); tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
            helper.assertTrue(Block.getDrops(state,level,centre,nodes.getFirst(),fixture.player,tool).isEmpty(),"Silk moves finite lair source into portable farm");
            helper.assertTrue(Math.abs(state.getCollisionShape(level,centre).max(Direction.Axis.Y)-.85)<.0001,"Native biomass source collision is not .85 high");
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_biomass_exhaustion",timeoutTicks=120)
    public static void FiveActuallyAdmittedSpawnsExhaustExactlyOneSavedNode(GameTestHelper helper) {
        var fixture=new Fixture(helper); var node=fixture.place(fixture.ground.node);
        helper.runAfterDelay(10,()->exhaust(helper,fixture,node,0));
    }
    private static void exhaust(GameTestHelper helper,Fixture fixture,ShoggothBiomassBlockEntity node,int count) {
        try { fixture.advance(node,400); }
        catch(RuntimeException error) { fixture.close(); throw error; }
        helper.runAfterDelay(1,()-> {
            boolean finish=count==4;
            try {
                helper.assertTrue(fixture.admitted()==1&&node.admittedCount()==count+1,"Actual source spawn count is not exactly transactional: "+node.admittedCount());
                var save=node.saveWithFullMetadata(fixture.ground.level.registryAccess());
                helper.assertTrue(save.getInt("SpawnedShoggoths")==count+1,"Successful count is not persisted"); fixture.clearMobs();
                if(finish) {
                    helper.assertTrue(fixture.ground.level.getBlockState(fixture.ground.node).is(ShoggothLairContent.MONOLITH_STONE.get())
                        &&fixture.ground.level.getBlockEntity(fixture.ground.node)==null,"Fifth accepted spawn does not exhaust native BE"); helper.succeed();
                } else { helper.assertTrue(!node.isRemoved(),"Node exhausts before five successful spawns"); exhaust(helper,fixture,node,count+1); }
            } catch(RuntimeException error) { fixture.close(); throw error; }
            finally { if(finish) fixture.close(); }
        });
    }

    @GameTest(template="empty",batch="shoggoth_biomass_cap",timeoutTicks=120)
    public static void TwoNodesShareSixNearbyNativeLivingShoggothsRatherThanASeparateLimitEach(GameTestHelper helper) {
        var fixture=new Fixture(helper); var first=fixture.place(fixture.ground.node); var second=fixture.place(fixture.ground.node.offset(3,0,0));
        helper.runAfterDelay(10,()->capped(helper,fixture,first,second,0));
    }
    private static void capped(GameTestHelper helper,Fixture fixture,ShoggothBiomassBlockEntity first,ShoggothBiomassBlockEntity second,int count) {
        boolean finish=count==6;
        try { fixture.advance(count%2==0?first:second,400); }
        catch(RuntimeException error) { fixture.close(); throw error; }
        helper.runAfterDelay(1,()-> {
            try {
                helper.assertTrue(fixture.admitted()==Math.min(6,count+1),"Native shared six-Shoggoth cap broke: "+fixture.admitted());
                if(finish) { helper.assertTrue(first.admittedCount()==3&&second.admittedCount()==3,"Cap attempt spends an exhausted count"); helper.succeed(); }
                else capped(helper,fixture,first,second,count+1);
            } catch(RuntimeException error) { fixture.close(); throw error; }
            finally { if(finish) fixture.close(); }
        });
    }

    @GameTest(template="empty",batch="shoggoth_remains_processing")
    public static void AdmittedShoggothDeathDropsSupplyActualSingleConsumedEightPhosphorusRecipe(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var level=fixture.ground.level; var pos=fixture.ground.node; var drops=new ArrayList<ItemEntity>();
            Consumer<EntityJoinLevelEvent> capture=event->{ if(event.getLevel()==level&&event.getEntity() instanceof ItemEntity item&&item.position().distanceTo(pos.getCenter())<8) drops.add(item); };
            NeoForge.EVENT_BUS.addListener(capture);
            try {
                for(int attempt=0;attempt<24&&drops.stream().filter(item->item.getItem().is(GraveyardContent.SHOGGOTH_FLESH.get())).mapToInt(item->item.getItem().getCount()).sum()<2;attempt++) {
                    var mob=AbyssalPort.LESSER_SHOGGOTH.get().create(level); mob.setNoAi(true); mob.setNoGravity(true); mob.moveTo(pos.getCenter());
                    helper.assertTrue(level.addFreshEntity(mob)&&mob.hurt(level.damageSources().playerAttack(fixture.player),1000),"Native admitted Shoggoth death failed");
                }
                var input=ItemStack.EMPTY;
                for(var item:drops) if(item.getItem().is(GraveyardContent.SHOGGOTH_FLESH.get())) {
                    helper.assertTrue(item.isAddedToLevel()&&!item.isRemoved(),"Remains are not admitted native drops");
                    int amount=Math.min(2-input.getCount(),item.getItem().getCount());
                    if(amount>0) { var taken=item.getItem().split(amount); if(input.isEmpty()) input=taken; else input.grow(taken.getCount()); }
                    if(input.getCount()==2) break;
                }
                helper.assertTrue(input.getCount()==2,"Bounded actual deaths supplied no two source remains; zero-drop rolls must remain legal");
                level.setBlock(pos,CrystallizationContent.MACHINE.get().defaultBlockState(),3);
                var machine=(dev.abyssalport.block.entity.CrystallizerBlockEntity)level.getBlockEntity(pos);
                try {
                    machine.setItem(0,input); machine.setItem(1,new ItemStack(Items.BLAZE_POWDER));
                    for(int tick=0;tick<200;tick++) dev.abyssalport.block.entity.CrystallizerBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
                    helper.assertTrue(machine.getItem(0).getCount()==1&&machine.getItem(2).is(CrystallizationContent.material("phosphorus").shard().get())
                        &&machine.getItem(2).getCount()==8&&machine.getItem(3).isEmpty(),"Actual native remains do not consume once into eight phosphorus without invented secondary"); helper.succeed();
                } finally { machine.clearContent(); level.setBlock(pos,Blocks.AIR.defaultBlockState(),3); }
            } finally { NeoForge.EVENT_BUS.unregister(capture); drops.forEach(Entity::discard); }
        }
    }

    @GameTest(template="empty",batch="shoggoth_residue_clearance")
    public static void GrowingResidueRaisesItsMakerNativelyButNeverPushesASecondEntityIntoACeiling(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var level=fixture.ground.level; var pos=fixture.ground.node;
            boolean oldRule=level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
            var mob=AbyssalPort.LESSER_SHOGGOTH.get().create(level); mob.setNoAi(true); mob.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
            try {
                helper.assertTrue(level.addFreshEntity(mob),"Native residue regression mob not admitted");
                for(int tick=0;tick<150;tick++) mob.aiStep();
                helper.assertTrue(level.getBlockState(pos).is(AbyssalPort.SHOGGOTH_OOZE.get())
                    &&level.getBlockState(pos).getValue(dev.abyssalport.block.ShoggothOozeBlock.LAYERS)>1,"Growing source residue is disabled rather than fixed");
                helper.assertTrue(level.noCollision(mob)&&mob.getY()>pos.getY(),"Maker is embedded in its growing solid residue");
                mob.discard(); fixture.mobs.remove(mob); level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                var trapped=AbyssalPort.LESSER_SHOGGOTH.get().create(level); trapped.setNoAi(true); trapped.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
                level.setBlock(pos.above(),Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.TOP),2);
                try {
                    helper.assertTrue(level.noCollision(trapped)&&level.addFreshEntity(trapped),"Tight but initially valid ceiling fixture is already colliding");
                    for(int tick=0;tick<150;tick++) trapped.aiStep();
                    helper.assertTrue(level.noCollision(trapped)&&trapped.getBoundingBox().maxY<=pos.getY()+1.5001,"Ooze displaced native body into ceiling");
                    helper.assertTrue(level.getBlockState(pos).getValue(dev.abyssalport.block.ShoggothOozeBlock.LAYERS)<=2,"Ooze growth ignores bounded headroom");
                } finally { trapped.discard(); }
                helper.succeed();
            } finally { mob.discard(); level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(oldRule,level.getServer()); }
        }
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="shoggoth_lair_native",timeoutTicks=2200)
    public static void NaturalSavedSwampRiverLairRaisesNativeShoggothsAndAdmittedDeathDrops(GameTestHelper helper) {
        nativeLair(helper,helper.getLevel(),ShoggothLairContent.LAIR,360008,ShoggothEntity.Habitat.OVERWORLD);
    }
    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="wasteland_shoggoth_lair_native",timeoutTicks=1800)
    public static void NaturalWastelandLairRaisesRealDimensionalFormsThroughNativeServerTicks(GameTestHelper helper) {
        nativeLair(helper,helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION),ShoggothLairContent.WASTELAND_LAIR,400008,ShoggothEntity.Habitat.ABYSSAL);
    }
    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="dreadlands_shoggoth_lair_native",timeoutTicks=1800)
    public static void NaturalDreadlandsLairRaisesRealDimensionalFormsThroughNativeServerTicks(GameTestHelper helper) {
        nativeLair(helper,helper.getLevel().getServer().getLevel(DreadlandsContent.DIMENSION),ShoggothLairContent.DREADLANDS_LAIR,440008,ShoggothEntity.Habitat.DREAD);
    }
    private static void nativeLair(GameTestHelper helper,ServerLevel level,net.minecraft.resources.ResourceKey<Structure> key,int centre,ShoggothEntity.Habitat habitat) {
        helper.assertTrue(level!=null,"Actual lair dimension is missing");
        var holder=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(key);
        // Preserve the first sampled pillar-lined lair, whose ticking revealed
        // the residue collision bug. Verify fresh, proper Monolith Stone plans.
        var state=level.getChunkSource().getGeneratorState(); var generator=level.getChunkSource().getGenerator(); var candidates=new ArrayList<ChunkPos>();
        for(var placement:state.getPlacementsForStructure(holder)) if(placement instanceof RandomSpreadStructurePlacement spread) {
            int rx=Math.floorDiv(centre>>4,spread.spacing()),rz=Math.floorDiv(centre>>4,spread.spacing());
            for(int dx=-32;dx<=32;dx++) for(int dz=-32;dz<=32;dz++) {
                var chunk=spread.getPotentialStructureChunk(state.getLevelSeed(),(rx+dx)*spread.spacing(),(rz+dz)*spread.spacing()); boolean valid=false;
                for(int ox:new int[]{-8,0,8}) for(int oz:new int[]{-8,0,8}) for(int y:new int[]{48,64,80,96})
                    if(holder.value().biomes().contains(generator.getBiomeSource().getNoiseBiome((chunk.getMinBlockX()+ox)>>2,y>>2,(chunk.getMinBlockZ()+oz)>>2,state.randomState().sampler()))) valid=true;
                if(valid) candidates.add(chunk);
            }
        }
        candidates.sort(Comparator.comparingDouble(chunk->chunk.getMiddleBlockPosition(64).distSqr(new BlockPos(centre,64,centre))));
        helper.assertTrue(!candidates.isEmpty(),"No bounded climate-compatible lairs"); findNative(helper,level,holder,candidates.iterator(),0,habitat);
    }
    private static void findNative(GameTestHelper helper,ServerLevel level,Holder<Structure> holder,Iterator<ChunkPos> candidates,int count,ShoggothEntity.Habitat habitat) {
        helper.assertTrue(candidates.hasNext()&&count<64,"No native saved lair start in 64 climate-compatible attempts");
        var chunk=candidates.next(); var proto=level.getChunk(chunk.x,chunk.z,ChunkStatus.STRUCTURE_STARTS); var start=proto.getStartForStructure(holder.value());
        if(start==null||!start.isValid()) { helper.runAfterDelay(1,()->findNative(helper,level,holder,candidates,count+1,habitat)); return; }
        var box=start.getBoundingBox(); helper.assertTrue(start.getPieces().size()==1,"Lair initial piece not saved exactly once");
        for(int x=box.minX()>>4;x<=box.maxX()>>4;x++) for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++) level.getChunk(x,z);
        var nodes=new ArrayList<BlockPos>();
        for(var pos:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ())) {
            var state=level.getBlockState(pos); helper.assertTrue(!state.is(Blocks.STRUCTURE_BLOCK)&&!state.is(Blocks.JIGSAW)&&!state.is(Blocks.STRUCTURE_VOID),"Generated lair exposes editor blocks");
            if(state.is(ShoggothLairContent.BIOMASS.get())) {
                helper.assertTrue(level.getBlockEntity(pos) instanceof ShoggothBiomassBlockEntity,"Natural node has no real BE"); nodes.add(pos.immutable());
            }
        }
        // This isolated audit world persists across runs. Previously sampled
        // nodes are finite and legitimately exhaust; never reset their state
        // or turn current counters into fresh expected values.
        if(nodes.size()<2||nodes.stream().anyMatch(pos->((ShoggothBiomassBlockEntity)level.getBlockEntity(pos)).admittedCount()>0)) {
            LOGGER.info("Preserving previously used lair {}, searching the next native start",box);
            helper.runAfterDelay(1,()->findNative(helper,level,holder,candidates,count+1,habitat)); return;
        }
        helper.assertTrue(nodes.size()<=4,"Natural lair has more than four native biomass nodes");
        var player=helper.makeMockServerPlayerInLevel(); var previous=player.position(); var playerLevel=player.serverLevel();
        var visit=nodes.getFirst().getCenter().add(0,2,0); player.teleportTo(level,visit.x,visit.y,visit.z,Set.of(),0,0); player.setNoGravity(true);
        var mobs=new ArrayList<ShoggothEntity>(); var forced=new ArrayList<ChunkPos>();
        var difficulty=level.getDifficulty(); boolean rule=level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        Consumer<EntityJoinLevelEvent> capture=event->{ if(event.getLevel()==level&&event.getEntity() instanceof ShoggothEntity mob&&mob.position().distanceTo(box.getCenter().getCenter())<24) {
            LOGGER.info("LAIR ADMISSION: bounds={}, pos={}, clear={}, dry={}",box,mob.position(),level.noCollision(mob),!level.containsAnyLiquid(mob.getBoundingBox()));
            mob.setNoAi(true); mobs.add(mob);
        } };
        NeoForge.EVENT_BUS.addListener(capture); level.getServer().setDifficulty(Difficulty.NORMAL,true); level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,level.getServer());
        for(int x=(box.minX()-5)>>4;x<=(box.maxX()+5)>>4;x++) for(int z=(box.minZ()-5)>>4;z<=(box.maxZ()+5)>>4;z++) {
            var key=new ChunkPos(x,z); if(!level.getForcedChunks().contains(key.toLong())) { level.setChunkForced(x,z,true); forced.add(key); }
        }
        helper.runAfterDelay(440,()-> {
            var drops=new ArrayList<ItemEntity>();
            try {
                var live=mobs.stream().filter(mob->mob.isAddedToLevel()&&!mob.isRemoved()).toList();
                for(var mob:live) {
                    var occupied=new ArrayList<String>(); var bounds=mob.getBoundingBox();
                    for(var block:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ),BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ))) {
                        var state=level.getBlockState(block); if(!state.isAir()) occupied.add(block+"="+state);
                    }
                    LOGGER.info("LAIR AFTER 440: pos={}, clear={}, dry={}, occupied={}, collisionShapes={}",mob.position(),level.noCollision(mob),!level.containsAnyLiquid(bounds),occupied,level.getEntityCollisions(mob,bounds).size());
                }
                helper.assertTrue(!live.isEmpty()&&live.size()<=6,"Naturally generated lair raises no native Shoggoth, or exceeds shared cap; nodes="+nodes.stream().map(pos-> {
                    var node=level.getBlockEntity(pos);
                    return node instanceof ShoggothBiomassBlockEntity biomass?"timer="+biomass.activeTicks()+",admitted="+biomass.admittedCount():level.getBlockState(pos).toString();
                }).toList());
                helper.assertTrue(live.stream().allMatch(mob->mob.habitat()==habitat&&level.noCollision(mob)&&!level.containsAnyLiquid(mob.getBoundingBox())),"Native lair spawn has wrong habitat or is obstructed/submerged");
                if(habitat!=ShoggothEntity.Habitat.OVERWORLD) {
                    var carrier=live.getFirst(); player.moveTo(carrier.position().add(0,0,2));
                    var staff=new ItemStack(habitat==ShoggothEntity.Habitat.ABYSSAL?WastelandProgressionContent.WASTELAND_STAFF.get():DreadlandsProgressionContent.STAFF.get());
                    var channel=habitat==ShoggothEntity.Habitat.ABYSSAL?dev.abyssalport.item.RendingStaffItem.Channel.ABYSSAL:dev.abyssalport.item.RendingStaffItem.Channel.DREAD;
                    player.getCooldowns().removeCooldown(staff.getItem()); carrier.invulnerableTime=0;
                    helper.assertTrue(dev.abyssalport.item.RendingStaffItem.drain(staff,player,carrier)
                        &&dev.abyssalport.item.RendingStaffItem.energy(staff,channel)==(habitat==ShoggothEntity.Habitat.ABYSSAL?2:3),"Naturally admitted dimensional Shoggoth does not supply native rending energy");
                }
                // Real deaths, not fabricated ingredients. Source zero-drop
                // rolls remain legal; the separate flat test checks processing.
                for(var mob:live) { mob.invulnerableTime=0; mob.hurt(level.damageSources().genericKill(),1000); }
                drops.addAll(level.getEntitiesOfClass(ItemEntity.class,new AABB(box.minX(),box.minY(),box.minZ(),box.maxX()+1,box.maxY()+1,box.maxZ()+1).inflate(5)));
                var flesh=habitat==ShoggothEntity.Habitat.OVERWORLD?GraveyardContent.SHOGGOTH_FLESH.get():habitat==ShoggothEntity.Habitat.ABYSSAL?GraveyardContent.ABYSSAL_SHOGGOTH_FLESH.get():GraveyardContent.DREADED_SHOGGOTH_FLESH.get();
                helper.assertTrue(drops.stream().allMatch(item->item.getItem().is(flesh)),"Native dimensional death selected wrong remains");
                LOGGER.info("NATIVE SHOGGOTH LAIR WORLD AUDIT: dimension={}, bounds={}, biomass={}, admitted={} through 440 actual server ticks, forms={}, actual death drops={}",level.dimension().location(),box,nodes.size(),live.size(),live.stream().map(ShoggothEntity::kind).toList(),drops.stream().map(ItemEntity::getItem).toList());
                // Deterministic actual death coverage is provided in the flat
                // test suite; this world sample is not a promise of nonzero loot.
                helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(capture); mobs.forEach(Entity::discard); drops.forEach(Entity::discard);
                player.teleportTo(playerLevel,previous.x,previous.y,previous.z,Set.of(),0,0); player.setNoGravity(false);
                level.getServer().setDifficulty(difficulty,true); level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(rule,level.getServer());
                forced.forEach(forcedChunk->level.setChunkForced(forcedChunk.x,forcedChunk.z,false));
            }
        });
    }
}
