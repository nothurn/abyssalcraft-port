package dev.abyssalport;

import dev.abyssalport.content.*;
import dev.abyssalport.block.TombstoneBlock;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.entity.GhoulEntity;
import dev.abyssalport.research.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GraveyardGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private GraveyardGameTests() { }
    static final class Fixture implements AutoCloseable {
        final ServerLevel level; final BlockPos node; final long day; final Difficulty difficulty;
        final boolean mobSpawning; final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
        final List<GhoulEntity> mobs=new ArrayList<>(); final List<ChunkPos> forced=new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> capture;
        Fixture(GameTestHelper helper) {
            level=helper.getLevel(); node=helper.absolutePos(new BlockPos(3,14,3)); day=level.getDayTime(); difficulty=level.getDifficulty();
            mobSpawning=level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
            for(var pos:BlockPos.betweenClosed(node.offset(-5,-1,-5),node.offset(5,3,5))) {
                helper.assertTrue(level.getBlockEntity(pos)==null,"Ghoul fixture overlaps a real block entity"); saved.put(pos.immutable(),level.getBlockState(pos));
            }
            saved.keySet().forEach(pos->level.setBlock(pos,pos.getY()==node.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2));
            for(int x=(node.getX()-5)>>4;x<=(node.getX()+5)>>4;x++) for(int z=(node.getZ()-5)>>4;z<=(node.getZ()+5)>>4;z++) {
                var chunk=new ChunkPos(x,z); if(!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(x,z,true); forced.add(chunk); }
            }
            level.setDayTime(18000); level.updateSkyBrightness(); level.getServer().setDifficulty(Difficulty.NORMAL,true);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,level.getServer());
            capture=event->{ if(event.getLevel()==level&&event.getEntity() instanceof GhoulEntity ghoul&&ghoul.position().distanceTo(node.getCenter())<8) {
                ghoul.setNoAi(true); mobs.add(ghoul);
            } };
            NeoForge.EVENT_BUS.addListener(capture);
        }
        TombstoneBlockEntity place(boolean shadow) {
            return place((shadow?GraveyardContent.DARKSTONE:GraveyardContent.STONE).get());
        }
        TombstoneBlockEntity place(TombstoneBlock block) {
            level.setBlock(node,block.defaultBlockState(),2);
            return (TombstoneBlockEntity)level.getBlockEntity(node);
        }
        void advance(TombstoneBlockEntity stone,int count) { for(int tick=0;tick<count;tick++) TombstoneBlockEntity.serverTick(level,node,stone.getBlockState(),stone); }
        long admitted() { return mobs.stream().filter(mob->mob.isAddedToLevel()&&!mob.isRemoved()).count(); }
        void clearMobs() { mobs.forEach(GhoulEntity::discard); mobs.clear(); }
        @Override public void close() {
            NeoForge.EVENT_BUS.unregister(capture); clearMobs(); saved.forEach((pos,state)->level.setBlock(pos,state,2));
            level.setDayTime(day); level.updateSkyBrightness(); level.getServer().setDifficulty(difficulty,true);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(mobSpawning,level.getServer());
            forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false));
        }
    }

    @GameTest(template="empty",batch="ghoul_attributes")
    public static void NativeGhoulAttributesClassificationPoisonAndMeleeUseRealEntities(GameTestHelper helper) {
        var level=helper.getLevel(); var normal=GraveyardContent.GHOUL.get().create(level); var shadow=GraveyardContent.SHADOW_GHOUL.get().create(level);
        var cow=EntityType.COW.create(level); cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100);
        try {
            helper.assertTrue(normal.getMaxHealth()==30&&shadow.getMaxHealth()==35&&normal.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==5
                &&shadow.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==6&&normal.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)==42,"Native source Ghoul attributes differ");
            helper.assertTrue(!normal.isShadow()&&shadow.isShadow()&&normal.isInvertedHealAndHarm()&&!shadow.isInvertedHealAndHarm(),"Normal undead and Shadow classifications are conflated");
            helper.assertTrue(!shadow.canBeAffected(new MobEffectInstance(MobEffects.POISON,100))&&shadow.canBeAffected(new MobEffectInstance(MobEffects.REGENERATION,100)),"Shadow acquired invented general potion immunity");
            normal.doHurtTarget(cow); helper.assertTrue(cow.getHealth()==95,"Actual normal Ghoul melee is not five damage");
            cow.invulnerableTime=0; shadow.doHurtTarget(cow); helper.assertTrue(cow.getHealth()==89,"Actual Shadow Ghoul melee is not six damage");
            helper.assertTrue(!cow.hasEffect(MobEffects.POISON),"An unreferenced Ghoul melee poison was invented"); helper.succeed();
        } finally { normal.discard(); shadow.discard(); cow.discard(); }
    }

    @GameTest(template="empty",batch="ghoul_food_and_research")
    public static void ActualFleshConsumptionAndDiscoveryKeepSourceEffectsAndPlayerState(GameTestHelper helper) {
        var player=helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL);
        player.setData(AbyssalPort.EARLY_RESEARCH.get(),0);
        var flesh=new ItemStack(GraveyardContent.FLESH.get(),2); player.getFoodData().setFoodLevel(10);
        flesh.getItem().finishUsingItem(flesh,helper.getLevel(),player);
        helper.assertTrue(flesh.getCount()==1&&player.getFoodData().getFoodLevel()==12&&player.getEffect(MobEffects.HUNGER).getDuration()==600
            &&player.getEffect(MobEffects.HUNGER).getAmplifier()==1&&player.getEffect(MobEffects.CONFUSION).getDuration()==600&&!player.hasEffect(MobEffects.BLINDNESS),"Native ordinary flesh consumption/effects changed");
        player.removeAllEffects(); var shadow=new ItemStack(GraveyardContent.SHADOW_FLESH.get(),2);
        player.getInventory().setItem(9,shadow); GhoulResearch.discoverFromInventory(player);
        helper.assertTrue(EarlyResearch.has(player,EarlyResearch.GHOULS)&&!GhoulResearch.discover(player,shadow)
            &&EarlyResearch.pageAvailable(26,EarlyResearch.discoveries(player)),"Actual remains discovery missing or repeated unlock spams");
        shadow.getItem().finishUsingItem(shadow,helper.getLevel(),player);
        helper.assertTrue(shadow.getCount()==1&&player.getEffect(MobEffects.BLINDNESS).getDuration()==600&&player.getEffect(MobEffects.NIGHT_VISION).getDuration()==600,"Native Shadow flesh effects/consumption differ");
        player.removeAllEffects(); player.getInventory().clearContent(); helper.succeed();
    }

    @GameTest(template="empty",batch="ghoul_tombstone_save")
    public static void TimerPersistsAndDayPeacefulAndMobRulePauseWithoutSpawns(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var stone=fixture.place(false); fixture.advance(stone,83);
            var saved=stone.saveWithFullMetadata(fixture.level.registryAccess());
            var loaded=new TombstoneBlockEntity(fixture.node,stone.getBlockState()); loaded.loadWithComponents(saved,fixture.level.registryAccess());
            helper.assertTrue(loaded.activeTicks()==83,"Native saved tombstone loses its active timer: "+loaded.activeTicks()+" / "+stone.activeTicks()+"; cached-day="+fixture.level.isDay());
            fixture.level.setDayTime(6000); fixture.level.updateSkyBrightness(); fixture.advance(stone,200); helper.assertTrue(stone.activeTicks()==83,"Daylight does not pause timer");
            fixture.level.setDayTime(18000); fixture.level.updateSkyBrightness(); fixture.level.getServer().setDifficulty(Difficulty.PEACEFUL,true); fixture.advance(stone,200);
            helper.assertTrue(stone.activeTicks()==83,"Peaceful does not pause timer");
            fixture.level.getServer().setDifficulty(Difficulty.NORMAL,true); fixture.level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,fixture.level.getServer()); fixture.advance(stone,200);
            helper.assertTrue(stone.activeTicks()==83&&fixture.mobs.isEmpty(),"Disabled spawning does not pause timer");
            saved.putInt("Timer",Integer.MAX_VALUE); loaded.loadWithComponents(saved,fixture.level.registryAccess()); helper.assertTrue(loaded.activeTicks()==199,"Malformed save starts an unbounded loop");
            var tool=new ItemStack(Items.DIAMOND_PICKAXE);
            for(var block:List.of(GraveyardContent.STONE.get(),GraveyardContent.DARKSTONE.get())) {
                var state=block.defaultBlockState(); helper.assertTrue(Block.getDrops(state,fixture.level,fixture.node,null,null,tool).isEmpty(),"Source tombstone is incorrectly harvestable");
                var north=state.getShape(fixture.level,fixture.node).bounds(); var east=state.setValue(TombstoneBlock.FACING,Direction.EAST).getShape(fixture.level,fixture.node).bounds();
                helper.assertTrue(north.getXsize()>north.getZsize()&&east.getXsize()<east.getZsize(),"Native tombstone facing does not rotate collision");
            }
            fixture.level.setBlock(fixture.node,Blocks.AIR.defaultBlockState(),3); helper.assertTrue(fixture.level.getBlockEntity(fixture.node)==null,"Broken tombstone remains a spawn source"); helper.succeed();
        }
    }

    @GameTest(template="empty",batch="ghoul_tombstone_vetos")
    public static void NativeFinalizeAndAdmissionVetosAndSolidObstructionsLeaveNoGhostMob(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var stone=fixture.place(true);
            Consumer<FinalizeSpawnEvent> veto=event->{ if(event.getEntity() instanceof GhoulEntity) event.setSpawnCancelled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
            try { fixture.advance(stone,200); helper.assertTrue(fixture.mobs.isEmpty()&&stone.activeTicks()==0,"Finalize cancellation still enters the level"); }
            finally { NeoForge.EVENT_BUS.unregister(veto); }
            Consumer<EntityJoinLevelEvent> reject=event->{ if(event.getEntity() instanceof GhoulEntity) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(reject);
            try { fixture.advance(stone,200); helper.assertTrue(fixture.admitted()==0,"Rejected native admission left a ghost Ghoul"); }
            finally { NeoForge.EVENT_BUS.unregister(reject); fixture.clearMobs(); }
            for(var pos:BlockPos.betweenClosed(fixture.node.offset(-4,0,-4),fixture.node.offset(4,3,4))) if(!pos.equals(fixture.node)) fixture.level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            fixture.advance(stone,200); helper.assertTrue(fixture.mobs.isEmpty(),"Blocked grave teleports its spawn into terrain"); helper.succeed();
        }
    }

    @GameTest(template="empty",batch="ghoul_tombstone_cap",timeoutTicks=160)
    public static void AcceptedNativeSpawnsHonorMaterialAndFiveMobCapWithVisibleEntities(GameTestHelper helper) {
        var fixture=new Fixture(helper); var stone=fixture.place(true);
        helper.runAfterDelay(20,()->stepSpawn(helper,fixture,stone,0));
    }
    private static void stepSpawn(GameTestHelper helper,Fixture fixture,TombstoneBlockEntity stone,int count) {
        try {
            fixture.advance(stone,200);
            if(count<4) { helper.runAfterDelay(2,()->stepSpawn(helper,fixture,stone,count+1)); return; }
            helper.runAfterDelay(3,()-> {
                try {
                    helper.assertTrue(fixture.admitted()==5,"Native accepted source has not admitted five visible Ghouls: "+fixture.admitted());
                    for(var mob:fixture.mobs) helper.assertTrue(mob.isShadow()&&fixture.level.noCollision(mob)&&!fixture.level.containsAnyLiquid(mob.getBoundingBox()),"Wrong material or obstructed/wet spawn");
                    fixture.advance(stone,200); helper.assertTrue(fixture.admitted()==5,"Local Ghoul cap permits a sixth native admission");
                    fixture.clearMobs(); fixture.level.setBlock(fixture.node,Blocks.AIR.defaultBlockState(),2);
                    var ordinary=fixture.place(false); fixture.advance(ordinary,200);
                    helper.assertTrue(fixture.admitted()==1&&!fixture.mobs.getFirst().isShadow(),"Stone source does not raise an ordinary Ghoul"); helper.succeed();
                } finally { fixture.close(); }
            });
        } catch(RuntimeException error) { fixture.close(); throw error; }
    }

    @GameTest(template="empty",batch="ghoul_machine_chain")
    public static void ActualFleshLootAndBothNativeCrystallizerOutputsConsumeOnce(GameTestHelper helper) {
        var level=helper.getLevel(); var player=helper.makeMockServerPlayerInLevel(); var pos=helper.absolutePos(new BlockPos(3,1,3)); var prior=level.getBlockState(pos);
        try {
            record Source(EntityType<GhoulEntity> type,Item flesh,Item secondary) { }
            for(var source:List.of(new Source(GraveyardContent.GHOUL.get(),GraveyardContent.FLESH.get(),Items.AIR),
                    new Source(GraveyardContent.SHADOW_GHOUL.get(),GraveyardContent.SHADOW_FLESH.get(),AbyssalPort.SHADOW_SHARD.get()),
                    new Source(GraveyardContent.DEPTHS_GHOUL.get(),GraveyardContent.DEPTHS_FLESH.get(),CrystallizationContent.material("coralium").shard().get()),
                    new Source(GraveyardContent.DREADED_GHOUL.get(),GraveyardContent.DREADED_FLESH.get(),CrystallizationContent.material("dreadium").shard().get()))) {
                var type=source.type(); var flesh=source.flesh();
                var mob=type.create(level); mob.moveTo(pos.getCenter());
                // Loaded native loot table, not a hand-created assumption about its output.
                var loot=level.getServer().reloadableRegistries().getLootTable(mob.getLootTable());
                var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,mob)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,mob.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(player))
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
                int dropped=0;
                for(int sample=0;sample<24;sample++) for(var output:loot.getRandomItems(params)) {
                    // set_count uniformly permits zero; that empty outcome is
                    // not a wrong item and must not be promoted to a guaranteed drop.
                    if(output.isEmpty()) continue;
                    helper.assertTrue(output.is(flesh)&&output.getCount()<=2,"Loaded Ghoul table changes source flesh bounds: "+output); dropped+=output.getCount();
                }
                helper.assertTrue(dropped>0,"Native loaded Ghoul loot table has no real flesh"); mob.discard();
                var actualDrops=new ArrayList<ItemEntity>(); var slain=new ArrayList<GhoulEntity>();
                Consumer<EntityJoinLevelEvent> deaths=event->{ if(event.getLevel()==level&&event.getEntity() instanceof ItemEntity item
                    &&item.position().distanceTo(pos.getCenter())<5) actualDrops.add(item); };
                NeoForge.EVENT_BUS.addListener(deaths);
                var input=ItemStack.EMPTY;
                try {
                    for(int attempt=0;attempt<24&&actualDrops.stream().filter(item->item.getItem().is(flesh)).mapToInt(item->item.getItem().getCount()).sum()<2;attempt++) {
                        var victim=type.create(level); victim.setNoAi(true); victim.setNoGravity(true); victim.moveTo(pos.getCenter()); slain.add(victim);
                        helper.assertTrue(level.addFreshEntity(victim)&&victim.hurt(level.damageSources().playerAttack(player),100),"Actual admitted Ghoul death failed");
                    }
                    helper.assertTrue(actualDrops.stream().allMatch(item->item.isAddedToLevel()&&!item.isRemoved())
                        &&actualDrops.stream().filter(item->item.getItem().is(flesh)).mapToInt(item->item.getItem().getCount()).sum()>=2,"Accepted native deaths have not supplied two real admitted flesh drops");
                    input=new ItemStack(flesh,0);
                    for(var item:actualDrops) {
                        if(!item.getItem().is(flesh)) continue;
                        int amount=Math.min(2-input.getCount(),item.getItem().getCount());
                        if(amount>0) { var taken=item.getItem().split(amount); if(input.isEmpty()) input=taken; else input.grow(taken.getCount()); }
                        if(input.getCount()==2) break;
                    }
                } finally { NeoForge.EVENT_BUS.unregister(deaths); actualDrops.forEach(ItemEntity::discard); slain.forEach(GhoulEntity::discard); }
                level.setBlock(pos,CrystallizationContent.MACHINE.get().defaultBlockState(),3);
                var machine=(CrystallizerBlockEntity)level.getBlockEntity(pos); machine.setItem(0,input); machine.setItem(1,new ItemStack(Items.BLAZE_POWDER));
                for(int tick=0;tick<200;tick++) CrystallizerBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
                helper.assertTrue(machine.getItem(0).getCount()==1&&machine.getItem(2).is(CrystallizationContent.material("phosphorus").shard().get())&&machine.getItem(2).getCount()==12,"Native Ghoul recipe has not consumed one flesh into twelve phosphorus shards");
                helper.assertTrue(source.secondary()==Items.AIR?machine.getItem(3).isEmpty():machine.getItem(3).is(source.secondary())&&machine.getItem(3).getCount()==1,"Native secondary Ghoul output differs");
                machine.clearContent(); level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            }
            helper.succeed();
        } finally { if(level.getBlockEntity(pos) instanceof FuelProcessingBlockEntity machine) machine.clearContent(); level.setBlock(pos,prior,3); }
    }

    @GameTest(template="empty",batch="ghoul_templates")
    public static void SixNativeGraveTemplatesResolveToRealSpawnSourcesAndBuriedLoot(GameTestHelper helper) {
        var level=helper.getLevel();
        for(String material:List.of("stone","dark")) for(String size:List.of("small","medium","large")) {
            var template=level.getStructureManager().getOrCreate(WastelandContent.id("graveyard/"+material+"_"+size));
            helper.assertTrue(template.getSize().equals(new Vec3i(13,6,13)),"Native graveyard template missing");
            var marker=material.equals("dark")?GraveyardContent.DARKSTONE.get():GraveyardContent.STONE.get();
            int count=template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),marker).size();
            helper.assertTrue(count==switch(size){case "small"->4;case "medium"->6;default->8;},"Own grave layout loses its usable markers");
            var chest=template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),Blocks.CHEST);
            helper.assertTrue(chest.size()==1&&chest.getFirst().pos().getY()==0&&chest.getFirst().nbt().getString("LootTable").equals("abyssalport:chests/graveyard"),"Grave treasure is not buried native loot");
            for(var editor:List.of(Blocks.JIGSAW,Blocks.STRUCTURE_BLOCK,Blocks.STRUCTURE_VOID)) helper.assertTrue(template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),editor).isEmpty(),"Editor marker is exposed in a grave");
        }
        helper.succeed();
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="graveyard_natural",timeoutTicks=1800)
    public static void NativeNormalDarklandsGraveyardRaisesActualShadowGhoulsWithoutManualTicking(GameTestHelper helper) {
        nativeGrave(helper,helper.getLevel(),GraveyardContent.DARKLANDS_GRAVEYARD,200008,GhoulEntity.Kind.SHADOW);
    }
    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="wasteland_ghoul_graveyard",timeoutTicks=1500)
    public static void NativeWastelandGraveyardRaisesDepthsAndConnectsAcceptedPlagueAndRending(GameTestHelper helper) {
        nativeGrave(helper,helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION),GraveyardContent.WASTELAND_GRAVEYARD,240008,GhoulEntity.Kind.DEPTHS);
    }
    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="dreadlands_ghoul_graveyard",timeoutTicks=1500)
    public static void NativeDreadlandsGraveyardRaisesDreadedAndConnectsActualRending(GameTestHelper helper) {
        // Earlier audit chunks record the failed unknown-soil palette; preserve
        // them and verify new generation rather than silently exchanging chunks.
        nativeGrave(helper,helper.getLevel().getServer().getLevel(DreadlandsContent.DIMENSION),GraveyardContent.DREADLANDS_GRAVEYARD,300008,GhoulEntity.Kind.DREADED);
    }
    private static void nativeGrave(GameTestHelper helper,ServerLevel level,net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> key,int centre,GhoulEntity.Kind kind) {
        helper.assertTrue(level!=null,"Actual graveyard dimension is not loaded");
        var holder=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(key);
        var state=level.getChunkSource().getGeneratorState(); var generator=level.getChunkSource().getGenerator(); var candidates=new ArrayList<ChunkPos>();
        for(var placement:state.getPlacementsForStructure(holder)) if(placement instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread) {
            int rx=Math.floorDiv(centre>>4,spread.spacing()),rz=Math.floorDiv(centre>>4,spread.spacing());
            int regionRadius=kind==GhoulEntity.Kind.SHADOW?32:8;
            for(int dx=-regionRadius;dx<=regionRadius;dx++) for(int dz=-regionRadius;dz<=regionRadius;dz++) {
                var chunk=spread.getPotentialStructureChunk(state.getLevelSeed(),(rx+dx)*spread.spacing(),(rz+dz)*spread.spacing()); boolean compatible=false;
                for(int ox:new int[]{-6,6}) for(int oz:new int[]{-6,6}) for(int y:new int[]{64,96,128,160})
                    if(holder.value().biomes().contains(generator.getBiomeSource().getNoiseBiome(
                            (chunk.getMinBlockX()+ox)>>2,y>>2,(chunk.getMinBlockZ()+oz)>>2,state.randomState().sampler()))) compatible=true;
                if(compatible) candidates.add(chunk);
            }
        }
        candidates.sort(Comparator.comparingDouble(chunk->chunk.getMiddleBlockPosition(64).distSqr(new BlockPos(centre,64,centre))));
        helper.assertTrue(!candidates.isEmpty(),"No bounded native graveyard candidates for "+kind);
        findNativeGrave(helper,level,holder,candidates.iterator(),0,kind);
    }
    private static void findNativeGrave(GameTestHelper helper,ServerLevel level,Holder<net.minecraft.world.level.levelgen.structure.Structure> holder,Iterator<ChunkPos> candidates,int count,GhoulEntity.Kind kind) {
        helper.assertTrue(candidates.hasNext()&&count<64,"No native graveyard start among 64 bounded climate-compatible placements");
        var chunk=candidates.next(); var nativeChunk=level.getChunk(chunk.x,chunk.z,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
        var start=nativeChunk.getStartForStructure(holder.value());
        if(start==null||!start.isValid()) { helper.runAfterDelay(1,()->findNativeGrave(helper,level,holder,candidates,count+1,kind)); return; }
        helper.assertTrue(start.getPieces().size()==1,"Natural graveyard is not one native saved structure piece");
        var box=start.getPieces().getFirst().getBoundingBox(); var markers=new ArrayList<BlockPos>();
        for(int x=box.minX()>>4;x<=box.maxX()>>4;x++) for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++) level.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ())) {
            var block=level.getBlockState(pos);
            helper.assertTrue(!block.is(Blocks.STRUCTURE_BLOCK)&&!block.is(Blocks.JIGSAW)&&!block.is(Blocks.STRUCTURE_VOID),"Native grave exposes editor blocks");
            if(block.getBlock() instanceof TombstoneBlock) {
                helper.assertTrue(level.getBlockEntity(pos) instanceof TombstoneBlockEntity,"Actual natural tombstone has no functional native BE"); markers.add(pos.immutable());
            }
        }
        helper.assertTrue(markers.size()==4||markers.size()==6||markers.size()==8,"Natural Darklands grave lacks its real spawn sources: "+markers.size());
        var forced=new ArrayList<ChunkPos>(); var admitted=new ArrayList<GhoulEntity>();
        // A real player exploring the grave prevents native hard-distance despawn.
        // Do not make the product's raised mobs permanently persistent just to
        // compensate for a headless test with all players 200,000 blocks away.
        var player=helper.makeMockServerPlayerInLevel(); var playerPos=player.position(); var playerLevel=player.serverLevel();
        var centre=box.getCenter().getCenter(); player.teleportTo(level,centre.x,centre.y+1,centre.z,Set.of(),0,0); player.setNoGravity(true);
        long oldDay=level.getDayTime(); var oldDifficulty=level.getDifficulty(); boolean oldRule=level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        Consumer<EntityJoinLevelEvent> capture=event->{ if(event.getLevel()==level&&event.getEntity() instanceof GhoulEntity mob&&mob.position().distanceTo(box.getCenter().getCenter())<20) { mob.setNoAi(true); admitted.add(mob); } };
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            level.setDayTime(18000); level.updateSkyBrightness(); level.getServer().setDifficulty(Difficulty.NORMAL,true);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,level.getServer());
            for(int x=(box.minX()-5)>>4;x<=(box.maxX()+5)>>4;x++) for(int z=(box.minZ()-5)>>4;z<=(box.maxZ()+5)>>4;z++) {
                var key=new ChunkPos(x,z); if(!level.getForcedChunks().contains(key.toLong())) { forced.add(key); level.setChunkForced(x,z,true); }
            }
        } catch(RuntimeException error) {
            player.teleportTo(playerLevel,playerPos.x,playerPos.y,playerPos.z,Set.of(),0,0); player.setNoGravity(false);
            finishNativeGrave(level,oldDay,oldDifficulty,oldRule,forced,admitted,capture); throw error;
        }
        helper.runAfterDelay(235,()-> {
            try {
                var actual=admitted.stream().filter(mob->mob.isAddedToLevel()&&!mob.isRemoved()).toList();
                var timers=markers.stream().map(pos->pos+":"+((TombstoneBlockEntity)level.getBlockEntity(pos)).activeTicks()).toList();
                LOGGER.info("NATIVE GRAVE DIAGNOSTIC: bounds={}, captured={}, admitted={}, removals={}, cached-day={}, time={}, spawning={}, difficulty={}, native timers={}",box,admitted.size(),actual.size(),admitted.stream().map(Entity::getRemovalReason).toList(),level.isDay(),level.getDayTime(),level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING),level.getDifficulty(),timers);
                if(actual.isEmpty()) {
                    var ghost=GraveyardContent.raisedBy(level.getBlockState(markers.getFirst())).create(level); var diagnostics=new ArrayList<String>();
                    try {
                        for(var candidate:BlockPos.betweenClosed(markers.getFirst().offset(-4,0,-4),markers.getFirst().offset(4,2,4))) {
                            boolean support=level.getBlockState(candidate.below()).isFaceSturdy(level,candidate.below(),Direction.UP);
                            if(!support) continue; ghost.moveTo(candidate.getX()+.5,candidate.getY(),candidate.getZ()+.5,0,0);
                            boolean clear=level.noCollision(ghost),dry=!level.containsAnyLiquid(ghost.getBoundingBox());
                            if(clear&&dry) diagnostics.add(candidate+":support="+level.getBlockState(candidate.below())+",free="+clear+",dry="+dry);
                        }
                        var nearby=level.getEntitiesOfClass(GhoulEntity.class,new AABB(markers.getFirst()).inflate(15),Entity::isAlive);
                        LOGGER.info("EMPTY GRAVE SPACE DIAGNOSTIC: kind={}, marker={}, candidates={}, existingNearby={}, actualPlayers={}",kind,level.getBlockState(markers.getFirst()),diagnostics,nearby.stream().map(mob->mob.kind()+"@"+mob.position()).toList(),level.players().stream().map(p->p.position().toString()).toList());
                    } finally { ghost.discard(); }
                }
                helper.assertTrue(!actual.isEmpty()&&actual.size()<=5,"Natural grave does not spawn through actual server ticks or exceeds its common local cap: "+actual.size()+"; timers="+timers+"; day="+level.isDay());
                for(var mob:actual) helper.assertTrue((mob.kind()==kind||kind!=GhoulEntity.Kind.SHADOW&&mob.isShadow())
                    &&level.noCollision(mob)&&!level.containsAnyLiquid(mob.getBoundingBox()),"Naturally raised Ghoul has wrong material/collision/liquid safety");
                if(kind!=GhoulEntity.Kind.SHADOW) {
                    var carrier=actual.stream().filter(mob->mob.kind()==kind).findFirst().orElseThrow(()->new GameTestAssertException("Natural dimensional grave only selected shadow replacements"));
                    var cow=EntityType.COW.create(level); cow.moveTo(carrier.position());
                    try {
                        carrier.doHurtTarget(cow); var disease=kind==GhoulEntity.Kind.DEPTHS?WastelandContent.CORALIUM_PLAGUE:DreadlandsContent.PLAGUE;
                        helper.assertTrue(cow.hasEffect(disease)&&cow.getEffect(disease).getDuration()==100,"Actual dimension-bound accepted Ghoul hit lacks matching plague");
                        var staff=new ItemStack(kind==GhoulEntity.Kind.DEPTHS?WastelandProgressionContent.WASTELAND_STAFF.get():DreadlandsProgressionContent.STAFF.get());
                        var channel=kind==GhoulEntity.Kind.DEPTHS?dev.abyssalport.item.RendingStaffItem.Channel.ABYSSAL:dev.abyssalport.item.RendingStaffItem.Channel.DREAD;
                        // Native ray visibility is tested elsewhere. Put this real
                        // exploring player beside the admitted carrier on its path.
                        player.moveTo(carrier.position().add(0,0,2)); player.getCooldowns().removeCooldown(staff.getItem()); carrier.invulnerableTime=0;
                        boolean drained=dev.abyssalport.item.RendingStaffItem.drain(staff,player,carrier);
                        helper.assertTrue(drained&&dev.abyssalport.item.RendingStaffItem.energy(staff,channel)==(kind==GhoulEntity.Kind.DEPTHS?2:3),"Naturally raised native Ghoul is not a functioning dimensional rending carrier: drained="+drained+", energy="+dev.abyssalport.item.RendingStaffItem.energy(staff,channel)+", sight="+player.hasLineOfSight(carrier)+", sameLevel="+(player.level()==carrier.level())+", alive="+carrier.isAlive()+", player="+player.position()+", carrier="+carrier.position()+", staff="+staff+", tag="+carrier.getType().is(net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE,WastelandContent.id(kind==GhoulEntity.Kind.DEPTHS?"abyssal_rending_targets":"dread_rending_targets"))));
                    } finally { cow.discard(); }
                }
                LOGGER.info("NATIVE GRAVEYARD WORLD AUDIT: dimension={}, kind={}, bounds={}, real native tombstones={}, admitted Ghouls={} after 235 actual server ticks",level.dimension().location(),kind,box,markers.size(),actual.size());
                helper.succeed();
            } finally { player.teleportTo(playerLevel,playerPos.x,playerPos.y,playerPos.z,Set.of(),0,0); player.setNoGravity(false);
                finishNativeGrave(level,oldDay,oldDifficulty,oldRule,forced,admitted,capture); }
        });
    }
    private static void finishNativeGrave(ServerLevel level,long day,Difficulty difficulty,boolean rule,List<ChunkPos> forced,List<GhoulEntity> mobs,Consumer<EntityJoinLevelEvent> capture) {
        NeoForge.EVENT_BUS.unregister(capture); mobs.forEach(GhoulEntity::discard);
        level.setDayTime(day); level.updateSkyBrightness(); level.getServer().setDifficulty(difficulty,true);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(rule,level.getServer()); forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false));
    }
}
