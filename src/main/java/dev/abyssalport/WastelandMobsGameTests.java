package dev.abyssalport;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandMineralsContent;
import dev.abyssalport.content.WastelandMobsContent;
import dev.abyssalport.effect.CoraliumPlagueEffect;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastelandMobsGameTests {
    private WastelandMobsGameTests() { }

    @GameTest(template = "empty", batch = "wasteland_mob_state")
    public static void zombieHasDistinctAttributesImmunityAndPersistentBabyDimensions(GameTestHelper helper) {
        var level = helper.getLevel(); var zombie = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level);
        helper.assertTrue(zombie.getMaxHealth() == 25 && zombie.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6
                && zombie.getAttributeValue(Attributes.FOLLOW_RANGE) == 42 && zombie.getArmorValue() == 2, "Abyssal Zombie is not source-informed");
        helper.assertTrue(zombie.canBreatheUnderwater() && CoraliumPlagueEffect.immune(zombie)
                && !zombie.addEffect(new MobEffectInstance(WastelandContent.CORALIUM_PLAGUE, 200)), "Zombie disease/water immunity is absent");
        float adultWidth = zombie.getBbWidth(), adultHeight = zombie.getBbHeight();
        zombie.setBaby(true); zombie.setBaby(true);
        helper.assertTrue(zombie.isBaby() && Math.abs(zombie.getBbWidth()-adultWidth*.5F) < .001
                && Math.abs(zombie.getBbHeight()-adultHeight*.5F) < .001 && Math.abs(zombie.getAttributeValue(Attributes.MOVEMENT_SPEED)-.345) < .001, "Baby dimensions/speed stacked or did not apply");
        var saved = zombie.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var restored = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level); restored.load(saved);
        helper.assertTrue(restored.isBaby() && Math.abs(restored.getAttributeValue(Attributes.MOVEMENT_SPEED)-.345) < .001, "Saved baby zombie changed speed");
        restored.setBaby(false);
        helper.assertTrue(Math.abs(restored.getBbHeight()-adultHeight) < .001 && Math.abs(restored.getAttributeValue(Attributes.MOVEMENT_SPEED)-.23) < .001, "Adult reset retained baby size or modifier");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "wasteland_mob_loot")
    public static void actualLootFoodAndChunkRefiningConnectTheMobToProgression(GameTestHelper helper) {
        var level = helper.getLevel(); var zombie = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level);
        var player = helper.makeMockServerPlayerInLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(zombie.getLootTable());
        var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, zombie)
                .withParameter(LootContextParams.ORIGIN, zombie.position()).withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
                .withParameter(LootContextParams.ATTACKING_ENTITY, player).withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).create(LootContextParamSets.ENTITY);
        boolean flesh = false, chunk = false;
        for (int seed = 1; seed <= 2000; seed++) {
            for (var drop : table.getRandomItems(params, seed)) {
                if (drop.isEmpty()) continue; // A source-faithful count of zero does not spawn an item entity.
                helper.assertTrue(drop.is(WastelandMobsContent.PLAGUED_FLESH.get()) || drop.is(WastelandMobsContent.CORALIUM_CHUNK.get()) || drop.is(Items.BONE), "Zombie loot includes unrelated items: "+drop);
                if (drop.is(WastelandMobsContent.PLAGUED_FLESH.get())) { flesh = true; helper.assertTrue(drop.getCount() <= 2, "Unenchanted flesh drop too large"); }
                if (drop.is(WastelandMobsContent.CORALIUM_CHUNK.get())) { chunk = true; helper.assertTrue(drop.getCount() <= 3, "Rare chunk yield too large"); }
            }
        }
        helper.assertTrue(flesh && chunk, "Actual zombie loot table is empty or has no rare Coralium route");
        var nonPlayer = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, zombie)
                .withParameter(LootContextParams.ORIGIN, zombie.position()).withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic()).create(LootContextParamSets.ENTITY);
        for (int seed = 1; seed <= 128; seed++) for (var drop : table.getRandomItems(nonPlayer, seed)) helper.assertTrue(drop.isEmpty() || drop.is(WastelandMobsContent.PLAGUED_FLESH.get()), "Non-player kill received player-only rare loot");
        var input = new SingleRecipeInput(new ItemStack(WastelandMobsContent.CORALIUM_CHUNK.get()));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).orElseThrow();
        var output = recipe.value().assemble(input, level.registryAccess());
        helper.assertTrue(output.is(WastelandMineralsContent.INGOT.get()) && output.getCount() == 2 && recipe.value().getExperience() == 3, "Chunk does not refine into two ingots");
        // makeMockServerPlayerInLevel is always creative; consumption needs a survival player.
        var consumer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        consumer.getFoodData().setFoodLevel(10);
        var food = new ItemStack(WastelandMobsContent.PLAGUED_FLESH.get(), 2);
        food.getItem().finishUsingItem(food, level, consumer);
        helper.assertTrue(food.getCount() == 1 && consumer.getFoodData().getFoodLevel() == 12
                && consumer.getEffect(MobEffects.HUNGER).getAmplifier() == 1 && consumer.getEffect(MobEffects.HUNGER).getDuration() == 600
                && consumer.hasEffect(MobEffects.CONFUSION) && consumer.getEffect(WastelandContent.CORALIUM_PLAGUE).getDuration() == 600, "Plagued flesh does not apply the correct food hazards");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "wasteland_conversion")
    public static void realLethalAttackConvertsOnlyAfterSuccessfulSpawnAndKeepsBabyState(GameTestHelper helper) {
        var level = helper.getLevel(); var server = level.getServer(); var oldDifficulty = level.getDifficulty();
        var attacker = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level);
        var victim = EntityType.ZOMBIE.create(level); var blocked = EntityType.VILLAGER.create(level);
        attacker.setNoAi(true); victim.setNoAi(true); blocked.setNoAi(true);
        var pos = helper.absolutePos(new BlockPos(2, 1, 2)); attacker.moveTo(pos, 0, 0); victim.moveTo(pos, 0, 0); blocked.moveTo(pos.east(3), 0, 0);
        victim.setBaby(true); victim.setHealth(1); blocked.setHealth(1);
        var existing = level.getEntitiesOfClass(dev.abyssalport.entity.AbyssalZombieEntity.class, helper.getBounds().inflate(2));
        server.setDifficulty(Difficulty.HARD, true);
        level.addFreshEntity(attacker); level.addFreshEntity(victim); level.addFreshEntity(blocked);
        try {
            helper.assertTrue(attacker.doHurtTarget(victim) && victim.isRemoved(), "Real lethal attack failed to convert a zombie");
            var replacements = level.getEntitiesOfClass(dev.abyssalport.entity.AbyssalZombieEntity.class, helper.getBounds().inflate(2)).stream()
                    .filter(entity -> entity != attacker && !existing.contains(entity)).toList();
            helper.assertTrue(replacements.size() == 1 && replacements.getFirst().isBaby(), "Conversion duplicated its result or lost baby state");
            Consumer<EntityJoinLevelEvent> veto = event -> {
                if (event.getEntity() instanceof dev.abyssalport.entity.AbyssalZombieEntity && event.getEntity() != attacker
                        && event.getEntity().position().distanceTo(blocked.position()) < .1) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(veto);
            try {
                helper.assertTrue(attacker.doHurtTarget(blocked) && !blocked.isRemoved(), "Spawn veto discarded the original victim");
            } finally { NeoForge.EVENT_BUS.unregister(veto); }
            replacements.forEach(net.minecraft.world.entity.Entity::discard);
        } finally { attacker.discard(); victim.discard(); blocked.discard(); server.setDifficulty(oldDifficulty, true); }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "wasteland_infection")
    public static void successfulWastelandAttackInfectsButCancelledDamageDoesNot(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION);
        var attacker = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level); var target = EntityType.COW.create(level);
        attacker.setNoAi(true); target.setNoAi(true);
        helper.assertTrue(attacker.doHurtTarget(target) && target.getEffect(WastelandContent.CORALIUM_PLAGUE).getDuration() == 100, "Wasteland melee did not transmit plague");
        var invulnerable = EntityType.COW.create(level); invulnerable.setInvulnerable(true);
        helper.assertTrue(!attacker.doHurtTarget(invulnerable) && !invulnerable.hasEffect(WastelandContent.CORALIUM_PLAGUE), "Failed attack still infected target");
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        for (var name : java.util.List.of("abyssal_wasteland", "coralium_lake")) helper.assertTrue(registry.get(WastelandContent.id(name))
                .getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap().stream()
                .anyMatch(spawn -> spawn.type == WastelandMobsContent.ABYSSAL_ZOMBIE.get() && spawn.getWeight().asInt() == 60), "Abyssal Zombie has no natural Wasteland spawn entry");
        helper.succeed();
    }
}
