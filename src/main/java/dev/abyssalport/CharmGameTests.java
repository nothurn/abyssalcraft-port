package dev.abyssalport;

import com.mojang.authlib.GameProfile;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.content.*;
import dev.abyssalport.energy.CharmAmplifier;
import dev.abyssalport.item.NecronomiconItem;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CharmGameTests {
    private CharmGameTests() { }
    private static ServerPlayer player(GameTestHelper helper) {
        var player = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CharmAudit"));
        player.setGameMode(GameType.SURVIVAL); return player;
    }
    private static Item ingredient(CharmAmplifier amplifier) {
        return switch(amplifier) { case RANGE -> Items.ARROW; case POWER -> Items.GUNPOWDER; case DURATION -> Items.REDSTONE; default -> Items.AIR; };
    }
    private static boolean use(ServerPlayer player, BlockPos pos) {
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(pos.getCenter(), Direction.UP, pos, false))).consumesAction();
    }
    private static DeityStatueBlockEntity statue(GameTestHelper helper, GraveyardGameTests.Fixture fixture, StatueContent.Deity deity) {
        fixture.level.setBlock(fixture.node, StatueContent.block(deity).defaultBlockState(), 2);
        return (DeityStatueBlockEntity)fixture.level.getBlockEntity(fixture.node);
    }
    private static void advance(GraveyardGameTests.Fixture fixture, DeityStatueBlockEntity statue, int ticks) {
        var block = (dev.abyssalport.block.DeityStatueBlock)statue.getBlockState().getBlock();
        var ticker = block.getTicker(fixture.level, statue.getBlockState(), StatueContent.ENTITY.get());
        for(int tick=0; tick<ticks; tick++) ticker.tick(fixture.level, fixture.node, statue.getBlockState(), statue);
    }

    @GameTest(template="empty", batch="charm_craft_and_native_anvil")
    public static void NativeCraftingAndTakingAllThreeAnvilResultsConsumeExactlyOneIngredientAndLevel(GameTestHelper helper) {
        var player = player(helper); var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2,1,2)); var prior = level.getBlockState(pos);
        try {
            for (var amplifier : List.of(CharmAmplifier.RANGE, CharmAmplifier.POWER, CharmAmplifier.DURATION)) {
                level.setBlock(pos, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                var craft = new CraftingMenu(7, player.getInventory(), ContainerLevelAccess.create(level,pos));
                for(int slot=0; slot<9; slot++) craft.getSlot(slot+1).set(new ItemStack(slot==4 ? Items.DIAMOND : Items.GOLD_INGOT));
                helper.assertTrue(craft.getSlot(0).getItem().is(CharmContent.item(CharmAmplifier.NONE)), "Real source charm recipe is not loaded");
                craft.quickMoveStack(player, 0);
                for(int slot=1; slot<=9; slot++) helper.assertTrue(craft.getSlot(slot).getItem().isEmpty(), "Crafted charm leaves unspent gold/diamond");
                ItemStack charm = ItemStack.EMPTY; int count=0;
                for(int slot=0; slot<36; slot++) if(player.getInventory().getItem(slot).is(CharmContent.item(CharmAmplifier.NONE))) {
                    count += player.getInventory().getItem(slot).getCount(); charm = player.getInventory().removeItem(slot, 1);
                }
                helper.assertTrue(count==1 && !charm.isEmpty(), "Native crafting duplicated or lost its charm");
                charm.set(DataComponents.CUSTOM_NAME, Component.literal("Survival charm"));
                var data = new net.minecraft.nbt.CompoundTag(); data.putInt("PrivateAudit", 71); charm.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                // NULL access retains native costs/slots but cannot damage a real anvil in this headless fixture.
                var anvil = new AnvilMenu(8, player.getInventory()); player.experienceLevel=5;
                anvil.getSlot(0).set(charm); anvil.getSlot(1).set(new ItemStack(ingredient(amplifier),3));
                var result = anvil.getSlot(2).getItem().copy();
                helper.assertTrue(result.is(CharmContent.item(amplifier)) && result.getCount()==1 && anvil.getCost()==1
                    && anvil.repairItemCountCost==1 && result.getHoverName().getString().equals("Survival charm")
                    && result.get(DataComponents.CUSTOM_DATA).copyTag().getInt("PrivateAudit")==71, "Native charm forging changed its source cost or lost components");
                var output = anvil.getSlot(2);
                helper.assertTrue(output.mayPickup(player), "Survival player cannot take a funded forged charm");
                var taken = output.remove(1); output.onTake(player, taken);
                helper.assertTrue(taken.is(CharmContent.item(amplifier)) && player.experienceLevel==4 && anvil.getSlot(0).getItem().isEmpty()
                    && anvil.getSlot(1).getItem().getCount()==2 && anvil.getSlot(2).getItem().isEmpty(), "Taking forged result fails exact material/XP consumption or duplicates preview");
                helper.assertTrue(ItemStack.matches(taken, ItemStack.parse(level.registryAccess(), taken.save(level.registryAccess())).orElseThrow()), "Forged charm loses saved components");
            }
            helper.succeed();
        } finally { level.setBlock(pos, prior, 3); player.getInventory().clearContent(); }
    }

    @GameTest(template="empty", batch="charm_anvil_guards")
    public static void InvalidInputsInsufficientExperienceAndStackedLeftCannotConsumeOrDuplicateCharms(GameTestHelper helper) {
        var player = player(helper); var anvil = new AnvilMenu(9, player.getInventory());
        var blank = new ItemStack(CharmContent.item(CharmAmplifier.NONE));
        anvil.getSlot(0).set(blank); anvil.getSlot(1).set(new ItemStack(Items.ARROW)); player.experienceLevel=0;
        helper.assertTrue(anvil.getSlot(2).hasItem() && !anvil.getSlot(2).mayPickup(player) && blank.getCount()==1
            && anvil.getSlot(1).getItem().getCount()==1, "Preview bypasses native XP gate or spends inputs before taking");
        player.experienceLevel=3;
        anvil.getSlot(0).set(new ItemStack(CharmContent.item(CharmAmplifier.NONE),2));
        helper.assertTrue(!anvil.getSlot(2).hasItem() && anvil.getSlot(0).getItem().getCount()==2, "Forging destroys additional left-slot charms");
        anvil.getSlot(0).set(new ItemStack(CharmContent.item(CharmAmplifier.POWER)));
        helper.assertTrue(!anvil.getSlot(2).hasItem(), "Already forged charm accepts an invented conversion");
        anvil.getSlot(0).set(blank); anvil.getSlot(1).set(new ItemStack(Items.DIAMOND));
        helper.assertTrue(!anvil.getSlot(2).hasItem(), "Unrecognized ingredient forges a charm");
        anvil.getSlot(0).set(new ItemStack(Items.ARROW)); anvil.getSlot(1).set(blank);
        helper.assertTrue(!anvil.getSlot(2).hasItem(), "Reversed inputs invent a source forging");
        anvil.getSlot(0).set(blank); anvil.getSlot(1).set(new ItemStack(Items.REDSTONE));
        anvil.setItemName(blank.getHoverName().getString());
        helper.assertTrue(!anvil.getSlot(2).getItem().has(DataComponents.CUSTOM_NAME), "Automatic native name field hides the forged charm's real item name");
        anvil.setItemName("Named duration");
        helper.assertTrue(anvil.getSlot(2).getItem().is(CharmContent.item(CharmAmplifier.DURATION))
            && anvil.getSlot(2).getItem().getHoverName().getString().equals("Named duration") && anvil.getCost()==1, "Native rename does not accompany source one-level forging");
        helper.succeed();
    }

    @GameTest(template="empty", batch="charm_all_native_statues")
    public static void RealCharmUseChargesBothHandsOnAllSevenDeitiesAndDoesNotReplaceOrRefundConsumedModifiers(GameTestHelper helper) {
        try(var fixture = new GraveyardGameTests.Fixture(helper)) {
            var player = helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL); player.moveTo(fixture.node.getCenter().add(2,0,0));
            try {
                for(var deity : StatueContent.Deity.values()) for(var amplifier : List.of(CharmAmplifier.RANGE, CharmAmplifier.POWER, CharmAmplifier.DURATION)) {
                    var statue = statue(helper, fixture, deity); var modifier = new ItemStack(CharmContent.item(amplifier),2);
                    player.setItemInHand(InteractionHand.MAIN_HAND, modifier);
                    helper.assertTrue(use(player,fixture.node) && modifier.getCount()==1 && statue.activeAmplifier()==amplifier && statue.activeDeity()==null,
                            "Native modifier use is not admitted/consumed exactly once: "+deity+"/"+amplifier);
                    helper.assertTrue(use(player,fixture.node) && modifier.getCount()==1, "Already active statue consumes another charm");
                    var replacement = new ItemStack(CharmContent.item(amplifier==CharmAmplifier.POWER ? CharmAmplifier.RANGE : CharmAmplifier.POWER),2);
                    player.setItemInHand(InteractionHand.MAIN_HAND,replacement); use(player,fixture.node);
                    helper.assertTrue(statue.activeAmplifier()==amplifier && replacement.getCount()==2, "Active statue is silently overwritten");
                    var saved = statue.saveWithFullMetadata(fixture.level.registryAccess());
                    var restored = new DeityStatueBlockEntity(fixture.node,statue.getBlockState()); restored.loadWithComponents(saved,fixture.level.registryAccess());
                    helper.assertTrue(restored.activeAmplifier()==amplifier && restored.energyQuanta()==statue.energyQuanta()
                        && restored.chargeInterval()==statue.chargeInterval(), "World save loses active generic modifier");
                    var main=new ItemStack(AbyssalPort.NECRONOMICON.get()); var off=new ItemStack(AbyssalPort.NECRONOMICON.get());
                    player.setItemInHand(InteractionHand.MAIN_HAND,main); player.setItemInHand(InteractionHand.OFF_HAND,off);
                    int interval=amplifier==CharmAmplifier.DURATION ? 60 : 120; float quanta=amplifier==CharmAmplifier.POWER ? 15 : 10;
                    advance(fixture,statue,interval-1);
                    helper.assertTrue(NecronomiconItem.getPotentialEnergy(main)==0 && NecronomiconItem.getPotentialEnergy(off)==0, "Charm skips production interval");
                    advance(fixture,statue,1);
                    helper.assertTrue(NecronomiconItem.getPotentialEnergy(main)==quanta && NecronomiconItem.getPotentialEnergy(off)==quanta,
                        "Real modified native ticker does not charge both hands: "+deity+"/"+amplifier);
                    var blank = new ItemStack(CharmContent.item(CharmAmplifier.NONE)); player.setItemInHand(InteractionHand.MAIN_HAND,blank);
                    helper.assertTrue(use(player,fixture.node) && blank.getCount()==1 && statue.activeAmplifier()==CharmAmplifier.NONE
                        && statue.activeDeity()==null && statue.energyQuanta()==5 && statue.chargeInterval()==120, "Blank charm fails reusable reset");
                    use(player,fixture.node); helper.assertTrue(blank.getCount()==1, "Inactive reset consumes blank charm");
                    fixture.level.setBlock(fixture.node,Blocks.AIR.defaultBlockState(),2);
                }
                helper.succeed();
            } finally { player.getInventory().clearContent(); fixture.level.getServer().getPlayerList().remove(player); }
        }
    }

    @GameTest(template="empty", batch="charm_range_and_isolation")
    public static void RangeModifierExtendsHeldAndIsolatedCollectorChargingButNotRoofOrNeighborRules(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var player=helper.makeMockServerPlayerInLevel(); player.moveTo(fixture.node.getCenter().add(10,0,0));
            try {
                var statue=statue(helper,fixture,StatueContent.Deity.CTHULHU); var book=new ItemStack(AbyssalPort.NECRONOMICON.get());
                player.setItemInHand(InteractionHand.MAIN_HAND,book); advance(fixture,statue,120);
                helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==0, "Unmodified statue charges outside source radius seven");
                statue.activateCharm(CharmAmplifier.RANGE,null);
                var collectorPos=fixture.node.east(5); fixture.level.setBlock(collectorPos,AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);
                var collector=(EnergyCollectorBlockEntity)fixture.level.getBlockEntity(collectorPos);
                advance(fixture,statue,120);
                statue.tickEnergy(fixture.level, new StatueEnergyGameTests.FixedRandom(0));
                helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==10 && collector.energy()>=10, "Range charm fails radius eleven or independent expanded collector ring five");
                float stored=collector.energy();
                fixture.level.setBlock(collectorPos.above(),AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);
                advance(fixture,statue,120); helper.assertTrue(collector.energy()==stored, "Charm ignores collector isolation");
                fixture.level.setBlock(collectorPos.above(),Blocks.AIR.defaultBlockState(),2);
                float before=NecronomiconItem.getPotentialEnergy(book);
                for(var adjacent:List.of(fixture.node.west(),fixture.node.above(2),fixture.node.below(2))) {
                    var prior=fixture.level.getBlockState(adjacent);
                    fixture.level.setBlock(adjacent,StatueContent.block(StatueContent.Deity.HASTUR).defaultBlockState(),2);
                    advance(fixture,statue,120); helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==before && collector.energy()==stored, "Adjacent manipulator rule is bypassed: "+adjacent);
                    fixture.level.setBlock(adjacent,prior,2);
                }
                var tag=statue.saveWithFullMetadata(fixture.level.registryAccess()); tag.putString("ActiveAmplifier","corrupted"); tag.putString("ActiveDeity","cthulhu");
                statue.loadWithComponents(tag,fixture.level.registryAccess());
                helper.assertTrue(statue.activeAmplifier()==CharmAmplifier.NONE && statue.activeDeity()==null, "Malformed saved modifier grants an active deity");
                helper.succeed();
            } finally { player.getInventory().clearContent(); fixture.level.getServer().getPlayerList().remove(player); }
        }
    }

    @GameTest(template="empty",batch="charm_saved_deity_affinity")
    public static void SavedDeityAffinitySelectsExactMatchingAndForeignFactorsWithoutAnEarlyInfusionRecipe(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            for(var deity:StatueContent.Deity.values()) {
                var statue=statue(helper,fixture,deity);
                for(var amplifier:List.of(CharmAmplifier.RANGE,CharmAmplifier.POWER,CharmAmplifier.DURATION)) {
                    helper.assertTrue(statue.activateCharm(amplifier,deity),"Cannot admit future source-matching affinity");
                    helper.assertTrue(statue.amplifierFactor(amplifier)==amplifier.factor(true)
                        &&statue.energyQuanta()==(amplifier==CharmAmplifier.POWER?25:10)
                        &&statue.chargeInterval()==(amplifier==CharmAmplifier.DURATION?30:120)
                        &&statue.chargingRange()==(amplifier==CharmAmplifier.RANGE?13:7),"Matching deity factors are disconnected: "+deity+"/"+amplifier);
                    var saved=statue.saveWithFullMetadata(fixture.level.registryAccess());
                    var restored=new DeityStatueBlockEntity(fixture.node,statue.getBlockState());restored.loadWithComponents(saved,fixture.level.registryAccess());
                    helper.assertTrue(restored.activeDeity()==deity&&restored.activeAmplifier()==amplifier
                        &&restored.amplifierFactor(amplifier)==amplifier.factor(true),"Saved deity affinity changes its native factors");
                    statue.clearCharm();var foreign=StatueContent.Deity.values()[(deity.ordinal()+1)%7];statue.activateCharm(amplifier,foreign);
                    helper.assertTrue(statue.amplifierFactor(amplifier)==amplifier.factor(false),"Foreign affinity incorrectly grants same-deity bonus");
                    saved=statue.saveWithFullMetadata(fixture.level.registryAccess());saved.putString("ActiveDeity","invalid-future-value");
                    restored.loadWithComponents(saved,fixture.level.registryAccess());
                    helper.assertTrue(restored.activeDeity()==null&&restored.amplifierFactor(amplifier)==amplifier.factor(false),"Unknown saved affinity grants a matching bonus or crashes loading");
                    statue.clearCharm();
                }
                fixture.level.setBlock(fixture.node,Blocks.AIR.defaultBlockState(),2);
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty", batch="charm_actual_roof_lighting", timeoutTicks=240)
    public static void ActualRoofLightingStopsChargingAndAdventureCannotConsumeACharm(GameTestHelper helper) {
        var fixture=new GraveyardGameTests.Fixture(helper); var player=helper.makeMockServerPlayerInLevel();
        Runnable cleanup=()->{ player.getInventory().clearContent(); fixture.level.getServer().getPlayerList().remove(player); fixture.close(); };
        try {
            player.moveTo(fixture.node.getCenter().add(2,0,0)); var statue=statue(helper,fixture,StatueContent.Deity.CTHULHU);
            var modifier=new ItemStack(CharmContent.item(CharmAmplifier.POWER),2); player.setItemInHand(InteractionHand.MAIN_HAND,modifier);
            player.setGameMode(GameType.ADVENTURE);
            helper.assertTrue(!use(player,fixture.node) && modifier.getCount()==2 && statue.activeAmplifier()==CharmAmplifier.NONE,
                    "Adventure permissions are bypassed by modifier use");
            player.setGameMode(GameType.SURVIVAL);
            helper.assertTrue(use(player,fixture.node) && modifier.getCount()==1,"Survival modifier use is rejected");
            var book=new ItemStack(AbyssalPort.NECRONOMICON.get()); player.setItemInHand(InteractionHand.MAIN_HAND,book);
            fixture.level.setBlock(fixture.node.above(),Blocks.STONE.defaultBlockState(),3);
            helper.startSequence()
                .thenWaitUntil(()->helper.assertTrue(!fixture.level.canSeeSky(fixture.node),"Native lighting has not propagated below the roof"))
                .thenExecute(()->{
                    try {
                        float before=NecronomiconItem.getPotentialEnergy(book);
                        var ticks=statue.saveWithFullMetadata(fixture.level.registryAccess()).getInt("ActiveTicks");
                        advance(fixture,statue,120);
                        helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==before
                            &&statue.saveWithFullMetadata(fixture.level.registryAccess()).getInt("ActiveTicks")==ticks,"Opaque roof does not pause the actual native statue ticker");
                        fixture.level.setBlock(fixture.node.above(),Blocks.AIR.defaultBlockState(),3);
                    } catch(RuntimeException|AssertionError error) { cleanup.run(); throw error; }
                })
                .thenWaitUntil(()->helper.assertTrue(fixture.level.canSeeSky(fixture.node),"Native lighting has not propagated after removing the roof"))
                .thenExecute(()->{
                    try {
                        float before=NecronomiconItem.getPotentialEnergy(book); advance(fixture,statue,120);
                        helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)>=before+15,"Roof removal does not resume modified charging");
                        var loot=Block.getDrops(statue.getBlockState(),fixture.level,fixture.node,statue,player,new ItemStack(Items.IRON_PICKAXE));
                        helper.assertTrue(loot.size()==1 && loot.getFirst().is(StatueContent.block(StatueContent.Deity.CTHULHU).asItem())
                            &&!loot.getFirst().has(DataComponents.CUSTOM_DATA),"Mining invents a refund or item-persisted consumed modifier");
                    } finally { cleanup.run(); }
                }).thenSucceed();
        } catch(RuntimeException|AssertionError error) { cleanup.run(); throw error; }
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="charms_actual_native_ticks",timeoutTicks=400)
    public static void ThreeGenericCharmsModifyRegisteredStatuesThroughNormalWorldServerTicks(GameTestHelper helper) {
        var level=helper.getLevel(); var saved=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var forced=new ArrayList<net.minecraft.world.level.ChunkPos>(); var players=new ArrayList<ServerPlayer>();
        var statues=new ArrayList<DeityStatueBlockEntity>(); var books=new ArrayList<ItemStack>();
        var amplifiers=List.of(CharmAmplifier.RANGE,CharmAmplifier.POWER,CharmAmplifier.DURATION);
        Runnable cleanup=()->{
            players.forEach(player->{player.getInventory().clearContent();level.getServer().getPlayerList().remove(player);});
            saved.forEach((pos,state)->level.setBlock(pos,state,3)); forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false));
        };
        try {
            for(int index=0;index<3;index++) {
                var pos=new BlockPos(560008+index*16,300,560008); var chunk=new net.minecraft.world.level.ChunkPos(pos);
                level.getChunk(chunk.x,chunk.z);
                if(!level.getForcedChunks().contains(chunk.toLong())) { level.setChunkForced(chunk.x,chunk.z,true);forced.add(chunk); }
                for(var cell:BlockPos.betweenClosed(pos.below(),pos.above())) {
                    helper.assertTrue(level.getBlockEntity(cell)==null,"Native charm audit overlaps an existing BE");saved.put(cell.immutable(),level.getBlockState(cell));
                }
                level.setBlock(pos.below(),ShoggothLairContent.MONOLITH_STONE.get().defaultBlockState(),3);
                level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),3);
                level.setBlock(pos,StatueContent.block(StatueContent.Deity.values()[index]).defaultBlockState(),3);
                var statue=(DeityStatueBlockEntity)level.getBlockEntity(pos); statues.add(statue);
                var player=helper.makeMockServerPlayerInLevel(); players.add(player); player.setGameMode(GameType.SURVIVAL);
                player.moveTo(pos.getCenter().add(2,0,0));player.setNoGravity(true);
                var charm=new ItemStack(CharmContent.item(amplifiers.get(index)),2);player.setItemInHand(InteractionHand.MAIN_HAND,charm);
                helper.assertTrue(use(player,pos)&&charm.getCount()==1,"Actual normal-world native charm use failed");
                player.setGameMode(GameType.CREATIVE);
                for(var hand:InteractionHand.values()) {
                    var book=new ItemStack(AbyssalPort.NECRONOMICON.get());player.setItemInHand(hand,book);books.add(book);
                }
            }
            helper.runAfterDelay(180,()->{
                try {
                    for(int index=0;index<3;index++) {
                        var statue=statues.get(index);float quanta=index==1?15:10;
                        float main=NecronomiconItem.getPotentialEnergy(books.get(index*2));
                        float off=NecronomiconItem.getPotentialEnergy(books.get(index*2+1));
                        helper.assertTrue(statue.activeAmplifier()==amplifiers.get(index)&&main==off
                            &&main>=quanta*(index==2?2:1)&&main<=quanta*(index==2?3:1)&&main%quanta==0,
                            "Actual native charm ticker differs: "+amplifiers.get(index)+", main="+main+", off="+off);
                    }
                    com.mojang.logging.LogUtils.getLogger().info("NATIVE CHARM WORLD AUDIT: Range/Power/Duration hands={} after 180 actual server ticks",
                        books.stream().map(NecronomiconItem::getPotentialEnergy).toList());
                    helper.succeed();
                } finally { cleanup.run(); }
            });
        } catch(RuntimeException|AssertionError error) {cleanup.run();throw error;}
    }
}
