package dev.abyssalport;

import com.mojang.authlib.GameProfile;
import dev.abyssalport.block.EnergyPedestalBlock;
import dev.abyssalport.block.EnergyRelayBlock;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.content.*;
import dev.abyssalport.content.EnergyDeviceContent.Kind;
import dev.abyssalport.energy.*;
import dev.abyssalport.item.*;
import dev.abyssalport.menu.EnergyContainerMenu;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SacrificialAltarGameTests {
    private SacrificialAltarGameTests() { }
    /** Separate vertical band avoids neighbouring ecology fixtures' living targets. */
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level; final BlockPos node;
        final Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
        final List<ChunkPos> forced=new ArrayList<>();
        final List<Entity> mobs=new ArrayList<>(); final List<ItemEntity> drops=new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> capture;
        Fixture(GameTestHelper helper) {
            level=helper.getLevel();node=helper.absolutePos(new BlockPos(3,30,3));
            for(var p:BlockPos.betweenClosed(node.offset(-4,-1,-4),node.offset(4,3,4))) {
                helper.assertTrue(level.getBlockEntity(p)==null,"Altar test overlaps an existing block entity");saved.put(p.immutable(),level.getBlockState(p));
            }
            saved.keySet().forEach(p->level.setBlock(p,p.getY()==node.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2));
            for(int x=(node.getX()-9)>>4;x<=(node.getX()+9)>>4;x++)for(int z=(node.getZ()-9)>>4;z<=(node.getZ()+9)>>4;z++) {
                var key=new ChunkPos(x,z);if(!level.getForcedChunks().contains(key.toLong())) {forced.add(key);level.setChunkForced(x,z,true);}
            }
            capture=event->{if(event.getLevel()==level && event.getEntity() instanceof ItemEntity item && item.position().distanceTo(node.getCenter())<12)drops.add(item);};
            NeoForge.EVENT_BUS.addListener(capture);
        }
        SacrificialAltarBlockEntity place(EnergyDeviceTier tier) {
            if(level.getBlockEntity(node) instanceof EnergyPedestalBlockEntity old)old.removeItem();
            level.setBlock(node,Blocks.AIR.defaultBlockState(),2);
            level.setBlock(node,SacrificialAltarContent.block(tier).defaultBlockState(),2);
            return (SacrificialAltarBlockEntity)level.getBlockEntity(node);
        }
        <T extends Mob> T spawn(EntityType<T> type, Vec3 location) {
            T mob=type.create(level);mob.moveTo(location);mob.setNoAi(true);mob.setNoGravity(true);
            if(!level.addFreshEntity(mob))throw new GameTestAssertException("Altar fixture mob rejected");mobs.add(mob);return mob;
        }
        Cow cow(float health) {
            var cow=spawn(EntityType.COW,node.getBottomCenter().add(2,0,0));
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);cow.setHealth(health);return cow;
        }
        void tick(SacrificialAltarBlockEntity altar,int count) {
            for(int tick=0;tick<count;tick++)SacrificialAltarBlockEntity.serverTick(level,node,altar.getBlockState(),altar);
        }
        void clearMobs() {mobs.forEach(Entity::discard);mobs.clear();drops.forEach(Entity::discard);drops.clear();}
        @Override public void close() {
            NeoForge.EVENT_BUS.unregister(capture);clearMobs();
            saved.keySet().forEach(p->{if(level.getBlockEntity(p) instanceof EnergyPedestalBlockEntity altar)altar.removeItem();
                if(level.getBlockEntity(p) instanceof EnergyContainerBlockEntity store)for(int slot=0;slot<2;slot++)store.items().setStackInSlot(slot,ItemStack.EMPTY);});
            saved.forEach((p,state)->level.setBlock(p,state,2));forced.forEach(key->level.setChunkForced(key.x,key.z,false));
        }
    }
    private static FakePlayer player(Fixture fixture) {
        var player=new FakePlayer(fixture.level,new GameProfile(UUID.randomUUID(),"altar-audit"));
        player.setGameMode(GameType.SURVIVAL);player.moveTo(fixture.node.getCenter().add(-2,0,0));return player;
    }
    private static void kill(GameTestHelper helper,Mob mob) {
        mob.invulnerableTime=0;
        helper.assertTrue(mob.hurt(mob.damageSources().genericKill(),10000) && mob.isDeadOrDying(),"Native fixture did not admit a real death");
    }
    private static ItemStack charged(Item item,float energy) {
        var stack=new ItemStack(item);EnergyStorageItem.addEnergy(stack,energy);return stack;
    }
    private static CompoundTag counters(int limit,int cooldown) {
        var data=new CompoundTag();data.putInt("CollectionLimit",limit);data.putInt("CoolDown",cooldown);return data;
    }

    @GameTest(template="empty",batch="sacrifice_craft_anvil")
    public static void CanonicalNativeCraftAndAllSixRealAnvilTakesPreserveSourceCountersAndComponents(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var player=player(fixture);fixture.level.setBlock(fixture.node,Blocks.CRAFTING_TABLE.defaultBlockState(),2);
            var craft=new CraftingMenu(13,player.getInventory(),ContainerLevelAccess.create(fixture.level,fixture.node));
            for(int slot=0;slot<9;slot++) {
                Item item=slot==0||slot==2?AbyssalPort.SHADOW_FRAGMENT.get():slot==1?CoraliumContent.PEARL.get():slot==4?AbyssalPort.SHADOW_GEM.get():ShoggothLairContent.MONOLITH_STONE.get().asItem();
                craft.getSlot(slot+1).set(new ItemStack(item));
            }
            helper.assertTrue(craft.getSlot(0).getItem().is(SacrificialAltarContent.block(EnergyDeviceTier.BASE).asItem()),"Source altar recipe is not a native survival craft");
            helper.assertTrue(!craft.quickMoveStack(player,0).isEmpty() && player.getInventory().countItem(SacrificialAltarContent.block(EnergyDeviceTier.BASE).asItem())==1,"Native craft take loses or duplicates altar");
            for(int slot=1;slot<=9;slot++)helper.assertTrue(craft.getSlot(slot).getItem().isEmpty(),"Altar craft leaves an unpaid ingredient");
            player.getInventory().clearContent();
            for(var old:EnergyDeviceContent.REACHABLE)for(var next:EnergyDeviceContent.REACHABLE)if(next.ordinal()>old.ordinal()) {
                var input=charged(SacrificialAltarContent.block(old).asItem(),321.5F);
                input.set(DataComponents.CUSTOM_NAME,Component.literal("Remembered altar"));
                var tag=input.get(DataComponents.CUSTOM_DATA).copyTag();tag.putInt("CollectionLimit",93);tag.putInt("CoolDown",1197);tag.putString("PrivateNote","retain");
                input.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));var snapshot=input.copy();player.experienceLevel=5;
                var menu=new AnvilMenu(14,player.getInventory());menu.getSlot(0).set(input);menu.getSlot(1).set(new ItemStack(EnergyDeviceContent.ring(next),3));
                var result=menu.getSlot(2).getItem();
                helper.assertTrue(result.is(SacrificialAltarContent.block(next).asItem()) && ItemStack.matches(snapshot,input)
                    && result.getComponentsPatch().equals(snapshot.getComponentsPatch()) && menu.getCost()==1 && menu.repairItemCountCost==1,"Actual altar forging changes counters/name/PE/components or cost");
                helper.assertTrue(menu.getSlot(2).mayPickup(player),"Actual altar result is inaccessible");
                var taken=menu.getSlot(2).remove(1);menu.getSlot(2).onTake(player,taken);
                helper.assertTrue(player.experienceLevel==4 && menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().getCount()==2 && !menu.getSlot(2).hasItem(),"Altar take bypasses native material/XP charge");
                helper.assertTrue(ItemStack.matches(taken,ItemStack.parse(fixture.level.registryAccess(),taken.save(fixture.level.registryAccess())).orElseThrow()),"Forged altar save loses actual components");
            }
            var invalid=new AnvilMenu(15,player.getInventory());invalid.getSlot(0).set(new ItemStack(SacrificialAltarContent.block(EnergyDeviceTier.BASE)));
            invalid.getSlot(1).set(new ItemStack(EnergyDeviceContent.ring(EnergyDeviceTier.OVERWORLD)));
            invalid.getSlot(0).getItem().setCount(2);invalid.createResult();
            helper.assertTrue(!invalid.getSlot(2).hasItem() && invalid.getSlot(0).getItem().getCount()==2,"Corrupt altar stack is duplicated/lost by forging");helper.succeed();
        }
    }

    @GameTest(template="empty",batch="sacrifice_target_admission")
    public static void ActualAdultTargetsExcludeBabiesNativeUndeadAndBothShadowFamiliesAtEveryTier(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            for(var tier:EnergyDeviceContent.REACHABLE) {
                fixture.clearMobs();var altar=fixture.place(tier);
                var baby=fixture.cow(10);baby.setAge(-24000);
                fixture.spawn(EntityType.ZOMBIE,fixture.node.getBottomCenter().add(-2,0,0));
                fixture.spawn(WastelandMobsContent.ABYSSAL_ZOMBIE.get(),fixture.node.getBottomCenter().add(-2,0,1));
                fixture.spawn(ShadowFamilyContent.MONSTER.get(),fixture.node.getBottomCenter().add(0,0,2));
                fixture.spawn(GraveyardContent.SHADOW_GHOUL.get(),fixture.node.getBottomCenter().add(0,0,-2));
                var outside=fixture.spawn(EntityType.COW,fixture.node.getBottomCenter().add(20,0,0));
                var tooHigh=fixture.spawn(EntityType.COW,fixture.node.getBottomCenter().add(0,10,0));
                fixture.tick(altar,1);helper.assertTrue(altar.targetCount()==0 && altar.energy()==0,"Altar admits child/undead/Shadow/outside/vertical target");
                var adults=new ArrayList<Cow>();for(int index=0;index<5;index++)adults.add(fixture.cow(10));
                fixture.tick(altar,1);
                helper.assertTrue(altar.targetCount()==1+tier.ordinal() && adults.stream().filter(mob->mob.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)).count()==altar.maxTargets()
                    && !baby.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING) && !outside.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING) && !tooHigh.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),"Native target cap or selected-mob glow differs at "+tier);
                helper.assertTrue(altar.capacity()==tier.pedestalCapacity() && altar.cooldownStart()==1200-200*tier.ordinal() && altar.energy()==0,"Source tier values or spontaneous PE differ");
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="sacrifice_native_deaths")
    public static void NativeDeathAdmissionVetoRemovedMobsAndCancelledLootRemainDistinct(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var altar=fixture.place(EnergyDeviceTier.BASE);var fractional=fixture.cow(10.5F);fixture.tick(altar,1);
            kill(helper,fractional);fixture.tick(altar,1);
            helper.assertTrue(altar.energy()==10.5F && altar.collectionLimit()==10 && altar.targetCount()==0,"Accepted native death does not grant source max HP once with source integer collection count");
            fixture.tick(altar,3);helper.assertTrue(altar.energy()==10.5F,"Already collected death grants twice");fixture.clearMobs();
            var vetoed=fixture.cow(17);fixture.tick(altar,1);
            Consumer<LivingDeathEvent> veto=event->{if(event.getEntity()==vetoed)event.setCanceled(true);};NeoForge.EVENT_BUS.addListener(veto);
            try {vetoed.hurt(vetoed.damageSources().genericKill(),10000);fixture.tick(altar,1);
                helper.assertTrue(altar.energy()==10.5F && altar.targetCount()==0 && !vetoed.isRemoved(),"Cancelled native death grants PE");}
            finally {NeoForge.EVENT_BUS.unregister(veto);fixture.clearMobs();}
            var discarded=fixture.cow(19);fixture.tick(altar,1);discarded.discard();fixture.tick(altar,1);
            helper.assertTrue(altar.energy()==10.5F && altar.targetCount()==0,"Discarding a living mob for a transformation grants PE");fixture.clearMobs();
            var noLoot=fixture.cow(12);fixture.tick(altar,1);
            Consumer<LivingDropsEvent> cancelDrops=event->{if(event.getEntity()==noLoot)event.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH,cancelDrops);
            try {kill(helper,noLoot);fixture.tick(altar,1);helper.assertTrue(altar.energy()==22.5F,"Cancelled loot wrongly cancels an admitted death's PE");}
            finally {NeoForge.EVENT_BUS.unregister(cancelDrops);fixture.clearMobs();}
            boolean oldRule=fixture.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
            fixture.level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false,fixture.level.getServer());
            try {var noRuleLoot=fixture.cow(13);fixture.tick(altar,1);kill(helper,noRuleLoot);fixture.tick(altar,1);
                helper.assertTrue(altar.energy()==35.5F,"doMobLoot=false removes death-energy progression");}
            finally {fixture.level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(oldRule,fixture.level.getServer());}
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="sacrifice_counter_cooldown")
    public static void EveryTierKeepsThresholdCooldownPendingDeathAndFractionalCapacityAcrossNativeSaves(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            for(var tier:EnergyDeviceContent.REACHABLE) {
                fixture.clearMobs();var altar=fixture.place(tier);altar.restoreItemCounters(counters(altar.capacity()/5-5,0));
                var victim=fixture.cow(10.5F);fixture.tick(altar,1);kill(helper,victim);fixture.tick(altar,1);
                helper.assertTrue(altar.energy()==10.5F && altar.collectionLimit()==0 && altar.cooldown()==altar.cooldownStart(),"Source threshold/cooldown is not connected at "+tier);
                var saved=altar.saveWithFullMetadata(fixture.level.registryAccess());var restored=new SacrificialAltarBlockEntity(fixture.node,altar.getBlockState());restored.loadWithComponents(saved,fixture.level.registryAccess());
                helper.assertTrue(restored.cooldown()==altar.cooldownStart() && restored.energy()==10.5F && restored.targetCount()==0,"Native save resets cooldown or persists live entity targets");
                fixture.tick(altar,20);helper.assertTrue(altar.cooldown()==altar.cooldownStart()-20,"Source cooldown is not measured in real ticks");fixture.clearMobs();
                altar.restoreItemCounters(counters(0,3));var pending=fixture.cow(7.25F);fixture.tick(altar,1);kill(helper,pending);
                fixture.tick(altar,1);helper.assertTrue(altar.energy()==10.5F && altar.targetCount()==1,"Cooling altar loses bounded pending death or grants it early");
                fixture.tick(altar,1);helper.assertTrue(altar.energy()==17.75F && altar.collectionLimit()==7 && altar.targetCount()==0 && altar.cooldown()==0,"Pending death is not collected once at cooldown end");
                fixture.clearMobs();altar.consumeEnergy(altar.energy());altar.addEnergy(altar.capacity());var full=fixture.cow(8);fixture.tick(altar,1);kill(helper,full);fixture.tick(altar,1);
                helper.assertTrue(altar.energy()==altar.capacity() && altar.targetCount()==1,"Full altar discards or exceeds bounded pending death");
                altar.consumeEnergy(.5F);fixture.tick(altar,1);
                helper.assertTrue(altar.energy()==altar.capacity() && altar.targetCount()==0 && altar.collectionLimit()==15,"Capacity clamp loses source pending/counter semantics");
                var malformed=counters(-20,Integer.MAX_VALUE);malformed.putFloat("PotEnergy",Float.NaN);restored.loadWithComponents(malformed,fixture.level.registryAccess());
                helper.assertTrue(restored.energy()==0 && restored.collectionLimit()==0 && restored.cooldown()==1200,"Invalid save escapes native counter/PE bounds");
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="sacrifice_real_roles")
    public static void AllAltarBEsRejectIncomingBookRelayAndStatuePEButOutputAndTierItemRolesWork(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            for(var tier:EnergyDeviceContent.REACHABLE) {
                var altar=fixture.place(tier);var book=new ItemStack(AbyssalPort.NECRONOMICON.get());PotentialEnergyItems.add(book,100);
                helper.assertTrue(altar.transferFromBook(book)==0 && PotentialEnergyItems.stored(book)==100,"Native altar BE accepts manual incoming PE");
                var relayPos=fixture.node.west();var relayState=AbyssalPort.ENERGY_RELAY.get().defaultBlockState().setValue(EnergyRelayBlock.FACING,Direction.EAST);
                fixture.level.setBlock(relayPos,relayState,2);var relay=(EnergyRelayBlockEntity)fixture.level.getBlockEntity(relayPos);relay.addEnergy(100);
                fixture.level.setBlock(fixture.node.east(2),AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(),2);var further=(EnergyContainerBlockEntity)fixture.level.getBlockEntity(fixture.node.east(2));
                for(int tick=0;tick<40;tick++)EnergyRelayBlockEntity.serverTick(fixture.level,relayPos,relayState,relay);
                helper.assertTrue(altar.energy()==0 && relay.energy()==100 && further.energy()==0,"Relay charges a rejecting first altar or skips it");
                altar.addEnergy(73.75F);fixture.level.setBlock(relayPos,Blocks.AIR.defaultBlockState(),2);
                relayPos=fixture.node.east();fixture.level.setBlock(relayPos,relayState,2);relay=(EnergyRelayBlockEntity)fixture.level.getBlockEntity(relayPos);
                for(int tick=0;tick<20;tick++)EnergyRelayBlockEntity.serverTick(fixture.level,relayPos,relayState,relay);
                helper.assertTrue(altar.energy()==63.75F && relay.energy()==10,"Relay cannot draw PE from actual altar source");
                helper.assertTrue(altar.transferToBook(book)==63.75F && PotentialEnergyItems.stored(book)==163.75F,"Blocked input also incorrectly blocks book output");
                var empty=new ItemStack(SacrificialAltarContent.block(tier));var loaded=charged(SacrificialAltarContent.block(tier).asItem(),53.75F);
                helper.assertTrue(PotentialEnergyItems.canReceive(empty)==(tier!=EnergyDeviceTier.BASE),"Source base/tier altar-item capability conflated");
                var store=(EnergyContainerBlockEntity)fixture.level.getBlockEntity(fixture.node.east(2));store.consumeEnergy(store.energy());store.addEnergy(40);
                store.items().setStackInSlot(1,empty);EnergyContainerBlockEntity.serverTick(fixture.level,store.getBlockPos(),store.getBlockState(),store);
                helper.assertTrue(PotentialEnergyItems.stored(empty)==(tier==EnergyDeviceTier.BASE?0:20) && store.energy()==(tier==EnergyDeviceTier.BASE?40:20),"Native output slot ignores actual altar-item capability");
                store.items().setStackInSlot(1,ItemStack.EMPTY);store.items().setStackInSlot(0,loaded);EnergyContainerBlockEntity.serverTick(fixture.level,store.getBlockPos(),store.getBlockState(),store);
                helper.assertTrue(PotentialEnergyItems.stored(loaded)==33.75F,"Source altar item cannot output its persisted PE");store.items().setStackInSlot(0,ItemStack.EMPTY);
                var user=player(fixture);user.getInventory().setItem(9,new ItemStack(SacrificialAltarContent.block(tier)));
                var menu=new EnergyContainerMenu(17,user.getInventory(),store);helper.assertTrue(!menu.quickMoveStack(user,2).isEmpty()
                    && !store.items().getStackInSlot(tier==EnergyDeviceTier.BASE?0:1).isEmpty(),"Shift-click routes a nonchargeable base altar into the charge slot");
                for(int slot=0;slot<2;slot++)store.items().setStackInSlot(slot,ItemStack.EMPTY);
                var statuePos=fixture.node.north(3);fixture.level.setBlock(statuePos,StatueContent.block(StatueContent.Deity.CTHULHU).defaultBlockState(),2);
                var statue=(DeityStatueBlockEntity)fixture.level.getBlockEntity(statuePos);statue.refreshCollectors(fixture.level);
                helper.assertTrue(statue.collectorPositions().contains(fixture.node),"Altar lost source Collector classification");statue.tickEnergy(fixture.level,new StatueEnergyGameTests.FixedRandom(0));
                helper.assertTrue(altar.energy()==0,"Source Collector marker wrongly allows statue input to altar");fixture.level.setBlock(statuePos,Blocks.AIR.defaultBlockState(),2);
                fixture.level.setBlock(relayPos,Blocks.AIR.defaultBlockState(),2);fixture.level.setBlock(further.getBlockPos(),Blocks.AIR.defaultBlockState(),2);
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="sacrifice_native_use_break_place")
    public static void ActualHandSaveBreakAndPlacementKeepOneBookPEAndCooldownWithoutTilt(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var player=player(fixture);
            for(var tier:EnergyDeviceContent.REACHABLE) {
                var altar=fixture.place(tier);var block=SacrificialAltarContent.block(tier);altar.addEnergy(721.5F);altar.restoreItemCounters(counters(137,1171));
                helper.assertTrue(altar.getType().isValid(altar.getBlockState()) && !altar.getBlockState().hasProperty(EnergyPedestalBlock.TILTED),"Altar has invalid native type or copied tilted pedestal state");
                var book=new ItemStack(AbyssalPort.NECRONOMICON.get());PotentialEnergyItems.add(book,100);book.set(DataComponents.CUSTOM_NAME,Component.literal("Bound book"));
                var before=book.copy();player.setItemInHand(InteractionHand.MAIN_HAND,book);var hit=new BlockHitResult(fixture.node.getCenter(),Direction.UP,fixture.node,false);
                altar.getBlockState().useItemOn(book,fixture.level,player,InteractionHand.MAIN_HAND,hit);
                helper.assertTrue(book.isEmpty() && ItemStack.matches(altar.item(),before),"Actual altar hand-use performs manual transfer instead of singleton insertion");
                var saved=altar.saveWithFullMetadata(fixture.level.registryAccess());var restored=new SacrificialAltarBlockEntity(fixture.node,altar.getBlockState());restored.loadWithComponents(saved,fixture.level.registryAccess());
                helper.assertTrue(ItemStack.matches(restored.item(),before) && restored.cooldown()==1171 && restored.collectionLimit()==137,"Native altar save loses singleton/counters");
                player.getInventory().clearContent();player.setGameMode(GameType.ADVENTURE);altar.getBlockState().useWithoutItem(fixture.level,player,hit);
                helper.assertTrue(ItemStack.matches(altar.item(),before),"Adventure retrieval bypasses native protection");player.setGameMode(GameType.SURVIVAL);
                for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));altar.getBlockState().useWithoutItem(fixture.level,player,hit);
                helper.assertTrue(ItemStack.matches(altar.item(),before),"Full inventory loses actual altar singleton");player.getInventory().clearContent();
                helper.assertTrue(fixture.level.destroyBlock(fixture.node,true),"Actual altar break rejected");
                helper.assertTrue(fixture.drops.size()==2 && fixture.drops.stream().filter(drop->ItemStack.matches(drop.getItem(),before)).count()==1,"Native break loses/duplicates displayed book");
                var dropped=fixture.drops.stream().map(ItemEntity::getItem).filter(stack->stack.is(block.asItem())).findFirst().orElseThrow();
                var data=dropped.get(DataComponents.CUSTOM_DATA).copyTag();
                helper.assertTrue(PotentialEnergyItems.stored(dropped)==721.5F && data.getInt("CollectionLimit")==137 && data.getInt("CoolDown")==1171,"Native altar loot loses PE/counter/cooldown");
                player.setItemInHand(InteractionHand.MAIN_HAND,dropped.copy());
                helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(fixture.node.below().getCenter().add(0,.5,0),Direction.UP,fixture.node.below(),false))).consumesAction(),"Actual native altar placement rejected");
                var placed=(SacrificialAltarBlockEntity)fixture.level.getBlockEntity(fixture.node);
                helper.assertTrue(placed.energy()==721.5F && placed.collectionLimit()==137 && placed.cooldown()==1171 && placed.item().isEmpty() && player.getMainHandItem().isEmpty(),"Actual placed altar resets cooldown, duplicates book or loses PE");
                fixture.drops.forEach(Entity::discard);fixture.drops.clear();
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="sacrifice_registered_live",timeoutTicks=120)
    public static void NativeRegisteredTickerCollectsAcceptedDeathAndChargesBookWithoutManualAltarTicking(GameTestHelper helper) {live(helper);}
    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="sacrifice_normal_live",timeoutTicks=160)
    public static void NormalWorldRegisteredAltarTickerConnectsAcceptedSurvivalDeathToBookPE(GameTestHelper helper) {live(helper);}
    private static void live(GameTestHelper helper) {
        var fixture=new Fixture(helper);
        try {
            var altar=fixture.place(EnergyDeviceTier.BASE);var held=new ItemStack(AbyssalPort.NECRONOMICON.get());held.set(DataComponents.CUSTOM_NAME,Component.literal("Survival book"));altar.placeOne(held);
            var cow=fixture.cow(23.75F);
            helper.runAfterDelay(30,()-> {
                try {
                    helper.assertTrue(altar.targetCount()==1 && cow.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),"Actual registered altar ticker does not select live target");
                    kill(helper,cow);
                    helper.runAfterDelay(30,()-> {
                        try {
                            helper.assertTrue(altar.energy()==0 && PotentialEnergyItems.stored(altar.item())==23.75F && altar.collectionLimit()==23 && altar.targetCount()==0
                                && altar.item().getHoverName().getString().equals("Survival book"),"Native world ticker does not connect real death to exact book PE");
                            com.mojang.logging.LogUtils.getLogger().info("NATIVE SACRIFICIAL ALTAR WORLD AUDIT: accepted cow maxHP=23.75, bookPE={}, altarPE={}, collection={}, targetCount={}",PotentialEnergyItems.stored(altar.item()),altar.energy(),altar.collectionLimit(),altar.targetCount());
                            helper.succeed();
                        } finally {fixture.close();}
                    });
                } catch(RuntimeException error) {fixture.close();throw error;}
            });
        } catch(RuntimeException error) {fixture.close();throw error;}
    }
}
