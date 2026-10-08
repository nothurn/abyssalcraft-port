package dev.abyssalport;

import dev.abyssalport.config.MobEcologyConfig;
import dev.abyssalport.content.*;
import dev.abyssalport.entity.*;
import dev.abyssalport.entity.CorruptedAnimalEntity.Kind;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AnimalEcologyGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private AnimalEcologyGameTests(){}
    private static CorruptedAnimalEntity create(Kind kind,boolean demon,ServerLevel level,net.minecraft.world.phys.Vec3 pos){var mob=MobEcologyContent.type(kind,demon).create(level);mob.moveTo(pos);mob.setNoAi(true);mob.setPersistenceRequired();level.addFreshEntity(mob);return mob;}
    private static ServerPlayer player(ServerLevel level,net.minecraft.world.phys.Vec3 pos){var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ecology-player"),false);
        var player=new ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();player.teleportTo(level,pos.x,pos.y,pos.z,0,0);player.setNoGravity(true);return player;}
    private static List<CorruptedAnimalEntity> nearby(GraveyardGameTests.Fixture fixture){return fixture.level.getEntitiesOfClass(CorruptedAnimalEntity.class,new net.minecraft.world.phys.AABB(fixture.node).inflate(5));}
    @GameTest(template="empty",batch="animal_attributes")
    public static void NineNativeTypesAndHardcoreConfigKeepSourceStatsImmunitiesAndNames(GameTestHelper helper){
        helper.assertTrue(MobEcologyConfig.SPEC.isLoaded(),"Common config was not registered/loaded by the real mod container");boolean hardcore=MobEcologyConfig.HARDCORE.get();
        try{for(boolean hard:new boolean[]{false,true}){MobEcologyConfig.HARDCORE.set(hard);
            for(boolean demon:new boolean[]{false,true})for(Kind kind:Kind.values()){var mob=MobEcologyContent.type(kind,demon).create(helper.getLevel());
                try{helper.assertTrue(mob.getMaxHealth()==CorruptedAnimalEntity.baseHealth(kind)*(hard?2:1)&&mob.getAttributeValue(Attributes.ATTACK_DAMAGE)==(hard?4:2)
                    &&mob.getAttributeValue(Attributes.MOVEMENT_SPEED)==.25&&mob.fireImmune()&&!mob.canBreatheUnderwater(),"Native source animal stats/classification differ");
                    helper.assertTrue(mob.canBeAffected(new MobEffectInstance(DreadlandsContent.PLAGUE,100))!=demon&&mob.canBeAffected(new MobEffectInstance(MobEffects.POISON,100)),"Demon plague immunity became general potion immunity");
                    if(!demon)helper.assertTrue(mob.getName().equals(net.minecraft.network.chat.Component.translatable("entity.minecraft."+kind.name().toLowerCase(Locale.ROOT))),"Evil disguise name missing");
                    helper.assertTrue(!mob.causeFallDamage(10,1,mob.damageSources().fall())==(kind==Kind.CHICKEN),"Chicken fall immunity lost or extended to all species");
                }finally{mob.discard();}}
            var shub=MobEcologyContent.OFFSPRING.get().create(helper.getLevel());try{helper.assertTrue(shub.getMaxHealth()==(hard?80:40)&&shub.getAttributeValue(Attributes.ATTACK_DAMAGE)==(hard?8:4)&&!shub.fireImmune(),"Native source offspring stats wrong");}finally{shub.discard();}
        }helper.succeed();}finally{MobEcologyConfig.HARDCORE.set(hardcore);}
    }
    @GameTest(template="empty",batch="animal_real_shearing")
    public static void RealSurvivalShearsTransformFourFormsOnceWithActualSpeciesDropsAndOneCost(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        var player=player(fixture.level,fixture.node.getBottomCenter().add(3,0,0));var drops=new ArrayList<ItemEntity>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&event.getEntity() instanceof ItemEntity item&&item.position().distanceTo(fixture.node.getCenter())<5)drops.add(item);};NeoForge.EVENT_BUS.addListener(capture);
        try{for(Kind kind:Kind.values()){var mob=create(kind,false,fixture.level,fixture.node.getBottomCenter());var tool=new ItemStack(Items.SHEARS);player.setItemInHand(InteractionHand.MAIN_HAND,tool);int before=drops.size();
            var result=player.interactOn(mob,InteractionHand.MAIN_HAND);var replacements=nearby(fixture).stream().filter(CorruptedAnimalEntity::demon).toList();
            helper.assertTrue(result.consumesAction()&&mob.isRemoved()&&replacements.size()==1&&replacements.getFirst().kind()==kind&&tool.getDamageValue()==1,"Actual shears did not transform once/pay exactly once: "+kind);
            helper.assertTrue(drops.size()==before+1&&drops.getLast().getItem().is(mob.shearingDrop())&&drops.getLast().getItem().getCount()>=1&&drops.getLast().getItem().getCount()<=3,"Species shearing loot wrong");
            player.interactOn(replacements.getFirst(),InteractionHand.MAIN_HAND);helper.assertTrue(tool.getDamageValue()==1&&drops.size()==before+1,"Demon sheared a second time");replacements.forEach(Entity::discard);drops.forEach(Entity::discard);
        }helper.succeed();}finally{NeoForge.EVENT_BUS.unregister(capture);drops.forEach(Entity::discard);nearby(fixture).forEach(Entity::discard);fixture.level.getServer().getPlayerList().remove(player);}
    }}
    @GameTest(template="empty",batch="animal_shearing_veto")
    public static void ConversionFinalizeAndJoinVetosRetainVictimAndDoNotSpendShearsOrDrops(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        var player=player(fixture.level,fixture.node.getBottomCenter().add(3,0,0));var mob=create(Kind.COW,false,fixture.level,fixture.node.getBottomCenter());var tool=new ItemStack(Items.SHEARS);player.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var drops=new ArrayList<ItemEntity>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&event.getEntity() instanceof ItemEntity item&&item.position().distanceTo(mob.position())<4)drops.add(item);};
        Consumer<LivingConversionEvent.Pre> conversion=event->{if(event.getEntity()==mob)event.setCanceled(true);};Consumer<FinalizeSpawnEvent> finalize=event->{if(event.getEntity().getType()==MobEcologyContent.type(Kind.COW,true))event.setSpawnCancelled(true);};
        Consumer<EntityJoinLevelEvent> join=event->{if(event.getEntity().getType()==MobEcologyContent.type(Kind.COW,true))event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(capture);
        try{NeoForge.EVENT_BUS.addListener(conversion);player.interactOn(mob,InteractionHand.MAIN_HAND);NeoForge.EVENT_BUS.unregister(conversion);checkVeto(helper,fixture,mob,tool,drops);
            NeoForge.EVENT_BUS.addListener(finalize);player.interactOn(mob,InteractionHand.MAIN_HAND);NeoForge.EVENT_BUS.unregister(finalize);checkVeto(helper,fixture,mob,tool,drops);
            NeoForge.EVENT_BUS.addListener(join);player.interactOn(mob,InteractionHand.MAIN_HAND);NeoForge.EVENT_BUS.unregister(join);checkVeto(helper,fixture,mob,tool,drops);
            fixture.level.setBlock(fixture.node,Blocks.STONE.defaultBlockState(),2);player.interactOn(mob,InteractionHand.MAIN_HAND);checkVeto(helper,fixture,mob,tool,drops);helper.succeed();
        }finally{NeoForge.EVENT_BUS.unregister(capture);NeoForge.EVENT_BUS.unregister(conversion);NeoForge.EVENT_BUS.unregister(finalize);NeoForge.EVENT_BUS.unregister(join);drops.forEach(Entity::discard);mob.discard();nearby(fixture).forEach(Entity::discard);fixture.level.getServer().getPlayerList().remove(player);}
    }}
    private static void checkVeto(GameTestHelper helper,GraveyardGameTests.Fixture fixture,CorruptedAnimalEntity mob,ItemStack tool,List<ItemEntity> drops){helper.assertTrue(mob.isAlive()&&!mob.isRemoved()&&tool.getDamageValue()==0&&drops.isEmpty()&&nearby(fixture).stream().noneMatch(CorruptedAnimalEntity::demon),"Refused real native shearing spent drops/tool or removed victim");}
    @GameTest(template="empty",batch="animal_conversion_reentry")
    public static void NativeConversionCallbackCannotReenterShearingAndDoubleAdmit(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        var mob=create(Kind.COW,false,fixture.level,fixture.node.getBottomCenter());int[] calls={0};
        Consumer<LivingConversionEvent.Pre> recurse=event->{if(event.getEntity()==mob){calls[0]++;helper.assertTrue(!mob.transform(false)&&mob.onSheared(null,new ItemStack(Items.SHEARS),fixture.level,fixture.node).isEmpty(),"Native pre-conversion callback reentered live shearing");}};
        NeoForge.EVENT_BUS.addListener(recurse);try{helper.assertTrue(mob.transform(false)&&calls[0]==1&&mob.isRemoved()&&nearby(fixture).stream().filter(CorruptedAnimalEntity::demon).count()==1,"Reentrant native conversion admits duplicate Demon phase");helper.succeed();}
        finally{NeoForge.EVENT_BUS.unregister(recurse);mob.discard();nearby(fixture).forEach(Entity::discard);}
    }}
    @GameTest(template="empty",batch="animal_death_phase")
    public static void RealAcceptedDeathCreatesOnlyMatchingDemonAndConfigAndShoggothDeathsDoNot(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        boolean value=MobEcologyConfig.DEMON_ON_DEATH.get();var victims=new ArrayList<Entity>();var drops=new ArrayList<ItemEntity>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&event.getEntity() instanceof ItemEntity item&&item.position().distanceTo(fixture.node.getCenter())<6)drops.add(item);};NeoForge.EVENT_BUS.addListener(capture);
        try{MobEcologyConfig.DEMON_ON_DEATH.set(true);for(Kind kind:Kind.values()){var mob=create(kind,false,fixture.level,fixture.node.getBottomCenter());victims.add(mob);mob.hurt(mob.damageSources().genericKill(),1000);
                var demons=nearby(fixture).stream().filter(CorruptedAnimalEntity::demon).toList();helper.assertTrue(mob.isRemoved()&&demons.size()==1&&demons.getFirst().kind()==kind,"Real accepted Evil death is not a single matching Demon phase: "+kind);demons.forEach(Entity::discard);}
            MobEcologyConfig.DEMON_ON_DEATH.set(false);var disabled=create(Kind.COW,false,fixture.level,fixture.node.getBottomCenter());victims.add(disabled);disabled.hurt(disabled.damageSources().genericKill(),1000);
            helper.assertTrue(nearby(fixture).stream().noneMatch(CorruptedAnimalEntity::demon),"Disabled death conversion still creates Demon");disabled.discard();
            MobEcologyConfig.DEMON_ON_DEATH.set(true);var food=create(Kind.PIG,false,fixture.level,fixture.node.getBottomCenter());victims.add(food);var shoggoth=AbyssalPort.LESSER_SHOGGOTH.get().create(fixture.level);victims.add(shoggoth);shoggoth.moveTo(fixture.node.getBottomCenter().add(4,0,0));shoggoth.setNoAi(true);fixture.level.addFreshEntity(shoggoth);
            food.hurt(shoggoth.damageSources().mobAttack(shoggoth),1000);helper.assertTrue(nearby(fixture).stream().noneMatch(CorruptedAnimalEntity::demon),"Shoggoth food death incorrectly respawns a Demon");helper.succeed();
        }finally{MobEcologyConfig.DEMON_ON_DEATH.set(value);NeoForge.EVENT_BUS.unregister(capture);victims.forEach(Entity::discard);nearby(fixture).forEach(Entity::discard);drops.forEach(Entity::discard);}
    }}
    @GameTest(template="empty",batch="animal_death_veto")
    public static void ActualDeathVetoAndShadowDeathNeverAdmitDoubleReplacement(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        var mob=create(Kind.SHEEP,false,fixture.level,fixture.node.getBottomCenter());Consumer<LivingDeathEvent> veto=event->{if(event.getEntity()==mob)event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(veto);
        try{mob.hurt(mob.damageSources().genericKill(),1000);helper.assertTrue(!mob.isRemoved()&&nearby(fixture).stream().noneMatch(CorruptedAnimalEntity::demon),"Cancelled death creates Demon phase");}
        finally{NeoForge.EVENT_BUS.unregister(veto);mob.discard();}
        var shadows=new ArrayList<Entity>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&ShadowFamily.isShadow(event.getEntity())&&event.getEntity().position().distanceTo(fixture.node.getCenter())<5)shadows.add(event.getEntity());};NeoForge.EVENT_BUS.addListener(capture);
        var attacker=ShadowFamilyContent.MONSTER.get().create(fixture.level);
        try{var target=create(Kind.COW,false,fixture.level,fixture.node.getBottomCenter());target.hurt(ShadowFamily.damage(attacker),1000);
            helper.assertTrue(target.isRemoved()&&nearby(fixture).stream().filter(CorruptedAnimalEntity::demon).count()==1&&shadows.isEmpty(),"Single Evil death creates both Demon and Shadow replacements");helper.succeed();}
        finally{NeoForge.EVENT_BUS.unregister(capture);attacker.discard();shadows.forEach(Entity::discard);nearby(fixture).forEach(Entity::discard);fixture.level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(fixture.node).inflate(5)).forEach(Entity::discard);}
    }}
    @GameTest(template="empty",batch="animal_killed_player")
    public static void NativePlayerDeathIdentityTextureSaveAndFleshShearingUseActualEvents(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        var sheep=create(Kind.SHEEP,false,fixture.level,fixture.node.getBottomCenter());var player=player(fixture.level,fixture.node.getBottomCenter().add(3,0,0));var restored=MobEcologyContent.type(Kind.SHEEP,false).create(fixture.level);
        try{player.getGameProfile().getProperties().put("textures",new com.mojang.authlib.properties.Property("textures","fixture-packed-texture","fixture-signature"));player.setHealth(0);player.die(sheep.damageSources().mobAttack(sheep));
            helper.assertTrue(sheep.killedPlayer().orElseThrow().equals(player.getUUID())&&sheep.killedPlayerName().equals("ecology-player")&&sheep.shearingDrop()==Items.ROTTEN_FLESH,"Native Evil sheep death hook lacks actual player identity/drop");
            var tag=new CompoundTag();sheep.addAdditionalSaveData(tag);restored.readAdditionalSaveData(tag);helper.assertTrue(restored.killedProfile().getProperties().get("textures").iterator().next().value().equals("fixture-packed-texture")&&restored.shearingDrop()==Items.ROTTEN_FLESH,"Persisted player skin/drop identity lost");
            var drops=sheep.onSheared(null,new ItemStack(Items.SHEARS),fixture.level,fixture.node);helper.assertTrue(drops.size()==1&&drops.getFirst().is(Items.ROTTEN_FLESH)&&sheep.isRemoved(),"Actual killed-player sheep shears ordinary wool");helper.succeed();
        }finally{restored.discard();sheep.discard();nearby(fixture).forEach(Entity::discard);fixture.level.getServer().getPlayerList().remove(player);}
    }}
    @GameTest(template="empty",batch="animal_fire_veto")
    public static void ActualMimicTrailHonorsMobGriefingNativePlacementVetoAndVanillaFireOption(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        var demon=create(Kind.COW,true,fixture.level,fixture.node.getBottomCenter().add(3,0,0));demon.setCanBurn(true);boolean grief=fixture.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING),mimic=MobEcologyConfig.MIMIC_FIRE.get();
        Consumer<BlockEvent.EntityPlaceEvent> veto=event->{if(event.getEntity()==demon){helper.assertTrue(event.getPlacedBlock().is(net.minecraft.tags.BlockTags.FIRE),"Native non-player placement reports old air instead of actual fire");event.setCanceled(true);}};
        try{fixture.level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,fixture.level.getServer());helper.assertTrue(!DemonFire.place(demon,fixture.node)&&fixture.level.isEmptyBlock(fixture.node),"Disabled griefing admits fire");
            fixture.level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,fixture.level.getServer());NeoForge.EVENT_BUS.addListener(veto);helper.assertTrue(!DemonFire.place(demon,fixture.node)&&fixture.level.isEmptyBlock(fixture.node),"Native fire placement veto leaves a block behind");NeoForge.EVENT_BUS.unregister(veto);
            MobEcologyConfig.MIMIC_FIRE.set(true);helper.assertTrue(DemonFire.place(demon,fixture.node)&&fixture.level.getBlockState(fixture.node).is(MobEcologyContent.MIMIC_FIRE.get()),"Mimic option not actual placed block");fixture.level.setBlock(fixture.node,Blocks.AIR.defaultBlockState(),2);
            MobEcologyConfig.MIMIC_FIRE.set(false);helper.assertTrue(DemonFire.place(demon,fixture.node)&&fixture.level.getBlockState(fixture.node).is(Blocks.FIRE),"Normal fire option ignored");helper.succeed();
        }finally{NeoForge.EVENT_BUS.unregister(veto);fixture.level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,fixture.level.getServer());MobEcologyConfig.MIMIC_FIRE.set(mimic);demon.discard();}
    }}
    @GameTest(template="empty",batch="animal_fire_activation")
    public static void OverworldFireDefaultsSaveIgnitionAndWaterExtinguishingRemainDistinct(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        boolean fire=MobEcologyConfig.DEMON_FIRE.get(),hard=MobEcologyConfig.HARDCORE.get();var demon=create(Kind.PIG,true,fixture.level,fixture.node.getBottomCenter());
        try{MobEcologyConfig.HARDCORE.set(false);MobEcologyConfig.DEMON_FIRE.set(false);demon.igniteForSeconds(3);helper.assertTrue(!demon.canBurn(),"Overworld default unexpectedly starts Demon trails");
            MobEcologyConfig.DEMON_FIRE.set(true);demon.igniteForSeconds(3);helper.assertTrue(demon.canBurn(),"Native ignition does not set enabled Demon burn state");
            var saved=new CompoundTag();demon.addAdditionalSaveData(saved);var copy=MobEcologyContent.type(Kind.PIG,true).create(fixture.level);try{copy.readAdditionalSaveData(saved);helper.assertTrue(copy.canBurn(),"Saved source CanBurn lost");}finally{copy.discard();}
            fixture.level.setBlock(fixture.node,Blocks.WATER.defaultBlockState(),2);demon.tick();helper.assertTrue(!demon.canBurn(),"Actual water does not extinguish Demon state");helper.succeed();
        }finally{MobEcologyConfig.DEMON_FIRE.set(fire);MobEcologyConfig.HARDCORE.set(hard);demon.discard();}
    }}
    @GameTest(template="empty",batch="animal_moon")
    public static void ActualMoonNightAltitudeAndConfigSpawnGatesMatchSource(GameTestHelper helper){var level=helper.getLevel();long day=level.getDayTime();boolean moon=MobEcologyConfig.EVIL_NEW_MOON.get();var pos=helper.absolutePos(new BlockPos(3,4,3));
        try{MobEcologyConfig.EVIL_NEW_MOON.set(true);level.setDayTime(18000);level.updateSkyBrightness();helper.assertTrue(!CorruptedAnimalEntity.checkSpawn(MobEcologyContent.type(Kind.COW,false),level,MobSpawnType.NATURAL,pos,net.minecraft.util.RandomSource.create(1)),"Full-moon Evil spawn gate bypassed");
            helper.assertTrue(!ShubOffspringEntity.moonAllows(level,new BlockPos(0,level.getSeaLevel()-1,0),net.minecraft.util.RandomSource.create(1)),"Offspring appears below source sea level");
            level.setDayTime(4*24000+18000);level.updateSkyBrightness();helper.assertTrue(level.getMoonBrightness()==0&&ShubOffspringEntity.moonAllows(level,new BlockPos(0,level.getSeaLevel(),0),net.minecraft.util.RandomSource.create(1)),"Native new moon/sea-level offspring branch unavailable");
            level.setDayTime(4*24000+6000);level.updateSkyBrightness();helper.assertTrue(!ShubOffspringEntity.moonAllows(level,new BlockPos(0,level.getSeaLevel(),0),net.minecraft.util.RandomSource.create(1)),"Daylight offspring gate bypassed");helper.succeed();
        }finally{level.setDayTime(day);level.updateSkyBrightness();MobEcologyConfig.EVIL_NEW_MOON.set(moon);}
    }
    @GameTest(template="empty",batch="animal_native_loot")
    public static void ActualDemonDeathsUseZeroToTwoFleshAndOffspringNoInventedLoot(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        var drops=new ArrayList<ItemEntity>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&event.getEntity() instanceof ItemEntity item&&item.position().distanceTo(fixture.node.getCenter())<5)drops.add(item);};NeoForge.EVENT_BUS.addListener(capture);
        try{int total=0;for(Kind kind:Kind.values())for(int i=0;i<10;i++){var demon=create(kind,true,fixture.level,fixture.node.getBottomCenter());int before=drops.size();demon.hurt(demon.damageSources().genericKill(),1000);
                for(var drop:drops.subList(before,drops.size())){helper.assertTrue(drop.getItem().is(Items.ROTTEN_FLESH)&&drop.getItem().getCount()<=2,"Demon actual source loot changed");total+=drop.getItem().getCount();}demon.discard();}
            helper.assertTrue(total>0&&total<=80,"Forty real Demon deaths have no actual 0–2 flesh loot");int before=drops.size();var shub=MobEcologyContent.OFFSPRING.get().create(fixture.level);shub.moveTo(fixture.node.getBottomCenter());fixture.level.addFreshEntity(shub);shub.hurt(shub.damageSources().genericKill(),1000);shub.discard();helper.assertTrue(drops.size()==before,"Offspring invented item loot");helper.succeed();
        }finally{NeoForge.EVENT_BUS.unregister(capture);drops.forEach(Entity::discard);nearby(fixture).forEach(Entity::discard);}
    }}
    @GameTest(template="empty",batch="animal_native_spawn_config")
    public static void RealNaturalSpawnPredicateHonorsNewMoonAndZeroWeightWithoutBypassingVanilla(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        int weight=MobEcologyConfig.EVIL_WEIGHT.get();boolean moon=MobEcologyConfig.EVIL_NEW_MOON.get();
        try{MobEcologyConfig.EVIL_WEIGHT.set(15);MobEcologyConfig.EVIL_NEW_MOON.set(true);fixture.level.setDayTime(4*24000+18000);fixture.level.updateSkyBrightness();int accepted=0;
            for(int seed=0;seed<64;seed++){var type=MobEcologyContent.type(Kind.COW,false);boolean nativeAllowed=net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type,fixture.level,MobSpawnType.NATURAL,fixture.node,net.minecraft.util.RandomSource.create(seed));
                boolean actual=CorruptedAnimalEntity.checkSpawn(type,fixture.level,MobSpawnType.NATURAL,fixture.node,net.minecraft.util.RandomSource.create(seed));helper.assertTrue(actual==nativeAllowed,"Actual new-moon animal bypasses native difficulty/ground/light rules");if(actual)accepted++;}
            helper.assertTrue(accepted>0,"Actual registered predicate has no reachable natural new-moon spawn in a valid native arena");MobEcologyConfig.EVIL_WEIGHT.set(0);
            helper.assertTrue(!CorruptedAnimalEntity.checkSpawn(MobEcologyContent.type(Kind.COW,false),fixture.level,MobSpawnType.NATURAL,fixture.node,net.minecraft.util.RandomSource.create(0)),"Zero configured weight still allows a natural Evil spawn");helper.succeed();
        }finally{MobEcologyConfig.EVIL_WEIGHT.set(weight);MobEcologyConfig.EVIL_NEW_MOON.set(moon);}
    }}
    @GameTest(template="empty",batch="animal_native_fire_tick")
    public static void ActualScheduledMimicFireIsFlammableTaggedAndHonorsFireTickRule(GameTestHelper helper){try(var fixture=new GraveyardGameTests.Fixture(helper)){
        boolean ticks=fixture.level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK);var block=MobEcologyContent.MIMIC_FIRE.get();var state=block.defaultBlockState();
        try{fixture.level.setBlock(fixture.node,state,2);fixture.level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false,fixture.level.getServer());
            fixture.level.getBlockState(fixture.node).tick(fixture.level,fixture.node,net.minecraft.util.RandomSource.create(10));helper.assertTrue(fixture.level.getBlockState(fixture.node).equals(state),"Disabled fire tick mutates Mimic Fire");
            fixture.level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true,fixture.level.getServer());
            fixture.level.setBlock(fixture.node,block.defaultBlockState().setValue(FireBlock.AGE,4),2);fixture.level.getBlockState(fixture.node).tick(fixture.level,fixture.node,net.minecraft.util.RandomSource.create(10));
            helper.assertTrue(fixture.level.isEmptyBlock(fixture.node),"Mature reduced fire on stone survives without any flammable fuel");
            helper.assertTrue(state.is(net.minecraft.tags.BlockTags.FIRE)&&block.getBurnOdds(Blocks.OAK_PLANKS.defaultBlockState())>0&&block.getBurnOdds(Blocks.STONE.defaultBlockState())==0,"Native Fire class lost flammability/tag semantics");helper.succeed();
        }finally{fixture.level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(ticks,fixture.level.getServer());}
    }}
    @GameTest(template="empty",batch="animal_dread_immunity")
    public static void DemonNativeDreadCarrierAttacksAreRefusedButOrdinaryDamageIsAccepted(GameTestHelper helper){var level=helper.getLevel();var demon=MobEcologyContent.type(Kind.COW,true).create(level);var evil=MobEcologyContent.type(Kind.COW,false).create(level);var dread=DreadlandsContent.DREADLING.get().create(level);
        try{helper.assertTrue(!demon.hurt(dread.damageSources().mobAttack(dread),2)&&demon.getHealth()==demon.getMaxHealth(),"Demon accepts source Dread-carrier friendly fire");
            helper.assertTrue(evil.hurt(dread.damageSources().mobAttack(dread),2)&&demon.hurt(demon.damageSources().generic(),2),"Dread immunity incorrectly protects Evil/ordinary generic damage");helper.succeed();}
        finally{demon.discard();evil.discard();dread.discard();}
    }
    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="animal_native_biomes")
    public static void LoadedNormalWorldActuallyUsesConfigurableSourceEcologyEntries(GameTestHelper helper){var registry=helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for(Kind kind:Kind.values()){assertSpawn(helper,registry.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("plains")),MobEcologyContent.type(kind,false),15);assertSpawn(helper,registry.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("nether_wastes")),MobEcologyContent.type(kind,true),15);}
        assertSpawn(helper,registry.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("forest")),MobEcologyContent.OFFSPRING.get(),5);assertSpawn(helper,registry.get(WastelandContent.id("darklands_forest")),MobEcologyContent.OFFSPRING.get(),10);
        LOGGER.info("NATIVE ANIMAL ECOLOGY: actual Plains/Nether weights 15x4; forest offspring 5, Darklands forest 10; common config loaded={}",MobEcologyConfig.SPEC.isLoaded());helper.succeed();
    }
    private static void assertSpawn(GameTestHelper helper,net.minecraft.world.level.biome.Biome biome,EntityType<?> type,int weight){var found=biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().filter(entry->entry.type==type).toList();helper.assertTrue(found.size()==1&&found.getFirst().getWeight().asInt()==weight&&found.getFirst().minCount==1&&found.getFirst().maxCount==3,"Native actual modifier missing/duplicate/wrong: "+type+" / "+found);}
    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="animal_real_native_ai",timeoutTicks=500)
    public static void RealNormalWorldTicksNavigateAndMeleeWithoutDirectAttackCalls(GameTestHelper helper){var level=helper.getLevel();var base=new BlockPos(600512,300,600512);var forced=new ArrayList<ChunkPos>();var saved=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();var players=new ArrayList<ServerPlayer>();var mobs=new ArrayList<Mob>();var starts=new ArrayList<net.minecraft.world.phys.Vec3>();
        for(int x=(base.getX()-3)>>4;x<=(base.getX()+19)>>4;x++)for(int z=(base.getZ()-8)>>4;z<=(base.getZ()+5)>>4;z++){var chunk=new ChunkPos(x,z);if(!level.getForcedChunks().contains(chunk.toLong())){level.setChunkForced(x,z,true);forced.add(chunk);}}
        for(var pos:BlockPos.betweenClosed(base.offset(-3,-1,-8),base.offset(19,4,5))){helper.assertTrue(level.getBlockEntity(pos)==null,"Native ecology arena overlaps real block entity");saved.put(pos.immutable(),level.getBlockState(pos));level.setBlock(pos,pos.getY()==299?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);}
        var difficulty=level.getDifficulty();level.getServer().setDifficulty(Difficulty.NORMAL,true);
        for(int i=0;i<3;i++){var player=player(level,base.getBottomCenter().add(i*8,0,2));player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);player.setHealth(200);player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);players.add(player);
            Mob mob=i==2?MobEcologyContent.OFFSPRING.get().create(level):MobEcologyContent.type(i==0?Kind.COW:Kind.SHEEP,i==1).create(level);var start=base.getBottomCenter().add(i*8,0,-4);mob.moveTo(start);mob.setPersistenceRequired();mob.setTarget(player);level.addFreshEntity(mob);mobs.add(mob);starts.add(start);}
        Runnable[] audit=new Runnable[1];audit[0]=()->{boolean ready=java.util.stream.IntStream.range(0,3).allMatch(i->mobs.get(i).tickCount>=180&&mobs.get(i).position().distanceTo(starts.get(i))>3&&players.get(i).getHealth()<200);
            if(!ready&&helper.getTick()<430){
                // GameTestInfo keys schedules by Runnable identity and removes the running entry.
                // Re-enqueuing that exact instance would silently delete its future schedule.
                helper.runAfterDelay(10,()->audit[0].run());return;}
            try{for(int i=0;i<3;i++)helper.assertTrue(!mobs.get(i).isNoAi()&&mobs.get(i).tickCount>=180&&mobs.get(i).position().distanceTo(starts.get(i))>3&&players.get(i).getHealth()<200,
                "Actual native ecology navigation/melee failed: "+mobs.get(i).getType()+" / ticks="+mobs.get(i).tickCount+" / moved="+mobs.get(i).position().distanceTo(starts.get(i))+" / hp="+players.get(i).getHealth());
                LOGGER.info("NATIVE ANIMAL AI: real entity ticks={}, movement distances={}, actual player health={}",mobs.stream().map(mob->mob.tickCount).toList(),java.util.stream.IntStream.range(0,3).mapToObj(i->mobs.get(i).position().distanceTo(starts.get(i))).toList(),players.stream().map(LivingEntity::getHealth).toList());helper.succeed();
            }finally{mobs.forEach(Entity::discard);players.forEach(player->level.getServer().getPlayerList().remove(player));saved.forEach((pos,state)->level.setBlock(pos,state,2));forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false));level.getServer().setDifficulty(difficulty,true);}};
        helper.runAfterDelay(20,audit[0]);
    }
}
