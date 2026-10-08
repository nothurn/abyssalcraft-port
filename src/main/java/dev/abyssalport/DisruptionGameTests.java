package dev.abyssalport;

import dev.abyssalport.block.entity.*;
import dev.abyssalport.config.MobEcologyConfig;
import dev.abyssalport.content.*;
import dev.abyssalport.energy.*;
import dev.abyssalport.entity.*;
import dev.abyssalport.item.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DisruptionGameTests {
    private DisruptionGameTests() { }
    private static final class CenterRandom extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        CenterRandom() {super(0);}
        @Override public int nextInt(int bound) {return bound==64?32:bound==32?16:bound==9?4:0;}
        @Override public boolean nextBoolean() {return true;}
        @Override public double nextDouble() {return .5;}
    }
    private static ServerPlayer player(ServerLevel level,Vec3 pos) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"disruption-player"),false);
        var player=new ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().clearContent();player.teleportTo(level,pos.x,pos.y,pos.z,0,0);player.setNoGravity(true);return player;
    }
    private static Mob mob(ServerLevel level,EntityType<?> type,Vec3 pos) {var mob=(Mob)type.create(level);mob.moveTo(pos);mob.setNoAi(true);mob.setPersistenceRequired();level.addFreshEntity(mob);return mob;}
    private static void apply(DisruptionCatalogue effect,GraveyardGameTests.Fixture fixture,List<ServerPlayer> players,RandomSource random) {
        DisruptionEffects.apply(new DisruptionEvent(fixture.level,fixture.node,effect.deity(),effect,players),random);
    }
    @GameTest(template="empty",batch="disruption_catalogue")
    public static void AllTwentySevenSourceEntriesAreSelectableOnlyForCorrectDeities(GameTestHelper helper) {
        helper.assertTrue(DisruptionCatalogue.values().length==27&&DisruptionCatalogue.eligible(null).size()==18,"Actual source catalogue incomplete");
        var seen=EnumSet.noneOf(DisruptionCatalogue.class);
        for(var deity:StatueContent.Deity.values()) {
            var expected=DisruptionCatalogue.eligible(deity);var selected=EnumSet.noneOf(DisruptionCatalogue.class);
            for(int index=0;index<expected.size();index++) {
                var effect=DisruptionCatalogue.choose(deity,new StatueEnergyGameTests.FixedRandom(index));selected.add(effect);seen.add(effect);
                helper.assertTrue(effect.deity()==null||effect.deity()==deity,"Selection leaks a different deity's effect");
            }
            helper.assertTrue(selected.size()==expected.size(),"Random index cannot reach entire eligible pool");
        }
        helper.assertTrue(seen.size()==27,"Registered source effect is unreachable");helper.succeed();
    }
    @GameTest(template="empty",batch="disruption_threshold")
    public static void NativeThresholdDispatchResetsOnceHonorsGlobalVetoDisableAndReentry(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            fixture.level.setBlock(fixture.node,StatueContent.block(StatueContent.Deity.SHUB_NIGGURATH).defaultBlockState(),2);
            var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(fixture.node);int[] calls={0};
            boolean disabled=MobEcologyConfig.DISABLE_DISRUPTIONS.get();
            Consumer<DisruptionEvent> veto=event->{if(event.level()==fixture.level&&event.pos().equals(fixture.node)){calls[0]++;event.setCanceled(true);
                statue.tickEnergy(fixture.level,new CenterRandom());helper.assertTrue(statue.tolerance()==0,"Native callback reentered tolerance dispatch");}};
            NeoForge.EVENT_BUS.addListener(veto);
            try {
                var tag=statue.saveWithFullMetadata(fixture.level.registryAccess());tag.putInt("Tolerance",99);statue.loadWithComponents(tag,fixture.level.registryAccess());
                statue.tickEnergy(fixture.level,new CenterRandom());helper.assertTrue(calls[0]==0&&statue.tolerance()==99,"Threshold triggered early");
                tag.putInt("Tolerance",Integer.MAX_VALUE);statue.loadWithComponents(tag,fixture.level.registryAccess());statue.tickEnergy(fixture.level,new CenterRandom());
                helper.assertTrue(calls[0]==1&&statue.tolerance()==0,"Saturated saved tolerance is not consumed once");
                statue.tickEnergy(fixture.level,new CenterRandom());helper.assertTrue(calls[0]==1,"Zero tolerance redispatches");
                MobEcologyConfig.DISABLE_DISRUPTIONS.set(true);tag.putInt("Tolerance",100);statue.loadWithComponents(tag,fixture.level.registryAccess());statue.tickEnergy(fixture.level,new CenterRandom());
                helper.assertTrue(calls[0]==1&&statue.tolerance()==0,"Source disabled setting changes threshold/reset semantics");helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(veto);MobEcologyConfig.DISABLE_DISRUPTIONS.set(disabled);}
        }
    }
    @GameTest(template="empty",batch="disruption_effects")
    public static void RealPlayerFireFreezeFamineAndFiveAreaEffectsKeepSourceDurationsAndImmunities(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var player=player(fixture.level,fixture.node.getBottomCenter().add(3,0,0));var cow=mob(fixture.level,EntityType.COW,fixture.node.getBottomCenter());
            var shadow=mob(fixture.level,AbyssalPort.SHADOW_CREATURE.get(),fixture.node.getBottomCenter().add(-3,0,0));
            try {
                apply(DisruptionCatalogue.FIRE,fixture,List.of(player),new CenterRandom());helper.assertTrue(player.getRemainingFireTicks()==400,"Fire duration not 20 seconds");player.clearFire();
                apply(DisruptionCatalogue.FREEZE,fixture,List.of(player),new CenterRandom());helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration()==100&&player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier()==14,"Freeze duration/amplifier changed");player.removeAllEffects();
                for(var effect:new DisruptionCatalogue[]{DisruptionCatalogue.POISON,DisruptionCatalogue.SLOWNESS,DisruptionCatalogue.WEAKNESS,DisruptionCatalogue.WITHER,DisruptionCatalogue.CORALIUM}) {
                    var nativeEffect=switch(effect){case POISON->MobEffects.POISON;case SLOWNESS->MobEffects.MOVEMENT_SLOWDOWN;case WEAKNESS->MobEffects.WEAKNESS;case WITHER->MobEffects.WITHER;default->WastelandContent.CORALIUM_PLAGUE;};
                    apply(effect,fixture,List.of(player),new CenterRandom());helper.assertTrue(cow.hasEffect(nativeEffect)&&cow.getEffect(nativeEffect).getDuration()==600,"Area effect not connected/duration differs: "+effect);cow.removeAllEffects();
                    if(effect==DisruptionCatalogue.POISON)helper.assertTrue(!shadow.hasEffect(nativeEffect),"Native Shadow immunity bypassed");
                }
                for(var famine:new DisruptionCatalogue[]{DisruptionCatalogue.FAMINE_AZATHOTH,DisruptionCatalogue.FAMINE_SHUB}){player.getFoodData().setFoodLevel(20);apply(famine,fixture,List.of(player),new CenterRandom());helper.assertTrue(player.getFoodData().getFoodLevel()==0,"Bound famine disconnected");}
                helper.succeed();
            } finally {cow.discard();shadow.discard();fixture.level.getServer().getPlayerList().remove(player);}
        }
    }
    @GameTest(template="empty",batch="disruption_energy")
    public static void RealInventoryAndCollectorsLoseSourceFractionsWithoutDestroyingOtherComponents(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var player=player(fixture.level,fixture.node.getBottomCenter().add(3,0,0));
            try {
                var book=new ItemStack(AbyssalPort.NECRONOMICON.get());NecronomiconItem.setPotentialEnergy(book,1000);
                var device=new ItemStack(AbyssalPort.ENERGY_COLLECTOR.get());var tag=new CompoundTag();tag.putFloat("PotEnergy",800);tag.putInt("PrivateAudit",51);device.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
                player.getInventory().setItem(0,book);player.getInventory().setItem(1,device);
                // Native new-player invulnerability lasts 60 ticks. Advance the real
                // player routine, not a mocked hurt() result or weakened damage source.
                for(int tick=0;tick<61;tick++)player.tick();
                apply(DisruptionCatalogue.POTENTIAL_ENERGY,fixture,List.of(player),new CenterRandom());
                helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==900&&EnergyStorageItem.storedEnergy(device)==800&&player.getHealth()==18,"Potential-energy effect did not consume only first container's tenth");
                player.invulnerableTime=0;apply(DisruptionCatalogue.DRAIN_PE,fixture,List.of(player),new StatueEnergyGameTests.FixedRandom(1));
                helper.assertTrue(NecronomiconItem.getPotentialEnergy(book)==810&&EnergyStorageItem.storedEnergy(device)==720&&device.get(DataComponents.CUSTOM_DATA).copyTag().getInt("PrivateAudit")==51,"Fallback drain loses custom components or skips later containers");
                var pos=fixture.node.west(3);fixture.level.setBlock(pos,AbyssalPort.ENERGY_COLLECTOR.get().defaultBlockState(),2);var collector=(EnergyCollectorBlockEntity)fixture.level.getBlockEntity(pos);collector.addEnergy(800);
                fixture.level.setBlock(fixture.node,StatueContent.block(StatueContent.Deity.CTHULHU).defaultBlockState(),2);
                apply(DisruptionCatalogue.DRAIN_PE,fixture,List.of(player),new StatueEnergyGameTests.FixedRandom(1));
                helper.assertTrue(collector.energy()==600&&NecronomiconItem.getPotentialEnergy(book)==810&&EnergyStorageItem.storedEnergy(device)==720,"Real statue collector drain is not a quarter or still drained players");
                apply(DisruptionCatalogue.DRAIN_PE,fixture,List.of(player),new CenterRandom());helper.assertTrue(collector.energy()==0,"One-in-four complete drain not connected");helper.succeed();
            } finally {fixture.level.getServer().getPlayerList().remove(player);}
        }
    }
    @GameTest(template="empty",batch="disruption_conversion_veto")
    public static void NativeCorruptionReplacementHonorsAllVetosReentryAndReturnsMountEquipment(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var cow=mob(fixture.level,EntityType.COW,fixture.node.getBottomCenter());var target=MobEcologyContent.type(CorruptedAnimalEntity.Kind.COW,false);
            Consumer<LivingConversionEvent.Pre> veto=event->{if(event.getEntity()==cow)event.setCanceled(true);};
            Consumer<FinalizeSpawnEvent> finalize=event->{if(event.getEntity().getType()==target)event.setSpawnCancelled(true);};
            Consumer<EntityJoinLevelEvent> join=event->{if(event.getEntity().getType()==target)event.setCanceled(true);};
            try {
                for(var stage:List.of("conversion","finalize","join")) {
                    if(stage.equals("conversion"))NeoForge.EVENT_BUS.addListener(veto);else if(stage.equals("finalize"))NeoForge.EVENT_BUS.addListener(finalize);else NeoForge.EVENT_BUS.addListener(join);
                    helper.assertTrue(!DisruptionEntities.convert(fixture.level,cow,target,mob->{})&&cow.isAlive()&&!cow.isRemoved(),"Veto removed source animal: "+stage);
                    NeoForge.EVENT_BUS.unregister(veto);NeoForge.EVENT_BUS.unregister(finalize);NeoForge.EVENT_BUS.unregister(join);
                }
                int[] calls={0};Consumer<LivingConversionEvent.Pre> recursive=event->{if(event.getEntity()==cow){calls[0]++;helper.assertTrue(!DisruptionEntities.convert(fixture.level,cow,target,mob->{}),"Conversion reentered native callback");}};
                NeoForge.EVENT_BUS.addListener(recursive);try {helper.assertTrue(DisruptionEntities.convert(fixture.level,cow,target,mob->{})&&cow.isRemoved()&&calls[0]==1,"Admitted conversion failed");}finally{NeoForge.EVENT_BUS.unregister(recursive);}
                var horse=(AbstractHorse)mob(fixture.level,EntityType.HORSE,fixture.node.getBottomCenter().add(3,0,0));
                try {
                    horse.getInventory().setItem(0,new ItemStack(Items.SADDLE));
                    var before=fixture.level.getEntitiesOfClass(ItemEntity.class,new AABB(fixture.node).inflate(5)).size();
                    helper.assertTrue(DisruptionEntities.convert(fixture.level,horse,EntityType.ZOMBIE_HORSE,mob->{})&&horse.isRemoved(),"Equipped mount is exempt from corruption");
                    helper.assertTrue(fixture.level.getEntitiesOfClass(ItemEntity.class,new AABB(fixture.node).inflate(5),item->item.getItem().is(Items.SADDLE)).size()==1
                            &&fixture.level.getEntitiesOfClass(ItemEntity.class,new AABB(fixture.node).inflate(5)).size()==before+1,"Mount saddle was lost or duplicated");
                }finally{horse.discard();fixture.level.getEntitiesOfClass(ZombieHorse.class,new AABB(fixture.node).inflate(5)).forEach(Entity::discard);
                    fixture.level.getEntitiesOfClass(ItemEntity.class,new AABB(fixture.node).inflate(5),item->item.getItem().is(Items.SADDLE)).forEach(Entity::discard);}
                helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(veto);NeoForge.EVENT_BUS.unregister(finalize);NeoForge.EVENT_BUS.unregister(join);cow.discard();fixture.level.getEntitiesOfClass(CorruptedAnimalEntity.class,new AABB(fixture.node).inflate(5)).forEach(Entity::discard);}
        }
    }
    @GameTest(template="empty",batch="disruption_possessions")
    public static void ActualAnimalAndSacrificeEffectsReturnExactHorseDonkeyPigAndVillagerPossessions(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var horse=(AbstractHorse)mob(fixture.level,EntityType.HORSE,fixture.node.getBottomCenter().add(-3,0,-3));
            var donkey=(AbstractChestedHorse)mob(fixture.level,EntityType.DONKEY,fixture.node.getBottomCenter().add(3,0,-3));
            var pig=(Pig)mob(fixture.level,EntityType.PIG,fixture.node.getBottomCenter().add(-3,0,3));
            var villager=(Villager)mob(fixture.level,EntityType.VILLAGER,fixture.node.getBottomCenter().add(3,0,3));
            var outputs=new ArrayList<Entity>();
            Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&event.getEntity().position().distanceTo(fixture.node.getCenter())<8)outputs.add(event.getEntity());};
            NeoForge.EVENT_BUS.addListener(capture);
            try {
                var saddle=new ItemStack(Items.SADDLE);saddle.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Original saddle"));
                var armor=new ItemStack(Items.DIAMOND_HORSE_ARMOR);var privateTag=new CompoundTag();privateTag.putInt("PossessionAudit",77);armor.set(DataComponents.CUSTOM_DATA,CustomData.of(privateTag));
                horse.getInventory().setItem(0,saddle.copy());horse.setBodyArmorItem(armor.copy());horse.setAge(-700);
                helper.assertTrue(donkey.getSlot(499).set(new ItemStack(Items.CHEST)),"Native chest inventory fixture failed");
                donkey.getInventory().setItem(0,new ItemStack(Items.SADDLE));
                var book=new ItemStack(AbyssalPort.NECRONOMICON.get());NecronomiconItem.setPotentialEnergy(book,321);
                donkey.getInventory().setItem(1,book.copy());donkey.getInventory().setItem(2,new ItemStack(Items.EMERALD,37));
                pig.equipSaddle(new ItemStack(Items.SADDLE),null);
                villager.getInventory().setItem(0,new ItemStack(Items.BREAD,23));
                var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(42);tool.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Kept tool"));
                villager.setItemSlot(EquipmentSlot.MAINHAND,tool.copy());
                apply(DisruptionCatalogue.ANIMAL_CORRUPTION,fixture,List.of(),new CenterRandom());
                helper.assertTrue(horse.isRemoved()&&donkey.isRemoved()&&pig.isRemoved()&&villager.isAlive(),"Actual corruption did not convert equipped animals");
                helper.assertTrue(outputs.stream().filter(entity->entity instanceof AbstractHorse mount&&mount.getAge()==-700).count()==1,"Horse native age not preserved");
                apply(DisruptionCatalogue.SACRIFICE_JZAHAR,fixture,List.of(),new CenterRandom());
                helper.assertTrue(villager.isRemoved(),"Actual sacrifice did not convert cargo-bearing Villager");
                var expected=List.of(saddle,armor,new ItemStack(Items.SADDLE),book,new ItemStack(Items.EMERALD,37),new ItemStack(Items.CHEST),new ItemStack(Items.SADDLE),new ItemStack(Items.BREAD,23),tool);
                var remaining=new ArrayList<>(outputs.stream().filter(entity->entity instanceof ItemEntity&&!entity.isRemoved()).map(entity->((ItemEntity)entity).getItem()).toList());
                helper.assertTrue(remaining.size()==expected.size(),"Possession stack count changed: "+remaining);
                for(var stack:expected) {
                    int match=-1;for(int index=0;index<remaining.size();index++)if(ItemStack.matches(stack,remaining.get(index))){match=index;break;}
                    helper.assertTrue(match>=0,"Missing exact components/count in returned possession: "+stack);remaining.remove(match);
                }
                helper.assertTrue(horse.getInventory().isEmpty()&&donkey.getInventory().isEmpty()&&villager.getInventory().isEmpty(),"Discarded source still owns copied cargo");
                for(var output:outputs)if(output instanceof ItemEntity item&&!item.isRemoved()) {
                    var saved=item.saveWithoutId(new CompoundTag());var restored=EntityType.ITEM.create(fixture.level);restored.load(saved);
                    try {helper.assertTrue(ItemStack.matches(item.getItem(),restored.getItem()),"Actual native ItemEntity save lost components");}finally{restored.discard();}
                }
                helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(capture);outputs.forEach(Entity::discard);horse.discard();donkey.discard();pig.discard();villager.discard();}
        }
    }
    @GameTest(template="empty",batch="disruption_possession_veto")
    public static void NativeItemJoinVetoRollsBackReplacementAllOutputsAndConversionPost(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var horse=(AbstractHorse)mob(fixture.level,EntityType.HORSE,fixture.node.getBottomCenter());
            var armor=new ItemStack(Items.IRON_HORSE_ARMOR);horse.getInventory().setItem(0,new ItemStack(Items.SADDLE));horse.setBodyArmorItem(armor.copy());
            var outputs=new ArrayList<Entity>();int[] joins={0},posts={0};
            Consumer<EntityJoinLevelEvent> veto=event->{if(event.getLevel()==fixture.level&&event.getEntity().position().distanceTo(fixture.node.getCenter())<4){outputs.add(event.getEntity());
                if(event.getEntity() instanceof ItemEntity&&++joins[0]==2)event.setCanceled(true);}};
            Consumer<LivingConversionEvent.Post> post=event->{if(event.getEntity()==horse)posts[0]++;};
            NeoForge.EVENT_BUS.addListener(veto);NeoForge.EVENT_BUS.addListener(post);
            try {
                helper.assertTrue(!DisruptionEntities.convert(fixture.level,horse,EntityType.ZOMBIE_HORSE,mob->{})&&horse.isAlive(),"Rejected possession join erased mount");
                helper.assertTrue(horse.getInventory().getItem(0).is(Items.SADDLE)&&ItemStack.matches(horse.getItemBySlot(EquipmentSlot.BODY),armor)
                        &&outputs.stream().allMatch(Entity::isRemoved)&&posts[0]==0,"Rejected transaction left a duplicate, lost gear or emitted success");
                helper.assertTrue(DisruptionEntities.convert(fixture.level,horse,EntityType.ZOMBIE_HORSE,mob->{})&&horse.isRemoved()&&posts[0]==1,"Allowed retry failed or success callback duplicated");
                helper.assertTrue(outputs.stream().filter(entity->entity instanceof ItemEntity&&!entity.isRemoved()).count()==2,"Allowed retry duplicated or lost possessions");helper.succeed();
            }finally{NeoForge.EVENT_BUS.unregister(veto);NeoForge.EVENT_BUS.unregister(post);outputs.forEach(Entity::discard);horse.discard();}
        }
    }
    @GameTest(template="empty",batch="disruption_possession_reentry")
    public static void NativeItemJoinSourceMutationAbortsWithoutOverwritingCallbackChanges(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var villager=(Villager)mob(fixture.level,EntityType.VILLAGER,fixture.node.getBottomCenter());
            villager.getInventory().setItem(0,new ItemStack(Items.BREAD,21));villager.getInventory().setItem(1,new ItemStack(Items.EMERALD,7));
            var outputs=new ArrayList<Entity>();int[] calls={0};
            Consumer<EntityJoinLevelEvent> mutation=event->{if(event.getLevel()==fixture.level&&event.getEntity().position().distanceTo(fixture.node.getCenter())<4){outputs.add(event.getEntity());
                if(event.getEntity() instanceof ItemEntity){calls[0]++;villager.getInventory().setItem(1,new ItemStack(Items.EMERALD,11));
                    helper.assertTrue(!DisruptionEntities.convert(fixture.level,villager,GraveyardContent.DEPTHS_GHOUL.get(),mob->{}),"Possession join reentered conversion");}}};
            NeoForge.EVENT_BUS.addListener(mutation);
            try {
                helper.assertTrue(!DisruptionEntities.convert(fixture.level,villager,GraveyardContent.DEPTHS_GHOUL.get(),mob->{})&&villager.isAlive(),"Source mutation was consumed using stale possession snapshot");
                helper.assertTrue(calls[0]==1&&villager.getInventory().getItem(0).getCount()==21&&villager.getInventory().getItem(1).getCount()==11
                        &&outputs.stream().allMatch(Entity::isRemoved),"Abort overwrote listener changes or left duplicate outputs");helper.succeed();
            }finally{NeoForge.EVENT_BUS.unregister(mutation);outputs.forEach(Entity::discard);villager.discard();}
        }
    }
    @GameTest(template="empty",batch="disruption_possession_strider")
    public static void ActualSacrificeReturnsStriderSaddleAndLaterDropMutationRollsBack(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var strider=(Strider)mob(fixture.level,EntityType.STRIDER,fixture.node.getBottomCenter().add(-3,0,0));
            strider.equipSaddle(new ItemStack(Items.SADDLE),null);
            var outputs=new ArrayList<Entity>();
            Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&event.getEntity().position().distanceTo(fixture.node.getCenter())<5)outputs.add(event.getEntity());};
            NeoForge.EVENT_BUS.addListener(capture);
            try {
                apply(DisruptionCatalogue.SACRIFICE_YOG,fixture,List.of(),new CenterRandom());
                helper.assertTrue(strider.isRemoved()&&outputs.stream().filter(entity->entity instanceof ItemEntity item&&!item.isRemoved()&&item.getItem().is(Items.SADDLE)).count()==1,"Source food Strider lost or duplicated its saddle");
                var owner=(AbstractHorse)mob(fixture.level,EntityType.HORSE,fixture.node.getBottomCenter().add(3,0,0));
                owner.getInventory().setItem(0,new ItemStack(Items.SADDLE));owner.setBodyArmorItem(new ItemStack(Items.DIAMOND_HORSE_ARMOR));
                Consumer<EntityJoinLevelEvent> alter=event->{if(event.getLevel()==fixture.level&&event.getEntity() instanceof ItemEntity item
                        &&item.position().distanceTo(owner.position())<1&&item.getItem().is(Items.DIAMOND_HORSE_ARMOR))
                    outputs.stream().filter(entity->entity instanceof ItemEntity previous&&previous!=item&&!previous.isRemoved()&&previous.position().distanceTo(owner.position())<1)
                            .findFirst().ifPresent(entity->((ItemEntity)entity).setItem(new ItemStack(Items.DIRT)));};
                NeoForge.EVENT_BUS.addListener(alter);
                try {
                    helper.assertTrue(!DisruptionEntities.convert(fixture.level,owner,EntityType.ZOMBIE_HORSE,mob->{})&&owner.isAlive()
                            &&owner.getInventory().getItem(0).is(Items.SADDLE)&&owner.getItemBySlot(EquipmentSlot.BODY).is(Items.DIAMOND_HORSE_ARMOR),"Later drop mutation consumed source possessions");
                    helper.assertTrue(outputs.stream().noneMatch(entity->entity!=owner&&!entity.isRemoved()&&entity.position().distanceTo(owner.position())<1),"Late output mutation left a partial converted entity/item");
                }finally{NeoForge.EVENT_BUS.unregister(alter);owner.discard();}
                helper.succeed();
            }finally{NeoForge.EVENT_BUS.unregister(capture);outputs.forEach(Entity::discard);strider.discard();}
        }
    }
    @GameTest(template="empty",batch="disruption_animals")
    public static void FourActualAnimalFormsHorseWolfCatRabbitAndBothSacrificeMappingsAreConnected(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var player=player(fixture.level,fixture.node.getBottomCenter().add(4,0,0));var made=new ArrayList<Mob>();
            try {
                for(var type:new EntityType<?>[]{EntityType.COW,EntityType.CHICKEN,EntityType.PIG,EntityType.SHEEP})made.add(mob(fixture.level,type,fixture.node.getBottomCenter()));
                var horse=(AbstractHorse)mob(fixture.level,EntityType.HORSE,fixture.node.getBottomCenter());horse.setAge(-500);made.add(horse);
                var wolf=(Wolf)mob(fixture.level,EntityType.WOLF,fixture.node.getBottomCenter());wolf.setTame(true,true);wolf.setOwnerUUID(player.getUUID());made.add(wolf);
                var cat=(Cat)mob(fixture.level,EntityType.CAT,fixture.node.getBottomCenter());cat.setTame(true,true);cat.setOwnerUUID(player.getUUID());made.add(cat);
                var rabbit=(Rabbit)mob(fixture.level,EntityType.RABBIT,fixture.node.getBottomCenter());made.add(rabbit);
                apply(DisruptionCatalogue.ANIMAL_CORRUPTION,fixture,List.of(player),new CenterRandom());
                var corrupted=fixture.level.getEntitiesOfClass(CorruptedAnimalEntity.class,new AABB(fixture.node).inflate(5));
                helper.assertTrue(corrupted.size()==4&&corrupted.stream().map(CorruptedAnimalEntity::kind).distinct().count()==4&&made.subList(0,4).stream().allMatch(Entity::isRemoved),"One of four native corruption mappings missing");
                helper.assertTrue(horse.isRemoved()&&fixture.level.getEntitiesOfClass(ZombieHorse.class,new AABB(fixture.node).inflate(5)).stream().anyMatch(mob->mob.getAge()==-500),"Native age-preserving horse corruption missing");
                helper.assertTrue(!wolf.isTame()&&wolf.getOwnerUUID()==null&&wolf.getTarget()==player&&!cat.isTame()&&cat.getOwnerUUID()==null&&rabbit.getVariant()==Rabbit.Variant.EVIL,"Native wolf/cat/rabbit state changes missing");
                // Clear first family; then each source-bound sacrifice converts a real named baby villager.
                fixture.level.getEntitiesOfClass(Mob.class,new AABB(fixture.node).inflate(6)).forEach(Entity::discard);
                for(var effect:new DisruptionCatalogue[]{DisruptionCatalogue.SACRIFICE_JZAHAR,DisruptionCatalogue.SACRIFICE_YOG}) {
                    var villager=(Villager)mob(fixture.level,EntityType.VILLAGER,fixture.node.getBottomCenter());villager.setBaby(true);villager.setCustomName(net.minecraft.network.chat.Component.literal("kept-name"));
                    apply(effect,fixture,List.of(player),new CenterRandom());var ghouls=fixture.level.getEntitiesOfClass(GhoulEntity.class,new AABB(fixture.node).inflate(5));
                    helper.assertTrue(villager.isRemoved()&&ghouls.size()==1&&ghouls.getFirst().kind()==GhoulEntity.Kind.DEPTHS&&ghouls.getFirst().isBaby()&&ghouls.getFirst().getName().getString().equals("kept-name"),"Native sacrifice age/name/type lost: "+effect);ghouls.forEach(Entity::discard);
                }
                helper.succeed();
            } finally {made.forEach(Entity::discard);fixture.level.getEntitiesOfClass(Mob.class,new AABB(fixture.node).inflate(6)).forEach(Entity::discard);fixture.level.getServer().getPlayerList().remove(player);}
        }
    }
    @GameTest(template="empty",batch="disruption_teleport")
    public static void TeleportRespectsNativeVetoModifiedTargetSolidLiquidUnloadedAndMalformedDestinations(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var player=player(fixture.level,fixture.node.getBottomCenter().add(-3,0,0));Vec3 original=player.position(),target=fixture.node.getBottomCenter().add(3,0,0);
            Consumer<EntityTeleportEvent.EnderEntity> veto=event->{if(event.getEntity()==player)event.setCanceled(true);};
            try {
                NeoForge.EVENT_BUS.addListener(veto);helper.assertTrue(!DisruptionEffects.teleport(fixture.level,player,target)&&player.position().equals(original),"Native teleport veto ignored");NeoForge.EVENT_BUS.unregister(veto);
                var targetBlock=BlockPos.containing(target);fixture.level.setBlock(targetBlock,Blocks.STONE.defaultBlockState(),2);
                helper.assertTrue(!DisruptionEffects.teleport(fixture.level,player,target)&&player.position().equals(original),"Solid teleport accepted");fixture.level.setBlock(targetBlock,Blocks.WATER.defaultBlockState(),2);
                helper.assertTrue(!DisruptionEffects.teleport(fixture.level,player,target)&&player.position().equals(original),"Liquid teleport accepted");fixture.level.setBlock(targetBlock,Blocks.AIR.defaultBlockState(),2);
                var far=new Vec3(10_654_320,fixture.node.getY(),10_654_320);var chunk=new ChunkPos(BlockPos.containing(far));helper.assertTrue(!fixture.level.getChunkSource().hasChunk(chunk.x,chunk.z),"Unloaded teleport fixture unexpectedly loaded");
                helper.assertTrue(!DisruptionEffects.teleport(fixture.level,player,far)&&!fixture.level.getChunkSource().hasChunk(chunk.x,chunk.z)&&player.position().equals(original),"Teleport forced unloaded terrain");
                helper.assertTrue(!DisruptionEffects.teleport(fixture.level,player,new Vec3(Double.NaN,target.y,target.z)),"Malformed teleport accepted");
                Consumer<EntityTeleportEvent.EnderEntity> redirect=event->{if(event.getEntity()==player){event.setTargetX(target.x);event.setTargetY(target.y);event.setTargetZ(target.z);}};
                NeoForge.EVENT_BUS.addListener(redirect);try {helper.assertTrue(DisruptionEffects.teleport(fixture.level,player,original)&&player.position().equals(target),"Native listener's checked target ignored");}finally{NeoForge.EVENT_BUS.unregister(redirect);}helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(veto);fixture.level.getServer().getPlayerList().remove(player);}
        }
    }
    @GameTest(template="empty",batch="disruption_spawns")
    public static void AllNativeSwarmAndSingleSpawnFormsUseActualTypesAndInvisibleLootIsEmpty(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var admitted=new ArrayList<Mob>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&event.getEntity() instanceof Mob mob&&mob.position().distanceTo(fixture.node.getCenter())<20){mob.setNoAi(true);admitted.add(mob);}};
            NeoForge.EVENT_BUS.addListener(capture);
            try {
                for(var effect:new DisruptionCatalogue[]{DisruptionCatalogue.SHOGGOTH,DisruptionCatalogue.SHADOW_SWARM,DisruptionCatalogue.SHEEP_SWARM,DisruptionCatalogue.OFFSPRING,DisruptionCatalogue.INVISIBLE_HASTUR,DisruptionCatalogue.INVISIBLE_NYARLATHOTEP}) {
                    admitted.clear();apply(effect,fixture,List.of(),new CenterRandom());
                    int count=switch(effect){case SHADOW_SWARM->12;case SHEEP_SWARM->8;case INVISIBLE_HASTUR,INVISIBLE_NYARLATHOTEP->2;default->1;};
                    helper.assertTrue(admitted.size()==count&&admitted.stream().allMatch(mob->mob.isAddedToLevel()&&!mob.isRemoved()&&DisruptionEntities.clear(fixture.level,mob,mob.getBoundingBox())),"Actual spawn admission/count failed: "+effect+" / "+admitted.size());
                    if(effect==DisruptionCatalogue.SHADOW_SWARM)helper.assertTrue(admitted.stream().map(Entity::getType).distinct().count()==3,"Shadow swarm dropped a real form");
                    if(effect==DisruptionCatalogue.SHEEP_SWARM)helper.assertTrue(admitted.stream().filter(Sheep.class::isInstance).count()==4&&admitted.stream().filter(CorruptedAnimalEntity.class::isInstance).count()==4,"Sheep swarm is not four Evil/four vanilla sheep");
                    if(effect==DisruptionCatalogue.INVISIBLE_HASTUR||effect==DisruptionCatalogue.INVISIBLE_NYARLATHOTEP)for(var mob:admitted)helper.assertTrue(mob instanceof EnderMan&&mob.getLootTable().equals(BuiltInLootTables.EMPTY)&&mob.getEffect(MobEffects.INVISIBILITY).getDuration()==12000,"Invisible native Enderman duration/empty loot wrong");
                    admitted.forEach(Entity::discard);
                }
                helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(capture);admitted.forEach(Entity::discard);}
        }
    }
    @GameTest(template="empty",batch="disruption_ooze")
    public static void OozeUsesRealLayersNativePlaceVetoRollbackAndDoesNotReplaceContainers(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var chest=fixture.node.east(3);fixture.level.setBlock(chest,Blocks.CHEST.defaultBlockState(),2);var container=(net.minecraft.world.level.block.entity.ChestBlockEntity)fixture.level.getBlockEntity(chest);container.setItem(0,new ItemStack(Items.DIAMOND,3));
            int[] calls={0};Consumer<BlockEvent.EntityPlaceEvent> veto=event->{if(event.getLevel()==fixture.level&&event.getPlacedBlock().is(AbyssalPort.SHOGGOTH_OOZE.get())){calls[0]++;event.setCanceled(true);}};
            NeoForge.EVENT_BUS.addListener(veto);try {apply(DisruptionCatalogue.OOZE,fixture,List.of(),new CenterRandom());helper.assertTrue(calls[0]>0,"Native ooze placement hook did not fire");
                for(var pos:BlockPos.betweenClosed(fixture.node.offset(-2,0,-2),fixture.node.offset(2,2,2)))helper.assertTrue(!fixture.level.getBlockState(pos).is(AbyssalPort.SHOGGOTH_OOZE.get()),"Vetoed ooze survived rollback");}
            finally{NeoForge.EVENT_BUS.unregister(veto);}
            apply(DisruptionCatalogue.OOZE,fixture,List.of(),new CenterRandom());helper.assertTrue(fixture.level.getBlockState(fixture.node).is(AbyssalPort.SHOGGOTH_OOZE.get())&&fixture.level.getBlockState(fixture.node).getValue(dev.abyssalport.block.ShoggothOozeBlock.LAYERS)==1,"Real own ooze/layer missing");
            helper.assertTrue(fixture.level.getBlockEntity(chest)==container&&container.getItem(0).getCount()==3,"Disruption overwrote real container/inventory");helper.succeed();
        }
    }
    @GameTest(template="empty",batch="disruption_random_biome")
    public static void RandomSpawnAndSwarmUseActualWeightedBiomeEntriesNotAnInventedEntityPool(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var admitted=new ArrayList<Mob>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&event.getEntity() instanceof Mob mob&&mob.position().distanceTo(fixture.node.getCenter())<20){mob.setNoAi(true);admitted.add(mob);}};
            NeoForge.EVENT_BUS.addListener(capture);
            try {
                var actual=fixture.level.getBiome(fixture.node).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap();helper.assertTrue(!actual.isEmpty(),"Real native monster biome list absent");
                for(var effect:new DisruptionCatalogue[]{DisruptionCatalogue.RANDOM_SPAWN,DisruptionCatalogue.RANDOM_SWARM}) {
                    admitted.clear();apply(effect,fixture,List.of(),new CenterRandom());
                    helper.assertTrue(admitted.size()==(effect==DisruptionCatalogue.RANDOM_SPAWN?1:4)&&admitted.stream().allMatch(mob->mob.getType()!=DreadEcologyContent.BEAST.get()
                        &&actual.stream().anyMatch(entry->entry.type==mob.getType())),"Native weighted biome spawn/count differs: "+effect+" / "+admitted.size());admitted.forEach(Entity::discard);
                }
                helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(capture);admitted.forEach(Entity::discard);}
        }
    }
    @GameTest(template="empty",batch="disruption_displace")
    public static void TwoSidedNativeSwapVetoIsAtomicAndRealRandomTeleportUsesCheckedSpace(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var cow=mob(fixture.level,EntityType.COW,fixture.node.getBottomCenter().add(-3,0,0));var pig=mob(fixture.level,EntityType.PIG,fixture.node.getBottomCenter().add(3,0,0));
            Vec3 cowBefore=cow.position(),pigBefore=pig.position();Consumer<EntityTeleportEvent.EnderEntity> veto=event->{if(event.getEntity()==cow)event.setCanceled(true);};
            var player=player(fixture.level,fixture.node.getBottomCenter().add(0,0,3));
            try {
                // No player participates in the swap fixture; park it outside the source radius.
                player.teleportTo(fixture.level,fixture.node.getX()+40,fixture.node.getY(),fixture.node.getZ(),0,0);
                var area=fixture.level.getEntitiesOfClass(LivingEntity.class,new AABB(fixture.node).inflate(16),LivingEntity::isAlive);
                int cowIndex=area.indexOf(cow);helper.assertTrue(cowIndex>=0,"Native displacement collection excludes actual fixture cow");
                RandomSource chooseCow=new net.minecraft.world.level.levelgen.LegacyRandomSource(0){@Override public int nextInt(int bound){return Math.min(cowIndex,bound-1);}};
                Consumer<EntityTeleportEvent.EnderEntity> isolate=event->{if(event.getEntity()!=cow&&event.getEntity()!=pig)event.setCanceled(true);};
                NeoForge.EVENT_BUS.addListener(isolate);
                try {
                    NeoForge.EVENT_BUS.addListener(veto);apply(DisruptionCatalogue.DISPLACE,fixture,List.of(),chooseCow);NeoForge.EVENT_BUS.unregister(veto);
                    helper.assertTrue(cow.position().equals(cowBefore)&&pig.position().equals(pigBefore),"Second-side veto allowed a partial swap");
                    apply(DisruptionCatalogue.DISPLACE,fixture,List.of(),chooseCow);helper.assertTrue(cow.position().equals(pigBefore)&&pig.position().equals(cowBefore),"Real displacement is not an actual checked two-entity swap: cow="+cow.position()+", pig="+pig.position()+", before="+cowBefore+" / "+pigBefore+", collection="+area.stream().map(entity->entity.getType().toString()).toList());
                } finally {NeoForge.EVENT_BUS.unregister(isolate);}
                player.teleportTo(fixture.level,fixture.node.getX()+.5,fixture.node.getY()+2,fixture.node.getZ()+.5,0,0);
                apply(DisruptionCatalogue.RANDOM_TELEPORT,fixture,List.of(player),new CenterRandom());
                helper.assertTrue(player.getY()==fixture.node.getY()&&player.fallDistance==0,"Native random-teleport ground descent/position update missing");helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(veto);cow.discard();pig.discard();fixture.level.getServer().getPlayerList().remove(player);}
        }
    }
    @GameTest(template="empty",batch="disruption_spawn_veto")
    public static void SingleAndSwarmSpawnVetosAndSolidFallbackNeverAdmitGhostCreatures(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var type=MobEcologyContent.OFFSPRING.get();var made=new ArrayList<Entity>();
            Consumer<FinalizeSpawnEvent> finalize=event->{if(event.getEntity().getType()==type)event.setSpawnCancelled(true);};
            Consumer<EntityJoinLevelEvent> join=event->{if(event.getEntity().getType()==type){made.add(event.getEntity());event.setCanceled(true);}};
            try {
                NeoForge.EVENT_BUS.addListener(finalize);helper.assertTrue(DisruptionEntities.spawn(fixture.level,fixture.node,type,new CenterRandom(),mob->{})==null,"Finalize-vetoed disruption creature returned");NeoForge.EVENT_BUS.unregister(finalize);
                NeoForge.EVENT_BUS.addListener(join);helper.assertTrue(DisruptionEntities.spawn(fixture.level,fixture.node,type,new CenterRandom(),mob->{})==null&&made.stream().noneMatch(entity->entity.isAddedToLevel()&&!entity.isRemoved()),"Join-vetoed disruption creature admitted");NeoForge.EVENT_BUS.unregister(join);
                helper.assertTrue(DisruptionEntities.spawn(fixture.level,fixture.node,type,new CenterRandom(),mob->fixture.level.setBlock(fixture.node,Blocks.STONE.defaultBlockState(),2))==null,"Post-finalize obstruction bypassed final collision guard");helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(finalize);NeoForge.EVENT_BUS.unregister(join);made.forEach(Entity::discard);}
        }
    }
    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="disruption_native_world",timeoutTicks=340)
    public static void RealNormalWorldStatueTickerCrossesHundredFromAcceptedHeldBookTransfer(GameTestHelper helper) {
        var fixture=new GraveyardGameTests.Fixture(helper);var player=player(fixture.level,fixture.node.getBottomCenter().add(3,0,0));
        fixture.level.setBlock(fixture.node,StatueContent.block(StatueContent.Deity.CTHULHU).defaultBlockState(),2);
        var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(fixture.node);
        ItemStack main=new ItemStack(AbyssalPort.NECRONOMICON.get()),off=new ItemStack(AbyssalPort.NECRONOMICON.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,main);player.setItemInHand(InteractionHand.OFF_HAND,off);
        var saved=statue.saveWithFullMetadata(fixture.level.registryAccess());saved.putInt("Tolerance",98);saved.putInt("ActiveTicks",0);statue.loadWithComponents(saved,fixture.level.registryAccess());
        int[] calls={0};Consumer<DisruptionEvent> veto=event->{if(event.level()==fixture.level&&event.pos().equals(fixture.node)){calls[0]++;event.setCanceled(true);
            helper.assertTrue(statue.tolerance()==0&&NecronomiconItem.getPotentialEnergy(player.getMainHandItem())==5&&NecronomiconItem.getPotentialEnergy(player.getOffhandItem())==5,"Native ticker did not dispatch after accepted real transfers/reset");}};
        NeoForge.EVENT_BUS.addListener(veto);
        helper.runAfterDelay(300,()->{try {
            helper.assertTrue(calls[0]==1&&statue.tolerance()<100&&NecronomiconItem.getPotentialEnergy(player.getMainHandItem())>=5,"Real native normal-world threshold never reached or redispatched without tolerance: "+calls[0]);
            com.mojang.logging.LogUtils.getLogger().info("NATIVE DISRUPTION WORLD: real registered statue ticker, initial tolerance 98, {} accepted dispatch, actual hands {}/{}, remaining tolerance {}",calls[0],NecronomiconItem.getPotentialEnergy(player.getMainHandItem()),NecronomiconItem.getPotentialEnergy(player.getOffhandItem()),statue.tolerance());helper.succeed();
        } finally {NeoForge.EVENT_BUS.unregister(veto);fixture.level.getServer().getPlayerList().remove(player);fixture.close();}});
    }
    @GameTest(template="empty",batch="disruption_projectiles")
    public static void FireRainAndLightningUseRealNativeProjectilesAndRespectJoinVeto(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var made=new ArrayList<Entity>();Consumer<EntityJoinLevelEvent> capture=event->{if(event.getLevel()==fixture.level&&(event.getEntity() instanceof LargeFireball||event.getEntity() instanceof LightningBolt))made.add(event.getEntity());};
            Consumer<EntityJoinLevelEvent> veto=event->{if(event.getLevel()==fixture.level&&event.getEntity() instanceof LargeFireball)event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(capture);
            try {apply(DisruptionCatalogue.FIRE_RAIN,fixture,List.of(),new CenterRandom());helper.assertTrue(made.size()==8&&made.stream().allMatch(entity->entity instanceof LargeFireball ball&&ball.getDeltaMovement().y<0&&ball.accelerationPower==.1&&entity.isAddedToLevel()),"Native fire rain directions/number differ");made.forEach(Entity::discard);made.clear();
                NeoForge.EVENT_BUS.addListener(veto);try {apply(DisruptionCatalogue.FIRE_RAIN,fixture,List.of(),new CenterRandom());helper.assertTrue(made.stream().noneMatch(entity->entity.isAddedToLevel()&&!entity.isRemoved()),"Projectile native join veto ignored");}finally{NeoForge.EVENT_BUS.unregister(veto);}made.clear();
                DisruptionEffects.lightning(fixture.level,fixture.node.getCenter(),true);helper.assertTrue(made.size()==1&&made.getFirst() instanceof LightningBolt,"Real warning lightning absent");helper.succeed();
            } finally {NeoForge.EVENT_BUS.unregister(capture);NeoForge.EVENT_BUS.unregister(veto);made.forEach(Entity::discard);}
        }
    }
}
