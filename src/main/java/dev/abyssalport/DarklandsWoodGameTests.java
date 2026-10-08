package dev.abyssalport;

import dev.abyssalport.block.DarklandsLogBlock;
import dev.abyssalport.content.DarklandsWoodContent;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DarklandsWoodGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private DarklandsWoodGameTests() { }
    private static Map<BlockPos,BlockState> prepare(GameTestHelper helper,BlockPos base) {
        var level=helper.getLevel(); Map<BlockPos,BlockState> prior=new LinkedHashMap<>();
        for(var pos:BlockPos.betweenClosed(base.offset(-6,-1,-6),base.offset(6,14,6))) {
            prior.put(pos.immutable(),level.getBlockState(pos));
            level.setBlock(pos,pos.getY()==base.getY()-1?Blocks.DIRT.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        }
        return prior;
    }
    private static void restore(GameTestHelper helper,Map<BlockPos,BlockState> prior) {
        prior.forEach((pos,state)->helper.getLevel().setBlock(pos,state,2));
    }
    @GameTest(template="empty",batch="darklands_oak_native_growth")
    public static void NativeSaplingStagesBonemealWideCrownAndObstructionAreSafe(GameTestHelper helper) {
        var level=helper.getLevel(); var base=helper.absolutePos(new BlockPos(2,2,2)); var prior=prepare(helper,base);
        try {
            level.setBlock(base.below(),AbyssalPort.DARKLANDS_SOIL.get().defaultBlockState(),3);
            var sapling=DarklandsWoodContent.SAPLING.get(); var state=sapling.defaultBlockState();
            level.setBlock(base,state,3); helper.assertTrue(state.canSurvive(level,base),"Native sapling cannot live on Darklands Soil");
            sapling.performBonemeal(level,RandomSource.create(34),base,state);
            helper.assertTrue(level.getBlockState(base).is(sapling)&&level.getBlockState(base).getValue(SaplingBlock.STAGE)==1,"First growth step skips the native sapling stage");
            level.setBlock(base.above(),Blocks.CHEST.defaultBlockState(),3);
            var blocked=level.getBlockState(base);
            sapling.performBonemeal(level,RandomSource.create(34),base,blocked);
            helper.assertTrue(level.getBlockState(base).equals(blocked)&&level.getBlockState(base.above()).is(Blocks.CHEST)
                    &&level.getBlockState(base.below()).is(AbyssalPort.DARKLANDS_SOIL.get()),"Failed preflight destroys sapling, chest or soil");
            level.setBlock(base.above(),Blocks.AIR.defaultBlockState(),3);
            var player=helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BONE_MEAL,32));
            int uses=0;
            while(level.getBlockState(base).is(sapling)&&uses<24) {
                Items.BONE_MEAL.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(base),Direction.UP,base,false))); uses++;
            }
            helper.assertTrue(!level.getBlockState(base).is(sapling)&&uses>0,"Actual Bone Meal cannot grow the native configured tree");
            helper.assertTrue(player.getMainHandItem().getCount()==32-uses,"Survival Bone Meal is not consumed exactly once");
            for(int y=0;y<9;y++) helper.assertTrue(level.getBlockState(base.above(y)).is(BlockTags.LOGS),"Source-informed trunk is below nine blocks");
            int leaves=0,branches=0,root=0;
            for(var pos:prior.keySet()) {
                var actual=level.getBlockState(pos);
                if(actual.is(DarklandsWoodContent.LEAVES.get())) {
                    leaves++; helper.assertTrue(!actual.getValue(LeavesBlock.PERSISTENT)&&actual.getValue(LeavesBlock.DISTANCE)<=6,"Natural leaves do not decay after harvest");
                }
                if(actual.getBlock() instanceof DarklandsLogBlock && actual.getValue(DarklandsLogBlock.BARK)
                        && (Math.abs(pos.getX()-base.getX())>=4||Math.abs(pos.getZ()-base.getZ())>=4)) branches++;
                if(pos.getY()==base.getY() && !pos.equals(base) && actual.is(BlockTags.LOGS)) root++;
            }
            helper.assertTrue(leaves>=15&&branches>=4&&root>=4,"Tree lacks the source crown, six-block branches or roots");
            helper.succeed();
        } finally { restore(helper,prior); }
    }

    private static ItemStack craft(GameTestHelper helper,net.minecraft.server.level.ServerPlayer player,List<ItemStack> slots) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(3,1,3)); var prior=level.getBlockState(pos);
        level.setBlock(pos,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        try {
            var menu=new CraftingMenu(12,player.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(level,pos));
            for(int i=0;i<9;i++) menu.getSlot(i+1).set(slots.get(i));
            var output=menu.getSlot(0).getItem().copy(); helper.assertTrue(!output.isEmpty(),"Native wood recipe not loaded");
            int before=player.getInventory().countItem(output.getItem()); menu.quickMoveStack(player,0);
            helper.assertTrue(player.getInventory().countItem(output.getItem())==before+output.getCount(),"Actual crafted wood output not delivered once");
            for(int i=1;i<=9;i++) helper.assertTrue(menu.getSlot(i).getItem().isEmpty(),"Actual crafting has not spent its one-item ingredients");
            return output;
        } finally { level.setBlock(pos,prior,3); }
    }
    private static List<ItemStack> input(String[] pattern) {
        var input=new ArrayList<ItemStack>();
        for(String row:pattern) for(char slot:row.toCharArray()) input.add(switch(slot) {
            case 'P'->new ItemStack(DarklandsWoodContent.PLANKS.get()); case 'S'->new ItemStack(Items.STICK);
            case 'L'->new ItemStack(DarklandsWoodContent.LOG.get()); case 'H'->new ItemStack(DarklandsWoodContent.HOLLOW_LOG.get()); default->ItemStack.EMPTY;
        });
        return input;
    }
    @GameTest(template="empty",batch="darklands_oak_native_crafting")
    public static void NativeWoodRecipesTagsCharcoalAndAllBuildingPartsAreReachable(GameTestHelper helper) {
        var level=helper.getLevel(); var player=helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(craft(helper,player,input(new String[]{"L  ","   ","   "})).getCount()==4,"Normal log does not produce four planks");
        helper.assertTrue(craft(helper,player,input(new String[]{"H  ","   ","   "})).is(DarklandsWoodContent.PLANKS.get().asItem()),"Hollow log has no native plank recipe");
        helper.assertTrue(craft(helper,player,input(new String[]{"PP ","PP ","   "})).is(Items.CRAFTING_TABLE),"Planks missing native crafting-table tag");
        helper.assertTrue(craft(helper,player,input(new String[]{"P  ","P  ","   "})).is(Items.STICK),"Planks missing native stick tag");
        var patterns=Map.of("stairs",new String[]{"P  ","PP ","PPP"},"slab",new String[]{"PPP","   ","   "},
                "door",new String[]{"PP ","PP ","PP "},"fence",new String[]{"PSP","PSP","   "},
                "fence_gate",new String[]{"SPS","SPS","   "},"button",new String[]{"P  ","   ","   "},
                "pressure_plate",new String[]{"PP ","   ","   "});
        for(var entry:patterns.entrySet()) helper.assertTrue(craft(helper,player,input(entry.getValue())).is(DarklandsWoodContent.WOOD.get(entry.getKey()).get().asItem()),"Unreachable wood building part: "+entry.getKey());
        for(var log:List.of(DarklandsWoodContent.LOG.get(),DarklandsWoodContent.HOLLOW_LOG.get())) {
            var stack=new ItemStack(log);
            var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(stack),level).orElseThrow().value();
            helper.assertTrue(recipe.getResultItem(level.registryAccess()).is(Items.CHARCOAL)&&recipe.getExperience()==1.0F&&recipe.getCookingTime()==200,"Source 1-XP/200-tick charcoal route changed");
            helper.assertTrue(stack.getBurnTime(RecipeType.SMELTING)>0,"Logs cannot fuel the furnace");
        }
        helper.assertTrue(new ItemStack(DarklandsWoodContent.PLANKS.get()).getBurnTime(RecipeType.SMELTING)>0,"Native planks have no furnace fuel");
        helper.succeed();
    }
    @GameTest(template="empty",batch="darklands_oak_native_leaf_loot")
    public static void NativeLeafLootSuppliesReplantingWithoutApplesOrGuaranteedDrops(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var state=DarklandsWoodContent.LEAVES.get().defaultBlockState();
        var player=helper.makeMockServerPlayerInLevel(); var shears=new ItemStack(Items.SHEARS);
        var silk=new ItemStack(Items.DIAMOND_HOE);
        silk.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
        for(var tool:List.of(shears,silk)) {
            var drops=Block.getDrops(state,level,pos,null,player,tool);
            helper.assertTrue(drops.size()==1&&drops.getFirst().is(DarklandsWoodContent.LEAVES.get().asItem()),"Native shears/Silk Touch leaf harvest is broken");
        }
        int seedlings=0,empty=0;
        for(int sample=0;sample<512;sample++) {
            var drops=Block.getDrops(state,level,pos,null,player,ItemStack.EMPTY);
            if(drops.isEmpty()) empty++;
            for(var drop:drops) { helper.assertTrue(drop.is(DarklandsWoodContent.SAPLING.get().asItem()),"Leaf loot invents apples/another resource"); seedlings+=drop.getCount(); }
        }
        helper.assertTrue(seedlings>0&&empty>0&&seedlings<128,"Saplings are absent or effectively guaranteed"); helper.succeed();
    }

    @GameTest(template="empty",batch="darklands_oak_native_parts")
    public static void NativeDoorButtonSlabAndFireKeepTheirRealBehaviours(GameTestHelper helper) {
        var level=helper.getLevel(); var player=helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL);
        var base=helper.absolutePos(new BlockPos(2,2,2)); var prior=prepare(helper,base);
        try {
            player.moveTo(base.getX()+4,base.getY(),base.getZ()+4,0,0);
            var door=new ItemStack(DarklandsWoodContent.DOOR.get(),3); player.setItemInHand(InteractionHand.MAIN_HAND,door);
            var hit=new BlockHitResult(Vec3.atCenterOf(base.below()),Direction.UP,base.below(),false);
            helper.assertTrue(door.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Native wood door item cannot place");
            var bottom=level.getBlockState(base); var top=level.getBlockState(base.above());
            helper.assertTrue(bottom.is(DarklandsWoodContent.DOOR.get())&&top.is(DarklandsWoodContent.DOOR.get())&&door.getCount()==2,"Door failed to place two halves for one item");
            var click=new BlockHitResult(Vec3.atCenterOf(base),Direction.SOUTH,base,false);
            bottom.useWithoutItem(level,player,click);
            helper.assertTrue(level.getBlockState(base).getValue(DoorBlock.OPEN)&&level.getBlockState(base.above()).getValue(DoorBlock.OPEN),"Door halves do not open together");
            helper.assertTrue(Block.getDrops(bottom,level,base,null,player,ItemStack.EMPTY).stream().mapToInt(ItemStack::getCount).sum()==1
                    &&Block.getDrops(top,level,base.above(),null,player,ItemStack.EMPTY).isEmpty(),"Door loot duplicates its upper half");
            var buttonPos=base.east(3); level.setBlock(buttonPos,DarklandsWoodContent.BUTTON.get().defaultBlockState(),3);
            level.getBlockState(buttonPos).useWithoutItem(level,player,new BlockHitResult(Vec3.atCenterOf(buttonPos),Direction.SOUTH,buttonPos,false));
            helper.assertTrue(level.getBlockState(buttonPos).getValue(ButtonBlock.POWERED)&&level.getBlockState(buttonPos).getSignal(level,buttonPos,Direction.NORTH)==15,"Native button fails to produce redstone");
            var doubleSlab=DarklandsWoodContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
            helper.assertTrue(Block.getDrops(doubleSlab,level,base,null,player,ItemStack.EMPTY).stream().mapToInt(ItemStack::getCount).sum()==2,"Double slab does not return exactly two halves");
            var logState=DarklandsWoodContent.LOG.get().defaultBlockState();
            helper.assertTrue(logState.getBlock().getFlammability(logState,level,base,Direction.UP)==5,"Source log flammability is missing");
            helper.succeed();
        } finally { restore(helper,prior); }
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="darklands_oak_natural",timeoutTicks=1200)
    public static void FreshNativeDarklandsTerrainActuallySuppliesTheRenewableWoodChain(GameTestHelper helper) {
        var level=helper.getLevel(); var generator=level.getChunkSource().getGenerator();
        var found=generator.getBiomeSource().findBiomeHorizontal(96008,64,96008,4096,8,
                biome->biome.is(dev.abyssalport.worldgen.DarklandsWorldgen.DARKLANDS),level.random,true,level.getChunkSource().randomState().sampler());
        helper.assertTrue(found!=null,"Native Overworld biome source has no Darklands in the bounded sample");
        int logs=0,leaves=0; BlockPos harvested=null; int cx=found.getFirst().getX()>>4,cz=found.getFirst().getZ()>>4;
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            var chunk=level.getChunk(cx+dx,cz+dz);
            for(int x=0;x<16;x++) for(int z=0;z<16;z++) {
                int top=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)+3;
                for(int y=Math.max(level.getMinBuildHeight(),top-30);y<=Math.min(level.getMaxBuildHeight()-1,top);y++) {
                    var pos=new BlockPos(((cx+dx)<<4)+x,y,((cz+dz)<<4)+z); var state=chunk.getBlockState(pos);
                    if(state.getBlock() instanceof DarklandsLogBlock) { logs++; if(harvested==null) harvested=pos; }
                    if(state.is(DarklandsWoodContent.LEAVES.get())) leaves++;
                }
            }
        }
        helper.assertTrue(logs>=9&&leaves>=15&&harvested!=null,"Naturally decorated Darklands sample has no real tree: "+found.getFirst()+" logs="+logs+" leaves="+leaves);
        var drops=Block.getDrops(level.getBlockState(harvested),level,harvested,null,null,ItemStack.EMPTY);
        helper.assertTrue(drops.size()==1&&drops.getFirst().is(net.minecraft.tags.ItemTags.LOGS),"Naturally generated log does not drop harvestable wood");
        var input=CraftingInput.of(1,1,List.of(drops.getFirst()));
        var planks=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input,level.registryAccess());
        helper.assertTrue(planks.is(DarklandsWoodContent.PLANKS.get().asItem())&&planks.getCount()==4,"Natural wood is disconnected from actual plank recipe");
        LOGGER.info("DARKLANDS WOOD NATIVE WORLD AUDIT: biome={}, logs={}, leaves={}, first harvest={}, native log -> four planks",found.getFirst(),logs,leaves,harvested);
        helper.succeed();
    }
}
