package dev.abyssalport;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandProgressionContent;
import dev.abyssalport.content.*;
import dev.abyssalport.block.entity.EnergyContainerBlockEntity;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.recipe.BookUpgradeRecipe;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.research.WastelandProgressionResearch;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.RitualCatalog;
import dev.abyssalport.ritual.RitualGroundFormation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import dev.abyssalport.item.RendingStaffItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DreadlandsProgressionGameTests {
    private DreadlandsProgressionGameTests() { }

    @GameTest(template="empty", batch="dreadlands_book_recipe")
    public static void realThirdBookUpgradePreservesComponentsAndRecipeNetworkRoundtrip(GameTestHelper helper) {
        var level=helper.getLevel(); var old=new ItemStack(WastelandProgressionContent.BOOK.get());
        NecronomiconItem.setPotentialEnergy(old,9876.5F); old.set(DataComponents.CUSTOM_NAME,Component.literal("Dread field notes"));
        var tag=old.get(DataComponents.CUSTOM_DATA).copyTag(); tag.putString("PrivateNote","retain"); old.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        var skin=new ItemStack(DreadlandsProgressionContent.SKIN.get());
        var input=CraftingInput.of(2,2,List.of(skin.copy(),skin.copy(),skin.copy(),old));
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value();
        helper.assertTrue(recipe instanceof BookUpgradeRecipe,"Third book is a component-losing vanilla recipe");
        var result=recipe.assemble(input,level.registryAccess());
        helper.assertTrue(result.is(DreadlandsProgressionContent.BOOK.get()) && result.getCount()==1 && NecronomiconItem.tier(result)==2
                && NecronomiconItem.capacity(result)==20000 && NecronomiconItem.getPotentialEnergy(result)==9876.5F
                && result.getHoverName().getString().equals("Dread field notes") && result.get(DataComponents.CUSTOM_DATA).copyTag().getString("PrivateNote").equals("retain"),"Third book loses data/capacity");
        helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty) && old.getCount()==1
                && NecronomiconItem.getPotentialEnergy(old)==9876.5F,"Craft preview mutates/duplicates source book");
        var mirrored=CraftingInput.of(2,2,List.of(skin.copy(),skin.copy(),old,skin.copy()));
        helper.assertTrue(recipe.matches(mirrored,level),"Native mirrored book recipe is unavailable");
        var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try {
            var codec=WastelandProgressionContent.BOOK_UPGRADE.get().streamCodec(); codec.encode(buffer,(BookUpgradeRecipe)recipe);
            var synced=codec.decode(buffer);
            helper.assertTrue(synced.matches(input,level) && ItemStack.matches(result,synced.assemble(input,level.registryAccess())),"Synced recipe loses book components");
        } finally { buffer.release(); }
        NecronomiconItem.setPotentialEnergy(result,19500);
        var saved=ItemStack.parse(level.registryAccess(),result.save(level.registryAccess())).orElseThrow();
        helper.assertTrue(NecronomiconItem.getPotentialEnergy(saved)==19500 && NecronomiconItem.capacity(saved)==20000,"Saved third book clamps to old capacity");
        var player=helper.makeMockServerPlayerInLevel(); var other=helper.makeMockServerPlayerInLevel();
        helper.assertTrue(WastelandProgressionResearch.discover(player,saved) && !WastelandProgressionResearch.discover(player,saved)
                && EarlyResearch.has(player,EarlyResearch.DREADLANDS_BOOK) && !EarlyResearch.has(other,EarlyResearch.DREADLANDS_BOOK),"Third book research repeats/leaks");
        helper.succeed();
    }

    @GameTest(template="empty", batch="dreadlands_book_capacity")
    public static void existingEnergyDevicesAndEffigyRespectTwentyThousandCapacity(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2));
        level.setBlock(pos,AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(),3);
        var storage=(EnergyContainerBlockEntity)level.getBlockEntity(pos);
        var book=new ItemStack(DreadlandsProgressionContent.BOOK.get()); NecronomiconItem.setPotentialEnergy(book,19990);
        storage.addEnergy(100); storage.items().setStackInSlot(1,book);
        EnergyContainerBlockEntity.serverTick(level,pos,storage.getBlockState(),storage);
        helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==20000 && storage.energy()==90,"Container truncates third tier or loses overflow");
        storage.items().setStackInSlot(1,ItemStack.EMPTY); storage.items().setStackInSlot(0,book);
        EnergyContainerBlockEntity.serverTick(level,pos,storage.getBlockState(),storage);
        helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==19980 && storage.energy()==110,"Third book cannot discharge");
        var player=helper.makeMockServerPlayerInLevel(); player.setItemInHand(InteractionHand.OFF_HAND,book);
        helper.assertTrue(dev.abyssalport.block.entity.PrototypeEffigyBlockEntity.chargeHeldBook(player)
                && NecronomiconItem.getPotentialEnergy(book)==19985,"Effigy rejects third book in offhand");
        var restored=new EnergyContainerBlockEntity(pos,storage.getBlockState()); restored.loadWithComponents(storage.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        helper.assertTrue(restored.energy()==110 && NecronomiconItem.getPotentialEnergy(restored.items().getStackInSlot(0))==19985,"Stored third book loses high PE");
        helper.succeed();
    }

    @GameTest(template="empty", batch="dreadlands_tier_gates")
    public static void weakBooksAndWrongDimensionCannotFormOrUseThirdTierGround(GameTestHelper helper) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(4,1,4));
        var player=helper.makeMockServerPlayerInLevel(); var old=new ItemStack(WastelandProgressionContent.BOOK.get());
        var book=new ItemStack(DreadlandsProgressionContent.BOOK.get()); NecronomiconItem.setPotentialEnergy(book,20000);
        level.setBlock(pos,DreadlandsContent.COBBLESTONE.get().defaultBlockState(),3);
        helper.assertTrue(RitualGroundFormation.form(level,pos,player,old)==RitualGroundFormation.Result.BOOK_TOO_WEAK
                && RitualGroundFormation.form(level,pos,player,book)==RitualGroundFormation.Result.WRONG_DIMENSION
                && level.getBlockState(pos).is(DreadlandsContent.COBBLESTONE.get()),"Denied third ground mutates material");
        level.setBlock(pos,DreadlandsProgressionContent.ALTAR.get().defaultBlockState(),3);
        var altar=(RitualAltarBlockEntity)level.getBlockEntity(pos); altar.placeInput(new ItemStack(WastelandProgressionContent.WASTELAND_STAFF.get()));
        fillStaff(level,pos); player.setItemInHand(InteractionHand.MAIN_HAND,book);
        helper.assertTrue(!staffDefinition().matches(level,pos) && !altar.start(level,player)
                && NecronomiconItem.getPotentialEnergy(book)==20000 && altar.getInput().is(WastelandProgressionContent.WASTELAND_STAFF.get()),"Third infusion bypasses dimension or eats denied inputs");
        for(var block:List.of(DreadlandsProgressionContent.ALTAR.get(),DreadlandsProgressionContent.PEDESTAL.get())) {
            var drops=net.minecraft.world.level.block.Block.getDrops(block.defaultBlockState(),level,pos,null,null,ItemStack.EMPTY);
            helper.assertTrue(drops.size()==1 && drops.getFirst().is(DreadlandsContent.COBBLESTONE.get().asItem()),"Tier-two ritual block does not return real ground material");
        }
        clear(level,pos); helper.succeed();
    }

    @GameTest(template="empty", batch="dread_rending_rejection")
    public static void unrelatedMobsAndOutOfDimensionCarriersNeverYieldDreadEnergy(GameTestHelper helper) {
        var level=helper.getLevel(); var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var staff=new ItemStack(DreadlandsProgressionContent.STAFF.get()); var carrier=DreadlandsContent.DREADLING.get().create(level);
        carrier.moveTo(player.position().add(0,0,2));
        helper.assertTrue(!RendingStaffItem.drain(staff,player,carrier) && RendingStaffItem.energy(staff,RendingStaffItem.Channel.DREAD)==0,"Carrier outside Dreadlands bypasses dimension gate");
        var cow=net.minecraft.world.entity.EntityType.COW.create(level); cow.moveTo(carrier.position());
        helper.assertTrue(!RendingStaffItem.drain(staff,player,cow),"Unrelated mob supplies Dread energy");
        carrier.discard(); cow.discard(); helper.succeed();
    }

    @GameTest(template="empty", templateNamespace="abyssalport_worldtest", batch="dreadlands_full_book_chain", timeoutTicks=600)
    public static void actualDreadRendingSkinBookSavedStaffAndLaterPowerstoneFormAConnectedChain(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(DreadlandsContent.DIMENSION); var pos=new BlockPos(7208,160,7208);
        var prior=prepare(level,pos); var player=helper.makeMockServerPlayerInLevel(); var returnPos=player.position();
        player.teleportTo(level,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        var target=DreadlandsContent.DREADLING.get().create(level); target.setNoAi(true); target.setNoGravity(true);
        target.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+2.5); level.addFreshEntity(target);
        try {
            var staff=new ItemStack(WastelandProgressionContent.RENDING_STAFF.get());
            for(int cycle=0;cycle<3;cycle++) {
                for(int hit=0;hit<100;hit++) {
                    target.setHealth(target.getMaxHealth()); target.invulnerableTime=0; player.getCooldowns().removeCooldown(staff.getItem());
                    helper.assertTrue(RendingStaffItem.drain(staff,player,target),"Successful carrier drain refused at "+hit);
                }
                helper.assertTrue(RendingStaffItem.energy(staff,RendingStaffItem.Channel.DREAD)==100 && RendingStaffItem.redeem(staff,player)
                        && RendingStaffItem.energy(staff,RendingStaffItem.Channel.DREAD)==0 && !RendingStaffItem.redeem(staff,player),"Dread channel cannot redeem exactly once");
            }
            helper.assertTrue(player.getInventory().countItem(DreadlandsProgressionContent.ESSENCE.get())==3
                    && RendingStaffItem.energy(staff,RendingStaffItem.Channel.ABYSSAL)==0,"Carrier channel mixes/misses essences");
            var materials=new ArrayList<ItemStack>();
            for(int slot=0;slot<9;slot++) materials.add(new ItemStack(slot==4?DreadlandsProgressionContent.ESSENCE.get():DreadlandsContent.FRAGMENT.get()));
            var skinInput=CraftingInput.of(3,3,materials);
            var skinRecipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,skinInput,level).orElseThrow().value();
            var skin=skinRecipe.assemble(skinInput,level.registryAccess()); helper.assertTrue(skin.is(DreadlandsProgressionContent.SKIN.get()) && skin.getCount()==1,"Actual fragment/essence recipe is unreachable");
            var old=new ItemStack(WastelandProgressionContent.BOOK.get()); NecronomiconItem.setPotentialEnergy(old,10000);
            var bookInput=CraftingInput.of(2,2,List.of(skin.copy(),skin.copy(),skin.copy(),old));
            var book=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,bookInput,level).orElseThrow().value().assemble(bookInput,level.registryAccess());
            helper.assertTrue(book.is(DreadlandsProgressionContent.BOOK.get()) && NecronomiconItem.getPotentialEnergy(book)==10000,"Second-to-third book chain breaks");
            var corner=pos.offset(BasicScrollRitual.pedestalOffsets().get(7)); level.setBlock(corner,WastelandContent.COBBLESTONE.get().defaultBlockState(),3);
            helper.assertTrue(RitualGroundFormation.form(level,pos,player,book)==RitualGroundFormation.Result.INCOMPLETE
                    && level.getBlockState(pos).is(DreadlandsContent.COBBLESTONE.get()),"Mixed third ground partially changes");
            level.setBlock(corner,DreadlandsContent.COBBLESTONE.get().defaultBlockState(),3);
            helper.assertTrue(RitualGroundFormation.form(level,pos,player,book)==RitualGroundFormation.Result.CREATED,"Native third ground cannot form");
            var altar=(RitualAltarBlockEntity)level.getBlockEntity(pos);
            helper.assertTrue(AbyssalPort.RITUAL_ALTAR_ENTITY.get().isValid(altar.getBlockState()),"Third altar BE is unsupported");
            var charged=new ItemStack(WastelandProgressionContent.WASTELAND_STAFF.get()); charged.set(DataComponents.CUSTOM_NAME,Component.literal("All charged channels"));
            var charges=new CompoundTag(); charges.putInt("energyDread",99); charges.putInt("energyAbyssal",17); charges.putInt("energyShadow",37); charges.putString("PrivateNote","keep");
            charged.set(DataComponents.CUSTOM_DATA,CustomData.of(charges)); altar.placeInput(charged); fillStaff(level,pos);
            player.setItemInHand(InteractionHand.MAIN_HAND,old); player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            helper.assertTrue(!altar.start(level,player),"Tier-one book starts tier-two ritual");
            player.setItemInHand(InteractionHand.OFF_HAND,book); helper.assertTrue(altar.start(level,player),"Third tier offhand start refused");
            for(int i=0;i<7;i++) RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
            var restored=new RitualAltarBlockEntity(pos,altar.getBlockState()); restored.loadWithComponents(altar.saveWithoutMetadata(level.registryAccess()),level.registryAccess()); level.setBlockEntity(restored);
            helper.assertTrue(restored.remainingTicks()==13,"Third saved infusion loses progress");
            for(int i=0;i<13;i++) RitualAltarBlockEntity.serverTick(level,pos,restored.getBlockState(),restored);
            var upgraded=restored.getInput();
            helper.assertTrue(upgraded.is(DreadlandsProgressionContent.STAFF.get()) && RendingStaffItem.tier(upgraded)==2
                    && upgraded.getHoverName().getString().equals("All charged channels") && upgraded.get(DataComponents.CUSTOM_DATA).copyTag().getString("PrivateNote").equals("keep")
                    && RendingStaffItem.energy(upgraded,RendingStaffItem.Channel.DREAD)==99 && RendingStaffItem.energy(upgraded,RendingStaffItem.Channel.ABYSSAL)==17
                    && RendingStaffItem.energy(upgraded,RendingStaffItem.Channel.SHADOW)==37 && NecronomiconItem.getPotentialEnergy(book)==8000
                    && NecronomiconItem.getPotentialEnergy(old)==10000,"Saved infusion loses identity/charges or debits wrong book");
            target.setHealth(30); target.invulnerableTime=0; player.getCooldowns().removeCooldown(upgraded.getItem());
            helper.assertTrue(RendingStaffItem.drain(upgraded,player,target) && target.getHealth()==27
                    && RendingStaffItem.energy(upgraded,RendingStaffItem.Channel.DREAD)==102 && RendingStaffItem.redeem(upgraded,player)
                    && RendingStaffItem.energy(upgraded,RendingStaffItem.Channel.DREAD)==2,"Tier-two amount/overflow is wrong");
            restored.removeInput(); restored.placeInput(new ItemStack(CoraliumContent.INFUSED_STONE.get()));
            for(var offset:BasicScrollRitual.pedestalOffsets()) ((RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(offset))).placeOne(new ItemStack(DreadlandsProgressionContent.ESSENCE.get()));
            helper.assertTrue(RitualCatalog.INSTANCE.get(WastelandContent.id("dreadlands_powerstone")).orElseThrow().matches(level,pos)
                    && restored.start(level,player),"Later gated Powerstone infusion refuses actual inputs");
            for(int i=0;i<60;i++) RitualAltarBlockEntity.serverTick(level,pos,restored.getBlockState(),restored);
            helper.assertTrue(restored.getInput().is(WastelandStructuresContent.POWERSTONE_ITEM.get()) && restored.getInput().getCount()==1
                    && NecronomiconItem.getPotentialEnergy(book)==3000,"Later Powerstone wrong reward/cost");
            for(var offset:BasicScrollRitual.pedestalOffsets()) helper.assertTrue(((RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(offset))).getOffering().isEmpty(),"Paid third tier duplicates offerings");
            com.mojang.logging.LogUtils.getLogger().info("DREADLANDS PROGRESSION AUDIT: 300 successful carrier drains, native skin/book, 20,000-capacity tier, saved charged staff upgrade, overflow, gated 5,000-PE Powerstone");
        } finally {
            target.discard(); clear(level,pos); prior.forEach((p,state)->level.setBlock(p,state,3));
            player.teleportTo(helper.getLevel(),returnPos.x,returnPos.y,returnPos.z,0,0);
        }
        helper.succeed();
    }

    @GameTest(template="empty", templateNamespace="abyssalport_worldtest", batch="dreadlands_third_tier_abort")
    public static void thirdTierInfusionAbortsOnBookOrGroundDowngradeWithoutCharging(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(DreadlandsContent.DIMENSION); var pos=new BlockPos(7240,160,7208);
        var prior=prepare(level,pos); var player=helper.makeMockServerPlayerInLevel(); var returnPos=player.position();
        player.teleportTo(level,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        var book=new ItemStack(DreadlandsProgressionContent.BOOK.get()); NecronomiconItem.setPotentialEnergy(book,2000); player.setItemInHand(InteractionHand.MAIN_HAND,book);
        try {
            helper.assertTrue(RitualGroundFormation.form(level,pos,player,book)==RitualGroundFormation.Result.CREATED,"Third abort fixture cannot form");
            var altar=(RitualAltarBlockEntity)level.getBlockEntity(pos); altar.placeInput(new ItemStack(WastelandProgressionContent.WASTELAND_STAFF.get())); fillStaff(level,pos);
            helper.assertTrue(altar.start(level,player),"Third infusion cannot start");
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WastelandProgressionContent.BOOK.get()));
            RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
            helper.assertTrue(!altar.isRunning() && NecronomiconItem.getPotentialEnergy(book)==2000 && altar.getInput().is(WastelandProgressionContent.WASTELAND_STAFF.get()),"Book downgrade consumed third inputs");
            player.setItemInHand(InteractionHand.MAIN_HAND,book); helper.assertTrue(altar.start(level,player),"Third restart refused");
            var broken=pos.offset(BasicScrollRitual.pedestalOffsets().getFirst());
            var stand=(RitualPedestalBlockEntity)level.getBlockEntity(broken); stand.removeOne(); level.setBlock(broken,Blocks.AIR.defaultBlockState(),3);
            RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
            helper.assertTrue(!altar.isRunning() && NecronomiconItem.getPotentialEnergy(book)==2000 && altar.getInput().is(WastelandProgressionContent.WASTELAND_STAFF.get()),"Ground downgrade consumed third inputs");
        } finally { clear(level,pos); prior.forEach((p,state)->level.setBlock(p,state,3)); player.teleportTo(helper.getLevel(),returnPos.x,returnPos.y,returnPos.z,0,0); }
        helper.succeed();
    }

    private static dev.abyssalport.ritual.RitualDefinition staffDefinition() { return RitualCatalog.INSTANCE.get(WastelandContent.id("dreadlands_staff")).orElseThrow(); }
    private static void fillStaff(ServerLevel level,BlockPos pos) {
        var inputs=List.of(DreadlandsContent.STONE.get().asItem(),DreadlandsContent.FRAGMENT.get(),DreadlandsContent.STONE.get().asItem(),
                DreadlandsContent.FRAGMENT.get(),AbyssalPort.SHADOW_GEM.get(),DreadlandsContent.STONE.get().asItem(),DreadlandsContent.FRAGMENT.get(),DreadlandsContent.STONE.get().asItem());
        for(int i=0;i<8;i++) {
            var p=pos.offset(BasicScrollRitual.pedestalOffsets().get(i));
            if(!(level.getBlockEntity(p) instanceof RitualPedestalBlockEntity)) level.setBlock(p,DreadlandsProgressionContent.PEDESTAL.get().defaultBlockState(),3);
            ((RitualPedestalBlockEntity)level.getBlockEntity(p)).placeOne(new ItemStack(inputs.get(i)));
        }
    }
    private static Map<BlockPos,BlockState> prepare(ServerLevel level,BlockPos pos) {
        Map<BlockPos,BlockState> prior=new HashMap<>();
        for(var p:BlockPos.betweenClosed(pos.offset(-4,0,-4),pos.offset(4,3,4))) { prior.put(p.immutable(),level.getBlockState(p)); level.setBlock(p,Blocks.AIR.defaultBlockState(),3); }
        level.setBlock(pos,DreadlandsContent.COBBLESTONE.get().defaultBlockState(),3);
        for(var offset:BasicScrollRitual.pedestalOffsets()) level.setBlock(pos.offset(offset),DreadlandsContent.COBBLESTONE.get().defaultBlockState(),3);
        return prior;
    }
    private static void clear(ServerLevel level,BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) altar.removeInput();
        for(var offset:BasicScrollRitual.pedestalOffsets()) if(level.getBlockEntity(pos.offset(offset)) instanceof RitualPedestalBlockEntity stand) stand.removeOne();
    }

    @GameTest(template="empty", templateNamespace="abyssalport_worldtest", batch="rending_untracked_delivery")
    public static void admittedDropInAnUntrackedChunkDebitsExactlyOnce(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION);
        level.getChunk(1875,1875);
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"DropAudit"));
        player.moveTo(30008,140,30008);
        for(int slot=0;slot<36;slot++) player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));
        var staff=new ItemStack(WastelandProgressionContent.RENDING_STAFF.get());
        var data=new CompoundTag(); data.putInt("energyShadow",200); staff.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        var drops=new java.util.ArrayList<ItemEntity>();
        java.util.function.Consumer<EntityJoinLevelEvent> capture=event->{
            if(event.getLevel()==level && event.getEntity() instanceof ItemEntity item && item.getItem().is(AbyssalPort.SHADOW_GEM.get())) drops.add(item);
        };
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            boolean redeemed=RendingStaffItem.redeem(staff,player);
            helper.assertTrue(drops.size()==1 && drops.getFirst().isAddedToLevel() && !drops.getFirst().isRemoved(),"Native reward was not admitted");
            helper.assertTrue(level.getEntity(drops.getFirst().getUUID())==null,"Fixture unexpectedly uses an entity-tracking chunk");
            helper.assertTrue(redeemed && RendingStaffItem.energy(staff,RendingStaffItem.Channel.SHADOW)==0
                    && !RendingStaffItem.redeem(staff,player) && drops.size()==1,"Admitted but untracked reward retains energy and can duplicate");
        } finally { NeoForge.EVENT_BUS.unregister(capture); drops.forEach(ItemEntity::discard); }
        helper.succeed();
    }
}
