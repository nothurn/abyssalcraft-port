package dev.abyssalport;

import dev.abyssalport.block.TransmutatorBlock;
import dev.abyssalport.block.entity.TransmutatorBlockEntity;
import dev.abyssalport.content.*;
import dev.abyssalport.menu.TransmutatorMenu;
import dev.abyssalport.recipe.TransmutationRecipe;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TransmutatorGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private TransmutatorGameTests() { }

    @GameTest(template="empty",batch="transmutator_native_recipes")
    public static void nativeStoneBrickMachineAndSyncedRecipesKeepRealRemainders(GameTestHelper helper) {
        var level=helper.getLevel();
        for(var pair:List.of(new Item[]{WastelandProcessingContent.COBBLESTONE_ITEM.get(),WastelandProcessingContent.STONE_ITEM.get()},
                new Item[]{WastelandProcessingContent.STONE_ITEM.get(),WastelandProcessingContent.BRICK.get()})) {
            var input=new SingleRecipeInput(new ItemStack(pair[0]));
            var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level).orElseThrow().value();
            helper.assertTrue(recipe.assemble(input,level.registryAccess()).is(pair[1]) && recipe.getCookingTime()==200
                    && recipe.getExperience()==.1F,"Stone/brick furnace chain differs from loaded source recipe");
        }
        var brick=new ItemStack(WastelandProcessingContent.BRICK.get()); var gem=new ItemStack(CoraliumContent.TRANSMUTATION_GEM.get());
        gem.setDamageValue(4); gem.set(DataComponents.CUSTOM_NAME,Component.literal("Reusable catalyst"));
        var block=new ItemStack(WastelandMineralsContent.BLOCK_ITEM.get()); var bucket=new ItemStack(WastelandContent.CORALIUM_BUCKET.get());
        var input=CraftingInput.of(3,3,List.of(brick.copy(),brick.copy(),brick.copy(),brick.copy(),gem,brick.copy(),block.copy(),bucket,block.copy()));
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value();
        helper.assertTrue(recipe.assemble(input,level.registryAccess()).is(WastelandProcessingContent.MACHINE_ITEM.get()),"Actual machine crafting is unavailable");
        var remainder=recipe.getRemainingItems(input);
        helper.assertTrue(remainder.get(7).is(Items.BUCKET) && remainder.get(4).is(gem.getItem()) && remainder.get(4).getDamageValue()==5
                && remainder.get(4).getHoverName().getString().equals("Reusable catalyst") && gem.getDamageValue()==4 && bucket.getCount()==1,
                "Machine preview consumes a bucket/gem or loses catalyst components");
        var processing=level.getRecipeManager().getRecipeFor(WastelandProcessingContent.TRANSMUTATION.get(),new SingleRecipeInput(new ItemStack(Items.STONE)),level).orElseThrow().value();
        var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try {
            var codec=WastelandProcessingContent.SERIALIZER.get().streamCodec(); codec.encode(buffer,processing); var decoded=codec.decode(buffer);
            helper.assertTrue(decoded.matches(new SingleRecipeInput(new ItemStack(Items.STONE)),level)
                    && decoded.assemble(new SingleRecipeInput(new ItemStack(Items.STONE)),level.registryAccess()).is(AbyssalPort.DARKSTONE.get().asItem())
                    && decoded.experience()==0,"Native processing recipe network roundtrip changes input/result");
        } finally { buffer.release(); }
        var player=helper.makeMockServerPlayerInLevel(); var other=helper.makeMockServerPlayerInLevel();
        helper.assertTrue(dev.abyssalport.research.WastelandProgressionResearch.discover(player,brick)
                && !dev.abyssalport.research.WastelandProgressionResearch.discover(player,brick)
                && dev.abyssalport.research.EarlyResearch.has(player,dev.abyssalport.research.EarlyResearch.PROCESSING)
                && !dev.abyssalport.research.EarlyResearch.has(other,dev.abyssalport.research.EarlyResearch.PROCESSING),"Processing research repeats/leaks across players");
        helper.succeed();
    }

    @GameTest(template="empty",batch="transmutator_saved_processing")
    public static void savedNativeMachineCompletesExactlyTwoHundredTicksAndKeepsBlockEntity(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        try {
            machine.setItem(0,new ItemStack(Items.STONE,2)); machine.setItem(1,new ItemStack(CoraliumContent.GEM.get()));
            tick(level,pos,machine,80);
            helper.assertTrue(machine.progress()==80 && machine.burnTime()==121 && machine.getItem(1).isEmpty()
                    && machine.getItem(2).isEmpty() && level.getBlockState(pos).getValue(TransmutatorBlock.LIT)
                    && level.getBlockState(pos).getLightEmission(level,pos)==14 && level.getBlockEntity(pos)==machine,"Fuel/LIT transition resets machine or processes early");
            var restored=new TransmutatorBlockEntity(pos,machine.getBlockState());
            restored.loadWithComponents(machine.saveWithFullMetadata(level.registryAccess()),level.registryAccess()); level.setBlockEntity(restored);
            tick(level,pos,restored,119);
            helper.assertTrue(restored.progress()==199 && restored.getItem(2).isEmpty() && restored.getItem(0).getCount()==2,"Saved progress advances/finishes early");
            tick(level,pos,restored,1);
            helper.assertTrue(restored.progress()==0 && restored.getItem(0).getCount()==1 && restored.getItem(2).is(AbyssalPort.DARKSTONE.get().asItem())
                    && restored.getItem(2).getCount()==1,"Two hundred ticks do not consume exactly one input");
            tick(level,pos,restored,10);
            helper.assertTrue(restored.getItem(2).getCount()==1 && restored.burnTime()==0 && !restored.getBlockState().getValue(TransmutatorBlock.LIT),"Exhausted fuel duplicates result");
            var rotated=restored.getBlockState().rotate(Rotation.CLOCKWISE_90); level.setBlock(pos,rotated,3);
            helper.assertTrue(level.getBlockEntity(pos)==restored && restored.getItem(0).getCount()==1,"Direction change loses inventory");
        } finally { clear(level,pos); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="transmutator_atomic_rejections")
    public static void invalidBlockedOrChangedInputsNeverSpendNewFuelOrMergeDifferentComponents(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        try {
            machine.setItem(0,new ItemStack(Items.DIRT)); machine.setItem(1,new ItemStack(CoraliumContent.GEM.get())); tick(level,pos,machine,5);
            helper.assertTrue(machine.burnTime()==0 && machine.getItem(1).getCount()==1,"Missing recipe spends fuel");
            machine.setItem(0,new ItemStack(Items.STONE)); machine.setItem(2,new ItemStack(AbyssalPort.DARKSTONE.get(),64)); tick(level,pos,machine,5);
            helper.assertTrue(machine.burnTime()==0 && machine.getItem(1).getCount()==1,"Full output spends new fuel");
            var named=new ItemStack(AbyssalPort.DARKSTONE.get()); named.set(DataComponents.CUSTOM_NAME,Component.literal("Private sample"));
            machine.setItem(2,named); tick(level,pos,machine,5);
            helper.assertTrue(machine.progress()==0 && machine.getItem(1).getCount()==1 && machine.getItem(2).getCount()==1,"Different output components were merged");
            machine.setItem(2,ItemStack.EMPTY); machine.setItem(1,new ItemStack(Items.COAL)); tick(level,pos,machine,5);
            helper.assertTrue(machine.burnTime()==0 && machine.getItem(1).is(Items.COAL),"Ordinary furnace fuel bypasses transmutation rules");
            machine.setItem(1,new ItemStack(Items.BLAZE_ROD)); tick(level,pos,machine,90);
            machine.setItem(0,new ItemStack(Items.COBBLESTONE)); tick(level,pos,machine,1);
            helper.assertTrue(machine.progress()==1 && machine.getItem(2).isEmpty(),"Input switch carries old processing progress");
            machine.setItem(2,new ItemStack(AbyssalPort.DARKSTONE_COBBLESTONE.get(),64)); int fuel=machine.burnTime(); tick(level,pos,machine,5);
            helper.assertTrue(machine.progress()==0 && machine.burnTime()==fuel-5 && machine.getItem(0).getCount()==1,"Blocked burning does not reset safely");
        } finally { clear(level,pos); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="transmutator_fuel_remainders")
    public static void burningConsumesCatalystButFluidFuelReturnsOneBucket(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        try {
            machine.setItem(0,new ItemStack(Items.STONE)); machine.setItem(1,new ItemStack(CoraliumContent.TRANSMUTATION_GEM.get())); tick(level,pos,machine,1);
            helper.assertTrue(machine.burnTime()==10000 && machine.getItem(1).isEmpty(),"Burned gem returned as a reusable crafting catalyst");
            clear(level,pos); machine=place(level,pos); machine.setItem(0,new ItemStack(Items.STONE,2));
            machine.setItem(1,new ItemStack(WastelandContent.CORALIUM_BUCKET.get())); tick(level,pos,machine,1);
            helper.assertTrue(machine.burnTime()==20000 && machine.getItem(1).is(Items.BUCKET) && machine.getItem(1).getCount()==1,"Fluid fuel loses/duplicates its container");
            helper.assertTrue(TransmutatorBlockEntity.fuelTicks(new ItemStack(CoraliumContent.PEARL.get()))==2000
                    && TransmutatorBlockEntity.fuelTicks(new ItemStack(WastelandMobsContent.CORALIUM_CHUNK.get()))==16200
                    && TransmutatorBlockEntity.fuelTicks(new ItemStack(CoraliumContent.CLUSTER_9.get()))==1800,"Source fuel durations differ");
        } finally { clear(level,pos); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="transmutator_menu_automation")
    public static void nativeMenuShiftClickSidedCapabilitiesAndHopperRespectSlots(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        var player=helper.makeMockServerPlayerInLevel(); player.moveTo(pos.getCenter());
        try {
            level.getBlockState(pos).useWithoutItem(level,player,new BlockHitResult(pos.getCenter(),Direction.NORTH,pos,false));
            helper.assertTrue(player.containerMenu instanceof TransmutatorMenu && player.containerMenu.slots.size()==39,"Native right-click does not open the machine menu");
            player.closeContainer();
            var menu=new TransmutatorMenu(31,player.getInventory(),machine);
            helper.assertTrue(menu.slots.size()==39 && menu.stillValid(player) && !menu.getSlot(2).mayPlace(new ItemStack(Items.STONE))
                    && !menu.getSlot(1).mayPlace(new ItemStack(Items.COAL)),"Native menu slot restrictions missing");
            player.getInventory().setItem(9,new ItemStack(Items.STONE,3)); menu.quickMoveStack(player,3);
            player.getInventory().setItem(10,new ItemStack(CoraliumContent.GEM.get(),2)); menu.quickMoveStack(player,4);
            helper.assertTrue(machine.getItem(0).is(Items.STONE) && machine.getItem(0).getCount()==3 && machine.getItem(1).getCount()==2,"Shift-click routes inputs/fuel incorrectly");
            tick(level,pos,machine,4); menu.setData(0,199); menu.setData(1,20000);
            helper.assertTrue(menu.progress()==4 && menu.burnTime()==197,"Client menu counters mutate authoritative processing");
            machine.clearContent(); var top=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.UP);
            var side=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.NORTH);
            var bottom=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.DOWN);
            helper.assertTrue(top!=null && side!=null && bottom!=null && top.getSlots()==1 && side.getSlots()==1 && bottom.getSlots()==2,"Native sided capability missing");
            helper.assertTrue(side.insertItem(0,new ItemStack(Items.COAL),false).is(Items.COAL) && top.insertItem(0,new ItemStack(Items.STONE),false).isEmpty(),"Sided handler accepts coal or refuses input");
            helper.assertTrue(HopperBlockEntity.addItem(null,machine,new ItemStack(CoraliumContent.GEM.get()),Direction.EAST).isEmpty(),"Native hopper cannot fuel machine");
            helper.assertTrue(bottom.extractItem(1,1,false).isEmpty(),"Bottom can steal live fuel");
            machine.setItem(1,new ItemStack(Items.BUCKET)); helper.assertTrue(bottom.extractItem(1,1,false).is(Items.BUCKET),"Bottom cannot remove spent container");
            machine.setItem(2,new ItemStack(AbyssalPort.DARKSTONE.get(),4)); menu.quickMoveStack(player,2);
            helper.assertTrue(machine.getItem(2).isEmpty() && player.getInventory().countItem(AbyssalPort.DARKSTONE.get().asItem())==4,"Shift-click duplicates/loses output");
            player.moveTo(pos.getCenter().add(20,0,0)); helper.assertTrue(!menu.stillValid(player),"Distant player retains machine access");
        } finally { clear(level,pos); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="transmutator_xp_break_reload")
    public static void loadedRecipeXpSavesAndNativeBreakingDropsInventoryOnlyOnce(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        var recipes=List.copyOf(level.getRecipeManager().getRecipes()); var spawned=new ArrayList<net.minecraft.world.entity.Entity>();
        java.util.function.Consumer<EntityJoinLevelEvent> capture=event->{
            if(event.getLevel()==level && event.getEntity().position().distanceTo(pos.getCenter())<4
                    && (event.getEntity() instanceof ItemEntity || event.getEntity() instanceof ExperienceOrb)) spawned.add(event.getEntity());
        };
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            var replacement=new ArrayList<RecipeHolder<?>>(recipes);
            replacement.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,"test_only_xp"),
                    new TransmutationRecipe(Ingredient.of(Items.DIRT),new ItemStack(Items.DIAMOND),1.25F)));
            level.getRecipeManager().replaceRecipes(replacement);
            machine.setItem(0,new ItemStack(Items.DIRT,2)); machine.setItem(1,new ItemStack(Items.BLAZE_POWDER)); tick(level,pos,machine,200);
            helper.assertTrue(machine.pendingExperience()==1.25F && machine.getItem(2).is(Items.DIAMOND),"Reloadable recipe XP not accrued once");
            var saved=machine.saveWithFullMetadata(level.registryAccess()); var restored=new TransmutatorBlockEntity(pos,machine.getBlockState());
            restored.loadWithComponents(saved,level.registryAccess()); level.setBlockEntity(restored);
            var player=helper.makeMockServerPlayerInLevel(); new TransmutatorMenu(1,player.getInventory(),restored).quickMoveStack(player,2);
            int first=spawned.stream().filter(e->e instanceof ExperienceOrb).mapToInt(e->((ExperienceOrb)e).getValue()).sum();
            helper.assertTrue(first==1 && restored.pendingExperience()==.25F,"Saved XP pays incorrectly");
            restored.payExperience(); helper.assertTrue(spawned.stream().filter(e->e instanceof ExperienceOrb).mapToInt(e->((ExperienceOrb)e).getValue()).sum()==1,"Repeated extraction duplicates XP");
            restored.setItem(2,new ItemStack(Items.DIAMOND,3)); level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            int diamonds=spawned.stream().filter(e->e instanceof ItemEntity).map(e->((ItemEntity)e).getItem()).filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(diamonds==3 && restored.isEmpty() && level.getBlockEntity(pos)==null,"Native break loses or duplicates inventory");
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            helper.assertTrue(spawned.stream().filter(e->e instanceof ItemEntity).map(e->((ItemEntity)e).getItem()).filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()==3,"Repeated block removal duplicates output");
        } finally {
            level.getRecipeManager().replaceRecipes(recipes); NeoForge.EVENT_BUS.unregister(capture); clear(level,pos); spawned.forEach(net.minecraft.world.entity.Entity::discard);
        }
        helper.succeed();
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="transmutator_natural_plateau",timeoutTicks=1200)
    public static void newNativePlateauProvidesMineableStoneForTheRealMachineChain(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION); var generator=level.getChunkSource().getGenerator();
        var target=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,WastelandContent.id("abyssal_plateau"));
        var found=generator.getBiomeSource().findBiomeHorizontal(32008,64,32008,2048,b->b.is(target),level.random,level.getChunkSource().randomState().sampler());
        helper.assertTrue(found!=null,"Native biome source has no reachable plateau");
        BlockPos mined=null; int stones=0;
        int cx=found.getFirst().getX()>>4,cz=found.getFirst().getZ()>>4;
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            var chunk=level.getChunk(cx+dx,cz+dz);
            for(int x=0;x<16;x++) for(int z=0;z<16;z++) {
                int y=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                var pos=new BlockPos(((cx+dx)<<4)+x,y,((cz+dz)<<4)+z);
                if(level.getBiome(pos).is(target) && chunk.getBlockState(pos).is(WastelandProcessingContent.STONE.get())) { stones++; if(mined==null) mined=pos; }
            }
        }
        helper.assertTrue(stones>=5 && mined!=null,"Actual plateau terrain has no native Coralium stone surface: "+found.getFirst());
        var tool=new ItemStack(Items.WOODEN_PICKAXE); var state=level.getBlockState(mined);
        var drops=Block.getDrops(state,level,mined,null,null,tool);
        helper.assertTrue(tool.isCorrectToolForDrops(state) && drops.size()==1 && drops.getFirst().is(WastelandProcessingContent.COBBLESTONE_ITEM.get()),"Natural stone cannot provide its furnace input");
        var smelt=new SingleRecipeInput(drops.getFirst()); var stone=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,smelt,level).orElseThrow().value().assemble(smelt,level.registryAccess());
        var input=new SingleRecipeInput(stone); var brick=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level).orElseThrow().value().assemble(input,level.registryAccess());
        helper.assertTrue(brick.is(WastelandProcessingContent.BRICK.get()),"Natural plateau harvest is disconnected from machine ingredient");
        LOGGER.info("TRANSMUTATOR NATIVE WORLD AUDIT: plateau={}, surface stones={}, first harvest={}, native cobble -> stone -> brick chain",found.getFirst(),stones,mined);
        helper.succeed();
    }

    private static TransmutatorBlockEntity place(ServerLevel level,BlockPos pos) {
        clear(level,pos); level.setBlock(pos,WastelandProcessingContent.TRANSMUTATOR.get().defaultBlockState(),3);
        return (TransmutatorBlockEntity)level.getBlockEntity(pos);
    }
    private static void tick(ServerLevel level,BlockPos pos,TransmutatorBlockEntity machine,int count) {
        for(int index=0;index<count;index++) TransmutatorBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
    }
    private static void clear(ServerLevel level,BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof TransmutatorBlockEntity machine) machine.clearContent();
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
    }
}
