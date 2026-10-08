package dev.abyssalport;

import dev.abyssalport.content.*;
import dev.abyssalport.entity.*;
import dev.abyssalport.effect.DreadPlagueEffect;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DreadEcologyGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private DreadEcologyGameTests() { }
    private static List<DreadSpawnEntity> near(ServerLevel level,BlockPos center,DreadSpawnEntity.Kind kind) {
        return level.getEntitiesOfClass(DreadSpawnEntity.class,new AABB(center).inflate(32),mob->mob.kind()==kind&&mob.isAlive());
    }
    private static void visible(GameTestHelper helper,Runnable run) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var changed=new ArrayList<ChunkPos>();
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            var chunk=new ChunkPos((pos.getX()>>4)+dx,(pos.getZ()>>4)+dz);
            if(!level.getForcedChunks().contains(chunk.toLong())) { changed.add(chunk); level.setChunkForced(chunk.x,chunk.z,true); }
        }
        helper.runAfterDelay(20,()->{
            try { run.run(); } finally { changed.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false)); }
        });
    }
    private static DreadSpawnEntity spawn(ServerLevel level,BlockPos center,DreadSpawnEntity.Kind kind) {
        var mob=DreadEcologyContent.type(kind).create(level); mob.setNoAi(true); mob.setNoGravity(true); mob.moveTo(center.getX()+.5,center.getY(),center.getZ()+.5,0,0);
        if(!level.addFreshEntity(mob)) throw new GameTestAssertException("Native fixture creature was vetoed"); return mob;
    }
    private static void clean(ServerLevel level,BlockPos pos) {
        for(var entity:level.getEntities((Entity)null,new AABB(pos).inflate(32),e->e instanceof DreadSpawnEntity||e instanceof DreadSlugEntity||e instanceof ItemEntity)) entity.discard();
    }

    @GameTest(template="empty",batch="dread_ecology_attributes")
    public static void SourceNormalAttributesImmunityMeleeAndNativeBiomeEntriesAreLoaded(GameTestHelper helper) {
        var level=helper.getLevel(); var biome=level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME,WastelandContent.id("dreadlands"))).value();
        for(var kind:DreadSpawnEntity.Kind.values()) {
            var mob=DreadEcologyContent.type(kind).create(level); var cow=EntityType.COW.create(level);
            helper.assertTrue(mob.getMaxHealth()==(kind==DreadSpawnEntity.Kind.SMALL?20:kind==DreadSpawnEntity.Kind.GREATER?50:100)
                    &&mob.getAttributeValue(Attributes.ATTACK_DAMAGE)==(kind==DreadSpawnEntity.Kind.SMALL?6:kind==DreadSpawnEntity.Kind.GREATER?12:18),"Creature source health/damage changed");
            helper.assertTrue(mob.fireImmune()&&mob.canBreatheUnderwater()&&DreadPlagueEffect.immune(mob)
                    &&!mob.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100)),"Dread creature immunity is broken");
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40); cow.setHealth(40);
            helper.assertTrue(mob.doHurtTarget(cow)&&cow.getEffect(DreadlandsContent.PLAGUE).getDuration()==100,"Accepted native melee fails to apply plague");
            cow.removeAllEffects(); cow.setInvulnerable(true);
            helper.assertTrue(!mob.doHurtTarget(cow)&&!cow.hasEffect(DreadlandsContent.PLAGUE),"Denied melee still causes disease");
            helper.assertTrue(biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(row->row.type==mob.getType()),"Creature exists only as a creative egg");
        }
        helper.assertTrue(DreadEcologyContent.BEAST.get().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD),"Lesser Dreadbeast lacks native arthropod classification"); helper.succeed();
    }

    @GameTest(template="empty",batch="dread_ecology_merge",timeoutTicks=100)
    public static void NativeMergeVetoKeepsAllInputsThenExactlyFiveMergeNotSix(GameTestHelper helper) {
        visible(helper,()->{
            var level=helper.getLevel(); var center=helper.absolutePos(new BlockPos(2,1,2)); clean(level,center);
            try {
                var members=new ArrayList<DreadSpawnEntity>(); for(int index=0;index<6;index++) members.add(spawn(level,center,DreadSpawnEntity.Kind.SMALL));
                Consumer<EntityJoinLevelEvent> veto=event->{ if(event.getEntity().getType()==DreadEcologyContent.GREATER.get()) event.setCanceled(true); };
                NeoForge.EVENT_BUS.addListener(veto);
                try { helper.assertTrue(!members.getFirst().tryMerge()&&members.stream().allMatch(Entity::isAlive)&&near(level,center,DreadSpawnEntity.Kind.GREATER).isEmpty(),"Denied replacement consumed participants"); }
                finally { NeoForge.EVENT_BUS.unregister(veto); }
                helper.assertTrue(members.getFirst().tryMerge()&&near(level,center,DreadSpawnEntity.Kind.SMALL).size()==1
                        &&near(level,center,DreadSpawnEntity.Kind.GREATER).size()==1,"Native merger consumed six or failed to produce one");
                helper.assertTrue(members.getLast().isAlive()&&members.subList(0,5).stream().allMatch(Entity::isRemoved),"Wrong exact five participants were removed");
                helper.assertTrue(!members.getLast().tryMerge(),"A single leftover merged again");
                var greater=near(level,center,DreadSpawnEntity.Kind.GREATER).getFirst();
                for(int index=0;index<4;index++) spawn(level,center,DreadSpawnEntity.Kind.GREATER);
                helper.assertTrue(greater.tryMerge()&&near(level,center,DreadSpawnEntity.Kind.GREATER).isEmpty()
                        &&near(level,center,DreadSpawnEntity.Kind.BEAST).size()==1,"Five Greater Spawns do not reach one beast");
                helper.succeed();
            } finally { clean(level,center); }
        });
    }

    @GameTest(template="empty",batch="dread_ecology_limits",timeoutTicks=100)
    public static void NativePopulationCapsAndSavedTimersPreventReloadBurst(GameTestHelper helper) {
        visible(helper,()->{
            var level=helper.getLevel(); var center=helper.absolutePos(new BlockPos(2,1,2)); clean(level,center);
            try {
                for(int index=0;index<5;index++) spawn(level,center,DreadSpawnEntity.Kind.SMALL);
                var greater=new ArrayList<DreadSpawnEntity>(); for(int index=0;index<10;index++) greater.add(spawn(level,center,DreadSpawnEntity.Kind.GREATER));
                helper.assertTrue(!near(level,center,DreadSpawnEntity.Kind.SMALL).getFirst().tryMerge()
                        &&near(level,center,DreadSpawnEntity.Kind.SMALL).size()==5,"Merge ignores the ten-greater local cap");
                for(var mob:near(level,center,DreadSpawnEntity.Kind.SMALL)) mob.discard();
                for(int index=0;index<20;index++) spawn(level,center,DreadSpawnEntity.Kind.SMALL);
                // Keep the timed parent inside the 32-block cap, but outside the five-block
                // merger group. A real aiStep must not accidentally exercise both fixtures.
                var parent=greater.getFirst(); parent.moveTo(center.getX()+.5,center.getY(),center.getZ()+8.5,0,0);
                var tag=new CompoundTag(); parent.saveWithoutId(tag); tag.putInt("SmallSpawnTimer",1); parent.load(tag); parent.setNoAi(false); parent.aiStep(); parent.setNoAi(true);
                helper.assertTrue(near(level,center,DreadSpawnEntity.Kind.SMALL).size()==20,"Periodic spawn ignores its source cap of twenty");
                var saved=new CompoundTag(); parent.saveWithoutId(saved);
                helper.assertTrue(saved.getInt("SmallSpawnTimer")==2000,"At-cap spawn retries every tick or loses its period");
                var one=near(level,center,DreadSpawnEntity.Kind.SMALL).getFirst(); one.discard();
                saved.putInt("SmallSpawnTimer",1); parent.load(saved); parent.setNoAi(false); parent.aiStep(); parent.setNoAi(true);
                helper.assertTrue(near(level,center,DreadSpawnEntity.Kind.SMALL).size()==20,"Free periodic slot cannot receive native offspring");
                var restored=DreadEcologyContent.GREATER.get().create(level); var state=new CompoundTag(); parent.saveWithoutId(state); restored.load(state); var reSaved=new CompoundTag(); restored.saveWithoutId(reSaved);
                helper.assertTrue(reSaved.getInt("SmallSpawnTimer")==2000&&reSaved.getInt("GreaterSpawnTimer")==state.getInt("GreaterSpawnTimer"),"Reload changes saved offspring timers");
                helper.succeed();
            } finally { clean(level,center); }
        });
    }

    @GameTest(template="empty",batch="dread_ecology_death",timeoutTicks=100)
    public static void OnlyAcceptedNativeDeathEmitsExactlyTwoChildrenAndRealShardLoot(GameTestHelper helper) {
        visible(helper,()->{
            var level=helper.getLevel(); var center=helper.absolutePos(new BlockPos(2,1,2)); clean(level,center);
            try {
                for(var kind:List.of(DreadSpawnEntity.Kind.GREATER,DreadSpawnEntity.Kind.BEAST)) {
                    clean(level,center); var parent=spawn(level,center,kind); var child=kind==DreadSpawnEntity.Kind.GREATER?DreadSpawnEntity.Kind.SMALL:DreadSpawnEntity.Kind.GREATER;
                    Consumer<LivingDeathEvent> veto=event->{ if(event.getEntity()==parent) event.setCanceled(true); };
                    NeoForge.EVENT_BUS.addListener(veto);
                    try { parent.hurt(level.damageSources().generic(),1000); helper.assertTrue(near(level,center,child).isEmpty(),"Cancelled death emitted children"); }
                    finally { NeoForge.EVENT_BUS.unregister(veto); }
                    parent.setHealth(parent.getMaxHealth()); parent.invulnerableTime=0; parent.hurt(level.damageSources().generic(),1000);
                    helper.assertTrue(near(level,center,child).size()==2,"Accepted native death does not emit exactly two offspring");
                    parent.die(level.damageSources().generic()); helper.assertTrue(near(level,center,child).size()==2,"Repeated native death duplicated offspring");
                }
                clean(level,center); int shards=0;
                for(int sample=0;sample<16;sample++) { var parent=spawn(level,center,DreadSpawnEntity.Kind.GREATER); parent.hurt(level.damageSources().generic(),1000); }
                for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(10))) {
                    helper.assertTrue(drop.getItem().is(DreadProcessingContent.SHARD.get())&&drop.getItem().getCount()<=2,"Real larger creature drops fabricated ore/loot"); shards+=drop.getItem().getCount();
                }
                helper.assertTrue(shards>0,"Actual native deaths never provide material shards"); helper.succeed();
            } finally { clean(level,center); }
        });
    }

    @GameTest(template="empty",batch="dread_ecology_slug",timeoutTicks=100)
    public static void NativeSlugCollisionHonorsImmunityDamageVetoWallsAndPersistedLifetime(GameTestHelper helper) {
        visible(helper,()->{
            var level=helper.getLevel(); var center=helper.absolutePos(new BlockPos(2,1,2)); clean(level,center);
            var cow=EntityType.COW.create(level); cow.setNoAi(true); cow.setNoGravity(true); cow.moveTo(center.getX()+.5,center.getY()+20,center.getZ()+3.5,0,0); level.addFreshEntity(cow);
            var owner=spawn(level,center,DreadSpawnEntity.Kind.GREATER); var wall=center.above(20).south(2); var prior=level.getBlockState(wall);
            try {
                for(boolean denied:new boolean[]{true,false}) {
                    cow.setHealth(10); cow.invulnerableTime=0; cow.setInvulnerable(denied); cow.removeAllEffects();
                    var slug=armedSlug(level,center); slug.setOwner(owner);
                    for(int tick=0;tick<8&&!slug.isRemoved();tick++) slug.tick();
                    helper.assertTrue(slug.isRemoved(),"Native projectile misses its fixture target");
                    helper.assertTrue(cow.getHealth()==(denied?10:4)&&cow.hasEffect(DreadlandsContent.PLAGUE)==!denied,"Slug ignores six damage/accepted-hit plague semantics");
                }
                cow.setHealth(10); cow.invulnerableTime=0; cow.removeAllEffects();
                var canceled=armedSlug(level,center);
                Consumer<net.neoforged.neoforge.event.entity.ProjectileImpactEvent> impactVeto=event->{ if(event.getProjectile()==canceled) event.setCanceled(true); };
                NeoForge.EVENT_BUS.addListener(impactVeto);
                try {
                    for(int tick=0;tick<8&&!canceled.isRemoved();tick++) canceled.tick();
                    helper.assertTrue(cow.getHealth()==10&&!cow.hasEffect(DreadlandsContent.PLAGUE),"Canceled native projectile impact still applied damage/disease");
                } finally { NeoForge.EVENT_BUS.unregister(impactVeto); canceled.discard(); }
                level.setBlock(wall,Blocks.STONE.defaultBlockState(),3); var blocked=armedSlug(level,center);
                for(int tick=0;tick<8&&!blocked.isRemoved();tick++) blocked.tick();
                helper.assertTrue(blocked.isRemoved()&&level.getBlockState(wall).is(Blocks.STONE),"Native solid impact does not stop a slug or destroys terrain");
                var expiring=DreadEcologyContent.SLUG.get().create(level); expiring.moveTo(center.getX(),center.getY()+40,center.getZ()); var save=new CompoundTag(); expiring.saveWithoutId(save); save.putInt("SlugAge",99); expiring.load(save); level.addFreshEntity(expiring); expiring.tick();
                helper.assertTrue(expiring.isRemoved(),"Saved projectile lifetime restarts on reload"); helper.succeed();
            } finally { cow.discard(); level.setBlock(wall,prior,3); clean(level,center); }
        });
    }
    private static DreadSlugEntity armedSlug(ServerLevel level,BlockPos center) {
        var slug=DreadEcologyContent.SLUG.get().create(level); slug.moveTo(center.getX()+.5,center.getY()+20.5,center.getZ()+.5,0,0);
        var save=new CompoundTag(); slug.saveWithoutId(save); save.putInt("SlugAge",6); slug.load(save); slug.shoot(0,0,1,1,0); level.addFreshEntity(slug); return slug;
    }

    @GameTest(template="empty",batch="dread_ecology_native_ranged",timeoutTicks=100)
    public static void NativeCombatGoalsSelectRangeAndProjectileSpawnVetoHasNoGhostOutput(GameTestHelper helper) {
        visible(helper,()->{
            // Own an elevated clear corridor: neighboring test structures must not
            // turn the product's legitimate line-of-sight guard into a false failure.
            var level=helper.getLevel(); var center=helper.absolutePos(new BlockPos(2,12,2)); clean(level,center);
            var terrain=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(var pos:BlockPos.betweenClosed(center.offset(-2,0,-1),center.offset(2,5,8))) {
                if(level.getBlockEntity(pos)!=null) throw new GameTestAssertException("Ranged fixture would overlap a block entity");
                terrain.put(pos.immutable(),level.getBlockState(pos)); level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
            var cow=EntityType.COW.create(level); cow.setNoAi(true); cow.setNoGravity(true); cow.moveTo(center.getX()+.5,center.getY(),center.getZ()+6.5,0,0); level.addFreshEntity(cow);
            try {
                for(var kind:DreadSpawnEntity.Kind.values()) {
                    var mob=spawn(level,center,kind); mob.setTarget(cow);
                    var ranged=mob.goalSelector.getAvailableGoals().stream().map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                            .filter(goal->goal instanceof net.minecraft.world.entity.ai.goal.RangedAttackGoal).toList();
                    helper.assertTrue(ranged.size()==(kind==DreadSpawnEntity.Kind.SMALL?0:1),"Combat goals are duplicated or wrongly installed during subtype construction");
                    if(kind==DreadSpawnEntity.Kind.SMALL) { mob.performRangedAttack(cow,1); helper.assertTrue(level.getEntitiesOfClass(DreadSlugEntity.class,new AABB(center).inflate(16)).isEmpty(),"Small Spawn gained an invented ranged attack"); mob.discard(); continue; }
                    helper.assertTrue(ranged.getFirst().canUse(),"Native far-range goal cannot start");
                    cow.moveTo(center.getX()+.5,center.getY(),center.getZ()+1.5,0,0); helper.assertTrue(!ranged.getFirst().canUse(),"Ranged goal does not yield to source melee distance");
                    cow.moveTo(center.getX()+.5,center.getY(),center.getZ()+6.5,0,0);
                    helper.assertTrue(mob.hasLineOfSight(cow),"Owned clear fixture corridor still blocks native sight for "+kind);
                    Consumer<EntityJoinLevelEvent> veto=event->{ if(event.getEntity() instanceof DreadSlugEntity) event.setCanceled(true); };
                    NeoForge.EVENT_BUS.addListener(veto);
                    try { mob.performRangedAttack(cow,1); helper.assertTrue(level.getEntitiesOfClass(DreadSlugEntity.class,new AABB(center).inflate(16)).isEmpty(),"Vetoed projectile still entered native world"); }
                    finally { NeoForge.EVENT_BUS.unregister(veto); }
                    mob.performRangedAttack(cow,1);
                    var shots=level.getEntitiesOfClass(DreadSlugEntity.class,new AABB(center).inflate(16));
                    helper.assertTrue(shots.size()==1&&shots.getFirst().getOwner()==mob&&shots.getFirst().getDeltaMovement().length()>1,
                            "Actual attack lacks one owned moving projectile: kind="+kind+", count="+shots.size()+", sight="+mob.hasLineOfSight(cow)+", targetAlive="+cow.isAlive());
                    shots.getFirst().discard(); mob.discard();
                }
                helper.succeed();
            } finally { cow.discard(); clean(level,center); terrain.forEach((pos,state)->level.setBlock(pos,state,2)); }
        });
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="dread_ecology_material_carriers",timeoutTicks=600)
    public static void NativeDreadlandsFamilyDropsFeedTheExistingMachineAndEveryFormCanBeRended(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(DreadlandsContent.DIMENSION); var pos=new BlockPos(28008,150,28008); var prior=level.getBlockState(pos);
        var admitted=new ArrayList<ItemEntity>(); var mobs=new ArrayList<DreadSpawnEntity>();
        Consumer<EntityJoinLevelEvent> capture=event->{ if(event.getLevel()==level&&event.getEntity() instanceof ItemEntity item&&item.position().distanceTo(pos.getCenter())<5&&item.getItem().is(DreadProcessingContent.SHARD.get())) admitted.add(item); };
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"EcologyAudit")); player.moveTo(pos.getCenter());
        NeoForge.EVENT_BUS.addListener(capture);
        try {
            for(int attempt=0;attempt<24&&admitted.stream().mapToInt(item->item.getItem().getCount()).sum()<3;attempt++) {
                var mob=spawn(level,pos,DreadSpawnEntity.Kind.GREATER); mobs.add(mob);
                helper.assertTrue(mob.hurt(level.damageSources().playerAttack(player),1000),"Native larger creature death failed");
            }
            int count=admitted.stream().mapToInt(item->item.getItem().getCount()).sum();
            helper.assertTrue(count>=3&&admitted.stream().allMatch(item->item.isAddedToLevel()&&!item.isRemoved()),"Larger family does not supply admitted native Dreadium ingredients");
            level.setBlock(pos,WastelandProcessingContent.TRANSMUTATOR.get().defaultBlockState(),3);
            var machine=(dev.abyssalport.block.entity.TransmutatorBlockEntity)level.getBlockEntity(pos);
            // Consume the actual admitted output stacks, rather than a separately invented input.
            int remaining=3;
            for(var item:admitted) { int amount=Math.min(remaining,item.getItem().getCount()); item.getItem().shrink(amount); remaining-=amount; if(remaining==0) break; }
            machine.setItem(0,new ItemStack(DreadProcessingContent.SHARD.get(),3)); machine.setItem(1,new ItemStack(CoraliumContent.PEARL.get()));
            for(int tick=0;tick<600;tick++) dev.abyssalport.block.entity.TransmutatorBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
            helper.assertTrue(machine.getItem(0).isEmpty()&&machine.getItem(2).is(DreadProcessingContent.INGOT.get())&&machine.getItem(2).getCount()==3,"New carrier shards are disconnected from native Dreadium processing");
            for(var kind:DreadSpawnEntity.Kind.values()) {
                var mob=DreadEcologyContent.type(kind).create(level); mobs.add(mob); mob.setNoAi(true); mob.moveTo(player.position().add(0,0,2));
                var staff=new ItemStack(DreadlandsProgressionContent.STAFF.get()); player.getCooldowns().removeCooldown(staff.getItem());
                helper.assertTrue(dev.abyssalport.item.RendingStaffItem.drain(staff,player,mob)
                        &&dev.abyssalport.item.RendingStaffItem.energy(staff,dev.abyssalport.item.RendingStaffItem.Channel.DREAD)==3,"New creature is not a real dimension-bound Dread carrier: "+kind);
            }
            LOGGER.info("DREAD ECOLOGY WORLD AUDIT: {} admitted actual shards, 3 native ingots, all three forms remain Dread rending carriers",count);
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(capture); admitted.forEach(ItemEntity::discard); mobs.forEach(Entity::discard); clean(level,pos);
            if(level.getBlockEntity(pos) instanceof dev.abyssalport.block.entity.TransmutatorBlockEntity machine) machine.clearContent(); level.setBlock(pos,prior,3);
        }
    }
}
