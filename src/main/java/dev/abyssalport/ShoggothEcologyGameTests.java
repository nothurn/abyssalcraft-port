package dev.abyssalport;

import dev.abyssalport.content.*;
import dev.abyssalport.entity.ShoggothEntity;
import dev.abyssalport.entity.ShoggothEntity.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShoggothEcologyGameTests {
    private ShoggothEcologyGameTests() { }
    private static final class Fixture implements AutoCloseable {
        final GraveyardGameTests.Fixture ground;
        final List<ShoggothEntity> mobs=new ArrayList<>();
        final List<Entity> prey=new ArrayList<>();
        final List<ItemEntity> drops=new ArrayList<>();
        final Consumer<EntityJoinLevelEvent> capture;
        Fixture(GameTestHelper helper) {
            ground=new GraveyardGameTests.Fixture(helper);
            capture=event->{ if(event.getLevel()==ground.level&&event.getEntity().position().distanceTo(ground.node.getCenter())<8) {
                if(event.getEntity() instanceof ShoggothEntity mob) { mob.setNoAi(true); mobs.add(mob); }
                if(event.getEntity() instanceof ItemEntity item) drops.add(item);
            } };
            NeoForge.EVENT_BUS.addListener(capture);
        }
        ShoggothEntity admit(GameTestHelper helper,Kind kind,Habitat habitat) {
            var mob=ShoggothEcologyContent.type(kind,habitat).create(ground.level);
            mob.setNoAi(true); mob.setNoGravity(true);
            mob.moveTo(ground.node.getX()+.5,ground.node.getY(),ground.node.getZ()+.5,0,0);
            helper.assertTrue(ground.level.noCollision(mob)&&ground.level.addFreshEntity(mob),"Ecology fixture must admit a real unobstructed parent");
            return mob;
        }
        <T extends LivingEntity> T kill(GameTestHelper helper,ShoggothEntity killer,EntityType<T> type) {
            var victim=type.create(ground.level); victim.moveTo(killer.position().add(2,0,0)); victim.setNoGravity(true);
            if(victim instanceof Mob mob) mob.setNoAi(true);
            victim.setHealth(1); prey.add(victim);
            helper.assertTrue(ground.level.addFreshEntity(victim)&&killer.doHurtTarget(victim)&&victim.isDeadOrDying(),"Actual native prey death failed");
            return victim;
        }
        ShoggothEntity child(ShoggothEntity parent) {
            return mobs.stream().filter(mob->mob!=parent&&mob.isAlive()&&!mob.isRemoved()&&mob.isAddedToLevel()).findFirst().orElseThrow();
        }
        void clear() { mobs.forEach(Entity::discard); mobs.clear(); prey.forEach(Entity::discard); prey.clear(); drops.forEach(Entity::discard); drops.clear(); }
        @Override public void close() { NeoForge.EVENT_BUS.unregister(capture); clear(); ground.close(); }
    }
    private static void savedFood(ShoggothEntity mob,int amount) {
        var tag=new CompoundTag(); mob.addAdditionalSaveData(tag); tag.putInt("FoodLevel",amount); mob.readAdditionalSaveData(tag);
    }

    @GameTest(template="empty",batch="shoggoth_ecology_native_values")
    public static void NineNativeTypesKeepSourceSizesHealthAttacksAndSpecificImmunities(GameTestHelper helper) {
        var level=helper.getLevel(); var arrow=EntityType.ARROW.create(level);
        try {
            for(var habitat:Habitat.values()) for(var kind:Kind.values()) {
                var mob=ShoggothEcologyContent.type(kind,habitat).create(level);
                try {
                    helper.assertTrue(mob.kind()==kind&&mob.habitat()==habitat&&mob.getMaxHealth()==kind.health
                        &&mob.getAttributeValue(Attributes.ATTACK_DAMAGE)==kind.attack
                        &&Math.abs(mob.getBbWidth()-kind.width)<.0001&&Math.abs(mob.getBbHeight()-kind.height)<.0001,"Native source form attributes or dimensions differ: "+kind+"/"+habitat);
                    helper.assertTrue(mob.canBreatheUnderwater()&&!mob.isInvertedHealAndHarm()&&!mob.isPushedByFluid(),"Forms lost breathing or acquired undead classification");
                    helper.assertTrue(!mob.addEffect(new MobEffectInstance(MobEffects.POISON,100))&&mob.canBeAffected(new MobEffectInstance(MobEffects.REGENERATION,100)),"Source poison immunity became general potion immunity");
                    helper.assertTrue(!mob.hurt(level.damageSources().arrow(arrow,null),5)&&!mob.hurt(level.damageSources().cactus(),5)
                        &&!mob.causeFallDamage(30,1,level.damageSources().fall())&&mob.getHealth()==kind.health,"Native projectile/cactus/fall immunity differs");
                    helper.assertTrue(mob.hurt(level.damageSources().generic(),1)&&mob.getHealth()==kind.health-1,"Immunity wrongly blocks ordinary damage");
                    if(habitat!=Habitat.OVERWORLD) {
                        var disease=habitat==Habitat.ABYSSAL?WastelandContent.CORALIUM_PLAGUE:DreadlandsContent.PLAGUE;
                        helper.assertTrue(!mob.addEffect(new MobEffectInstance(disease,100)),"Matching dimensional plague harms its carrier");
                        helper.assertTrue(mob.getType().is(TagKey.create(Registries.ENTITY_TYPE,WastelandContent.id(habitat==Habitat.ABYSSAL?"abyssal_rending_targets":"dread_rending_targets"))),"Dimensional form is not a native rending target");
                    }
                } finally { mob.discard(); }
            }
            helper.succeed();
        } finally { arrow.discard(); }
    }

    @GameTest(template="empty",batch="shoggoth_ecology_actual_food")
    public static void ActualCowAndChickenDeathsFeedOnceThenGrowWithNameFoodAndCooldownPreserved(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            for(var habitat:Habitat.values()) {
                var parent=fixture.admit(helper,Kind.LESSER,habitat);
                parent.setCustomName(Component.literal("Lair keeper")); parent.setCustomNameVisible(true); parent.setPersistenceRequired();
                var save=new CompoundTag(); parent.addAdditionalSaveData(save); save.putInt("MonolithTimer",891); parent.readAdditionalSaveData(save);
                var cow=fixture.kill(helper,parent,EntityType.COW);
                helper.assertTrue(parent.foodLevel()==(int)Math.ceil(cow.getBbWidth()*cow.getBbHeight()/.25F)&&parent.foodLevel()==6,"Actual cow death did not supply exactly six points");
                int before=parent.foodLevel(); cow.invulnerableTime=0; cow.hurt(fixture.ground.level.damageSources().mobAttack(parent),10);
                helper.assertTrue(parent.foodLevel()==before&&!parent.tryFoodAction(),"Dead prey feeds twice or growth ignores eight-point threshold");
                fixture.kill(helper,parent,EntityType.CHICKEN); fixture.kill(helper,parent,EntityType.CHICKEN);
                helper.assertTrue(parent.foodLevel()==10&&parent.tryFoodAction(),"Actual native deaths do not reach and admit growth");
                var grown=fixture.child(parent);
                helper.assertTrue(parent.isRemoved()&&grown.kind()==Kind.NORMAL&&grown.habitat()==habitat&&grown.foodLevel()==2
                    &&grown.monolithTimer()==891&&grown.isPersistenceRequired()&&grown.hasCustomName()
                    &&grown.getCustomName().getString().equals("Lair keeper")&&grown.isCustomNameVisible(),"Accepted growth loses native habitat/name/cooldown/remaining food");
                helper.assertTrue(fixture.drops.stream().anyMatch(drop->drop.getItem().is(Items.BEEF)),"Feeding suppressed native prey loot");
                fixture.clear();
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_ecology_nonfood")
    public static void NativeZombieAndSkeletonDeathsDoNotFeedAndSavedFoodIsBounded(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var mob=fixture.admit(helper,Kind.LESSER,Habitat.OVERWORLD);
            fixture.kill(helper,mob,EntityType.ZOMBIE); fixture.kill(helper,mob,EntityType.SKELETON);
            helper.assertTrue(mob.foodLevel()==0,"Undead combat targets became edible prey");
            fixture.kill(helper,mob,EntityType.CHICKEN); var tag=new CompoundTag(); mob.addAdditionalSaveData(tag);
            var copy=ShoggothEcologyContent.type(Kind.LESSER,Habitat.OVERWORLD).create(fixture.ground.level);
            try {
                copy.readAdditionalSaveData(tag); helper.assertTrue(copy.foodLevel()==2,"Native food save loses progress");
                tag.putInt("FoodLevel",Integer.MAX_VALUE); tag.putInt("MonolithTimer",Integer.MAX_VALUE); copy.readAdditionalSaveData(tag);
                helper.assertTrue(copy.foodLevel()==1_000_000&&copy.monolithTimer()==1500,"Corrupt saved ecology exceeds bounds");
                copy.readAdditionalSaveData(new CompoundTag()); helper.assertTrue(copy.foodLevel()==0,"Legacy missing food creates nutrition");
            } finally { copy.discard(); }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_ecology_vetos")
    public static void GrowthWaitsForHeadroomFinalizationAndNativeAdmissionWithoutSpendingFood(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var parent=fixture.admit(helper,Kind.LESSER,Habitat.ABYSSAL); savedFood(parent,8);
            var ceiling=fixture.ground.node.above();
            fixture.ground.level.setBlock(ceiling,Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.TOP),2);
            helper.assertTrue(fixture.ground.level.noCollision(parent)&&!parent.tryFoodAction()&&parent.foodLevel()==8&&!parent.isRemoved(),"Blocked larger body deletes parent or food");
            fixture.ground.level.setBlock(ceiling,Blocks.AIR.defaultBlockState(),2);
            Consumer<FinalizeSpawnEvent> veto=event->{ if(event.getEntity() instanceof ShoggothEntity) event.setSpawnCancelled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
            try { helper.assertTrue(!parent.tryFoodAction()&&parent.foodLevel()==8&&!parent.isRemoved(),"Finalize veto spends transformation"); }
            finally { NeoForge.EVENT_BUS.unregister(veto); }
            Consumer<EntityJoinLevelEvent> reject=event->{ if(event.getEntity() instanceof ShoggothEntity) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(reject);
            try { helper.assertTrue(!parent.tryFoodAction()&&parent.foodLevel()==8&&!parent.isRemoved(),"Admission veto spends transformation"); }
            finally { NeoForge.EVENT_BUS.unregister(reject); }
            Consumer<EntityJoinLevelEvent> remove=event->{ if(event.getEntity() instanceof ShoggothEntity) event.getEntity().discard(); };
            NeoForge.EVENT_BUS.addListener(remove);
            try { helper.assertTrue(!parent.tryFoodAction()&&parent.foodLevel()==8&&!parent.isRemoved(),"Removed-on-admission child spends parent"); }
            finally { NeoForge.EVENT_BUS.unregister(remove); }
            helper.assertTrue(parent.tryFoodAction()&&fixture.child(parent).habitat()==Habitat.ABYSSAL,"Later valid space/admission cannot resume growth"); helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_ecology_offspring")
    public static void NativeNormalAndGreaterParentsCreateMatchingSmallerChildrenButRemainAlive(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            for(var habitat:Habitat.values()) for(var kind:List.of(Kind.NORMAL,Kind.GREATER)) {
                var parent=fixture.admit(helper,kind,habitat); savedFood(parent,10); parent.setCustomName(Component.literal("Parent"));
                helper.assertTrue(parent.tryFoodAction(),"Source parent cannot admit smaller child: "+kind+"/"+habitat);
                var child=fixture.child(parent);
                helper.assertTrue(parent.isAlive()&&parent.isAddedToLevel()&&!parent.isRemoved()&&parent.foodLevel()==2
                    &&child.kind()==(kind==Kind.NORMAL?Kind.LESSER:Kind.NORMAL)&&child.habitat()==habitat&&child.foodLevel()==0
                    &&!child.hasCustomName(),"Parent/birth habitat/source food rules differ"); fixture.clear();
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",batch="shoggoth_ecology_plague")
    public static void MatchingPlagueOnlyFollowsAcceptedNativeMeleeNotDamageVeto(GameTestHelper helper) {
        var level=helper.getLevel();
        for(var habitat:List.of(Habitat.ABYSSAL,Habitat.DREAD)) for(var kind:Kind.values()) {
            var mob=ShoggothEcologyContent.type(kind,habitat).create(level); var cow=EntityType.COW.create(level);
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100);
            var disease=habitat==Habitat.ABYSSAL?WastelandContent.CORALIUM_PLAGUE:DreadlandsContent.PLAGUE;
            Consumer<LivingIncomingDamageEvent> veto=event->{ if(event.getEntity()==cow) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
            try { helper.assertTrue(!mob.doHurtTarget(cow)&&!cow.hasEffect(disease)&&cow.getHealth()==100,"Vetoed melee still applies plague"); }
            finally { NeoForge.EVENT_BUS.unregister(veto); }
            try {
                cow.invulnerableTime=0; helper.assertTrue(mob.doHurtTarget(cow)&&cow.getHealth()==100-kind.attack&&cow.hasEffect(disease)
                    &&cow.getEffect(disease).getDuration()==100,"Actual accepted dimensional melee lacks matching source damage/plague");
            } finally { mob.discard(); cow.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="shoggoth_ecology_actual_remains")
    public static void ActualDeathsOfAllNineFormsSupplyTheirConsumedDimensionalCrystallizationOutputs(GameTestHelper helper) {
        try(var fixture=new Fixture(helper)) {
            var level=fixture.ground.level; var pos=fixture.ground.node;
            for(var habitat:Habitat.values()) for(var kind:Kind.values()) {
                var flesh=habitat==Habitat.OVERWORLD?GraveyardContent.SHOGGOTH_FLESH.get():habitat==Habitat.ABYSSAL?GraveyardContent.ABYSSAL_SHOGGOTH_FLESH.get():GraveyardContent.DREADED_SHOGGOTH_FLESH.get();
                for(int attempt=0;attempt<24&&fixture.drops.stream().mapToInt(drop->drop.getItem().getCount()).sum()<2;attempt++) {
                    var mob=fixture.admit(helper,kind,habitat);
                    helper.assertTrue(mob.hurt(level.damageSources().genericKill(),1000),"Actual admitted form did not die");
                }
                helper.assertTrue(!fixture.drops.isEmpty()&&fixture.drops.stream().allMatch(drop->drop.isAddedToLevel()&&!drop.isRemoved()&&drop.getItem().is(flesh)&&drop.getItem().getCount()<=2),"Real form death loses matching 0..2 native remains");
                var input=ItemStack.EMPTY;
                for(var drop:fixture.drops) {
                    int amount=Math.min(2-input.getCount(),drop.getItem().getCount());
                    if(amount>0) { var taken=drop.getItem().split(amount); if(input.isEmpty()) input=taken; else input.grow(taken.getCount()); }
                    if(input.getCount()==2) break;
                }
                helper.assertTrue(input.getCount()==2,"Bounded native deaths provided no two actual remains");
                level.setBlock(pos,CrystallizationContent.MACHINE.get().defaultBlockState(),3);
                var machine=(dev.abyssalport.block.entity.CrystallizerBlockEntity)level.getBlockEntity(pos);
                try {
                    machine.setItem(0,input); machine.setItem(1,new ItemStack(Items.BLAZE_POWDER));
                    for(int tick=0;tick<200;tick++) dev.abyssalport.block.entity.CrystallizerBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
                    helper.assertTrue(machine.getItem(0).getCount()==1&&machine.getItem(2).is(CrystallizationContent.material("phosphorus").shard().get())&&machine.getItem(2).getCount()==8,"Native form remains are not consumed once into eight phosphorus");
                    helper.assertTrue(habitat==Habitat.OVERWORLD?machine.getItem(3).isEmpty():machine.getItem(3).is(CrystallizationContent.material(habitat==Habitat.ABYSSAL?"coralium":"dreadium").fragment().get())&&machine.getItem(3).getCount()==2,"Native dimensional remains lose their two matching fragments");
                } finally { machine.clearContent(); level.setBlock(pos,Blocks.AIR.defaultBlockState(),3); fixture.clear(); }
            }
            helper.succeed();
        }
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="shoggoth_ecology_draw",timeoutTicks=300)
    public static void DimensionalBiomassDrawReachablySuppliesAllThreeFormsWhileOverworldStaysLesser(GameTestHelper helper) {
        var random=net.minecraft.util.RandomSource.create(81055);
        for(var habitat:Habitat.values()) {
            var level=habitat==Habitat.OVERWORLD?helper.getLevel():helper.getLevel().getServer().getLevel(habitat==Habitat.ABYSSAL?WastelandContent.DIMENSION:DreadlandsContent.DIMENSION);
            helper.assertTrue(level!=null,"Ecology dimension missing"); var counts=new EnumMap<Kind,Integer>(Kind.class);
            for(int i=0;i<4000;i++) {
                var mob=ShoggothEcologyContent.biomassType(level,random).create(level);
                try { helper.assertTrue(mob.habitat()==habitat,"Biomass loses actual dimensional habitat"); counts.merge(mob.kind(),1,Integer::sum); }
                finally { mob.discard(); }
            }
            helper.assertTrue(habitat==Habitat.OVERWORLD?counts.size()==1&&counts.get(Kind.LESSER)==4000:
                counts.size()==3&&counts.get(Kind.LESSER)>1800&&counts.get(Kind.NORMAL)>1800&&counts.get(Kind.GREATER)>0&&counts.get(Kind.GREATER)<60,"Native seeded source draw lost a form or changed its rarity: "+counts);
        }
        helper.succeed();
    }
}
