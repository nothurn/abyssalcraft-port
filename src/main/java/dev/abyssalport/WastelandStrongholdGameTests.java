package dev.abyssalport;

import com.mojang.authlib.GameProfile;
import dev.abyssalport.content.*;
import dev.abyssalport.item.PowerstoneTrackerItem;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.research.WastelandProgressionResearch;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastelandStrongholdGameTests {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private WastelandStrongholdGameTests() { }

    @GameTest(template="empty", batch="powerstone_materials")
    public static void nativeRecipeMiningTierShapeDropsAndResearchAreReal(GameTestHelper helper) {
        var level=helper.getLevel(); var state=WastelandStructuresContent.POWERSTONE.get().defaultBlockState();
        for (Item weak : List.of(Items.WOODEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.NETHERITE_PICKAXE,AbyssalniteContent.PICKAXE.get()))
            helper.assertTrue(!new ItemStack(weak).isCorrectToolForDrops(state), "Powerstone tier five bypassed by "+weak);
        var tool=new ItemStack(WastelandMineralsContent.PICKAXE.get());
        helper.assertTrue(tool.isCorrectToolForDrops(state), "Refined Coralium cannot harvest the Powerstone");
        var shape=state.getShape(level,BlockPos.ZERO).bounds();
        helper.assertTrue(Math.abs(shape.minX-.2)<.001 && Math.abs(shape.maxX-.8)<.001 && shape.maxY==1 && !state.isSolidRender(level,BlockPos.ZERO), "Powerstone lost its narrow non-opaque shape");
        var drops=Block.getDrops(state,level,helper.absolutePos(new BlockPos(3,1,3)),null,null,tool);
        helper.assertTrue(drops.size()==1 && drops.getFirst().is(WastelandStructuresContent.POWERSTONE_ITEM.get()) && drops.getFirst().getCount()==1, "Powerstone native loot is not exactly one item");
        var grid=new ArrayList<ItemStack>();
        for(int i=0;i<9;i++) grid.add(new ItemStack(i==4?Items.ENDER_EYE:CoraliumContent.GEM.get()));
        var input=CraftingInput.of(3,3,grid); var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow();
        var result=recipe.value().assemble(input,level.registryAccess());
        helper.assertTrue(result.is(WastelandStructuresContent.TRACKER.get()) && result.getCount()==4, "Tracker must use eight gems, not refined ingots, and return four");
        grid.set(0,new ItemStack(WastelandMineralsContent.INGOT.get()));
        helper.assertTrue(!recipe.value().matches(CraftingInput.of(3,3,grid),level), "An ingot incorrectly substitutes the original gem ingredient");
        var doubled=WastelandStructuresContent.SLAB.get().defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,SlabType.DOUBLE);
        var slabDrops=Block.getDrops(doubled,level,BlockPos.ZERO,null,null,tool);
        helper.assertTrue(slabDrops.size()==1 && slabDrops.getFirst().getCount()==2 && slabDrops.getFirst().is(WastelandStructuresContent.SLAB_ITEM.get()), "Double slab loses one half");
        var finder=helper.makeMockServerPlayerInLevel(); var other=helper.makeMockServerPlayerInLevel();
        helper.assertTrue(WastelandProgressionResearch.discover(finder,new ItemStack(WastelandStructuresContent.POWERSTONE_ITEM.get()))
                && !WastelandProgressionResearch.discover(finder,new ItemStack(WastelandStructuresContent.POWERSTONE_ITEM.get()))
                && EarlyResearch.has(finder,EarlyResearch.POWERSTONE) && !EarlyResearch.has(other,EarlyResearch.POWERSTONE), "Powerstone discovery is not player-scoped/once-only");
        helper.succeed();
    }

    @GameTest(template="empty", batch="stronghold_templates")
    public static void nativeTemplatesResolveBlocksConnectorsChestsAndTheUniquePowerstone(GameTestHelper helper) {
        var manager=helper.getLevel().getStructureManager();
        for(String name:List.of("ritual_chamber","corridor","archive","store","guard_room","stair_hall","end")) {
            var template=manager.getOrCreate(WastelandContent.id("wasteland_stronghold/"+name));
            helper.assertTrue(template.getSize().getX()>=7 && template.getSize().getY()>=7, "Empty/missing template: "+name);
            var connectors=template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),Blocks.JIGSAW);
            helper.assertTrue(!connectors.isEmpty(), "Room has no native connector: "+name);
            for(var joint:connectors) helper.assertTrue(joint.nbt()!=null && "abyssalport:stronghold_door".equals(joint.nbt().getString("name"))
                    && "minecraft:air".equals(joint.nbt().getString("final_state")), "Door leaves a structure/editor block");
            var stones=template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),WastelandStructuresContent.POWERSTONE.get());
            helper.assertTrue(stones.size()==(name.equals("ritual_chamber")?1:0), "Powerstone not guaranteed and unique in central chamber");
            for(var chest:template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),Blocks.CHEST)) helper.assertTrue(chest.nbt()!=null
                    && chest.nbt().getString("LootTable").startsWith("abyssalport:chests/wasteland_stronghold_"), "Chest has no actual loot table");
            helper.assertTrue(template.filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),Blocks.STRUCTURE_BLOCK).isEmpty(), "Authored marker reached Survival");
        }
        helper.succeed();
    }

    @GameTest(template="empty", templateNamespace="abyssalport_worldtest", batch="wasteland_stronghold_world",timeoutTicks=1600)
    public static void naturalStrongholdContainsAccessiblePowerstoneLootAndPersistedExploration(GameTestHelper helper) {
        ServerLevel level=helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION);
        var origin=new BlockPos(12000,70,16000);
        var target=PowerstoneTrackerItem.findUnclaimedStronghold(level,origin);
        helper.assertTrue(target!=null, "Native ring generation has no valid unclaimed Stronghold");
        var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(WastelandStructuresContent.STRONGHOLD);
        level.getChunkAt(target);
        var start=level.structureManager().getStructureAt(target,structure);
        helper.assertTrue(start.isValid() && start.getPieces().size()>4 && start.getReferences()==0, "Not a real modular, unclaimed native start");
        var main=start.getPieces().getFirst().getBoundingBox();
        for(int x=main.minX()>>4;x<=main.maxX()>>4;x++) for(int z=main.minZ()>>4;z<=main.maxZ()>>4;z++) level.getChunk(x,z);
        BlockPos stone=null; List<BlockPos> chests=new ArrayList<>(); int editorBlocks=0;
        for(var cursor:BlockPos.betweenClosed(main.minX(),main.minY(),main.minZ(),main.maxX(),main.maxY(),main.maxZ())) {
            var state=level.getBlockState(cursor);
            if(state.is(WastelandStructuresContent.POWERSTONE.get())) { helper.assertTrue(stone==null,"More than one Powerstone in chamber"); stone=cursor.immutable(); }
            if(state.is(Blocks.CHEST)) chests.add(cursor.immutable());
            if(state.is(Blocks.JIGSAW)||state.is(Blocks.STRUCTURE_BLOCK)) editorBlocks++;
        }
        helper.assertTrue(stone!=null && !chests.isEmpty() && editorBlocks==0, "Generated chamber missing stone/chest or retained jigsaws");
        var floor=target.atY(main.minY()+1); var queue=new ArrayDeque<BlockPos>(); var reached=new HashSet<BlockPos>();
        queue.add(floor); boolean access=false; int walkableCells=0;
        while(!queue.isEmpty()) {
            var pos=queue.remove(); if(!reached.add(pos) || !main.isInside(pos)) continue;
            if(!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty() || !level.getBlockState(pos.above()).getCollisionShape(level,pos.above()).isEmpty()
                    || !level.getFluidState(pos).isEmpty() || !level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)) continue;
            walkableCells++;
            if(pos.distSqr(stone)<10) access=true;
            for(var direction:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)) if(!reached.contains(pos.relative(direction))) queue.add(pos.relative(direction));
        }
        helper.assertTrue(access && walkableCells>40, "No dry standing-height route from chamber center to mining reach");
        var chest=(ChestBlockEntity)level.getBlockEntity(chests.getFirst()); chest.unpackLootTable(null);
        helper.assertTrue(!chest.isEmpty(), "Native generated chest rolled no Survival supplies");
        var stoneState=level.getBlockState(stone); var minedPosition=stone;
        var player=survivalPlayer(level,target); var tool=new ItemStack(WastelandMineralsContent.PICKAXE.get());
        int prior=start.getReferences();
        try {
            helper.assertTrue(PowerstoneTrackerItem.findUnclaimedStronghold(level,origin).equals(target) && start.getReferences()==0, "Repeated seeking claims a start");
            // Exercise the same block callback used by ServerPlayerGameMode after its
            // correct-tool gate; avoid creative-only GameTest mock player semantics.
            helper.assertTrue(level.removeBlock(stone,false),"Native Powerstone removal failed");
            WastelandStructuresContent.POWERSTONE.get().playerDestroy(level,player,stone,stoneState,null,tool);
            helper.assertTrue(start.getReferences()==1 && !start.canBeReferenced(), "Successful mining did not mark the start explored");
            var tag=start.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level),start.getChunkPos());
            var restored=StructureStart.loadStaticStart(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level),tag,level.getSeed());
            helper.assertTrue(restored!=null && restored.isValid() && restored.getReferences()==1 && !restored.canBeReferenced(), "Explored flag is lost across actual structure NBT");
            var next=PowerstoneTrackerItem.findUnclaimedStronghold(level,origin);
            helper.assertTrue(next!=null && !next.equals(target), "Tracker keeps seeking a harvested Stronghold");
            LOGGER.info("STRONGHOLD AUDIT: target={}, native pieces={}, dry walkable cells={}, powerstone={}, chest loot=true, next={}",target,start.getPieces().size(),walkableCells,stone,next);
        } finally {
            level.setBlock(minedPosition,stoneState,3);
            level.getChunk(start.getChunkPos().x,start.getChunkPos().z).setStartForStructure(structure,new StructureStart(structure,start.getChunkPos(),prior,new PiecesContainer(start.getPieces())));
            for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(minedPosition).inflate(2)))
                if(item.getItem().is(WastelandStructuresContent.POWERSTONE_ITEM.get())) item.discard();
        }
        helper.succeed();
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="wasteland_tracker_world",timeoutTicks=1200)
    public static void actualTrackerUseConsumesOnlyAnAdmittedProjectileAndKeepsTheTarget(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION); var pos=new BlockPos(12512,90,16064);
        var player=survivalPlayer(level,pos); var tracker=WastelandStructuresContent.TRACKER.get();
        var stack=new ItemStack(tracker,3); player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        helper.assertTrue(!tracker.use(helper.getLevel(),player,InteractionHand.MAIN_HAND).getResult().consumesAction() && stack.getCount()==3, "Wrong dimension consumes a tracker");
        var admitted=new ArrayList<EyeOfEnder>();
        Consumer<EntityJoinLevelEvent> observe=event->{ if(event.getLevel()==level && event.getEntity() instanceof EyeOfEnder eye && eye.getItem().is(tracker)) admitted.add(eye); };
        Consumer<EntityJoinLevelEvent> veto=event->{ if(event.getLevel()==level && event.getEntity() instanceof EyeOfEnder eye && eye.getItem().is(tracker)) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(observe);
        try {
            NeoForge.EVENT_BUS.addListener(veto);
            try { helper.assertTrue(!tracker.use(level,player,InteractionHand.MAIN_HAND).getResult().consumesAction() && stack.getCount()==3 && !player.getCooldowns().isOnCooldown(tracker), "Veto consumed or cooled down the item"); }
            finally { NeoForge.EVENT_BUS.unregister(veto); }
            admitted.clear();
            var target=PowerstoneTrackerItem.findUnclaimedStronghold(level,pos);
            helper.assertTrue(tracker.use(level,player,InteractionHand.MAIN_HAND).getResult().consumesAction() && stack.getCount()==2
                    && admitted.size()==1 && admitted.getFirst().isAddedToLevel() && player.getCooldowns().isOnCooldown(tracker), "Real survival use did not admit exactly one synced eye/consume one/cooldown");
            var eye=admitted.getFirst(); var before=eye.position(); for(int i=0;i<10;i++) eye.tick();
            helper.assertTrue(eye.position().distanceToSqr(before)>0 && eye.getItem().is(tracker) && eye.getItem().getCount()==1, "Native projectile does not fly/render the exact tracker item");
            helper.assertTrue(PowerstoneTrackerItem.findUnclaimedStronghold(level,pos).equals(target), "Throwing silently marks/explores the structure");
        } finally { NeoForge.EVENT_BUS.unregister(observe); admitted.forEach(EyeOfEnder::discard); }
        helper.succeed();
    }

    private static Player survivalPlayer(ServerLevel level,BlockPos pos) {
        return new Player(level,pos,0,new GameProfile(UUID.randomUUID(),"PowerstoneTest")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
    }
}
