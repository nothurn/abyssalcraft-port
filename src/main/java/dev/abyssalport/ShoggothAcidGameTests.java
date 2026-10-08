package dev.abyssalport;

import dev.abyssalport.content.*;
import dev.abyssalport.entity.*;
import dev.abyssalport.entity.ShoggothEntity.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShoggothAcidGameTests {
    private ShoggothAcidGameTests() { }
    private static final class Fixture implements AutoCloseable {
        final GraveyardGameTests.Fixture ground;
        final List<Entity> entities=new ArrayList<>(); final List<ShoggothAcidProjectile> shots=new ArrayList<>();
        final boolean grief; final Consumer<EntityJoinLevelEvent> capture;
        private boolean closed;
        Fixture(GameTestHelper helper) {
            ground=new GraveyardGameTests.Fixture(helper); grief=ground.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            capture=event->{ if(event.getLevel()==ground.level&&event.getEntity() instanceof ShoggothAcidProjectile shot&&shot.position().distanceTo(ground.node.getCenter())<10) { entities.add(shot); shots.add(shot); } };
            NeoForge.EVENT_BUS.addListener(capture);
        }
        ShoggothEntity parent(GameTestHelper helper,Kind kind) {
            var mob=ShoggothEcologyContent.type(kind,Habitat.OVERWORLD).create(ground.level); mob.setNoAi(true); mob.setNoGravity(true);
            mob.moveTo(ground.node.getX()+.5,ground.node.getY(),ground.node.getZ()+.5,0,0); entities.add(mob);
            helper.assertTrue(ground.level.addFreshEntity(mob),"Acid parent not admitted"); return mob;
        }
        net.minecraft.world.entity.animal.Cow cow(GameTestHelper helper,ShoggothEntity parent,double offset) {
            var cow=EntityType.COW.create(ground.level); cow.setNoAi(true); cow.setNoGravity(true); cow.moveTo(parent.position().add(offset,0,0)); entities.add(cow);
            helper.assertTrue(ground.level.addFreshEntity(cow),"Acid target not admitted"); return cow;
        }
        @Override public void close() {
            if (closed) return;
            closed = true;
            NeoForge.EVENT_BUS.unregister(capture); entities.forEach(entity->{ if(entity instanceof ShoggothAcidProjectile shot) shot.finish(); else entity.discard(); });
            ground.level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,ground.level.getServer()); ground.close();
        }
    }

    @GameTest(template="empty",batch="shoggoth_acid_damage")
    public static void AcidBypassesArmorButOnlyAcceptedDamageCorrodesItAndNeverHealsOrHarmsShoggoths(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var parent=fixture.parent(helper,Kind.NORMAL); var cow=fixture.cow(helper,parent,3); var armor=new ItemStack(Items.DIAMOND_CHESTPLATE); cow.setItemSlot(EquipmentSlot.CHEST,armor);
            float initialHealth=cow.getHealth();
            Consumer<LivingIncomingDamageEvent> veto=event->{ if(event.getEntity()==cow) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
            try { helper.assertTrue(!ShoggothAcid.damage(fixture.ground.level,parent,cow,6,2,12)&&cow.getHealth()==initialHealth&&armor.getDamageValue()==0,"Damage veto spends armor durability"); }
            finally { NeoForge.EVENT_BUS.unregister(veto); }
            helper.assertTrue(ShoggothAcid.damage(fixture.ground.level,parent,cow,6,2,12)&&cow.getHealth()==initialHealth-6&&armor.getDamageValue()==2,"Native absolute acid or accepted armor wear differs");
            helper.assertTrue(!ShoggothAcid.damage(fixture.ground.level,parent,parent,6,2,12)&&parent.getHealth()==50
                &&!ShoggothAcid.damage(fixture.ground.level,parent,cow,-2,2,12)&&cow.getHealth()==initialHealth-6,"Acid heals with negative damage or hits an allied form"); helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_acid_corrosion")
    public static void CorrosionHonorsGriefingAndDestroyVetosAndProtectsNativeNodesInventoryAndHardBlocks(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var level=fixture.ground.level; var parent=fixture.parent(helper,Kind.LESSER); var pos=fixture.ground.node.offset(3,0,0);
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),2); level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
            helper.assertTrue(!ShoggothAcid.corrode(level,pos,parent)&&level.getBlockState(pos).is(Blocks.STONE),"Disabled griefing still removes terrain");
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
            Consumer<EntityMobGriefingEvent> griefVeto=event->{ if(event.getEntity()==parent) event.setCanGrief(false); };
            NeoForge.EVENT_BUS.addListener(griefVeto);
            try { helper.assertTrue(!ShoggothAcid.corrode(level,pos,parent),"Per-entity griefing veto ignored"); }
            finally { NeoForge.EVENT_BUS.unregister(griefVeto); }
            Consumer<LivingDestroyBlockEvent> veto=event->{ if(event.getEntity()==parent) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
            try { helper.assertTrue(!ShoggothAcid.corrode(level,pos,parent)&&level.getBlockState(pos).is(Blocks.STONE),"Block veto removes terrain"); }
            finally { NeoForge.EVENT_BUS.unregister(veto); }
            helper.assertTrue(ShoggothAcid.corrode(level,pos,parent)&&level.isEmptyBlock(pos),"Actual low-hardness corridor cannot corrode");
            for(var protectedBlock:List.of(Blocks.BEDROCK,Blocks.OBSIDIAN,Blocks.CHEST,ShoggothLairContent.MONOLITH_STONE.get(),ShoggothLairContent.BIOMASS.get(),AbyssalPort.SHOGGOTH_OOZE.get())) {
                level.setBlock(pos,protectedBlock.defaultBlockState(),2);
                helper.assertTrue(!ShoggothAcid.corrode(level,pos,parent)&&level.getBlockState(pos).is(protectedBlock),"Acid removes protected native block "+protectedBlock);
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_acid_shield",timeoutTicks=120)
    public static void NativeFacingShieldTakesWearAndOneChipDamageOnlyAfterAcceptedHit(GameTestHelper helper) {
        var fixture=new Fixture(helper); var parent=fixture.parent(helper,Kind.LESSER);
        // GameTestHelper's mock permanently overrides isCreative() to true.
        // Use native player damage, connection ticking and shield duration instead.
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"acid-survival-test"),false);
        var player=new net.minecraft.server.level.ServerPlayer(fixture.ground.level.getServer(),fixture.ground.level,cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        fixture.ground.level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setNoGravity(true);
        player.teleportTo(fixture.ground.level,parent.getX(),parent.getY(),parent.getZ()-2,0,0); player.setYHeadRot(0);
        var shield=new ItemStack(Items.SHIELD); player.setItemInHand(InteractionHand.OFF_HAND,shield); player.startUsingItem(InteractionHand.OFF_HAND);
        // An embedded connection has no real client movement packets, so the
        // network-driven doTick path is not scheduled automatically here.
        // Advance native item-use ticks, not a mocked isBlocking return value.
        for(int tick=0;tick<6;tick++) player.doTick();
        // Also let the native 60-tick login protection expire, without changing
        // the acid damage type to bypass player invulnerability.
        helper.runAfterDelay(75,()-> {
            Consumer<LivingIncomingDamageEvent> veto=event->{ if(event.getEntity()==player) event.setCanceled(true); };
            try {
                helper.assertTrue(player.isDamageSourceBlocked(fixture.ground.level.damageSources().mobAttack(parent)),"Native directional shield fixture not actively blocking: using="+player.isUsingItem()+", ticks="+player.getTicksUsingItem()+", pos="+player.position()+", yaw="+player.getYHeadRot());
                float hp=player.getHealth(); NeoForge.EVENT_BUS.addListener(veto);
                try { helper.assertTrue(!ShoggothAcid.damage(fixture.ground.level,parent,player,6,2,12)&&shield.getDamageValue()==0&&player.getHealth()==hp,"Vetoed shield impact spends shield"); }
                finally { NeoForge.EVENT_BUS.unregister(veto); }
                helper.assertTrue(ShoggothAcid.damage(fixture.ground.level,parent,player,6,2,12)&&shield.getDamageValue()==12&&player.getHealth()==hp-1,"Native shield acid does not wear twelve and chip exactly one HP"); helper.succeed();
            } finally { player.stopUsingItem(); fixture.ground.level.getServer().getPlayerList().remove(player); fixture.close(); }
        });
    }

    @GameTest(template="empty",batch="shoggoth_acid_native_flight",timeoutTicks=100)
    public static void NativeProjectileFlightActuallyHitsItsTargetAndRemovesTheProjectile(GameTestHelper helper) {
        var fixture=new Fixture(helper); var parent=fixture.parent(helper,Kind.NORMAL); var cow=fixture.cow(helper,parent,4); float initialHealth=cow.getHealth();
        var projectile=parent.shootAcidAt(cow,1,0);
        try { helper.assertTrue(projectile!=null&&projectile.isAddedToLevel()&&projectile.getOwner()==parent,"Real native acid shot not admitted"); }
        catch(RuntimeException error) { fixture.close(); throw error; }
        helper.runAfterDelay(20,()-> {
            try { helper.assertTrue(projectile.isRemoved()&&cow.getHealth()==initialHealth-6,"Actual throwing physics did not deliver one six-point hit: removed="+projectile.isRemoved()+", hp="+cow.getHealth()+", pos="+projectile.position()); helper.succeed(); }
            finally { fixture.close(); }
        });
    }

    @GameTest(template="empty",batch="shoggoth_acid_impact_veto",timeoutTicks=100)
    public static void NativeProjectileImpactVetoPreventsDamageWithoutCreatingAnotherShot(GameTestHelper helper) {
        var fixture=new Fixture(helper); var parent=fixture.parent(helper,Kind.NORMAL); var cow=fixture.cow(helper,parent,4); float initialHealth=cow.getHealth(); int[] impacts={0};
        var projectile=parent.shootAcidAt(cow,1,0);
        helper.assertTrue(projectile!=null&&projectile.isAddedToLevel(),"Veto fixture has no admitted native shot");
        Consumer<ProjectileImpactEvent> veto=event->{ if(event.getProjectile()==projectile) { impacts[0]++; event.setCanceled(true); } };
        NeoForge.EVENT_BUS.addListener(veto);
        // Chunk ticking can begin after the test callback. Wait for the actual
        // native collision event, not twenty global ticks with no flight proof.
        helper.startSequence().thenWaitUntil(()->helper.assertTrue(impacts[0]>0,
            "No native impact yet: shotTicks="+projectile.tickCount+", position="+projectile.position()+", target="+cow.position()+", removed="+projectile.isRemoved()))
            .thenExecute(()->{
                try { helper.assertTrue(cow.getHealth()==initialHealth&&fixture.shots.size()==1,"Native impact cancellation bypassed or duplicated"); }
                finally { NeoForge.EVENT_BUS.unregister(veto); fixture.close(); }
            }).thenSucceed();
        // A timeout must release its scoped listener and native fixture too.
        helper.runAfterDelay(99,()->{NeoForge.EVENT_BUS.unregister(veto);fixture.close();});
    }

    @GameTest(template="empty",batch="shoggoth_acid_saved_lifetime")
    public static void SavedProjectileLifetimeAndCooldownAreBoundedAndReleasedPassengersRemainAlive(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var parent=fixture.parent(helper,Kind.NORMAL); var projectile=ShoggothEcologyContent.ACID_PROJECTILE.get().create(fixture.ground.level);
            projectile.moveTo(parent.position().add(2,2,0)); projectile.setNoGravity(true); projectile.setOwner(parent); fixture.entities.add(projectile);
            helper.assertTrue(fixture.ground.level.addFreshEntity(projectile),"Native expiration shot not admitted");
            var child=fixture.parent(helper,Kind.LESSER); helper.assertTrue(child.startRiding(projectile,true),"Native newborn cannot ride acid");
            var tag=new CompoundTag(); projectile.addAdditionalSaveData(tag); tag.putInt("AcidTicks",599); projectile.readAdditionalSaveData(tag); projectile.tick();
            helper.assertTrue(projectile.isRemoved()&&!child.isPassenger()&&child.isAlive()&&child.isAddedToLevel()&&!child.isRemoved(),"Expired flight deletes or keeps riding newborn");
            var parentSave=new CompoundTag(); parent.addAdditionalSaveData(parentSave); parentSave.putInt("AcidCooldown",99999); parent.readAdditionalSaveData(parentSave);
            helper.assertTrue(parent.acidCooldown()==120,"Malformed saved cooldown unbounded"); parent.aiStep();
            var copy=ShoggothEcologyContent.type(Kind.NORMAL,Habitat.OVERWORLD).create(fixture.ground.level);
            try { parent.addAdditionalSaveData(parentSave); copy.readAdditionalSaveData(parentSave); helper.assertTrue(copy.acidCooldown()==119,"Native acid cooldown does not tick/persist"); }
            finally { copy.discard(); }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_acid_actual_ai",timeoutTicks=100)
    public static void RealServerTicksStartOneSpitForEachLargerFormButNotTheLesser(GameTestHelper helper) {
        var fixture=new Fixture(helper); var parents=new ArrayList<ShoggothEntity>();
        var previousModes=new HashMap<net.minecraft.server.level.ServerPlayer,GameType>();
        helper.getLevel().getServer().getPlayerList().getPlayers().forEach(player->{
            previousModes.put(player,player.gameMode.getGameModeForPlayer()); player.setGameMode(GameType.SPECTATOR);
        });
        for(var kind:Kind.values()) {
            var parent=fixture.parent(helper,kind); var cow=fixture.cow(helper,parent,8);
            parent.setTarget(cow); parent.setNoAi(false); parents.add(parent);
        }
        // A forced chunk becomes entity-ticking asynchronously. Three world
        // ticks do not prove that any parent ticked (the former failure had
        // tickCount=0 on all three). Wait for three real native entity ticks.
        helper.startSequence().thenWaitUntil(()->helper.assertTrue(parents.stream().allMatch(parent->parent.tickCount>=3),
            "Waiting for native entity-ticking chunks")).thenExecute(()-> {
            try {
                // A successful shot may already have hit and left the level.
                // Count captured native admissions, not surviving projectiles.
                helper.assertTrue(fixture.shots.size()==2,"Native ticking AI does not start exactly the two larger spits: shots="+fixture.shots.size()
                    +", parents="+parents.stream().map(parent->parent.kind()+":ticks="+parent.tickCount+",cooldown="+parent.acidCooldown()+",target="+parent.getTarget()).toList());
                for(var parent:parents) {
                    boolean lesser=parent.kind()==Kind.LESSER;
                    long owned=fixture.shots.stream().filter(shot->shot.getOwner()==parent).count();
                    helper.assertTrue(owned==(lesser?0:1)&&(lesser?parent.acidCooldown()==0:parent.acidCooldown()>=117&&parent.acidCooldown()<=120),"Actual form/cooldown differs for "+parent.kind()+": shots="+owned+", cooldown="+parent.acidCooldown());
                }
            } finally { previousModes.forEach((player,mode)->player.setGameMode(mode)); fixture.close(); }
        }).thenSucceed();
    }

    @GameTest(template="empty",batch="shoggoth_acid_spray_layers")
    public static void EachSprayFormCorrodesTwoRealLayersAndAppliesOnlyItsAcceptedEquipmentWear(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var level=fixture.ground.level; level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
            for(var kind:Kind.values()) {
                var parent=fixture.parent(helper,kind); var cow=fixture.cow(helper,parent,1.5); float hp=cow.getHealth();
                var armor=new ItemStack(Items.DIAMOND_CHESTPLATE); cow.setItemSlot(EquipmentSlot.CHEST,armor);
                int upper=(int)Math.floor(parent.getBoundingBox().maxY);
                var marker=new net.minecraft.core.BlockPos(fixture.ground.node.getX()-1,upper,fixture.ground.node.getZ()+1);
                level.setBlock(marker,Blocks.STONE.defaultBlockState(),2); level.setBlock(marker.above(),Blocks.STONE.defaultBlockState(),2);
                helper.assertTrue(ShoggothAcid.splash(parent,true)&&level.isEmptyBlock(marker)&&level.isEmptyBlock(marker.above()),"Spray repeats one layer instead of two for "+kind);
                helper.assertTrue(cow.getHealth()==hp-3&&armor.getDamageValue()==(kind==Kind.LESSER?4:kind==Kind.NORMAL?8:12),"Accepted nearby spray damage/wear differs for "+kind+": hp="+cow.getHealth()+", wear="+armor.getDamageValue());
                cow.invulnerableTime=0; hp=cow.getHealth(); int wear=armor.getDamageValue();
                level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
                helper.assertTrue(!ShoggothAcid.splash(parent,true)&&cow.getHealth()==hp&&armor.getDamageValue()==wear,"Disabled griefing still executes the source-gated spray");
                level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
                parent.discard(); cow.discard();
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_acid_protected_wall",timeoutTicks=100)
    public static void ActualProtectedWallImpactEndsFlightEvenWithGriefingDisabled(GameTestHelper helper) {
        var fixture=new Fixture(helper); var parent=fixture.parent(helper,Kind.NORMAL); var cow=fixture.cow(helper,parent,4); float hp=cow.getHealth();
        var wall=fixture.ground.node.offset(2,0,0);
        for(int y=0;y<3;y++) fixture.ground.level.setBlock(wall.above(y),Blocks.OBSIDIAN.defaultBlockState(),2);
        fixture.ground.level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,fixture.ground.level.getServer());
        var shot=parent.shootAcidAt(cow,1,0);
        helper.runAfterDelay(20,()-> {
            try { helper.assertTrue(shot!=null&&shot.isRemoved()&&cow.getHealth()==hp&&fixture.ground.level.getBlockState(wall).is(Blocks.OBSIDIAN),"Protected wall leaks a projectile or damages its sheltered target"); helper.succeed(); }
            finally { fixture.close(); }
        });
    }

    @GameTest(template="empty",batch="shoggoth_acid_passenger_clearance")
    public static void FullPassengerBodyDismountsOutsideImpactBlocksAndNativeDismountVetoIsPreserved(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var parent=fixture.parent(helper,Kind.NORMAL); var child=fixture.parent(helper,Kind.LESSER);
            child.moveTo(parent.position().add(-2,0,0));
            var shot=ShoggothEcologyContent.ACID_PROJECTILE.get().create(fixture.ground.level); shot.moveTo(parent.position().add(2,1,0)); shot.setNoGravity(true); shot.setOwner(parent); fixture.entities.add(shot);
            helper.assertTrue(fixture.ground.level.addFreshEntity(shot)&&child.startRiding(shot,true),"Clearance carrier/child admission failed");
            var wall=fixture.ground.node.offset(2,0,0);
            for(int y=0;y<3;y++) fixture.ground.level.setBlock(wall.above(y),Blocks.OBSIDIAN.defaultBlockState(),2);
            shot.positionRider(child);
            helper.assertTrue(!fixture.ground.level.noCollision(child,child.getBoundingBox()),"Impact fixture does not contain the actual full rider body");
            Consumer<EntityMountEvent> veto=event->{ if(event.getEntity()==child&&event.isDismounting()) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
            try {
                shot.finish(); helper.assertTrue(!shot.isRemoved()&&child.isPassenger()&&child.isAlive(),"Dismount veto leaves a newborn on a removed vehicle");
                var save=new CompoundTag(); shot.addAdditionalSaveData(save);
                helper.assertTrue(save.getBoolean("ReleasePending")&&!save.getList("LastClearPassengers",net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty(),"Pending release loses its clear body fallback on save");
                shot.readAdditionalSaveData(save);
            } finally { NeoForge.EVENT_BUS.unregister(veto); }
            shot.tick();
            helper.assertTrue(shot.isRemoved()&&!child.isPassenger()&&child.isAlive()&&fixture.ground.level.noCollision(child,child.getBoundingBox()),"Actual native release embeds or deletes the full newborn body"); helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_acid_birth")
    public static void AcceptedNativeBirthCanRideASpitAndProjectileAdmissionVetoKeepsTheChild(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            for(boolean reject:new boolean[]{false,true}) {
                var parent=fixture.parent(helper,Kind.NORMAL); var target=fixture.cow(helper,parent,4); parent.setTarget(target);
                var tag=new CompoundTag(); parent.addAdditionalSaveData(tag); tag.putInt("FoodLevel",8); parent.readAdditionalSaveData(tag);
                long seed=0; while(net.minecraft.util.RandomSource.create(seed).nextInt(3)!=0) seed++; parent.getRandom().setSeed(seed);
                var children=new ArrayList<ShoggothEntity>();
                Consumer<EntityJoinLevelEvent> capture=event->{ if(event.getLevel()==fixture.ground.level&&event.getEntity() instanceof ShoggothEntity mob&&mob!=parent) children.add(mob); };
                Consumer<EntityJoinLevelEvent> veto=event->{ if(reject&&event.getEntity() instanceof ShoggothAcidProjectile) event.setCanceled(true); };
                NeoForge.EVENT_BUS.addListener(capture); NeoForge.EVENT_BUS.addListener(veto);
                try {
                    helper.assertTrue(parent.tryFoodAction()&&parent.foodLevel()==0&&parent.isAlive()&&children.size()==1,"Native parent did not commit exactly one child");
                    var child=children.getFirst(); fixture.entities.add(child);
                    helper.assertTrue(child.isAlive()&&child.isAddedToLevel()&&(reject?!child.isPassenger():child.getVehicle() instanceof ShoggothAcidProjectile),"Birth shot veto removes child, or accepted child does not ride");
                } finally { NeoForge.EVENT_BUS.unregister(capture); NeoForge.EVENT_BUS.unregister(veto); fixture.entities.forEach(Entity::discard); fixture.entities.clear(); }
            }
            helper.succeed();
        }
    }
}
