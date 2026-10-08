package dev.abyssalport;

import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.content.CoraliumContent;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.LivingSacrifice;
import dev.abyssalport.ritual.RitualCatalog;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SacrificeGameTests {
    private static final BlockPos ALTAR = new BlockPos(4, 1, 4);
    private SacrificeGameTests() { }

    @GameTest(template = "empty", batch = "sacrifice_types")
    public static void onlyLivingAdultFoodAndVillagersAreEligible(GameTestHelper helper) {
        for (var type : new EntityType<?>[]{EntityType.COW, EntityType.CHICKEN, EntityType.BAT,
                EntityType.SQUID, EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.VILLAGER}) {
            helper.assertTrue(LivingSacrifice.eligible((Mob) type.create(helper.getLevel())), "Missing eligible food type " + type);
        }
        for (var type : new EntityType<?>[]{EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.SKELETON,
                EntityType.CREEPER, EntityType.ENDERMAN, EntityType.WITHER}) {
            helper.assertTrue(!LivingSacrifice.eligible((Mob) type.create(helper.getLevel())), "Ineligible monster accepted " + type);
        }
        Cow child = EntityType.COW.create(helper.getLevel());
        child.setBaby(true);
        helper.assertTrue(!LivingSacrifice.eligible(child), "Baby can be sacrificed");
        child.setBaby(false);
        child.setHealth(0);
        helper.assertTrue(!LivingSacrifice.eligible(child), "Dead creature can start a ritual");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "sacrifice_complete")
    public static void selectedDeathAndSavedProofCompleteExactlyOnce(GameTestHelper helper) {
        var f = fixture(helper);
        Cow cow = cow(helper);
        try {
            helper.assertTrue(f.altar.start(helper.getLevel(), f.player), "Sacrifice failed to start");
            helper.assertTrue(cow.hasEffect(MobEffects.GLOWING), "Selected creature is not marked");
            CompoundTag before = f.altar.saveWithoutMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(before.hasUUID("SacrificeId") && before.getUUID("SacrificeId").equals(cow.getUUID())
                    && !before.getBoolean("SacrificeDeathConfirmed"), "Selected UUID is not persisted");
            var restoredAlive = restore(helper, before);
            tick(helper, restoredAlive, 1);
            helper.assertTrue(restoredAlive.isRunning(), "Reload cannot recover a living selected UUID");
            cow.hurt(helper.getLevel().damageSources().generic(), 1000);
            helper.assertTrue(cow.getPose() == Pose.DYING, "Native death did not reach DYING");
            tick(helper, restoredAlive, 1);
            CompoundTag proof = restoredAlive.saveWithoutMetadata(helper.getLevel().registryAccess());
            helper.assertTrue(proof.getBoolean("SacrificeDeathConfirmed"), "Actual death was not recorded");
            cow.discard(); // Body removal after confirmed death must not invalidate the saved proof.
            var restoredDead = restore(helper, proof);
            tick(helper, restoredDead, 200);
            helper.assertTrue(!restoredDead.isRunning() && restoredDead.getInput().is(CoraliumContent.OBLIVION_CATALYST.get())
                    && restoredDead.getInput().getCount() == 1 && NecronomiconItem.getPotentialEnergy(f.book) == 0,
                    "Sacrifice failed to produce one catalyst for exactly 5,000 PE");
            helper.assertTrue(EarlyResearch.has(f.player, EarlyResearch.OBLIVION), "Catalyst discovery missing");
            for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) helper.assertTrue(
                    ((RitualPedestalBlockEntity) helper.getBlockEntity(ALTAR.offset(offset))).getOffering().isEmpty(), "Offering was not consumed");
            tick(helper, restoredDead, 5);
            helper.assertTrue(restoredDead.getInput().getCount() == 1, "Sacrifice duplicated its result");
        } finally { cow.discard(); clean(helper); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "sacrifice_timeout")
    public static void noCreatureUnderchargeAndLivingTimeoutNeverSpendInputs(GameTestHelper helper) {
        var f = fixture(helper);
        helper.assertTrue(!f.altar.start(helper.getLevel(), f.player), "Ritual started without an eligible creature");
        Cow cow = cow(helper);
        try {
            NecronomiconItem.setPotentialEnergy(f.book, 4999);
            helper.assertTrue(!f.altar.start(helper.getLevel(), f.player) && !cow.hasEffect(MobEffects.GLOWING), "Undercharged ritual selected a sacrifice");
            NecronomiconItem.setPotentialEnergy(f.book, 5000);
            helper.assertTrue(f.altar.start(helper.getLevel(), f.player), "Charged sacrifice failed");
            tick(helper, f.altar, 200);
            assertPreserved(helper, f, f.altar);
            helper.assertTrue(cow.isAlive(), "The altar killed the creature automatically");
        } finally { cow.discard(); clean(helper); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "sacrifice_missing")
    public static void disappearanceUnknownUuidEscapeAndHealthyKilledRemovalCannotCountAsDeath(GameTestHelper helper) {
        for (int mode = 0; mode < 5; mode++) {
            var f = fixture(helper);
            Cow cow = cow(helper);
            try {
                helper.assertTrue(f.altar.start(helper.getLevel(), f.player), "Disappearance fixture failed to start");
                RitualAltarBlockEntity active = f.altar;
                if (mode == 0) cow.discard();
                if (mode == 1) cow.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                if (mode == 2) cow.moveTo(cow.getX() + 10, cow.getY(), cow.getZ());
                if (mode == 3) {
                    var tag = active.saveWithoutMetadata(helper.getLevel().registryAccess());
                    tag.putUUID("SacrificeId", UUID.randomUUID());
                    active = restore(helper, tag);
                }
                if (mode == 4) cow.remove(Entity.RemovalReason.KILLED); // A removal reason alone is not death proof.
                tick(helper, active, 200);
                assertPreserved(helper, f, active);
            } finally { cow.discard(); clean(helper); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "sacrifice_cancelled_death")
    public static void cancelledNativeDeathHookIsNotSacrificeProof(GameTestHelper helper) {
        var f = fixture(helper);
        Cow cow = cow(helper);
        Consumer<LivingDeathEvent> preventDeath = event -> {
            if (event.getEntity() == cow) event.setCanceled(true);
        };
        NeoForge.EVENT_BUS.addListener(preventDeath);
        try {
            helper.assertTrue(f.altar.start(helper.getLevel(), f.player), "Cancelled-death fixture failed to start");
            cow.hurt(helper.getLevel().damageSources().generic(), 1000);
            helper.assertTrue(cow.getHealth() == 0 && cow.getPose() != Pose.DYING,
                    "Test did not reproduce cancelled death with zero health");
            tick(helper, f.altar, 200);
            assertPreserved(helper, f, f.altar);
        } finally { NeoForge.EVENT_BUS.unregister(preventDeath); cow.discard(); clean(helper); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "sacrifice_reload")
    public static void missingProofIdentityAndChangedDefinitionCancelSavedRitual(GameTestHelper helper) {
        for (int mode = 0; mode < 2; mode++) {
            var f = fixture(helper);
            Cow cow = cow(helper);
            try {
                helper.assertTrue(f.altar.start(helper.getLevel(), f.player), "Reload fixture failed to start");
                var tag = f.altar.saveWithoutMetadata(helper.getLevel().registryAccess());
                if (mode == 0) {
                    tag.remove("SacrificeId");
                    tag.putBoolean("SacrificeDeathConfirmed", true);
                } else tag.putString("RitualSignature", "old-definition-without-sacrifice");
                var active = restore(helper, tag);
                tick(helper, active, 200);
                assertPreserved(helper, f, active);
            } finally { cow.discard(); clean(helper); }
        }
        helper.succeed();
    }

    private record Fixture(RitualAltarBlockEntity altar, ServerPlayer player, ItemStack book) { }
    private static Fixture fixture(GameTestHelper helper) {
        helper.setBlock(ALTAR, AbyssalPort.RITUAL_ALTAR.get());
        var altar = (RitualAltarBlockEntity) helper.getBlockEntity(ALTAR);
        altar.removeInput();
        helper.assertTrue(altar.placeInput(new ItemStack(Items.ENDER_EYE)), "Cannot insert center eye");
        for (int i = 0; i < 8; i++) {
            BlockPos pos = ALTAR.offset(BasicScrollRitual.pedestalOffsets().get(i));
            helper.setBlock(pos, AbyssalPort.RITUAL_PEDESTAL.get());
            var stand = (RitualPedestalBlockEntity) helper.getBlockEntity(pos);
            stand.removeOne();
            helper.assertTrue(stand.placeOne(new ItemStack(i % 2 == 0 ? Items.REDSTONE : CoraliumContent.OBLIVION_SHARD.get())), "Cannot insert offering");
        }
        var definition = RitualCatalog.INSTANCE.get(ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "oblivion_catalyst")).orElseThrow();
        helper.assertTrue(definition.livingSacrifice() && definition.matches(helper.getLevel(), helper.absolutePos(ALTAR)), "Loaded recipe does not require sacrifice");
        var player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(ALTAR);
        player.teleportTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5);
        ItemStack book = new ItemStack(AbyssalPort.NECRONOMICON.get());
        NecronomiconItem.setPotentialEnergy(book, 5000);
        player.setItemInHand(InteractionHand.MAIN_HAND, book);
        return new Fixture(altar, player, book);
    }

    private static Cow cow(GameTestHelper helper) {
        Cow cow = EntityType.COW.create(helper.getLevel());
        var pos = helper.absolutePos(ALTAR);
        cow.moveTo(pos.getX()+0.5, pos.getY()+1, pos.getZ()+1.5);
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    private static RitualAltarBlockEntity restore(GameTestHelper helper, CompoundTag saved) {
        var altar = new RitualAltarBlockEntity(helper.absolutePos(ALTAR), helper.getBlockState(ALTAR));
        altar.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(altar);
        return altar;
    }

    private static void tick(GameTestHelper helper, RitualAltarBlockEntity altar, int count) {
        for (int i = 0; i < count; i++) RitualAltarBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(ALTAR), helper.getBlockState(ALTAR), altar);
    }

    private static void assertPreserved(GameTestHelper helper, Fixture f, RitualAltarBlockEntity active) {
        helper.assertTrue(!active.isRunning() && active.getInput().is(Items.ENDER_EYE)
                && NecronomiconItem.getPotentialEnergy(f.book) == 5000, "Aborted sacrifice consumed PE or center");
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) helper.assertTrue(
                !((RitualPedestalBlockEntity) helper.getBlockEntity(ALTAR.offset(offset))).getOffering().isEmpty(), "Aborted sacrifice consumed offerings");
    }

    private static void clean(GameTestHelper helper) {
        for (BlockPos offset : BasicScrollRitual.pedestalOffsets()) {
            BlockPos pos = ALTAR.offset(offset);
            if (helper.getBlockEntity(pos) instanceof RitualPedestalBlockEntity stand) stand.removeOne();
            helper.setBlock(pos, Blocks.AIR);
        }
        if (helper.getBlockEntity(ALTAR) instanceof RitualAltarBlockEntity altar) altar.removeInput();
        helper.setBlock(ALTAR, Blocks.AIR);
    }
}
