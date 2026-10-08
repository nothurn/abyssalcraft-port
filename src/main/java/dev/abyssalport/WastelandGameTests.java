package dev.abyssalport;

import dev.abyssalport.block.GatewayAnchorBlock;
import dev.abyssalport.block.entity.GatewayAnchorBlockEntity;
import dev.abyssalport.content.GatewayContent;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.world.GatewayTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastelandGameTests {
    private WastelandGameTests() { }
    @GameTest(template = "empty", batch = "wasteland_fluid")
    public static void coraliumSourceCanBeBucketedAndExposureIsNotMilkCurable(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var cow = EntityType.COW.create(level); cow.setNoAi(true); cow.moveTo(pos.getX()+0.5, pos.getY(), pos.getZ()+0.5);
        level.addFreshEntity(cow);
        try {
            var state = WastelandContent.LIQUID_CORALIUM.get().defaultBlockState();
            level.setBlock(pos, state, 2);
            state.entityInside(level, pos, cow);
            var effect = cow.getEffect(WastelandContent.CORALIUM_PLAGUE);
            helper.assertTrue(effect != null && effect.getDuration() == 200, "Fluid exposure has wrong plague duration");
            helper.assertTrue(!cow.removeEffectsCuredBy(EffectCures.MILK) && cow.hasEffect(WastelandContent.CORALIUM_PLAGUE), "Milk incorrectly cured Coralium Plague");
            cow.tickCount = 40; var before = cow.getHealth();
            WastelandContent.CORALIUM_PLAGUE.get().applyEffectTick(cow, 0);
            helper.assertTrue(cow.getHealth() == before-2, "Plague damage/cadence is not applied on the server");
            var bucket = WastelandContent.LIQUID_CORALIUM.get().pickupBlock(null, level, pos, state);
            helper.assertTrue(bucket.is(WastelandContent.CORALIUM_BUCKET.get()) && level.getBlockState(pos).isAir(), "Source pickup lost or duplicated fluid");
            helper.assertTrue(WastelandContent.CORALIUM_BUCKET.get().emptyContents(null, level, pos, null, bucket)
                    && level.getFluidState(pos).isSource() && level.getFluidState(pos).getType() == WastelandContent.CORALIUM.get(), "Native bucket placement has wrong fluid");
            var flowing = state.setValue(LiquidBlock.LEVEL, 1);
            level.setBlock(pos, flowing, 2);
            helper.assertTrue(WastelandContent.LIQUID_CORALIUM.get().pickupBlock(null, level, pos, flowing).isEmpty(), "Flowing fluid produced an unlimited source bucket");
            helper.assertTrue(bucket.getCraftingRemainingItem().is(Items.BUCKET), "Bucket crafting did not preserve its container");
        } finally { cow.discard(); level.removeBlock(pos, false); }
        helper.succeed();
    }
    @GameTest(template = "empty", batch = "gateway_invalid_anchor", timeoutTicks = 80)
    public static void corruptedOrBrokenAnchorNeverFallsBackToAnUnrelatedDimension(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        var anchor = GatewayTravel.createAnchor(level, pos, WastelandContent.DIMENSION, Direction.Axis.X);
        var id = anchor.portalId(); var saved = anchor.saveWithoutMetadata(level.registryAccess());
        saved.putString("Destination", "invalid dimension!");
        var restored = new GatewayAnchorBlockEntity(pos, level.getBlockState(pos));
        restored.loadWithComponents(saved, level.registryAccess()); level.setBlockEntity(restored);
        helper.assertTrue(restored.portalId().equals(id) && restored.destination() == null && !restored.intact(), "Corrupt destination silently selected another world");
        var cow = EntityType.COW.create(level);
        helper.assertTrue(GatewayTravel.transition(level, cow, pos.above()) == null, "Invalid anchor allowed travel");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(level.getGameTime()%20 == 0, "Waiting for native anchor tick"))
                .thenExecute(() -> {
                    GatewayAnchorBlockEntity.serverTick(level, pos, level.getBlockState(pos), restored);
                    helper.assertTrue(!level.getBlockState(pos).getValue(GatewayAnchorBlock.ACTIVE)
                            && level.getBlockState(pos.above()).isAir() && level.getBlockState(pos.above(2)).isAir(), "Broken anchor left an active orphan portal");
                    level.removeBlock(pos, false);
                }).thenSucceed();
    }
}
