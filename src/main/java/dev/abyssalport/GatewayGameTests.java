package dev.abyssalport;

import dev.abyssalport.block.GatewayAnchorBlock;
import dev.abyssalport.block.entity.GatewayAnchorBlockEntity;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.content.CoraliumContent;
import dev.abyssalport.content.GatewayContent;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.item.GatewayKeyItem;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.RitualCatalog;
import dev.abyssalport.world.GatewayTravel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GatewayGameTests {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final BlockPos ALTAR = new BlockPos(4, 1, 4);
    private GatewayGameTests() { }

    @GameTest(template = "empty", batch = "gateway_recipe")
    public static void realRecipeAndDestinationSelectionArePerStack(GameTestHelper helper) {
        var slots = new ArrayList<ItemStack>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        slots.set(1, new ItemStack(CoraliumContent.PEARL.get())); slots.set(2, new ItemStack(CoraliumContent.OBLIVION_CATALYST.get()));
        slots.set(4, new ItemStack(Items.BLAZE_ROD)); slots.set(5, new ItemStack(CoraliumContent.PEARL.get())); slots.set(6, new ItemStack(Items.BLAZE_ROD));
        var input = CraftingInput.of(3, 3, slots);
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
        var key = recipe.value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(key.is(GatewayContent.KEY.get()) && key.getMaxStackSize() == 1, "Original key recipe or stack size is wrong");
        var other = key.copy();
        var player = helper.makeMockServerPlayerInLevel(); player.setItemInHand(InteractionHand.MAIN_HAND, key);
        key.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(GatewayKeyItem.destination(key).equals(WastelandContent.DIMENSION) && GatewayKeyItem.destination(other).equals(Level.OVERWORLD), "Key selection leaked between stacks");
        var saved = ItemStack.parse(helper.getLevel().registryAccess(), key.save(helper.getLevel().registryAccess())).orElseThrow();
        helper.assertTrue(GatewayKeyItem.destination(saved).equals(WastelandContent.DIMENSION), "Selected destination was not saved");
        key.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(GatewayKeyItem.destination(key).equals(Level.OVERWORLD), "Tier-zero destination cycling failed");
        helper.assertTrue(!GatewayKeyItem.connected(Level.NETHER, WastelandContent.DIMENSION), "Tier-zero key can bypass dimension connections");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "gateway_ritual")
    public static void chargedCardinalRitualFormsAnchorPillarsAndReturnsTheSameKey(GameTestHelper helper) {
        var f = fixture(helper);
        try {
            NecronomiconItem.setPotentialEnergy(f.book, 999);
            helper.assertTrue(!f.altar.start(helper.getLevel(), f.player), "Undercharged portal started");
            NecronomiconItem.setPotentialEnergy(f.book, 1000);
            helper.assertTrue(f.altar.start(helper.getLevel(), f.player), "Gateway ritual did not start");
            for (int i = 0; i < 40; i++) RitualAltarBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(ALTAR), helper.getBlockState(ALTAR), f.altar);
            helper.assertTrue(helper.getBlockEntity(ALTAR) instanceof GatewayAnchorBlockEntity anchor && anchor.intact()
                    && anchor.destination().equals(WastelandContent.DIMENSION), "Completed ritual has no active destination anchor");
            helper.assertTrue(NecronomiconItem.getPotentialEnergy(f.book) == 0 && f.player.getInventory().contains(f.key), "Portal consumed its key or wrong PE amount");
            long returned = f.player.getInventory().items.stream().filter(stack -> stack.is(GatewayContent.KEY.get()) && GatewayKeyItem.destination(stack).equals(WastelandContent.DIMENSION)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(returned == 1, "Portal returned duplicate or unconfigured keys");
            for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) helper.assertTrue(helper.getBlockState(ALTAR.offset(offset)).is(GatewayContent.MONOLITH_PILLAR.get()), "Pedestal did not become a monolith pillar");
        } finally { clean(helper); }
        LOGGER.info("Gateway world test PASS: charged cardinal ritual, returned key, pillars, exact energy");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "gateway_abort")
    public static void incorrectLayoutSameDimensionUnknownDestinationAndObstructionStayUnchanged(GameTestHelper helper) {
        var f = fixture(helper);
        try {
            var cardinal = (RitualPedestalBlockEntity) helper.getBlockEntity(ALTAR.offset(BasicScrollRitual.pedestalOffsets().getFirst()));
            var diagonal = (RitualPedestalBlockEntity) helper.getBlockEntity(ALTAR.offset(BasicScrollRitual.pedestalOffsets().get(4)));
            diagonal.placeOne(cardinal.removeOne());
            helper.assertTrue(!f.altar.start(helper.getLevel(), f.player), "Unordered substitute accepted for ordered portal ritual");
            cardinal.placeOne(diagonal.removeOne());
            GatewayKeyItem.setDestination(f.altar.getInput(), Level.OVERWORLD);
            helper.assertTrue(!f.altar.start(helper.getLevel(), f.player), "Portal to current dimension started");
            GatewayKeyItem.setDestination(f.altar.getInput(), Level.NETHER);
            helper.assertTrue(!f.altar.start(helper.getLevel(), f.player), "Unknown/unsupported destination started");
            GatewayKeyItem.setDestination(f.altar.getInput(), WastelandContent.DIMENSION);
            helper.assertTrue(f.altar.start(helper.getLevel(), f.player), "Valid portal cannot restart");
            helper.setBlock(ALTAR.above(), Blocks.STONE);
            RitualAltarBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(ALTAR), helper.getBlockState(ALTAR), f.altar);
            helper.assertTrue(!f.altar.isRunning() && NecronomiconItem.getPotentialEnergy(f.book) == 1000
                    && f.altar.getInput().is(GatewayContent.KEY.get()) && helper.getBlockState(ALTAR.above()).is(Blocks.STONE), "Obstructed portal spent resources or destroyed obstruction");
            helper.setBlock(ALTAR.above(), Blocks.AIR);
        } finally { clean(helper); }
        LOGGER.info("Gateway world test PASS: incorrect layout, unsupported destination, obstruction preserve resources");
        helper.succeed();
    }

    private record Fixture(RitualAltarBlockEntity altar, ServerPlayer player, ItemStack book, ItemStack key) { }
    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "gateway_wide_entity")
    public static void wideEntitiesHaveFullLandingSupportAndAReusedUnsafeAnchorIsRejected(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(ALTAR);
        var destination = level.getServer().getLevel(WastelandContent.DIMENSION);
        var horse = EntityType.HORSE.create(level);
        for (BlockPos air : BlockPos.betweenClosed(pos.above(), pos.above(4))) level.setBlock(air, Blocks.AIR.defaultBlockState(), 2);
        var anchor = GatewayTravel.createAnchor(level, pos, WastelandContent.DIMENSION, Direction.Axis.X);
        BlockPos partnerPos = null;
        try {
            var travel = GatewayTravel.transition(level, horse, pos.above());
            helper.assertTrue(travel != null, "Wide horse has no valid landing");
            partnerPos = anchor.partner();
            helper.assertTrue(GatewayTravel.safeArrival(destination, partnerPos, horse), "Horse landing has partial unsupported feet");
            var outerFloor = partnerPos.offset(2, 0, 0); var original = destination.getBlockState(outerFloor);
            destination.setBlock(outerFloor, Blocks.AIR.defaultBlockState(), 2);
            helper.assertTrue(!GatewayTravel.safeArrival(destination, partnerPos, horse), "Unsafe outer floor was ignored for a broad entity");
            destination.setBlock(outerFloor, original, 2);
        } finally {
            level.removeBlock(pos, false);
            if (partnerPos != null) destination.removeBlock(partnerPos, false);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "gateway_player_travel", timeoutTicks = 300)
    public static void nativePlayerTravelPreservesBookInventoryResearchAndRespawn(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(ALTAR);
        var destination = level.getServer().getLevel(WastelandContent.DIMENSION);
        helper.assertTrue(destination != null, "Wasteland missing from player audit");
        for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) level.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
        for (BlockPos air : BlockPos.betweenClosed(pos.offset(-1, 1, -1), pos.offset(1, 4, 1))) level.setBlock(air, Blocks.AIR.defaultBlockState(), 2);
        var anchor = GatewayTravel.createAnchor(level, pos, WastelandContent.DIMENSION, Direction.Axis.X);
        var player = helper.makeMockServerPlayerInLevel();
        var book = new ItemStack(AbyssalPort.NECRONOMICON.get()); NecronomiconItem.setPotentialEnergy(book, 1234);
        player.setItemInHand(InteractionHand.MAIN_HAND, book); player.getInventory().setItem(7, new ItemStack(Items.DIAMOND, 3));
        var packedBag=new ItemStack(dev.abyssalport.content.CrystalBagContent.LARGE.get());
        packedBag.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(
                new ItemStack(dev.abyssalport.content.CrystallizationContent.material("carbon").crystal().get(),64),
                new ItemStack(dev.abyssalport.content.CrystallizationContent.material("sulfur").clusterItem().get(),3))));
        player.getInventory().setItem(8,packedBag);
        var expectedBag=packedBag.copy();
        player.setData(AbyssalPort.EARLY_RESEARCH.get(), dev.abyssalport.research.EarlyResearch.MONOLITH);
        var respawn = pos.offset(10, 0, 0); player.setRespawnPosition(Level.OVERWORLD, respawn, 0, true, false);
        player.moveTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5);
        try {
            level.getBlockState(pos.above()).entityInside(level, pos.above(), player); player.baseTick();
            helper.assertTrue(player.level() == destination && player.isOnPortalCooldown(), "Native player collision did not change worlds");
            helper.assertTrue(ItemStack.matches(player.getInventory().getItem(8),expectedBag),"Gateway lost the filled bag on entry");
            helper.assertTrue(player.getInventory().getItem(7).is(Items.DIAMOND) && player.getInventory().getItem(7).getCount() == 3
                    && NecronomiconItem.getPotentialEnergy(player.getMainHandItem()) == 1234, "Travel changed player inventory or stored book PE");
            helper.assertTrue(player.getRespawnDimension().equals(Level.OVERWORLD) && respawn.equals(player.getRespawnPosition()), "Travel replaced the Overworld respawn point");
            helper.assertTrue(dev.abyssalport.research.EarlyResearch.has(player, dev.abyssalport.research.EarlyResearch.MONOLITH), "Travel reset discoveries");
            helper.assertTrue(dev.abyssalport.research.GatewayResearch.discoverDimension(player)
                    && !dev.abyssalport.research.GatewayResearch.discoverDimension(player), "Dimension discovery missing or repeated");
            var partner = (GatewayAnchorBlockEntity) destination.getBlockEntity(anchor.partner());
            player.setPortalCooldown(0); var back = partner.getBlockPos().above();
            player.moveTo(back.getX()+0.5, back.getY(), back.getZ()+0.5);
            destination.getBlockState(back).entityInside(destination, back, player); player.baseTick();
            helper.assertTrue(player.level() == level && player.position().distanceToSqr(GatewayTravel.arrival(pos)) < 0.01, "Player did not return to the source anchor");
            var restoredBag=ItemStack.parse(level.registryAccess(),player.getInventory().getItem(8).save(level.registryAccess())).orElseThrow();
            helper.assertTrue(ItemStack.matches(restoredBag,expectedBag) && dev.abyssalport.item.CrystalBagItem.hasValidContents(restoredBag),"Gateway roundtrip/native item serialization lost crystal-bag contents");
        } finally {
            if (anchor.partner() != null) destination.removeBlock(anchor.partner(), false);
            level.removeBlock(pos, false);
        }
        LOGGER.info("Gateway world test PASS: native player roundtrip, inventory including filled crystal bag/native serialized contents, book PE, discoveries and respawn preserved");
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "gateway_cancelled_travel")
    public static void cancelledNativeTravelHookKeepsTheEntityInItsSourceWorld(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(ALTAR);
        var destination = level.getServer().getLevel(WastelandContent.DIMENSION);
        helper.assertTrue(destination != null, "Wasteland missing from world audit");
        var anchor = GatewayTravel.createAnchor(level, pos, WastelandContent.DIMENSION, Direction.Axis.X);
        var cow = EntityType.COW.create(level); cow.setNoAi(true); cow.setNoGravity(true);
        cow.moveTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5); level.addFreshEntity(cow);
        boolean[] called = {false};
        Consumer<EntityTravelToDimensionEvent> veto = event -> {
            if (event.getEntity() == cow && event.getDimension().equals(WastelandContent.DIMENSION)) { called[0] = true; event.setCanceled(true); }
        };
        NeoForge.EVENT_BUS.addListener(veto);
        try {
            level.getBlockState(pos.above()).entityInside(level, pos.above(), cow); cow.baseTick();
            helper.assertTrue(called[0] && !cow.isRemoved() && cow.level() == level, "Native travel veto was ignored");
            helper.assertTrue(destination.getEntity(cow.getUUID()) == null, "Cancelled travel created a destination entity");
        } finally {
            NeoForge.EVENT_BUS.unregister(veto); cow.discard();
            if (anchor.partner() != null) destination.removeBlock(anchor.partner(), false);
            level.removeBlock(pos, false);
        }
        LOGGER.info("Gateway world test PASS: native dimension-travel hook can cancel transfer");
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "gateway_native_travel", timeoutTicks = 300)
    public static void nativePortalProcessorTravelsAndSavedPairReturnsToItsSource(GameTestHelper helper) {
        var level = helper.getLevel();
        var wasteland = level.getServer().getLevel(WastelandContent.DIMENSION);
        helper.assertTrue(wasteland != null, "Wasteland level was not loaded");
        BlockPos pos = helper.absolutePos(ALTAR);
        for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) level.setBlock(floor, Blocks.STONE.defaultBlockState(), 2);
        for (BlockPos air : BlockPos.betweenClosed(pos.offset(-1, 1, -1), pos.offset(1, 4, 1))) level.setBlock(air, Blocks.AIR.defaultBlockState(), 2);
        var sourceAnchor = GatewayTravel.createAnchor(level, pos, WastelandContent.DIMENSION, Direction.Axis.X);
        Cow cow = EntityType.COW.create(level); cow.setNoAi(true); cow.setHealth(7); cow.setNoGravity(true);
        cow.moveTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5); level.addFreshEntity(cow);
        UUID id = cow.getUUID();
        Runnable cleanTravel = () -> {
            if (level.getEntity(id) != null) level.getEntity(id).discard();
            if (wasteland.getEntity(id) != null) wasteland.getEntity(id).discard();
            if (sourceAnchor.partner() != null) wasteland.removeBlock(sourceAnchor.partner(), false);
            level.removeBlock(pos, false);
        };
        try {
            helper.assertTrue(sourceAnchor.intact(), "Source gateway was invalid before collision");
            helper.assertTrue(sourceAnchor.partner() == null, "New source unexpectedly has a partner before native collision");
            level.getBlockState(pos.above()).entityInside(level, pos.above(), cow);
            helper.assertTrue(cow.portalProcess != null, "Portal collision failed to register the native processor");
            cow.baseTick(); // Runs native PortalProcessor -> changeDimension, including travel hooks.
            helper.assertTrue(cow.isRemoved() && cow.getRemovalReason() == net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION,
                    "Native processor did not perform transfer, cooldown=" + cow.isOnPortalCooldown());
        } catch (RuntimeException failure) { cleanTravel.run(); throw failure; }
        // Destination entity visibility follows the native portal ticket on subsequent server ticks.
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(wasteland.getEntity(id) instanceof Cow,
                "Destination entity is not visible after its portal ticket")).thenExecute(() -> {
        try {
            helper.assertTrue(wasteland.getEntity(id) instanceof Cow, "Native portal collision did not travel to Wasteland");
            Cow arrived = (Cow) wasteland.getEntity(id);
            helper.assertTrue(arrived.getHealth() == 7 && arrived.isNoAi() && arrived.isOnPortalCooldown(),
                    "Native transfer lost state: health=" + arrived.getHealth() + ", noAi=" + arrived.isNoAi() + ", cooldown=" + arrived.isOnPortalCooldown());
            var partner = (GatewayAnchorBlockEntity) wasteland.getBlockEntity(sourceAnchor.partner());
            helper.assertTrue(partner != null && partner.intact() && partner.destination().equals(level.dimension()), "No reciprocal return gateway");
            helper.assertTrue(GatewayTravel.safeArrival(wasteland, partner.getBlockPos(), arrived), "Arrival is obstructed, liquid or unsupported");
            var restoredSource = new GatewayAnchorBlockEntity(pos, level.getBlockState(pos));
            restoredSource.loadWithComponents(sourceAnchor.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
            level.setBlockEntity(restoredSource);
            var restoredPartner = new GatewayAnchorBlockEntity(partner.getBlockPos(), wasteland.getBlockState(partner.getBlockPos()));
            restoredPartner.loadWithComponents(partner.saveWithoutMetadata(wasteland.registryAccess()), wasteland.registryAccess());
            wasteland.setBlockEntity(restoredPartner); partner = restoredPartner;
            helper.assertTrue(restoredSource.portalId().equals(sourceAnchor.portalId()) && partner.partnerId().equals(sourceAnchor.portalId()), "Reload lost portal identity");
            arrived.setPortalCooldown(0);
            BlockPos back = partner.getBlockPos().above(); arrived.moveTo(back.getX()+0.5, back.getY(), back.getZ()+0.5);
            wasteland.getBlockState(back).entityInside(wasteland, back, arrived); arrived.baseTick();
            helper.assertTrue(restoredSource.partnerId().equals(partner.portalId()), "Return trip unnecessarily recreated its gateway");
        } catch (RuntimeException failure) { cleanTravel.run(); throw failure; }
        }).thenWaitUntil(() -> helper.assertTrue(level.getEntity(id) instanceof Cow
                && level.getEntity(id).position().distanceToSqr(GatewayTravel.arrival(pos)) < 0.01,
                "Saved reciprocal portal did not return to the exact source"))
                .thenExecute(() -> { cleanTravel.run(); LOGGER.info("Gateway world test PASS: native collision, entity state, cooldown, saved reciprocal anchors and exact return"); })
                .thenSucceed();
    }
    private static Fixture fixture(GameTestHelper helper) {
        helper.setBlock(ALTAR, AbyssalPort.RITUAL_ALTAR.get());
        helper.setBlock(ALTAR.above(), Blocks.AIR); helper.setBlock(ALTAR.above(2), Blocks.AIR);
        var altar = (RitualAltarBlockEntity) helper.getBlockEntity(ALTAR); altar.removeInput();
        ItemStack key = new ItemStack(GatewayContent.KEY.get()); GatewayKeyItem.setDestination(key, WastelandContent.DIMENSION);
        altar.placeInput(key.copy());
        for (int i = 0; i < 8; i++) {
            var pos = ALTAR.offset(BasicScrollRitual.pedestalOffsets().get(i)); helper.setBlock(pos, AbyssalPort.RITUAL_PEDESTAL.get());
            var stand = (RitualPedestalBlockEntity) helper.getBlockEntity(pos); stand.removeOne();
            if (i < 4) stand.placeOne(new ItemStack(AbyssalPort.SHADOW_GEM.get()));
        }
        var player = helper.makeMockServerPlayerInLevel(); var pos = helper.absolutePos(ALTAR);
        player.teleportTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5);
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get()); NecronomiconItem.setPotentialEnergy(book, 1000);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        helper.assertTrue(RitualCatalog.INSTANCE.get(WastelandContent.id("gateway")).orElseThrow().matches(helper.getLevel(), pos), "Gateway recipe fixture does not match");
        return new Fixture(altar, player, book, key);
    }
    private static void clean(GameTestHelper helper) {
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            var pos = ALTAR.offset(offset); if (helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof RitualPedestalBlockEntity stand) stand.removeOne(); helper.setBlock(pos, Blocks.AIR);
        }
        if (helper.getLevel().getBlockEntity(helper.absolutePos(ALTAR)) instanceof RitualAltarBlockEntity altar) altar.removeInput();
        helper.setBlock(ALTAR, Blocks.AIR);
    }
}
