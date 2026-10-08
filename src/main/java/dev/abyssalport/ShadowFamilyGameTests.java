package dev.abyssalport;

import dev.abyssalport.content.*;
import dev.abyssalport.entity.*;
import dev.abyssalport.entity.ShadowCreatureEntity.Kind;
import dev.abyssalport.item.RendingStaffItem;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShadowFamilyGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private ShadowFamilyGameTests() { }
    private static ShadowCreatureEntity create(Kind kind, ServerLevel level, Vec3 pos) {
        var mob=ShadowFamilyContent.type(kind).create(level); mob.moveTo(pos); mob.setNoAi(true); mob.setPersistenceRequired();
        if(!level.addFreshEntity(mob)) throw new GameTestAssertException("Native Shadow admission rejected"); return mob;
    }
    private static net.minecraft.world.entity.animal.Cow cow(ServerLevel level, Vec3 pos) {
        var cow=EntityType.COW.create(level);cow.moveTo(pos);cow.setNoAi(true);cow.setPersistenceRequired();
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);cow.setHealth(200);
        if(!level.addFreshEntity(cow)) throw new GameTestAssertException("Native target admission rejected");return cow;
    }
    private static void aim(ShadowCreatureEntity beast,LivingEntity target) {
        double x=target.getX()-beast.getX(),z=target.getZ()-beast.getZ();
        float yaw=(float)Math.toDegrees(Math.atan2(-x,z));
        float pitch=(float)Math.toDegrees(Math.atan2(beast.getEyeY()-target.getBoundingBox().getCenter().y,Math.hypot(x,z)));
        beast.setYRot(yaw);beast.setYHeadRot(yaw);beast.setYBodyRot(yaw);beast.setXRot(pitch);
    }
    @GameTest(template="empty",batch="shadow_family_attributes")
    public static void RealFormsPreserveAttributesImmunitiesAndNativeMelee(GameTestHelper helper) {
        var level=helper.getLevel();var victim=EntityType.COW.create(level);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);victim.setHealth(200);
        int index=0;
        try { for(Kind kind:Kind.values()) {var mob=ShadowFamilyContent.type(kind).create(level);
            try {
                helper.assertTrue(mob.kind()==kind&&mob.getMaxHealth()==20+20*index&&mob.getAttributeValue(Attributes.ATTACK_DAMAGE)==6+2*index
                    &&mob.getAttributeValue(Attributes.FOLLOW_RANGE)==(kind==Kind.CREATURE?20:42)
                    &&Math.abs(mob.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)-(kind==Kind.BEAST?.3:.2))<.001,"Native source family attributes are wrong");
                helper.assertTrue(mob.fireImmune()&&mob.canBreatheUnderwater()&&!mob.canBeAffected(new MobEffectInstance(MobEffects.POISON,100))
                    &&mob.canBeAffected(new MobEffectInstance(MobEffects.REGENERATION,100)),"Shadow source immunities conflated");
                victim.invulnerableTime=0;float health=victim.getHealth();helper.assertTrue(mob.doHurtTarget(victim)&&victim.getHealth()==health-6-2*index,"Native melee differs");
                var friend=GraveyardContent.SHADOW_GHOUL.get().create(level);try {
                    helper.assertTrue(!friend.hurt(ShadowFamily.damage(mob),2)&&!mob.hurt(friend.damageSources().mobAttack(friend),2),"Friendly Shadow damage accepted");
                } finally {friend.discard();}
            } finally {mob.discard();} index++;
        } helper.succeed();} finally {victim.discard();}
    }
    @GameTest(template="empty",batch="shadow_native_halloween")
    public static void RealDateHolidayHeadDoesNotReplaceEquipmentOrCreateOffDatePumpkins(GameTestHelper helper) {
        for(Kind kind:Kind.values()) {
            var mob=ShadowFamilyContent.type(kind).create(helper.getLevel());
            try {
                for(int i=0;i<100;i++)mob.equipHolidayHead(java.time.LocalDate.of(2026,10,30));
                helper.assertTrue(mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"Holiday head appears outside actual date");
                mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
                for(int i=0;i<100;i++)mob.equipHolidayHead(java.time.LocalDate.of(2026,10,31));
                helper.assertTrue(mob.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET),"Holiday overwrites pre-equipped native head slot");
                mob.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
                for(long seed=0;seed<10000;seed++) {mob.getRandom().setSeed(seed);if(mob.getRandom().nextFloat()<.25F) {
                    mob.getRandom().setSeed(seed);mob.equipHolidayHead(java.time.LocalDate.of(2026,10,31));break;
                }}
                helper.assertTrue(mob.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)||mob.getItemBySlot(EquipmentSlot.HEAD).is(Items.JACK_O_LANTERN),"Source holiday branch not linked to real native equipment");
            } finally {mob.discard();}
        }helper.succeed();
    }
    @GameTest(template="empty",batch="shadow_breath_native")
    public static void RealBreathCooldownSaveRayDamageAndEffectsStayBounded(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var beast=create(Kind.BEAST,fixture.level,fixture.node.getBottomCenter().add(0,0,-3));
            var target=cow(fixture.level,fixture.node.getBottomCenter());
            try {
                beast.setTarget(target);aim(beast,target);
                for(int tick=0;tick<300;tick++) beast.aiStep();
                helper.assertTrue(beast.breathTimer()==-300&&!beast.isBreathing()&&target.getHealth()==200,"Breath starts before source 300-tick delay");
                beast.aiStep();helper.assertTrue(beast.isBreathing()&&beast.breathTimer()==99&&target.hasEffect(MobEffects.BLINDNESS)
                    &&target.getEffect(MobEffects.BLINDNESS).getDuration()==200&&target.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==1,"Real first breath/ray/direct status missing");
                var saved=new CompoundTag();beast.addAdditionalSaveData(saved);var restored=ShadowFamilyContent.BEAST.get().create(fixture.level);
                try {restored.readAdditionalSaveData(saved);helper.assertTrue(restored.breathTimer()==99&&restored.isBreathing(),"Saved phase not restored");
                    saved.putInt("BreathTimer",Integer.MIN_VALUE);restored.readAdditionalSaveData(saved);helper.assertTrue(restored.breathTimer()==-300,"Malformed timer not saturated");
                } finally {restored.discard();}
                for(int tick=0;tick<100;tick++) beast.aiStep();helper.assertTrue(beast.breathTimer()==-1&&!beast.isBreathing(),"Native breath duration exceeds 100 ticks");
                target.removeAllEffects();target.invulnerableTime=0;float health=target.getHealth();
                target.moveTo(beast.position().add(0,0,6));aim(beast,target);
                helper.assertTrue(!beast.breathHit(target,true)&&target.getHealth()==health&&!target.hasEffect(MobEffects.BLINDNESS),"Far breath submitted negative damage");
                helper.succeed();
            } finally {beast.discard();target.discard();}
        }
    }
    @GameTest(template="empty",batch="shadow_breath_safety")
    public static void WallNearestHitAbsoluteDamageAndNativeVetoAreAuthoritative(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var beast=create(Kind.BEAST,fixture.level,fixture.node.getBottomCenter().add(0,0,-3));var target=cow(fixture.level,fixture.node.getBottomCenter());
            Consumer<LivingIncomingDamageEvent> veto=event->{if(event.getEntity()==target&&event.getSource().is(ShadowFamily.SHADOW))event.setCanceled(true);};
            try {
                aim(beast,target);helper.assertTrue(beast.breathTarget(12)==target,"Real ray misses nearest visible entity");
                var wall=fixture.node.offset(0,1,-1);fixture.level.setBlock(wall,Blocks.STONE.defaultBlockState(),2);
                fixture.level.setBlock(wall.above(),Blocks.STONE.defaultBlockState(),2);
                helper.assertTrue(beast.breathTarget(12)==null&&!beast.breathHit(target,true),"Shadow hits through actual opaque wall");
                fixture.level.setBlock(wall,Blocks.AIR.defaultBlockState(),2);fixture.level.setBlock(wall.above(),Blocks.AIR.defaultBlockState(),2);
                target.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.NETHERITE_CHESTPLATE));target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,4));
                NeoForge.EVENT_BUS.addListener(veto);helper.assertTrue(!beast.breathHit(target,true)&&target.getHealth()==200&&!target.hasEffect(MobEffects.BLINDNESS),"Vetoed native hit applied damage/effects");
                NeoForge.EVENT_BUS.unregister(veto);target.invulnerableTime=0;
                helper.assertTrue(beast.breathHit(target,true)&&Math.abs(target.getHealth()-198.5)<.001,"Native absolute source breath is reduced by armour/effects");
                helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(veto);beast.discard();target.discard();}
        }
    }
    @GameTest(template="empty",batch="shadow_conversion_safety")
    public static void NativeDeathConversionVetoSizeAndIdentityAreReal(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var target=cow(fixture.level,fixture.node.getBottomCenter());
            var replacements=new ArrayList<Mob>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&ShadowFamily.isShadow(event.getEntity())
                    &&event.getEntity().position().distanceTo(target.position())<4) replacements.add((Mob)event.getEntity());};
            Consumer<EntityJoinLevelEvent> veto=event->{if(event.getLevel()==fixture.level&&event.getEntity().getType()==ShadowFamilyContent.MONSTER.get())event.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(capture);NeoForge.EVENT_BUS.addListener(veto);
            try {
                target.setCustomName(net.minecraft.network.chat.Component.literal("source-audit"));target.setHealth(0);
                helper.assertTrue(!ShadowFamily.convertDeath(target)&&!target.isRemoved(),"Refused replacement discarded original entity");
                NeoForge.EVENT_BUS.unregister(veto);replacements.clear();
                helper.assertTrue(ShadowFamily.convertDeath(target)&&target.isRemoved()&&replacements.size()==1&&replacements.getFirst().getType()==ShadowFamilyContent.MONSTER.get()
                    &&replacements.getFirst().getName().getString().equals("source-audit")&&replacements.getFirst().isPersistenceRequired(),"Native replacement loses source form/name/persistence");
                var small=EntityType.CHICKEN.create(fixture.level);var tall=EntityType.IRON_GOLEM.create(fixture.level);var ghoul=GraveyardContent.GHOUL.get().create(fixture.level);
                try {helper.assertTrue(ShadowFamily.deathForm(small)==AbyssalPort.SHADOW_CREATURE.get()&&ShadowFamily.deathForm(tall)==ShadowFamilyContent.BEAST.get()
                    &&ShadowFamily.deathForm(ghoul)==GraveyardContent.SHADOW_GHOUL.get(),"Source height/Ghoul mapping is wrong");} finally {small.discard();tall.discard();ghoul.discard();}
                helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(capture);NeoForge.EVENT_BUS.unregister(veto);target.discard();replacements.forEach(Entity::discard);}
        }
    }
    @GameTest(template="empty",batch="shadow_death_native_hooks")
    public static void ActualDeathAndConversionEventsKeepNativePlayerRulesAndRefusedDeaths(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var beast=create(Kind.BEAST,fixture.level,fixture.node.getBottomCenter().add(3,0,0));
            var replacements=new ArrayList<Mob>();
            Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&ShadowFamily.isShadow(event.getEntity())
                &&event.getEntity()!=beast&&event.getEntity().position().distanceTo(fixture.node.getCenter())<5)replacements.add((Mob)event.getEntity());};
            Consumer<LivingDeathEvent> deathVeto=event->{if(event.getEntity().getName().getString().equals("death-veto"))event.setCanceled(true);};
            Consumer<LivingConversionEvent.Pre> conversionVeto=event->{if(event.getEntity().getName().getString().equals("conversion-veto"))event.setCanceled(true);};
            Consumer<FinalizeSpawnEvent> spawnVeto=event->{if(event.getEntity().getType()==ShadowFamilyContent.MONSTER.get())event.setSpawnCancelled(true);};
            NeoForge.EVENT_BUS.addListener(capture);NeoForge.EVENT_BUS.addListener(deathVeto);NeoForge.EVENT_BUS.addListener(conversionVeto);
            var victims=new ArrayList<LivingEntity>();ServerPlayer player=null;
            try {
                for(String name:new String[]{"death-veto","conversion-veto","spawn-veto","accepted-death"}) {
                    var target=cow(fixture.level,fixture.node.getBottomCenter());victims.add(target);
                    target.setCustomName(net.minecraft.network.chat.Component.literal(name));
                    // Select an actual native random seed that reaches the source's 50-percent branch.
                    for(long seed=0;;seed++) {target.getRandom().setSeed(seed);if(target.getRandom().nextBoolean()){target.getRandom().setSeed(seed);break;}}
                    if(name.equals("spawn-veto"))NeoForge.EVENT_BUS.addListener(spawnVeto);
                    target.setHealth(0);target.die(ShadowFamily.damage(beast));
                    if(name.equals("spawn-veto"))NeoForge.EVENT_BUS.unregister(spawnVeto);
                    if(name.equals("accepted-death")) helper.assertTrue(target.isRemoved()&&replacements.size()==1,"Accepted native death did not transform once");
                    else helper.assertTrue(!target.isRemoved()&&replacements.isEmpty(),"Native death/conversion/finalization veto was bypassed: "+name);
                }
                replacements.forEach(Entity::discard);replacements.clear();
                var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"shadow-player"),false);
                player=new ServerPlayer(fixture.level.getServer(),fixture.level,cookie.gameProfile(),cookie.clientInformation());
                var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
                fixture.level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
                player.teleportTo(fixture.level,fixture.node.getX()+.5,fixture.node.getY(),fixture.node.getZ()+.5,0,0);
                helper.assertTrue(!player.isCreative()&&!player.isSpectator(),"Real native player fixture is not Survival");
                player.setHealth(0);player.die(ShadowFamily.damage(beast));
                helper.assertTrue(player.isDeadOrDying()&&!player.isRemoved()&&replacements.size()==1&&replacements.getFirst().getType()==ShadowFamilyContent.MONSTER.get()
                    &&replacements.getFirst().isPersistenceRequired()&&replacements.getFirst().getName().getString().equals("shadow-player"),"Actual player death lost native player or source named persistent Shadow Monster");
                helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(capture);NeoForge.EVENT_BUS.unregister(deathVeto);NeoForge.EVENT_BUS.unregister(conversionVeto);NeoForge.EVENT_BUS.unregister(spawnVeto);
                victims.forEach(Entity::discard);replacements.forEach(Entity::discard);beast.discard();if(player!=null)fixture.level.getServer().getPlayerList().remove(player);}
        }
    }
    @GameTest(template="empty",batch="shadow_loot_rending")
    public static void ActualDeathLootAndRendingWorkForBothNewForms(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var player=helper.makeMockServerPlayerInLevel();var prior=player.position();player.moveTo(fixture.node.getBottomCenter().add(0,0,-2));
            var drops=new ArrayList<ItemEntity>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getEntity() instanceof ItemEntity item&&item.position().distanceTo(fixture.node.getCenter())<5)drops.add(item);};
            NeoForge.EVENT_BUS.addListener(capture);
            try {for(Kind kind:new Kind[]{Kind.MONSTER,Kind.BEAST}) {
                int total=0;for(int sample=0;sample<12;sample++) {var mob=create(kind,fixture.level,fixture.node.getBottomCenter());
                    var staff=new ItemStack(WastelandProgressionContent.RENDING_STAFF.get());player.getCooldowns().removeCooldown(staff.getItem());
                    helper.assertTrue(RendingStaffItem.drain(staff,player,mob)&&RendingStaffItem.energy(staff,RendingStaffItem.Channel.SHADOW)==1,"New native form is not an actual Shadow rending carrier");
                    int before=drops.size();mob.hurt(mob.damageSources().genericKill(),1000);for(var drop:drops.subList(before,drops.size())) {
                        helper.assertTrue(drop.getItem().is(kind==Kind.MONSTER?AbyssalPort.SHADOW_SHARD.get():AbyssalPort.SHADOW_GEM.get()),"Native death emitted unrelated loot");total+=drop.getItem().getCount();
                    }mob.discard();
                }helper.assertTrue(total>0&&total<=24,"Twelve real deaths do not use 0–2 source loot: "+total);
            }helper.succeed();} finally {NeoForge.EVENT_BUS.unregister(capture);drops.forEach(Entity::discard);player.moveTo(prior);player.getInventory().clearContent();}
        }
    }
    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="shadow_real_world",timeoutTicks=900)
    public static void BothNativeIdPhasesActuallyWarmUpAndBreatheInNormalWorld(GameTestHelper helper) {
        var level=helper.getLevel();var base=new BlockPos(600208,300,600208);
        var saved=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();var forced=new ArrayList<ChunkPos>();
        var entities=new ArrayList<Entity>();var beasts=new ArrayList<ShadowCreatureEntity>();var targets=new ArrayList<LivingEntity>();
        for(int x=(base.getX()-6)>>4;x<=(base.getX()+6)>>4;x++)for(int z=(base.getZ()-6)>>4;z<=(base.getZ()+6)>>4;z++) {
            var chunk=new ChunkPos(x,z);if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(x,z,true);forced.add(chunk);}}
        for(var pos:BlockPos.betweenClosed(base.offset(-6,-1,-6),base.offset(6,4,6))) {
            helper.assertTrue(level.getBlockEntity(pos)==null,"Own audit overlaps existing block entity");saved.put(pos.immutable(),level.getBlockState(pos));
            level.setBlock(pos,pos.getY()==299?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        }
        var explorer=helper.makeMockServerPlayerInLevel();var old=explorer.position();explorer.moveTo(base.getBottomCenter().add(5,0,0));explorer.setNoGravity(true);
        for(int i=0;i<2;i++) {
            var beast=create(Kind.BEAST,level,base.getBottomCenter().add(i*4-2,0,-3));entities.add(beast);
            // Allocate a non-added probe if necessary to cover both native id parities, not tickCount guesses.
            if(i==1&&(beast.getId()&1)==(beasts.getFirst().getId()&1)) {beast.discard();beast=create(Kind.BEAST,level,base.getBottomCenter().add(i*4-2,0,-3));entities.add(beast);}
            var target=cow(level,base.getBottomCenter().add(i*4-2,0,0));entities.add(target);beast.setTarget(target);aim(beast,target);beasts.add(beast);targets.add(target);
        }
        Runnable[] audit = new Runnable[1];
        audit[0] = () -> {
            // Forced-chunk readiness can precede actual entity ticking by dozens of world ticks.
            // Measure product ticks, don't lower the native 300-tick warmup or assume immediate activation.
            if (beasts.stream().anyMatch(mob -> mob.tickCount < 330) && helper.getTick() < 800) {
                helper.runAfterDelay(10,()->audit[0].run()); return;
            }
            try {
                for(int i=0;i<2;i++) helper.assertTrue(beasts.get(i).tickCount>=330&&beasts.get(i).isBreathing()&&targets.get(i).getHealth()<200
                    &&targets.get(i).hasEffect(MobEffects.BLINDNESS),"Registered normal-world ticks fail Shadow breath: "+beasts.get(i).getId()+"/"+beasts.get(i).tickCount+"/"+beasts.get(i).breathTimer()+"/"+targets.get(i).getHealth());
                var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
                for(String name:new String[]{"darklands","darklands_plains","darklands_forest","darklands_highlands","darklands_mountains"}) {
                    var biome=registry.get(WastelandContent.id(name));var spawns=biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
                    for(Kind kind:Kind.values()) helper.assertTrue(spawns.stream().filter(entry->entry.type==ShadowFamilyContent.type(kind)).count()==1,"Loaded native biome lacks/excess family entry");
                }
                LOGGER.info("NATIVE SHADOW FAMILY: ids={}, actual ticks={}, breath timers={}, real target health={}",beasts.stream().map(Entity::getId).toList(),
                    beasts.stream().map(mob->mob.tickCount).toList(),beasts.stream().map(ShadowCreatureEntity::breathTimer).toList(),targets.stream().map(LivingEntity::getHealth).toList());
                helper.succeed();
            } finally {entities.forEach(Entity::discard);explorer.moveTo(old);explorer.setNoGravity(false);saved.forEach((pos,state)->level.setBlock(pos,state,2));forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false));}
        };
        helper.runAfterDelay(340,audit[0]);
    }
}
