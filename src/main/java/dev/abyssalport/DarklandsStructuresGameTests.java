package dev.abyssalport;

import dev.abyssalport.content.*;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.ritual.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DarklandsStructuresGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private DarklandsStructuresGameTests() { }
    private static BlockPos center(GameTestHelper helper) {
        var anchor=helper.absolutePos(new BlockPos(2,12,2));
        return new BlockPos(((anchor.getX()>>4)<<4)+8,anchor.getY(),((anchor.getZ()>>4)<<4)+8);
    }
    private static Map<BlockPos,BlockState> save(ServerLevel level,BlockPos center) {
        var saved=new LinkedHashMap<BlockPos,BlockState>();
        for(var pos:BlockPos.betweenClosed(center.offset(-6,0,-6),center.offset(6,7,6))) {
            if(level.getBlockEntity(pos)!=null) throw new GameTestAssertException("Surface fixture overlaps an existing block entity");
            saved.put(pos.immutable(),level.getBlockState(pos));
        }
        return saved;
    }
    private static void restore(ServerLevel level,Map<BlockPos,BlockState> saved) {
        saved.forEach((pos,state)-> {
            if(level.getBlockEntity(pos) instanceof RitualPedestalBlockEntity pedestal) pedestal.removeOne();
            if(level.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) altar.removeInput();
            level.setBlock(pos,state,2);
        });
    }
    private static void place(ServerLevel level,BlockPos center,String name,Rotation rotation) {
        var template=level.getStructureManager().getOrCreate(WastelandContent.id("darklands_ruins/"+name));
        var settings=new StructurePlaceSettings().setRotation(rotation);
        var transformed=net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(
                new BlockPos(6,0,6),Mirror.NONE,rotation,BlockPos.ZERO);
        var origin=center.subtract(transformed);
        if(!template.placeInWorld(level,origin,origin,settings,level.random,2)) throw new GameTestAssertException("Actual native template placement failed: "+name);
    }

    @GameTest(template="empty",batch="darklands_ruin_materials")
    public static void NativeGlowingBrickCraftingMiningDropsAndLightMatchTheSource(GameTestHelper helper) {
        var level=helper.getLevel(); var player=helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL);
        var table=helper.absolutePos(new BlockPos(3,1,3)); var prior=level.getBlockState(table);
        level.setBlock(table,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        try {
            var menu=new CraftingMenu(23,player.getInventory(),ContainerLevelAccess.create(level,table));
            Item[] input={AbyssalPort.DARKSTONE_BRICKS_ITEM.get(),Items.DIAMOND,AbyssalPort.DARKSTONE_BRICKS_ITEM.get(),
                    Items.OBSIDIAN,Items.GLOWSTONE,Items.OBSIDIAN,AbyssalPort.DARKSTONE_BRICKS_ITEM.get(),Items.OBSIDIAN,AbyssalPort.DARKSTONE_BRICKS_ITEM.get()};
            for(int index=0;index<9;index++) menu.getSlot(index+1).set(new ItemStack(input[index]));
            helper.assertTrue(menu.getSlot(0).getItem().is(DarklandsStructuresContent.GLOWING_ITEM.get())&&menu.getSlot(0).getItem().getCount()==4,"Loaded native glowing-brick source recipe changed");
            menu.quickMoveStack(player,0);
            helper.assertTrue(player.getInventory().countItem(DarklandsStructuresContent.GLOWING_ITEM.get())==4,"Crafted four bricks were not actually delivered");
            for(int index=1;index<=9;index++) helper.assertTrue(menu.getSlot(index).getItem().isEmpty(),"Native crafting has not consumed source ingredients");
            var state=DarklandsStructuresContent.GLOWING_BRICKS.get().defaultBlockState();
            helper.assertTrue(state.getLightEmission(level,table)==15&&state.getDestroySpeed(level,table)==55&&state.getBlock().getExplosionResistance()==3000,"Glowing brick lost source light/strength/resistance");
            helper.assertTrue(!new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state)&&new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(state),"Source diamond-tier harvest requirement changed");
            var drops=Block.getDrops(state,level,table,null,player,new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertTrue(drops.size()==1&&drops.getFirst().is(DarklandsStructuresContent.GLOWING_ITEM.get()),"Harvest does not return its real glowing brick");
            for(var block:List.of(DarklandsStructuresContent.ALTAR.get(),DarklandsStructuresContent.PEDESTAL.get())) {
                var returned=Block.getDrops(block.defaultBlockState(),level,table,null,player,new ItemStack(Items.DIAMOND_PICKAXE));
                helper.assertTrue(returned.size()==1&&returned.getFirst().is(AbyssalPort.DARKSTONE_COBBLESTONE_ITEM.get()),"Darkstone ritual block does not return its source ground material");
            }
            helper.succeed();
        } finally { level.setBlock(table,prior,3); }
    }

    @GameTest(template="empty",batch="darklands_ruin_templates")
    public static void AllTenNativeTemplatesResolveWithoutEditorBlocksOrInventedLoot(GameTestHelper helper) {
        var level=helper.getLevel();
        for(var name:DarklandsStructuresContent.PLANS) {
            var template=level.getStructureManager().getOrCreate(WastelandContent.id("darklands_ruins/"+name));
            helper.assertTrue(template.getSize().equals(new Vec3i(13,8,13)),"Missing surface plan: "+name);
            for(var editor:List.of(Blocks.JIGSAW,Blocks.STRUCTURE_BLOCK,Blocks.STRUCTURE_VOID))
                helper.assertTrue(template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),editor).isEmpty(),"Editor marker reached Survival: "+name);
            helper.assertTrue(template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),Blocks.CHEST).isEmpty(),"Unreferenced free treasure was invented for a source house/shrine");
            int effigies=template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),AbyssalPort.PROTOTYPE_EFFIGY.get()).size();
            helper.assertTrue(effigies==(name.contains("shrine")?1:0),"Native shrine lost its existing functional PE source");
            if(name.startsWith("house")) helper.assertTrue(template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),DarklandsWoodContent.WOOD.get("door").get()).size()==2,"Native house door is missing a half");
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="darklands_ruin_formations")
    public static void BothGroundTemplatesWorkInEveryRotationAndRejectMixedMaterials(GameTestHelper helper) {
        var level=helper.getLevel(); var base=center(helper); var saved=save(level,base);
        var player=helper.makeMockServerPlayerInLevel(); var book=new ItemStack(AbyssalPort.NECRONOMICON.get()); var altar=base.above();
        try {
            for(String name:List.of("ritual_ground","ritual_ground_columns")) for(var rotation:Rotation.values()) {
                place(level,base,name,rotation);
                helper.assertTrue(RitualGroundFormation.inspect(level,altar,player,book)==RitualGroundFormation.Result.CREATED,"Authored ground is obstructed or crosses chunks: "+name+" "+rotation);
                var one=altar.offset(BasicScrollRitual.pedestalOffsets().getFirst()); level.setBlock(one,Blocks.COBBLESTONE.defaultBlockState(),2);
                helper.assertTrue(RitualGroundFormation.form(level,altar,player,book)==RitualGroundFormation.Result.INCOMPLETE&&level.getBlockState(altar).is(AbyssalPort.DARKSTONE_COBBLESTONE.get()),"Mixed raw ring partially transforms");
                level.setBlock(one,AbyssalPort.DARKSTONE_COBBLESTONE.get().defaultBlockState(),2);
                helper.assertTrue(RitualGroundFormation.form(level,altar,player,book)==RitualGroundFormation.Result.CREATED&&RitualRequirements.BASIC.matchesGround(level,altar),"Valid rotated Darkstone ring cannot form/use a native tier-zero altar");
                level.setBlock(one,AbyssalPort.RITUAL_PEDESTAL.get().defaultBlockState(),2);
                helper.assertTrue(!RitualRequirements.BASIC.matchesGround(level,altar),"Mixed formed rings are accepted as uniform source material");
                restore(level,saved);
            }
            helper.succeed();
        } finally { restore(level,saved); }
    }

    @GameTest(template="empty",batch="darklands_ruin_native_ritual")
    public static void NativeBookAndOfferingClicksCompleteTheActualDarkstoneBasicScroll(GameTestHelper helper) {
        var level=helper.getLevel(); var base=center(helper); var altarPos=base.above(); var saved=save(level,base);
        var player=helper.makeMockServerPlayerInLevel(); var book=new ItemStack(AbyssalPort.NECRONOMICON.get());
        var admitted=new ArrayList<ItemEntity>();
        Consumer<EntityJoinLevelEvent> capture=event->{ if(event.getLevel()==level&&event.getEntity() instanceof ItemEntity item
                &&item.getItem().is(AbyssalPort.BASIC_SCROLL.get())&&item.position().distanceTo(altarPos.getCenter())<3) admitted.add(item); };
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            place(level,base,"ritual_ground_columns",Rotation.CLOCKWISE_90); player.setGameMode(GameType.SURVIVAL); player.setItemInHand(InteractionHand.MAIN_HAND,book);
            player.moveTo(altarPos.getX()+.5,altarPos.getY()+1,altarPos.getZ()+.5,0,0);
            var clicked=AbyssalPort.NECRONOMICON.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(altarPos.getCenter(),Direction.UP,altarPos,false)));
            helper.assertTrue(clicked.consumesAction()&&level.getBlockEntity(altarPos) instanceof RitualAltarBlockEntity,"Actual book click does not form its supported native BE");
            for(int index=0;index<4;index++) {
                var pos=altarPos.offset(BasicScrollRitual.pedestalOffsets().get(index)); var offering=new ItemStack(Items.BOOK);
                player.setItemInHand(InteractionHand.MAIN_HAND,offering);
                var result=level.getBlockState(pos).useItemOn(offering,level,player,InteractionHand.MAIN_HAND,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));
                helper.assertTrue(result.consumesAction()&&offering.isEmpty(),"Native Darkstone pedestal click does not spend one book");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND,book); NecronomiconItem.addPotentialEnergy(book,100);
            var altar=(RitualAltarBlockEntity)level.getBlockEntity(altarPos);
            level.getBlockState(altarPos).useItemOn(book,level,player,InteractionHand.MAIN_HAND,new BlockHitResult(altarPos.getCenter(),Direction.UP,altarPos,false));
            helper.assertTrue(altar.isRunning(),"Actual Darkstone altar click cannot start its loaded source ritual");
            for(int tick=0;tick<BasicScrollRitual.DURATION_TICKS;tick++) RitualAltarBlockEntity.serverTick(level,altarPos,level.getBlockState(altarPos),altar);
            helper.assertTrue(!altar.isRunning()&&NecronomiconItem.getPotentialEnergy(book)==0&&admitted.size()==1
                    &&admitted.getFirst().isAddedToLevel()&&!admitted.getFirst().isRemoved(),"Native Darkstone ritual has no actual one-scroll/100-PE outcome");
            for(var offset:BasicScrollRitual.pedestalOffsets()) helper.assertTrue(((RitualPedestalBlockEntity)level.getBlockEntity(altarPos.offset(offset))).getOffering().isEmpty(),"Actual ritual does not consume every offering");
            helper.succeed();
        } finally { NeoForge.EVENT_BUS.unregister(capture); admitted.forEach(ItemEntity::discard); restore(level,saved); }
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="darklands_ruins_natural",timeoutTicks=1800)
    public static void NativeNormalWorldHasSavedSurfaceRuinsWithAccessibleUsableContent(GameTestHelper helper) {
        var level=helper.getLevel(); var holder=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(DarklandsStructuresContent.RUINS);
        var state=level.getChunkSource().getGeneratorState(); var generator=level.getChunkSource().getGenerator();
        var placements=state.getPlacementsForStructure(holder); var candidates=new ArrayList<net.minecraft.world.level.ChunkPos>();
        // A large native /locate loops surface-noise setup for thousands of rare
        // starts inside one server tick. Sample only the native placement candidates
        // with compatible climate, then ask the real generator for one per tick.
        for(var placement:placements) if(placement instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread) {
            // Unmodified audit save: use previously ungenerated terrain rather
            // than erasing chunks whose structure starts predate the depth fix.
            int rx=Math.floorDiv(168008>>4,spread.spacing()),rz=Math.floorDiv(168008>>4,spread.spacing());
            for(int dx=-32;dx<=32;dx++) for(int dz=-32;dz<=32;dz++) {
                var chunk=spread.getPotentialStructureChunk(state.getLevelSeed(),(rx+dx)*spread.spacing(),(rz+dz)*spread.spacing());
                boolean compatible=false;
                for(int offset:new int[]{-6,6}) for(int y:new int[]{64,96,128,160})
                    if(dev.abyssalport.worldgen.DarklandsWorldgen.isDarklands(generator.getBiomeSource().getNoiseBiome(
                            (chunk.getMinBlockX()+offset)>>2,y>>2,(chunk.getMinBlockZ()+offset)>>2,state.randomState().sampler()))) compatible=true;
                if(compatible) candidates.add(chunk);
            }
        }
        candidates.sort(Comparator.comparingDouble(chunk->chunk.getMiddleBlockPosition(64).distSqr(new BlockPos(168008,64,168008))));
        helper.assertTrue(!candidates.isEmpty(),"Bounded native placement/climate sample contains no ruin candidates");
        LOGGER.info("Darklands ruin audit has {} climate-compatible native placement candidates; testing one actual start per tick",candidates.size());
        checkNextStart(helper,holder,candidates.iterator(),0);
    }
    private static void checkNextStart(GameTestHelper helper,Holder<net.minecraft.world.level.levelgen.structure.Structure> holder,
            Iterator<net.minecraft.world.level.ChunkPos> candidates,int checked) {
        helper.assertTrue(candidates.hasNext()&&checked<64,"No actual native ruin start among 64 bounded climate-compatible placements");
        var level=helper.getLevel(); var candidate=candidates.next();
        var chunk=level.getChunk(candidate.x,candidate.z,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);
        var start=chunk.getStartForStructure(holder.value());
        if(start==null||!start.isValid()) { helper.runAfterDelay(1,()->checkNextStart(helper,holder,candidates,checked+1)); return; }
        inspectActualRuin(helper,start);
    }
    private static void inspectActualRuin(GameTestHelper helper,net.minecraft.world.level.levelgen.structure.StructureStart start) {
        var level=helper.getLevel();
        helper.assertTrue(start.isValid()&&start.getPieces().size()==1,"Surface ruin is not one actual saved native structure start");
        // The terrain-adapted start box includes a twelve-block noise margin;
        // use the actual template piece for floor/access checks.
        var box=start.getPieces().getFirst().getBoundingBox(); int bricks=0,glowing=0,effigies=0,grounds=0,doors=0,editor=0;
        BlockPos effigyPos=null;
        for(int x=box.minX()>>4;x<=box.maxX()>>4;x++) for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++) level.getChunk(x,z);
        for(var pos:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ())) {
            var state=level.getBlockState(pos);
            if(state.is(AbyssalPort.DARKSTONE_BRICKS.get())||state.is(AbyssalPort.CRACKED_DARKSTONE_BRICKS.get())) bricks++;
            if(state.is(DarklandsStructuresContent.GLOWING_BRICKS.get())) glowing++;
            if(state.is(AbyssalPort.DARKSTONE_COBBLESTONE.get())&&pos.getY()==box.minY()+1) grounds++;
            if(state.is(DarklandsWoodContent.WOOD.get("door").get())) doors++;
            if(state.is(AbyssalPort.PROTOTYPE_EFFIGY.get())) {
                effigies++; helper.assertTrue(level.getBlockEntity(pos) instanceof PrototypeEffigyBlockEntity,"Naturally placed shrine PE source has no native BE");
                effigyPos=pos.immutable();
            }
            if(state.is(Blocks.JIGSAW)||state.is(Blocks.STRUCTURE_BLOCK)||state.is(Blocks.STRUCTURE_VOID)) editor++;
        }
        helper.assertTrue(bricks>40&&editor==0&&(grounds==9||doors==2||effigies==1||glowing>0),"Native ruin lacks material/usable building purpose or retained editor blocks");
        var persisted=level.getChunk(start.getChunkPos().x,start.getChunkPos().z,net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS).getStartForStructure(start.getStructure());
        helper.assertTrue(persisted!=null&&persisted.isValid()&&persisted.getReferences()==0,"Reading the saved native ruin lost the start or changed its explored reference");
        LOGGER.info("DARKLANDS RUIN WORLD AUDIT: sample={}, bounds={}, bricks={}, glow={}, raw-ground={}, door-halves={}, PE-sources={}, editor={}",box.getCenter(),box,bricks,glowing,grounds,doors,effigies,editor);
        if(effigyPos==null) { helper.succeed(); return; }
        helper.assertTrue(level.canSeeSky(effigyPos.above()),"Actual natural shrine is roofed over and cannot charge PE");
        BlockPos standing=null;
        for(var pos:BlockPos.betweenClosed(box.minX(),box.minY()+1,box.minZ(),box.maxX(),box.minY()+3,box.maxZ())) {
            if(pos.distSqr(effigyPos)>36||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)) continue;
            if(level.getBlockState(pos).getCollisionShape(level,pos).isEmpty()&&level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()) { standing=pos.immutable(); break; }
        }
        helper.assertTrue(standing!=null,"Actual natural shrine has no two-block-high supported charging position");
        var player=helper.makeMockServerPlayerInLevel(); var priorPos=player.position(); var book=new ItemStack(AbyssalPort.NECRONOMICON.get());
        player.setGameMode(GameType.SPECTATOR); player.setItemInHand(InteractionHand.MAIN_HAND,book); player.moveTo(standing.getCenter().add(0,-.5,0));
        var chunk=new net.minecraft.world.level.ChunkPos(effigyPos); boolean alreadyForced=level.getForcedChunks().contains(chunk.toLong());
        if(!alreadyForced) level.setChunkForced(chunk.x,chunk.z,true);
        helper.runAfterDelay(145,()-> {
            try {
                helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)>=5,"Naturally generated shrine does not charge a held book through actual server BE ticks");
                LOGGER.info("DARKLANDS RUIN NATIVE PE AUDIT: actual held-book PE={} after 145 real server ticks",NecronomiconItem.getPotentialEnergy(book));
                helper.succeed();
            } finally {
                player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY); player.moveTo(priorPos);
                if(!alreadyForced) level.setChunkForced(chunk.x,chunk.z,false);
            }
        });
    }
}
