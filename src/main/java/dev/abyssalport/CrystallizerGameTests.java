package dev.abyssalport;

import java.util.*;
import dev.abyssalport.block.CrystallizerBlock;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.content.*;
import dev.abyssalport.menu.CrystallizerMenu;
import dev.abyssalport.recipe.CrystallizationRecipe;
import dev.abyssalport.research.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrystallizerGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private CrystallizerGameTests() { }
    private static ItemStack crystal(String material,String size,int count) {
        var family=CrystallizationContent.material(material);
        return new ItemStack(switch(size) { case "shard"->family.shard().get(); case "fragment"->family.fragment().get();
            case "cluster"->family.clusterItem().get(); default->family.crystal().get(); },count);
    }
    private static ItemStack water() { var stack=new ItemStack(Items.POTION); stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.WATER)); return stack; }
    private static CrystallizerBlockEntity place(ServerLevel level,BlockPos pos) {
        clear(level,pos); level.setBlock(pos,CrystallizationContent.MACHINE.get().defaultBlockState(),3);
        return (CrystallizerBlockEntity)level.getBlockEntity(pos);
    }
    private static void clear(ServerLevel level,BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof FuelProcessingBlockEntity machine) machine.clearContent();
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
    }
    private static void tick(ServerLevel level,BlockPos pos,CrystallizerBlockEntity machine,int count) {
        for(int index=0;index<count;index++) CrystallizerBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
    }
    private static ItemStack craft(ServerLevel level,List<ItemStack> slots,int width,int height) {
        var input=CraftingInput.of(width,height,slots);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input,level.registryAccess());
    }
    private static ItemStack machineCraft(ServerLevel level) {
        var brick=new ItemStack(DreadProcessingContent.BRICKS.get()); var block=new ItemStack(DreadProcessingContent.STORAGE_ITEM.get());
        return craft(level,List.of(brick.copy(),brick.copy(),brick.copy(),block.copy(),new ItemStack(Items.FURNACE),block.copy(),brick.copy(),brick.copy(),brick.copy()),3,3);
    }

    @GameTest(template="empty",batch="crystallizer_native_recipes")
    public static void realCraftingFractionsWaterPredicateAndNetworkAreLoaded(GameTestHelper helper) {
        var level=helper.getLevel(); helper.assertTrue(machineCraft(level).is(CrystallizationContent.MACHINE_ITEM.get()),"Native Dreadium/brick machine recipe is not loaded");
        int loaded=0;
        for(var holder:level.getRecipeManager().getRecipes()) if(holder.value() instanceof CrystallizationRecipe recipe) {
            loaded++; helper.assertTrue(!recipe.result().isEmpty() && !recipe.ingredient().isEmpty(),"Crystallization references an absent ingredient/result: "+holder.id());
            var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            try {
                var codec=CrystallizationContent.SERIALIZER.get().streamCodec(); codec.encode(buffer,recipe); var decoded=codec.decode(buffer);
                helper.assertTrue(ItemStack.matches(recipe.result(),decoded.result()) && ItemStack.matches(recipe.secondary(),decoded.secondary())
                        && recipe.waterOnly()==decoded.waterOnly() && recipe.experience()==decoded.experience(),"Dual output network roundtrip loses data");
            } finally { buffer.release(); }
        }
        helper.assertTrue(loaded==63,"Native crystallization recipe count differs: "+loaded);
        for(var family:CrystallizationContent.CRYSTALS.values()) {
            var fractions=List.of(family.fragment().get(),family.shard().get(),family.crystal().get(),family.clusterItem().get());
            for(int index=0;index<3;index++) {
                var input=new ArrayList<ItemStack>(); for(int slot=0;slot<9;slot++) input.add(new ItemStack(fractions.get(index)));
                var packed=craft(level,input,3,3); var unpacked=craft(level,List.of(packed),1,1);
                helper.assertTrue(packed.is(fractions.get(index+1)) && packed.getCount()==1 && unpacked.is(fractions.get(index)) && unpacked.getCount()==9,"Crystal compression is not reversible");
            }
        }
        var accepted=level.getRecipeManager().getRecipeFor(CrystallizationContent.TYPE.get(),new SingleRecipeInput(water()),level).orElseThrow().value();
        var healing=new ItemStack(Items.POTION); healing.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.HEALING));
        helper.assertTrue(!accepted.matches(new SingleRecipeInput(healing),level) && !accepted.matches(new SingleRecipeInput(new ItemStack(Items.POTION)),level),"Non-water potions match water chemistry");
        var player=helper.makeMockServerPlayerInLevel(); var other=helper.makeMockServerPlayerInLevel();
        helper.assertTrue(WastelandProgressionResearch.discover(player,crystal("carbon","crystal",1)) && !WastelandProgressionResearch.discover(player,crystal("oxygen","shard",1))
                && EarlyResearch.has(player,EarlyResearch.CRYSTALLIZATION) && !EarlyResearch.has(other,EarlyResearch.CRYSTALLIZATION),"Crystal research repeats or crosses players");
        helper.succeed();
    }

    @GameTest(template="empty",batch="crystallizer_save_containers")
    public static void savedDualOutputFinishesAtTwoHundredTicksAndKeepsBucketsAndBottles(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        try {
            machine.setItem(0,new ItemStack(Items.WATER_BUCKET)); machine.setItem(1,new ItemStack(Items.BLAZE_ROD)); tick(level,pos,machine,88);
            helper.assertTrue(machine.progress()==88 && machine.getItem(2).isEmpty() && machine.getItem(3).isEmpty()
                    && level.getBlockState(pos).getValue(CrystallizerBlock.LIT) && level.getBlockEntity(pos)==machine,"Dual machine processes early or replaces itself on LIT transition");
            var restored=new CrystallizerBlockEntity(pos,machine.getBlockState());
            restored.loadWithComponents(machine.saveWithFullMetadata(level.registryAccess()),level.registryAccess()); level.setBlockEntity(restored);
            tick(level,pos,restored,111); helper.assertTrue(restored.progress()==199 && restored.getItem(2).isEmpty() && restored.getItem(0).is(Items.WATER_BUCKET),"Saved machine finishes early");
            tick(level,pos,restored,1);
            helper.assertTrue(ItemStack.matches(restored.getItem(2),crystal("hydrogen","crystal",12)) && ItemStack.matches(restored.getItem(3),crystal("oxygen","shard",6))
                    && restored.getItem(0).is(Items.BUCKET) && restored.getItem(0).getCount()==1,"Water transaction loses/duplicates either output/container");
            tick(level,pos,restored,5); helper.assertTrue(restored.getItem(2).getCount()==12,"Returned empty bucket loops into extra water");
            restored.setItem(0,water()); restored.setItem(2,ItemStack.EMPTY); restored.setItem(3,ItemStack.EMPTY); tick(level,pos,restored,200);
            helper.assertTrue(ItemStack.matches(restored.getItem(2),crystal("hydrogen","shard",6)) && restored.getItem(3).getCount()==3 && restored.getItem(0).is(Items.GLASS_BOTTLE),"Bottle transaction differs from the actual water recipe");
            int burn=restored.burnTime(); level.setBlock(pos,restored.getBlockState().rotate(Rotation.CLOCKWISE_90),3);
            helper.assertTrue(level.getBlockEntity(pos)==restored && restored.burnTime()==burn,"Rotation discards saved state");
        } finally { clear(level,pos); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="crystallizer_dual_atomicity")
    public static void eitherBlockedOutputOrWrongComponentsPreserveInputAndFreshFuel(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        try {
            machine.setItem(0,new ItemStack(Items.STONE)); machine.setItem(1,new ItemStack(Items.BLAZE_ROD));
            machine.setItem(3,crystal("magnesia","shard",63)); tick(level,pos,machine,10);
            helper.assertTrue(machine.burnTime()==0 && machine.getItem(1).getCount()==1 && machine.getItem(2).isEmpty(),"Blocked secondary output burns fresh fuel or emits only the first result");
            var named=crystal("magnesia","shard",1); named.set(DataComponents.CUSTOM_NAME,Component.literal("Protected sample")); machine.setItem(3,named); tick(level,pos,machine,10);
            helper.assertTrue(machine.burnTime()==0 && machine.getItem(3).getCount()==1,"Secondary output merges different components");
            machine.setItem(3,ItemStack.EMPTY); machine.setItem(2,crystal("silica","shard",61)); tick(level,pos,machine,10);
            helper.assertTrue(machine.burnTime()==0 && machine.getItem(0).is(Items.STONE),"Blocked primary output consumes input/fuel");
            machine.setItem(2,ItemStack.EMPTY); tick(level,pos,machine,90);
            var changed=new ItemStack(Items.STONE); changed.set(DataComponents.CUSTOM_NAME,Component.literal("Different specimen")); machine.setItem(0,changed); tick(level,pos,machine,1);
            helper.assertTrue(machine.progress()==1 && machine.getItem(2).isEmpty(),"Input components keep stale progress");
            machine.setItem(3,crystal("magnesia","shard",64)); int burn=machine.burnTime(); tick(level,pos,machine,5);
            helper.assertTrue(machine.progress()==0 && machine.burnTime()==burn-5 && machine.getItem(0).getCount()==1,"Blocked active machine does not reset safely");
            clear(level,pos); machine=place(level,pos); machine.setItem(0,new ItemStack(Items.COAL)); machine.setItem(1,new ItemStack(Items.COAL)); tick(level,pos,machine,10);
            helper.assertTrue(machine.burnTime()==0 && machine.getItem(1).getCount()==1,"Coal used as direct furnace fuel");
            machine.setItem(1,new ItemStack(DreadlandsContent.FRAGMENT.get(),2)); tick(level,pos,machine,200);
            helper.assertTrue(machine.getItem(2).is(CrystallizationContent.material("carbon").crystal().get()) && machine.getItem(1).isEmpty(),"Two 100-tick fragments cannot sustain an operation");
            helper.assertTrue(CrystallizerBlockEntity.fuelTicks(crystal("carbon","crystal",1))==200 && CrystallizerBlockEntity.fuelTicks(crystal("carbon","cluster",1))==1800
                    && CrystallizerBlockEntity.fuelTicks(new ItemStack(DreadProcessingContent.SHARD.get()))==1000,"Source fuel times differ");
        } finally { clear(level,pos); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="crystallizer_recipe_reload")
    public static void ReloadingEitherOutputOrExperienceResetsTheWholeTransaction(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        var original=List.copyOf(level.getRecipeManager().getRecipes()); var id=ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,"test_only_dual_reload");
        try {
            var recipes=new ArrayList<RecipeHolder<?>>(original); recipes.add(new RecipeHolder<>(id,new CrystallizationRecipe(Ingredient.of(Items.DIRT),new ItemStack(Items.DIAMOND),new ItemStack(Items.GOLD_INGOT),1F,false)));
            level.getRecipeManager().replaceRecipes(recipes); machine.setItem(0,new ItemStack(Items.DIRT)); machine.setItem(1,new ItemStack(Items.BLAZE_ROD)); tick(level,pos,machine,120);
            recipes.set(recipes.size()-1,new RecipeHolder<>(id,new CrystallizationRecipe(Ingredient.of(Items.DIRT),new ItemStack(Items.DIAMOND),new ItemStack(Items.IRON_INGOT,2),2F,false)));
            level.getRecipeManager().replaceRecipes(recipes); tick(level,pos,machine,1);
            helper.assertTrue(machine.progress()==1 && machine.getItem(2).isEmpty() && machine.getItem(3).isEmpty(),"Reloaded secondary output reused prior progress");
            tick(level,pos,machine,199);
            helper.assertTrue(machine.getItem(2).is(Items.DIAMOND) && machine.getItem(3).is(Items.IRON_INGOT) && machine.getItem(3).getCount()==2
                    && machine.pendingExperience()==2F,"Reloaded dual recipe uses stale output/experience");
        } finally { level.getRecipeManager().replaceRecipes(original); clear(level,pos); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="crystallizer_menu_hoppers")
    public static void NativeMenuAndAutomationProtectBothOutputsAndLiveFuel(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        var player=helper.makeMockServerPlayerInLevel(); player.moveTo(pos.getCenter());
        try {
            level.getBlockState(pos).useWithoutItem(level,player,new BlockHitResult(pos.getCenter(),Direction.NORTH,pos,false));
            helper.assertTrue(player.containerMenu instanceof CrystallizerMenu && player.containerMenu.slots.size()==40,"Native right-click does not open both output slots"); player.closeContainer();
            var menu=new CrystallizerMenu(9,player.getInventory(),machine);
            helper.assertTrue(!menu.getSlot(2).mayPlace(new ItemStack(Items.DIRT)) && !menu.getSlot(3).mayPlace(new ItemStack(Items.DIRT)),"Secondary result slot permits insertion");
            player.getInventory().setItem(9,new ItemStack(Items.STONE)); menu.quickMoveStack(player,4);
            player.getInventory().setItem(10,new ItemStack(Items.BLAZE_ROD)); menu.quickMoveStack(player,5); tick(level,pos,machine,5);
            menu.setData(0,199); menu.setData(1,20000); helper.assertTrue(menu.progress()==5 && menu.burnTime()==2396,"Client changes authoritative machine counters");
            machine.clearContent(); var top=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.UP);
            var side=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.NORTH); var bottom=level.getCapability(Capabilities.ItemHandler.BLOCK,pos,Direction.DOWN);
            helper.assertTrue(top!=null && side!=null && bottom!=null && top.getSlots()==1 && side.getSlots()==1 && bottom.getSlots()==4,"Sided dual output capability missing");
            helper.assertTrue(side.insertItem(0,new ItemStack(Items.COAL),false).is(Items.COAL)
                    && bottom.insertItem(0,new ItemStack(Items.STONE),false).is(Items.STONE),"Automation bypasses fuel/output restrictions");
            helper.assertTrue(HopperBlockEntity.addItem(null,machine,new ItemStack(Items.BLAZE_ROD),Direction.WEST).isEmpty(),"Native hopper cannot fuel machine");
            helper.assertTrue(bottom.extractItem(2,1,false).isEmpty(),"Bottom steals unburned fuel");
            machine.setItem(0,new ItemStack(Items.GLASS_BOTTLE)); helper.assertTrue(bottom.extractItem(3,1,false).is(Items.GLASS_BOTTLE),"Bottom cannot remove returned bottle");
            machine.setItem(2,crystal("silica","shard",4)); machine.setItem(3,crystal("magnesia","shard",4));
            menu.quickMoveStack(player,3); menu.quickMoveStack(player,2);
            helper.assertTrue(machine.getItem(2).isEmpty() && machine.getItem(3).isEmpty() && player.getInventory().countItem(CrystallizationContent.material("magnesia").shard().get())==4,"Shift-click loses secondary output");
            player.moveTo(pos.getCenter().add(20,0,0)); helper.assertTrue(!menu.stillValid(player),"Distant player retains access");
        } finally { clear(level,pos); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="crystallizer_single_xp")
    public static void EitherOutputPaysSavedExperienceOnlyOnceAndBreakingDropsBothOnce(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var machine=place(level,pos);
        var recipes=List.copyOf(level.getRecipeManager().getRecipes()); var spawned=new ArrayList<net.minecraft.world.entity.Entity>();
        java.util.function.Consumer<EntityJoinLevelEvent> capture=event->{ if(event.getLevel()==level && event.getEntity().position().distanceTo(pos.getCenter())<4
                && (event.getEntity() instanceof ItemEntity || event.getEntity() instanceof ExperienceOrb)) spawned.add(event.getEntity()); };
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            var replaced=new ArrayList<RecipeHolder<?>>(recipes); replaced.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,"test_only_dual_xp"),
                    new CrystallizationRecipe(Ingredient.of(Items.DIRT),new ItemStack(Items.DIAMOND),new ItemStack(Items.EMERALD),1.25F,false)));
            level.getRecipeManager().replaceRecipes(replaced); machine.setItem(0,new ItemStack(Items.DIRT)); machine.setItem(1,new ItemStack(Items.BLAZE_ROD)); tick(level,pos,machine,200);
            var restored=new CrystallizerBlockEntity(pos,machine.getBlockState()); restored.loadWithComponents(machine.saveWithFullMetadata(level.registryAccess()),level.registryAccess()); level.setBlockEntity(restored);
            var player=helper.makeMockServerPlayerInLevel(); var menu=new CrystallizerMenu(1,player.getInventory(),restored);
            menu.quickMoveStack(player,3); menu.quickMoveStack(player,2); restored.payExperience();
            helper.assertTrue(spawned.stream().filter(e->e instanceof ExperienceOrb).mapToInt(e->((ExperienceOrb)e).getValue()).sum()==1 && restored.pendingExperience()==.25F,"Dual output extraction duplicates saved experience");
            restored.setItem(2,new ItemStack(Items.DIAMOND,3)); restored.setItem(3,new ItemStack(Items.EMERALD,4)); level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            helper.assertTrue(restored.isEmpty() && drops(spawned,Items.DIAMOND)==3 && drops(spawned,Items.EMERALD)==4,"Native removal duplicates or loses either output");
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3); helper.assertTrue(drops(spawned,Items.EMERALD)==4,"Repeated removal duplicates secondary output");
        } finally { level.getRecipeManager().replaceRecipes(recipes); NeoForge.EVENT_BUS.unregister(capture); clear(level,pos); spawned.forEach(net.minecraft.world.entity.Entity::discard); }
        helper.succeed();
    }
    private static int drops(List<net.minecraft.world.entity.Entity> entities,Item item) {
        return entities.stream().filter(e->e instanceof ItemEntity).map(e->((ItemEntity)e).getItem()).filter(stack->stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static ItemStack executeCraft(GameTestHelper helper,net.minecraft.server.level.ServerPlayer player,List<ItemStack> input) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,4)); var previous=level.getBlockState(pos);
        level.setBlock(pos,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        try {
            var menu=new CraftingMenu(7,player.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(level,pos));
            for(int index=0;index<9;index++) menu.getSlot(index+1).set(input.get(index));
            var result=menu.getSlot(0).getItem().copy(); helper.assertTrue(!result.isEmpty(),"Native crafting-menu output is empty");
            int before=player.getInventory().countItem(result.getItem()); menu.quickMoveStack(player,0);
            helper.assertTrue(player.getInventory().countItem(result.getItem())==before+result.getCount(),"Native crafting-menu result was not delivered exactly once");
            for(int index=1;index<=9;index++) helper.assertTrue(menu.getSlot(index).getItem().isEmpty(),"Actual crafting did not consume exactly one ingredient per occupied slot");
            return result;
        } finally { level.setBlock(pos,previous,3); }
    }
    private static ItemStack take(net.minecraft.server.level.ServerPlayer player,Item item) {
        for(int index=0;index<player.getInventory().getContainerSize();index++)
            if(player.getInventory().getItem(index).is(item)) return player.getInventory().removeItem(index,1);
        throw new GameTestAssertException("Missing actual inventory ingredient: "+item);
    }

    @GameTest(template="empty",batch="crystal_carbon_to_diamond")
    public static void RealMenusSpendSixtyFourCarbonCrystalsAndObsidianForExactlyOneDiamond(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var dest=pos.east(2);
        var machine=place(level,pos); var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CarbonAudit"));
        player.moveTo(pos.getCenter());
        try {
            machine.setItem(0,new ItemStack(Items.COAL,64)); machine.setItem(1,new ItemStack(Items.BLAZE_ROD,6)); tick(level,pos,machine,12800);
            helper.assertTrue(ItemStack.matches(machine.getItem(2),crystal("carbon","crystal",64)) && machine.getItem(0).isEmpty(),"Coal does not form exactly 64 carbon crystals");
            new CrystallizerMenu(1,player.getInventory(),machine).quickMoveStack(player,2);
            for(int operation=0;operation<8;operation++) {
                var slots=new ArrayList<ItemStack>();
                for(int slot=0;slot<9;slot++) slots.add(slot==4?ItemStack.EMPTY:take(player,CrystallizationContent.material("carbon").crystal().get()));
                helper.assertTrue(executeCraft(helper,player,slots).is(CrystallizationContent.CARBON_CLUSTER.get()),"Eight crystals do not form the source carbon cluster");
            }
            helper.assertTrue(player.getInventory().countItem(CrystallizationContent.material("carbon").crystal().get())==0
                    && player.getInventory().countItem(CrystallizationContent.CARBON_CLUSTER.get())==8,"Carbon cluster crafting duplicates or loses materials");
            var denseInput=new ArrayList<ItemStack>();
            for(int slot=0;slot<9;slot++) denseInput.add(slot==4?new ItemStack(Items.OBSIDIAN):take(player,CrystallizationContent.CARBON_CLUSTER.get()));
            helper.assertTrue(executeCraft(helper,player,denseInput).is(CrystallizationContent.DENSE_CARBON_CLUSTER.get()),"Dense cluster lacks its source Obsidian core");
            clear(level,dest); level.setBlock(dest,WastelandProcessingContent.TRANSMUTATOR.get().defaultBlockState(),3);
            var trans=(TransmutatorBlockEntity)level.getBlockEntity(dest);
            trans.setItem(0,take(player,CrystallizationContent.DENSE_CARBON_CLUSTER.get())); trans.setItem(1,new ItemStack(CoraliumContent.GEM.get()));
            for(int tick=0;tick<200;tick++) TransmutatorBlockEntity.serverTick(level,dest,trans.getBlockState(),trans);
            helper.assertTrue(trans.getItem(0).isEmpty() && trans.getItem(2).is(Items.DIAMOND) && trans.getItem(2).getCount()==1 && trans.pendingExperience()==.5F,"Dense cluster transmutation duplicates or loses the diamond");
            helper.assertTrue(CrystallizerBlockEntity.fuelTicks(new ItemStack(CrystallizationContent.CARBON_CLUSTER.get()))==1600
                    && CrystallizerBlockEntity.fuelTicks(new ItemStack(CrystallizationContent.DENSE_CARBON_CLUSTER.get()))==12800,"Compound carbon source fuel differs");
        } finally { clear(level,pos); clear(level,dest); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="crystal_sulfur_to_gunpowder")
    public static void NativeLapisSeparationAndSulfurRecoveryReachTheRealNitreGunpowderRecipe(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var dest=pos.east(2); var machine=place(level,pos);
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SulfurAudit")); player.moveTo(pos.getCenter());
        try {
            machine.setItem(0,new ItemStack(Items.LAPIS_LAZULI,3)); machine.setItem(1,crystal("carbon","crystal",3)); tick(level,pos,machine,600);
            helper.assertTrue(ItemStack.matches(machine.getItem(2),crystal("silica","crystal",18)) && ItemStack.matches(machine.getItem(3),crystal("sulfur","crystal",12)),"Actual lapis separates into the wrong quantities");
            new CrystallizerMenu(1,player.getInventory(),machine).quickMoveStack(player,3);
            var slots=new ArrayList<ItemStack>(); for(int slot=0;slot<9;slot++) slots.add(take(player,CrystallizationContent.material("sulfur").crystal().get()));
            helper.assertTrue(executeCraft(helper,player,slots).is(CrystallizationContent.material("sulfur").clusterItem().get()),"Native sulfur compression is unreachable");
            helper.assertTrue(player.getInventory().countItem(CrystallizationContent.material("sulfur").crystal().get())==3,"Compression consumes more or fewer than nine crystals");
            clear(level,dest); level.setBlock(dest,WastelandProcessingContent.TRANSMUTATOR.get().defaultBlockState(),3); var trans=(TransmutatorBlockEntity)level.getBlockEntity(dest);
            trans.setItem(0,take(player,CrystallizationContent.material("sulfur").clusterItem().get())); trans.setItem(1,new ItemStack(CoraliumContent.GEM.get()));
            for(int tick=0;tick<200;tick++) TransmutatorBlockEntity.serverTick(level,dest,trans.getBlockState(),trans);
            helper.assertTrue(trans.getItem(2).is(CrystallizationContent.SULFUR.get()) && trans.getItem(2).getCount()==1,"Sulfur cluster does not yield one native dust");
            var gunpowder=new ArrayList<ItemStack>();
            for(int slot=0;slot<9;slot++) gunpowder.add(slot==1?trans.removeItem(2,1):slot==4?new ItemStack(Items.CHARCOAL):new ItemStack(WastelandMineralsContent.NITRE.get()));
            var crafted=executeCraft(helper,player,gunpowder);
            helper.assertTrue(crafted.is(Items.GUNPOWDER) && crafted.getCount()==4 && trans.getItem(2).isEmpty(),"Seven Nitre / sulfur / charcoal do not make exactly four gunpowder");
        } finally { clear(level,pos); clear(level,dest); }
        helper.succeed();
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="crystallizer_real_dimension_ticks",timeoutTicks=600)
    public static void GenuineDimensionTicksProcessWaterAndReturnItsContainer(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(DreadlandsContent.DIMENSION); var pos=new BlockPos(26344,150,26344);
        var previous=level.getBlockState(pos); int cx=pos.getX()>>4,cz=pos.getZ()>>4;
        boolean wasForced=level.getForcedChunks().contains(ChunkPos.asLong(cx,cz));
        helper.assertTrue(machineCraft(level).is(CrystallizationContent.MACHINE_ITEM.get()),"Real dimension has no native machine recipe");
        level.setChunkForced(cx,cz,true);
        helper.runAfterDelay(20,()->{
            var machine=place(level,pos); machine.setItem(0,new ItemStack(Items.WATER_BUCKET)); machine.setItem(1,new ItemStack(Items.BLAZE_ROD));
            helper.runAfterDelay(205,()->{
                try {
                    helper.assertTrue(ItemStack.matches(machine.getItem(2),crystal("hydrogen","crystal",12)) && ItemStack.matches(machine.getItem(3),crystal("oxygen","shard",6))
                            && machine.getItem(0).is(Items.BUCKET) && machine.burnTime()>0 && machine.pendingExperience()==.1F,"Actual native dimension ticks fail the dual-output transaction");
                    LOGGER.info("CRYSTALLIZER WORLD AUDIT: native Dreadium/brick crafting, actual dimension ticks, 12 hydrogen / 6 oxygen / one bucket, no manual processing tick");
                    helper.succeed();
                } finally { clear(level,pos); level.setBlock(pos,previous,3); if(!wasForced) level.setChunkForced(cx,cz,false); }
            });
        });
    }
}
