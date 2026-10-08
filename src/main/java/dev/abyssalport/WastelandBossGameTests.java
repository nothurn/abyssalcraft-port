package dev.abyssalport;

import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.content.*;
import dev.abyssalport.entity.AbyssalDragonEntity;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.research.WastelandProgressionResearch;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.RitualCatalog;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastelandBossGameTests {
    private WastelandBossGameTests() { }

    @GameTest(template = "empty", batch = "asorah_parts")
    public static void nativePartsHealthImmunityDamageAndBossTrackingAreDistinct(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(3, 12, 3));
        var boss = WastelandBossContent.ASORAH.get().create(level); var minion = WastelandBossContent.SPECTRAL_DRAGON.get().create(level);
        boss.moveTo(pos, 0, 0); boss.setNoAi(true); minion.moveTo(pos.east(12), 0, 0); minion.setNoAi(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL); var tracker = helper.makeMockServerPlayerInLevel();
        tracker.setGameMode(GameType.SPECTATOR);
        try {
            helper.assertTrue(level.addFreshEntity(boss) && level.addFreshEntity(minion), "Multipart fixtures did not spawn");
            boss.tick(); minion.tick();
            helper.assertTrue(boss.getMaxHealth() == 400 && minion.getMaxHealth() == 30 && boss.isMultipartEntity(), "Original-inspired health/multipart contract is wrong");
            for (int i=0;i<boss.getParts().length;i++) helper.assertTrue(boss.getParts()[i].getId() == boss.getId()+i+1
                    && level.getPartEntities().contains(boss.getParts()[i]) && boss.getParts()[i].isPickable(), "A native part is not tracked/hittable");
            helper.assertTrue(boss.getParts()[0].getBoundingBox().getZsize() > minion.getParts()[0].getBoundingBox().getZsize(), "Boss/minion hitboxes have the same size");
            helper.assertTrue(!boss.hurt(level.damageSources().mobAttack(minion), 10) && boss.getHealth() == 400
                    && !boss.addEffect(new MobEffectInstance(MobEffects.POISON, 100)) && boss.canBreatheUnderwater(), "Boss ignored source-informed immunity");
            helper.assertTrue(boss.getParts()[0].hurt(level.damageSources().playerAttack(player), 20) && boss.getHealth() == 380, "Head hit does not deal full player damage");
            boss.invulnerableTime = 0;
            helper.assertTrue(boss.getParts()[1].hurt(level.damageSources().playerAttack(player), 20) && boss.getHealth() == 374, "Body hit is not amount/4+1");
            boss.invulnerableTime = 0;
            float health = boss.getHealth(); boss.getParts()[0].hurt(level.damageSources().playerAttack(player), 100);
            helper.assertTrue(health-boss.getHealth() >= 10 && health-boss.getHealth() <= 19, "Huge boss hits are not bounded");
            boss.setHealth(190); boss.startSeenByPlayer(tracker); boss.tick();
            helper.assertTrue(boss.bossEvent().getPlayers().contains(tracker) && boss.bossEvent().getColor() == BossEvent.BossBarColor.RED
                    && Math.abs(boss.bossEvent().getProgress()-.475F)<.001, "Boss bar is not tracking actual health/phase");
            boss.stopSeenByPlayer(tracker); helper.assertTrue(!boss.bossEvent().getPlayers().contains(tracker), "Boss bar retained an untracked player");
            helper.assertTrue(!SpawnPlacements.checkSpawnRules(WastelandBossContent.SPECTRAL_DRAGON.get(), level, MobSpawnType.NATURAL, pos, level.random), "Spectral Dragon can spawn outside the Wasteland");
        } finally { boss.discard(); minion.discard(); }
        helper.assertTrue(level.getPartEntities().stream().noneMatch(part -> part.getParent() == boss), "Removed boss left stale part IDs");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "asorah_flight")
    public static void flightIsLocalAndHealingConsumesTheNearestMinionsHealth(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(4, 20, 4));
        var boss = WastelandBossContent.ASORAH.get().create(level); var donor = WastelandBossContent.SPECTRAL_DRAGON.get().create(level);
        var far = WastelandBossContent.SPECTRAL_DRAGON.get().create(level);
        boss.moveTo(pos, 0, 0); boss.setHome(pos); boss.setHealth(350);
        donor.moveTo(pos.east(5), 0, 0); donor.setNoAi(true); donor.setHealth(2);
        far.moveTo(pos.east(45), 0, 0); far.setNoAi(true);
        level.addFreshEntity(boss); level.addFreshEntity(donor); level.addFreshEntity(far);
        try {
            for (int i=0;i<10;i++) boss.tick();
            helper.assertTrue(boss.getHealth() == 351 && donor.getHealth() == 1 && far.getHealth() == 30
                    && donor.getUUID().equals(boss.healer()), "Healing is free, uses a distant minion or ignores the nearest donor");
            for (int i=0;i<10;i++) boss.tick();
            helper.assertTrue(boss.getHealth() == 352 && donor.isDeadOrDying() && donor.getPose() == Pose.DYING, "Exhausted donor did not die after paying its health");
            for (int i=0;i<20;i++) boss.tick();
            helper.assertTrue(boss.getHealth() == 352 && boss.healer() == null, "Dead/distant donor keeps healing the boss");
            var restored = WastelandBossContent.ASORAH.get().create(level); restored.load(boss.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
            helper.assertTrue(restored.home().equals(pos) && restored.getHealth() == boss.getHealth(), "Save lost summon home or boss health");
            for (int i=0;i<160;i++) boss.tick();
            helper.assertTrue(boss.position().distanceTo(pos.getCenter()) < 40 && boss.position().distanceTo(pos.getCenter()) > 2,
                    "Dragon did not fly or navigated toward world origin instead of its summon home");
        } finally { boss.discard(); donor.discard(); far.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "asorah_contact")
    public static void nativeHeadContactDamagesSurvivalButNotCreativePlayers(GameTestHelper helper) {
        var level = helper.getLevel(); var oldDifficulty = level.getDifficulty(); level.getServer().setDifficulty(Difficulty.NORMAL, true);
        var boss = WastelandBossContent.ASORAH.get().create(level); var pos = helper.absolutePos(new BlockPos(3, 14, 3));
        boss.moveTo(pos, 0, 0); boss.setNoAi(true); boss.tick();
        var victim = helper.makeMockPlayer(GameType.SURVIVAL); victim.moveTo(boss.getParts()[0].position()); boss.setTarget(victim); boss.setNoAi(false);
        try {
            boss.tick(); float health = victim.getHealth();
            helper.assertTrue(health < 20, "Native head contact did not hurt a survival player");
            victim.invulnerableTime=0; boss.tick();
            helper.assertTrue(victim.getHealth() == health, "Contact damage ignored the one-second cooldown");
            var creative = helper.makeMockPlayer(GameType.CREATIVE); creative.moveTo(boss.getParts()[0].position());
            boss.setTarget(creative); boss.tick(); helper.assertTrue(creative.getHealth() == 20 && boss.getTarget() != creative, "Boss attacks creative players");
        } finally { boss.discard(); level.getServer().setDifficulty(oldDifficulty, true); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "asorah_rewards")
    public static void savedDeathPaysOneEyeAndExperienceOnlyAfterTwoHundredTicks(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(4, 20, 4)); var area = new AABB(pos).inflate(48);
        var boss = WastelandBossContent.ASORAH.get().create(level); boss.moveTo(pos, 0, 0); boss.setNoAi(true);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var priorDrops = level.getEntitiesOfClass(ItemEntity.class, area); var priorXp = level.getEntitiesOfClass(ExperienceOrb.class, area);
        boolean oldLoot = level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true, level.getServer());
        try {
            helper.assertTrue(boss.hurt(level.damageSources().genericKill(), Float.MAX_VALUE) && boss.getPose() == Pose.DYING, "Administrative damage did not start native boss death");
            for (int tick=0;tick<100;tick++) boss.tick();
            helper.assertTrue(eyeCount(level, area, priorDrops) == 0 && !boss.isRemoved() && boss.deathTime == 100, "Death rewards appeared too early");
            var restored = WastelandBossContent.ASORAH.get().create(level); restored.load(boss.saveWithoutId(new net.minecraft.nbt.CompoundTag())); boss.discard();
            for (int tick=0;tick<99;tick++) restored.tick();
            helper.assertTrue(eyeCount(level, area, priorDrops) == 0 && restored.deathTime == 199, "Reload changed the death clock");
            restored.tick();
            helper.assertTrue(restored.isRemoved() && eyeCount(level, area, priorDrops) == 1, "Native delayed loot did not release exactly one Eye");
            int xp = level.getEntitiesOfClass(ExperienceOrb.class, area).stream().filter(orb -> !priorXp.contains(orb))
                    .mapToInt(orb -> orb.getValue()*Math.max(1, orb.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getInt("Count"))).sum();
            helper.assertTrue(xp == 5000, "Native experience payout is not the consolidated ten 500-XP waves: "+xp);
            var paid = WastelandBossContent.ASORAH.get().create(level); paid.load(restored.saveWithoutId(new net.minecraft.nbt.CompoundTag())); paid.tick();
            helper.assertTrue(eyeCount(level, area, priorDrops) == 1, "Paid death save duplicated the Eye");
            var eye = level.getEntitiesOfClass(ItemEntity.class, area).stream().filter(drop -> drop.getItem().is(WastelandBossContent.EYE.get())).findFirst().orElseThrow();
            var discoverer = helper.makeMockServerPlayerInLevel(); discoverer.setGameMode(GameType.SPECTATOR);
            helper.assertTrue(WastelandProgressionResearch.discover(discoverer, eye.getItem()) && EarlyResearch.has(discoverer, EarlyResearch.ASORAH)
                    && !WastelandProgressionResearch.discover(discoverer, eye.getItem()), "Eye discovery is missing or repeats");
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false, level.getServer());
            var disabled = WastelandBossContent.ASORAH.get().create(level); disabled.moveTo(pos, 0, 0); disabled.setNoAi(true);
            disabled.hurt(level.damageSources().genericKill(), Float.MAX_VALUE); for (int i=0;i<200;i++) disabled.tick();
            helper.assertTrue(eyeCount(level, area, priorDrops) == 1 && level.getEntitiesOfClass(ExperienceOrb.class, area).stream().filter(orb -> !priorXp.contains(orb))
                    .mapToInt(orb -> orb.getValue()*Math.max(1, orb.saveWithoutId(new net.minecraft.nbt.CompoundTag()).getInt("Count"))).sum() == 5000,
                    "Disabled mob loot still produces items or boss XP");
        } finally {
            level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(oldLoot, level.getServer()); boss.discard();
            level.getEntitiesOfClass(ItemEntity.class, area).stream().filter(drop -> !priorDrops.contains(drop)).forEach(Entity::discard);
            level.getEntitiesOfClass(ExperienceOrb.class, area).stream().filter(orb -> !priorXp.contains(orb)).forEach(Entity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "asorah_summon")
    public static void realWastelandSummoningConsumesSevenOfferingsAndChargesOnce(GameTestHelper helper) {
        withGround(helper, new BlockPos(5208,144,5128), (level,pos,altar,player,book) -> {
            java.util.List<AbyssalDragonEntity> admitted = new java.util.ArrayList<>();
            Consumer<EntityJoinLevelEvent> watch = event -> { if (event.getLevel()==level && event.getEntity() instanceof AbyssalDragonEntity dragon && dragon.isBoss() && pos.equals(dragon.home())) admitted.add(dragon); };
            NeoForge.EVENT_BUS.addListener(watch);
            try {
            var ritual = RitualCatalog.INSTANCE.get(WastelandContent.id("summon_asorah")).orElseThrow();
            helper.assertTrue(ritual.matches(level,pos) && ritual.summonType() == WastelandBossContent.ASORAH.get()
                    && ritual.resultStack().isEmpty() && ritual.offerings().values().stream().mapToInt(Integer::intValue).sum() == 7,
                    "Loaded summoning definition is not the actual seven-offering entity outcome");
            helper.assertTrue(SpawnPlacements.checkSpawnRules(WastelandBossContent.SPECTRAL_DRAGON.get(), level, MobSpawnType.NATURAL, pos.above(), level.random), "Native Wasteland minion spawn rule rejects its dimension");
            helper.assertTrue(altar.start(level,player), "Native Asorah summoning did not start");
            for (int tick=0;tick<20;tick++) RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
            var spawned = admitted.stream().filter(dragon->dragon.isAddedToLevel() && dragon.isAlive() && !dragon.isRemoved()).toList();
            helper.assertTrue(spawned.size() == 1 && spawned.getFirst().home().equals(pos) && spawned.getFirst().getMaxHealth() == 400
                    && NecronomiconItem.getPotentialEnergy(book) == 1000 && altar.getInput().isEmpty(),
                    "Summon outcome: bosses="+spawned.size()+" pe="+NecronomiconItem.getPotentialEnergy(book)
                        +" center="+altar.getInput()+" entities="+spawned.stream().map(entity->entity.home()+":"+entity.getHealth()).toList());
            for (var offset : BasicScrollRitual.pedestalOffsets()) helper.assertTrue(((RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(offset))).getOffering().isEmpty(), "Summon left an unconsumed offering");
            RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
            helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==1000 && admitted.size()==1, "Summoning ran twice");
            } finally { NeoForge.EVENT_BUS.unregister(watch); admitted.forEach(Entity::discard); }
        }); helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "asorah_native_ticks", timeoutTicks = 120)
    public static void acceptedBossActuallyTracksAndFliesOnNativeWastelandTicks(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION); var pos=new BlockPos(5312,170,5128);
        var chunks=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for (int x=-2;x<=2;x++) for (int z=-2;z<=2;z++) {
            var chunk=new net.minecraft.world.level.ChunkPos(new net.minecraft.world.level.ChunkPos(pos).x+x,new net.minecraft.world.level.ChunkPos(pos).z+z);
            level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,chunk,2,chunk); level.getChunk(chunk.x,chunk.z); chunks.add(chunk);
        }
        var boss=WastelandBossContent.ASORAH.get().create(level); boss.moveTo(pos,0,0); boss.setHome(pos);
        helper.assertTrue(level.addFreshEntity(boss), "Native-tick boss was refused");
        helper.runAfterDelay(60,()->{
            try {
                helper.assertTrue(boss.tickCount>=20 && !boss.isRemoved() && level.getEntity(boss.getUUID())==boss
                        && boss.position().distanceTo(pos.getCenter())>2 && boss.position().distanceTo(pos.getCenter())<40,
                        "Accepted boss is not actually ticking/tracked/flying: ticks="+boss.tickCount+" position="+boss.position());
                for (var part : boss.getParts()) helper.assertTrue(level.getPartEntities().contains(part),"Native Wasteland tracking lost a multipart hitbox");
                helper.succeed();
            } finally { boss.discard(); for (var chunk:chunks) level.getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.FORCED,chunk,2,chunk); }
        });
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "asorah_spawn_veto")
    public static void joinAndFinalizeVetoOrHookBookChangeRetainAllInputs(GameTestHelper helper) {
        withGround(helper, new BlockPos(5240,144,5128), (level,pos,altar,player,book) -> {
            Consumer<EntityJoinLevelEvent> veto = event -> { if (event.getEntity().getType() == WastelandBossContent.ASORAH.get()) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
            try { rejectedSummon(helper,level,pos,altar,player,book); } finally { NeoForge.EVENT_BUS.unregister(veto); }
            Consumer<FinalizeSpawnEvent> finalize = event -> { if (event.getEntity().getType() == WastelandBossContent.ASORAH.get()) event.setSpawnCancelled(true); };
            NeoForge.EVENT_BUS.addListener(finalize);
            try { rejectedSummon(helper,level,pos,altar,player,book); } finally { NeoForge.EVENT_BUS.unregister(finalize); }
            Consumer<EntityJoinLevelEvent> changeBook = event -> { if (event.getEntity().getType() == WastelandBossContent.ASORAH.get()) player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); };
            NeoForge.EVENT_BUS.addListener(changeBook);
            try { rejectedSummon(helper,level,pos,altar,player,book); } finally { NeoForge.EVENT_BUS.unregister(changeBook); player.setItemInHand(InteractionHand.MAIN_HAND,book); }
        }); helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "asorah_summon_space")
    public static void blockedSpaceAndPeacefulDifficultyNeverConsumeSummonInputs(GameTestHelper helper) {
        withGround(helper, new BlockPos(5272,144,5128), (level,pos,altar,player,book) -> {
            var obstruct = pos.above(3); level.setBlock(obstruct,Blocks.STONE.defaultBlockState(),3);
            rejectedSummon(helper,level,pos,altar,player,book); level.setBlock(obstruct,Blocks.AIR.defaultBlockState(),3);
            var old = level.getDifficulty(); level.getServer().setDifficulty(Difficulty.PEACEFUL,true);
            try { rejectedSummon(helper,level,pos,altar,player,book); } finally { level.getServer().setDifficulty(old,true); }
        }); helper.succeed();
    }

    private static int eyeCount(ServerLevel level,AABB area,java.util.List<ItemEntity> before) {
        return level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(drop -> !before.contains(drop) && drop.getItem().is(WastelandBossContent.EYE.get())).mapToInt(drop -> drop.getItem().getCount()).sum();
    }
    private static void rejectedSummon(GameTestHelper helper,ServerLevel level,BlockPos pos,RitualAltarBlockEntity altar,net.minecraft.server.level.ServerPlayer player,ItemStack book) {
        helper.assertTrue(altar.start(level,player), "Rejected summon fixture could not start");
        for (int tick=0;tick<20;tick++) RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
        int ingredients=0; for (var offset : BasicScrollRitual.pedestalOffsets()) if (!((RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(offset))).getOffering().isEmpty()) ingredients++;
        helper.assertTrue(!altar.isRunning() && NecronomiconItem.getPotentialEnergy(book)==2000 && ingredients==7
                && level.getEntitiesOfClass(AbyssalDragonEntity.class,new AABB(pos).inflate(16),AbyssalDragonEntity::isBoss).isEmpty(), "Rejected summon consumed offerings/PE or retained a boss");
    }
    @FunctionalInterface private interface GroundTest { void run(ServerLevel level,BlockPos pos,RitualAltarBlockEntity altar,net.minecraft.server.level.ServerPlayer player,ItemStack book); }
    private static void withGround(GameTestHelper helper,BlockPos pos,GroundTest test) {
        var level=helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION); Map<BlockPos,BlockState> saved=new HashMap<>();
        for (var at : BlockPos.betweenClosed(pos.offset(-5,0,-5),pos.offset(5,8,5))) { saved.put(at.immutable(),level.getBlockState(at)); level.setBlock(at,Blocks.AIR.defaultBlockState(),3); }
        level.setBlock(pos,WastelandProgressionContent.ALTAR.get().defaultBlockState(),3);
        Item[] items={Items.GOLD_INGOT,CoraliumContent.TRANSMUTATION_GEM.get(),Items.GOLD_INGOT,Items.ENCHANTING_TABLE,
                Items.GOLD_INGOT,WastelandContent.CORALIUM_BUCKET.get(),Items.GOLD_INGOT,Items.AIR};
        for (int i=0;i<8;i++) { var at=pos.offset(BasicScrollRitual.pedestalOffsets().get(i)); level.setBlock(at,WastelandProgressionContent.PEDESTAL.get().defaultBlockState(),3);
            if (items[i]!=Items.AIR) ((RitualPedestalBlockEntity)level.getBlockEntity(at)).placeOne(new ItemStack(items[i])); }
        var altar=(RitualAltarBlockEntity)level.getBlockEntity(pos); var player=helper.makeMockServerPlayerInLevel(); var back=player.position();
        player.teleportTo(level,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        var book=new ItemStack(WastelandProgressionContent.BOOK.get()); NecronomiconItem.setPotentialEnergy(book,2000); player.setItemInHand(InteractionHand.MAIN_HAND,book);
        try { test.run(level,pos,altar,player,book); } finally {
            level.getEntitiesOfClass(AbyssalDragonEntity.class,new AABB(pos).inflate(24),AbyssalDragonEntity::isBoss).forEach(Entity::discard);
            for (var offset : BasicScrollRitual.pedestalOffsets()) if (level.getBlockEntity(pos.offset(offset)) instanceof RitualPedestalBlockEntity pedestal) pedestal.removeOne();
            altar.removeInput(); saved.forEach((at,state)->level.setBlock(at,state,3)); player.teleportTo(helper.getLevel(),back.x,back.y,back.z,0,0);
        }
    }
}
