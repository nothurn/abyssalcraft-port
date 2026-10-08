package dev.abyssalport;

import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandMobsContent;
import dev.abyssalport.content.WastelandProgressionContent;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.item.RendingStaffItem;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.RitualCatalog;
import dev.abyssalport.ritual.RitualGroundFormation;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastelandRitualGameTests {
    private WastelandRitualGameTests() { }

    @GameTest(template = "empty", batch = "wasteland_ritual_gates")
    public static void higherGroundAndRecipeRejectTheWrongDimensionWithoutChangingItems(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(4, 1, 4));
        var player = helper.makeMockServerPlayerInLevel(); player.moveTo(pos.getX()+.5, pos.getY()+1, pos.getZ()+.5);
        var base = new ItemStack(AbyssalPort.NECRONOMICON.get()); var book = new ItemStack(WastelandProgressionContent.BOOK.get());
        level.setBlock(pos, WastelandContent.COBBLESTONE.get().defaultBlockState(), 3);
        helper.assertTrue(RitualGroundFormation.form(level, pos, player, base) == RitualGroundFormation.Result.BOOK_TOO_WEAK,
                "Base book formed a tier-one ritual ground");
        helper.assertTrue(RitualGroundFormation.form(level, pos, player, book) == RitualGroundFormation.Result.WRONG_DIMENSION
                && level.getBlockState(pos).is(WastelandContent.COBBLESTONE.get()), "Wrong-world formation changed a block");
        level.setBlock(pos, WastelandProgressionContent.ALTAR.get().defaultBlockState(), 3);
        var altar = (RitualAltarBlockEntity) level.getBlockEntity(pos); altar.placeInput(new ItemStack(WastelandProgressionContent.RENDING_STAFF.get()));
        fillPedestals(level, pos);
        NecronomiconItem.setPotentialEnergy(book, 2000); player.setItemInHand(InteractionHand.MAIN_HAND, book);
        helper.assertTrue(!definition().matches(level, pos) && !altar.start(level, player)
                && NecronomiconItem.getPotentialEnergy(book) == 2000 && altar.getInput().is(WastelandProgressionContent.RENDING_STAFF.get()),
                "Tier-one infusion ignored its dimension or consumed rejected input");
        clearInputs(level, pos); helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "wasteland_tier_ritual")
    public static void actualWastelandFormationSavedInfusionAndStaffPreserveProgress(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION);
        var pos = new BlockPos(5128, 144, 5128); var blocks = prepareGround(level, pos);
        var player = helper.makeMockServerPlayerInLevel(); var returnPos = player.position();
        player.teleportTo(level, pos.getX()+.5, pos.getY()+1, pos.getZ()+.5, 0, 0);
        var book = new ItemStack(WastelandProgressionContent.BOOK.get()); NecronomiconItem.setPotentialEnergy(book, 7500);
        var base = new ItemStack(AbyssalPort.NECRONOMICON.get()); NecronomiconItem.setPotentialEnergy(base, 5000);
        player.setItemInHand(InteractionHand.MAIN_HAND, base); player.setItemInHand(InteractionHand.OFF_HAND, book);
        try {
            var corner = pos.offset(BasicScrollRitual.pedestalOffsets().get(7));
            level.setBlock(corner, Blocks.COBBLESTONE.defaultBlockState(), 3);
            helper.assertTrue(RitualGroundFormation.form(level, pos, player, book) == RitualGroundFormation.Result.INCOMPLETE
                    && level.getBlockState(pos).is(WastelandContent.COBBLESTONE.get()), "Mixed-tier ground was partly formed");
            level.setBlock(corner, WastelandContent.COBBLESTONE.get().defaultBlockState(), 3);
            helper.assertTrue(RitualGroundFormation.form(level, pos, player, book) == RitualGroundFormation.Result.CREATED,
                    "Native Wasteland ground did not form");
            var altar = (RitualAltarBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(altar != null && AbyssalPort.RITUAL_ALTAR_ENTITY.get().isValid(altar.getBlockState()), "New altar has an invalid BE type");
            var staff = new ItemStack(WastelandProgressionContent.RENDING_STAFF.get());
            staff.set(DataComponents.CUSTOM_NAME, Component.literal("My charged staff"));
            var data = new CompoundTag(); data.putInt("energyAbyssal", 99); data.putInt("energyShadow", 37); data.putString("PrivateNote", "keep");
            staff.set(DataComponents.CUSTOM_DATA, CustomData.of(data)); altar.placeInput(staff.copy()); fillPedestals(level, pos);
            helper.assertTrue(definition().matches(level, pos), "Actual loaded upgrade recipe does not match shuffled offerings");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            helper.assertTrue(!altar.start(level, player), "Base book can start the higher-tier ritual");
            player.setItemInHand(InteractionHand.OFF_HAND, book);
            helper.assertTrue(altar.start(level, player), "Eligible offhand book was hidden behind a weaker main-hand book");
            for (int tick = 0; tick < 7; tick++) RitualAltarBlockEntity.serverTick(level, pos, altar.getBlockState(), altar);
            var restored = new RitualAltarBlockEntity(pos, altar.getBlockState());
            restored.loadWithComponents(altar.saveWithoutMetadata(level.registryAccess()), level.registryAccess()); level.setBlockEntity(restored);
            helper.assertTrue(restored.remainingTicks() == 13 && restored.getInput().getHoverName().getString().equals("My charged staff"), "Mid-ritual reload lost timing/input");
            for (int tick = 0; tick < 13; tick++) RitualAltarBlockEntity.serverTick(level, pos, restored.getBlockState(), restored);
            var result = restored.getInput();
            helper.assertTrue(!restored.isRunning() && result.is(WastelandProgressionContent.WASTELAND_STAFF.get())
                    && RendingStaffItem.energy(result, RendingStaffItem.Channel.ABYSSAL) == 99
                    && RendingStaffItem.energy(result, RendingStaffItem.Channel.SHADOW) == 37
                    && result.getHoverName().getString().equals("My charged staff") && result.get(DataComponents.CUSTOM_DATA).copyTag().getString("PrivateNote").equals("keep"),
                    "Upgrade did not preserve charged staff components");
            helper.assertTrue(NecronomiconItem.getPotentialEnergy(book) == 6500 && NecronomiconItem.getPotentialEnergy(base) == 5000,
                    "Ritual charged the wrong book or wrong amount");
            for (var offset : BasicScrollRitual.pedestalOffsets()) helper.assertTrue(((RitualPedestalBlockEntity) level.getBlockEntity(pos.offset(offset))).getOffering().isEmpty(), "Upgrade duplicated an offering");
            RitualAltarBlockEntity.serverTick(level, pos, restored.getBlockState(), restored);
            helper.assertTrue(NecronomiconItem.getPotentialEnergy(book) == 6500 && restored.getInput() == result, "Completed ritual ran twice");
            var target = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level); target.setNoAi(true); target.moveTo(pos.getX()+.5, pos.getY()+1, pos.getZ()+2.5);
            // Isolate the staff's native two-damage input from the zombie's separate armor reduction.
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
            helper.assertTrue(RendingStaffItem.drain(result, player, target) && target.getHealth() == target.getMaxHealth()-2
                    && RendingStaffItem.energy(result, RendingStaffItem.Channel.ABYSSAL) == 101, "Tier-one staff did not drain two or lost overflow");
            helper.assertTrue(RendingStaffItem.redeem(result, player) && RendingStaffItem.energy(result, RendingStaffItem.Channel.ABYSSAL) == 1
                    && RendingStaffItem.energy(result, RendingStaffItem.Channel.SHADOW) == 37
                    && player.getInventory().countItem(WastelandProgressionContent.ESSENCE.get()) == 1 && !RendingStaffItem.redeem(result, player), "Redemption lost excess energy or duplicated the essence");
            var loot = level.getBlockState(pos).getDrops(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, pos.getCenter())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY));
            helper.assertTrue(loot.size() == 1 && loot.getFirst().is(WastelandContent.COBBLESTONE.get().asItem()), "Broken higher altar does not return its ground material");
        } finally {
            clearInputs(level, pos); blocks.forEach((p, state) -> level.setBlock(p, state, 3));
            player.teleportTo(helper.getLevel(), returnPos.x, returnPos.y, returnPos.z, 0, 0);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "wasteland_tier_abort")
    public static void higherRitualAbortsOnBookOrGroundDowngradeWithoutCharging(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION);
        var pos = new BlockPos(5160, 144, 5128); var blocks = prepareGround(level, pos);
        var player = helper.makeMockServerPlayerInLevel(); var returnPos = player.position();
        player.teleportTo(level, pos.getX()+.5, pos.getY()+1, pos.getZ()+.5, 0, 0);
        var book = new ItemStack(WastelandProgressionContent.BOOK.get()); NecronomiconItem.setPotentialEnergy(book, 2000);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        try {
            helper.assertTrue(RitualGroundFormation.form(level, pos, player, book) == RitualGroundFormation.Result.CREATED, "Ground fixture did not form");
            var altar = (RitualAltarBlockEntity) level.getBlockEntity(pos); altar.placeInput(new ItemStack(WastelandProgressionContent.RENDING_STAFF.get())); fillPedestals(level, pos);
            helper.assertTrue(altar.start(level, player), "Higher ritual did not start");
            var base = new ItemStack(AbyssalPort.NECRONOMICON.get()); NecronomiconItem.setPotentialEnergy(base, 2000);
            player.setItemInHand(InteractionHand.MAIN_HAND, base);
            RitualAltarBlockEntity.serverTick(level, pos, altar.getBlockState(), altar);
            helper.assertTrue(!altar.isRunning() && NecronomiconItem.getPotentialEnergy(base) == 2000 && NecronomiconItem.getPotentialEnergy(book) == 2000, "Book downgrade consumed energy");
            player.setItemInHand(InteractionHand.MAIN_HAND, book); helper.assertTrue(altar.start(level, player), "Aborted ritual could not restart");
            var pedestalPos = pos.offset(BasicScrollRitual.pedestalOffsets().getFirst());
            ((RitualPedestalBlockEntity) level.getBlockEntity(pedestalPos)).removeOne();
            level.setBlock(pedestalPos, AbyssalPort.RITUAL_PEDESTAL.get().defaultBlockState(), 3);
            ((RitualPedestalBlockEntity) level.getBlockEntity(pedestalPos)).placeOne(new ItemStack(WastelandContent.STONE.get()));
            RitualAltarBlockEntity.serverTick(level, pos, altar.getBlockState(), altar);
            helper.assertTrue(!altar.isRunning() && NecronomiconItem.getPotentialEnergy(book) == 2000
                    && altar.getInput().is(WastelandProgressionContent.RENDING_STAFF.get()), "Ground downgrade consumed center/PE");
            for (var offset : BasicScrollRitual.pedestalOffsets()) helper.assertTrue(!((RitualPedestalBlockEntity) level.getBlockEntity(pos.offset(offset))).getOffering().isEmpty(), "Abort consumed a remaining offering");
        } finally {
            clearInputs(level, pos); blocks.forEach((p, state) -> level.setBlock(p, state, 3));
            player.teleportTo(helper.getLevel(), returnPos.x, returnPos.y, returnPos.z, 0, 0);
        }
        helper.succeed();
    }

    private static dev.abyssalport.ritual.RitualDefinition definition() {
        return RitualCatalog.INSTANCE.get(WastelandContent.id("abyssal_wasteland_staff")).orElseThrow();
    }
    private static void fillPedestals(ServerLevel level, BlockPos pos) {
        Item[] items = {WastelandContent.STONE.get().asItem(), WastelandMobsContent.PLAGUED_FLESH.get(), AbyssalPort.SHADOW_GEM.get(),
                WastelandContent.STONE.get().asItem(), WastelandMobsContent.PLAGUED_FLESH.get(), WastelandContent.STONE.get().asItem(),
                WastelandContent.STONE.get().asItem(), WastelandMobsContent.PLAGUED_FLESH.get()};
        for (int i = 0; i < 8; i++) {
            var at = pos.offset(BasicScrollRitual.pedestalOffsets().get(i));
            if (!(level.getBlockEntity(at) instanceof RitualPedestalBlockEntity)) level.setBlock(at, WastelandProgressionContent.PEDESTAL.get().defaultBlockState(), 3);
            var pedestal = (RitualPedestalBlockEntity) level.getBlockEntity(at); pedestal.removeOne(); pedestal.placeOne(new ItemStack(items[i]));
        }
    }
    private static Map<BlockPos, BlockState> prepareGround(ServerLevel level, BlockPos pos) {
        Map<BlockPos, BlockState> saved = new HashMap<>();
        for (var at : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 2, 4))) {
            saved.put(at.immutable(), level.getBlockState(at)); level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(pos, WastelandContent.COBBLESTONE.get().defaultBlockState(), 3);
        for (var offset : BasicScrollRitual.pedestalOffsets()) level.setBlock(pos.offset(offset), WastelandContent.COBBLESTONE.get().defaultBlockState(), 3);
        return saved;
    }
    private static void clearInputs(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) altar.removeInput();
        for (var offset : BasicScrollRitual.pedestalOffsets()) if (level.getBlockEntity(pos.offset(offset)) instanceof RitualPedestalBlockEntity pedestal) pedestal.removeOne();
    }
}
