package dev.abyssalport;

import dev.abyssalport.block.entity.TransmutatorBlockEntity;
import dev.abyssalport.content.*;
import dev.abyssalport.entity.DreadguardEntity;
import dev.abyssalport.research.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DreadProcessingGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private DreadProcessingGameTests() { }

    @GameTest(template="empty",batch="dreadguard_melee")
    public static void nativeEliteAttributesMeleeAndImmunityMatchTheActualMaterialCarrier(GameTestHelper helper) {
        var level=helper.getLevel(); var guard=DreadProcessingContent.GUARD.get().create(level); var cow=EntityType.COW.create(level);
        helper.assertTrue(guard.getMaxHealth()==60 && guard.getAttributeValue(Attributes.ATTACK_DAMAGE)==10 && guard.getArmorValue()==20
                && guard.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==4 && guard.getAttributeValue(Attributes.FOLLOW_RANGE)==42
                && guard.getBbWidth()==1 && guard.getBbHeight()==3 && guard.fireImmune() && guard.canBreatheUnderwater(),"Native elite source attributes absent");
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40); cow.setHealth(40);
        helper.assertTrue(guard.doHurtTarget(cow) && cow.getHealth()==30 && cow.getEffect(DreadlandsContent.PLAGUE).getDuration()==100,"Actual melee does not cause source damage/plague");
        cow.removeAllEffects(); cow.invulnerableTime=20;
        helper.assertTrue(!guard.doHurtTarget(cow) && !cow.hasEffect(DreadlandsContent.PLAGUE),"Vetoed hit still adds plague");
        helper.assertTrue(dev.abyssalport.effect.DreadPlagueEffect.immune(guard) && !guard.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100))
                && guard.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD),"Guard is not a real undead immune carrier");
        helper.assertTrue(!DreadguardEntity.checkSpawnRules(DreadProcessingContent.GUARD.get(),level,MobSpawnType.NATURAL,BlockPos.ZERO,level.random),"Natural guard leaks outside Dreadlands");
        guard.discard(); cow.discard(); helper.succeed();
    }

    @GameTest(template="empty",batch="dreadguard_native_spray")
    public static void sprayUsesServerRayRespectsSolidCoverAndDamageVetoAndResumesSavedCooldown(GameTestHelper helper) {
        var level=helper.getLevel(); var origin=helper.absolutePos(new BlockPos(4,2,2)); var before=new LinkedHashMap<BlockPos,BlockState>();
        for(var mutable:BlockPos.betweenClosed(origin.offset(-1,0,0),origin.offset(1,4,4))) {
            var pos=mutable.immutable(); before.put(pos,level.getBlockState(pos)); level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        }
        var guard=DreadProcessingContent.GUARD.get().create(level); guard.setNoAi(true); guard.setNoGravity(true); guard.moveTo(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
        var cow=EntityType.COW.create(level); cow.setNoAi(true); cow.setNoGravity(true); cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40); cow.setHealth(40);
        cow.moveTo(origin.getX()+.5,origin.getY(),origin.getZ()+2.5); level.addFreshEntity(cow); level.addFreshEntity(guard);
        guard.setYRot(0); guard.setYHeadRot(0); guard.setXRot((float)Math.toDegrees(Math.atan2(guard.getEyeY()-cow.getBoundingBox().getCenter().y,2)));
        var wall=origin.south().above();
        try {
            helper.assertTrue(!guard.sprayAlongLook() && cow.getHealth()==40,"Spray runs without its active phase");
            guard.setSprayTimer(60); level.setBlock(wall,Blocks.STONE.defaultBlockState(),3);
            helper.assertTrue(!guard.sprayAlongLook() && cow.getHealth()==40,"Spray hits through solid cover"); level.setBlock(wall,Blocks.AIR.defaultBlockState(),3);
            cow.setInvulnerable(true); helper.assertTrue(!guard.sprayAlongLook() && !cow.hasEffect(DreadlandsContent.PLAGUE),"Rejected spray inflicts disease");
            cow.setInvulnerable(false); helper.assertTrue(guard.sprayAlongLook() && cow.getHealth()==37.5F && cow.hasEffect(DreadlandsContent.PLAGUE),"Actual native ray cannot hit a visible nearby target");
            cow.invulnerableTime=0; cow.getAttribute(Attributes.ARMOR).setBaseValue(20); float health=cow.getHealth();
            helper.assertTrue(guard.sprayAlongLook() && cow.getHealth()==health-2.5F,"Magic spray does not bypass armour");
            guard.setSprayTimer(-200); guard.setTarget(cow); guard.setDeltaMovement(.2,0,.1); guard.aiStep();
            helper.assertTrue(guard.sprayTimer()==59 && guard.getDeltaMovement().x==0 && guard.getDeltaMovement().z==0,"Native spray phase does not freeze horizontal motion");
            var saved=new CompoundTag(); guard.addAdditionalSaveData(saved); var restored=DreadProcessingContent.GUARD.get().create(level); restored.setNoAi(true); restored.setNoGravity(true);
            restored.readAdditionalSaveData(saved); helper.assertTrue(restored.sprayTimer()==59,"Saved burst resets timer");
            restored.setTarget(null); for(int index=0;index<59;index++) restored.aiStep();
            helper.assertTrue(restored.sprayTimer()==0,"Saved burst does not finish at three seconds");
            for(int index=0;index<200;index++) restored.aiStep(); helper.assertTrue(restored.sprayTimer()==-200,"Cooldown duration is not ten seconds");
            var exhausted=new CompoundTag(); restored.addAdditionalSaveData(exhausted); var copy=DreadProcessingContent.GUARD.get().create(level); copy.readAdditionalSaveData(exhausted);
            helper.assertTrue(copy.sprayTimer()==-200,"Save loses fully elapsed cooldown"); copy.discard(); restored.discard();
        } finally { guard.discard(); cow.discard(); before.forEach((pos,state)->level.setBlock(pos,state,3)); }
        helper.succeed();
    }

    @GameTest(template="empty",batch="dreadium_native_tools")
    public static void actualRecipesAndAnvilMakeAllFiveToolsAndKeepTheLevelSixStorageGate(GameTestHelper helper) {
        var level=helper.getLevel();
        for(String recipeId:new String[]{"dreadium_block","dreadium_from_block","dreadium_nugget","dreadium_from_nuggets","dreadstone_bricks",
                "dreadium_pickaxe","dreadium_axe","dreadium_shovel","dreadium_sword","dreadium_hoe"}) {
            var recipe=(ShapedRecipe)level.getRecipeManager().byKey(WastelandContent.id(recipeId)).orElseThrow().value(); var ingredients=recipe.getIngredients();
            var input=CraftingInput.of(recipe.getWidth(),recipe.getHeight(),ingredients.stream().map(i->i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy()).toList());
            helper.assertTrue(recipe.matches(input,level) && !recipe.assemble(input,level.registryAccess()).isEmpty(),"Actual native material/tool recipe fails: "+recipeId);
        }
        var pick=new ItemStack(DreadProcessingContent.PICKAXE.get());
        helper.assertTrue(pick.getMaxDamage()==2300 && pick.getDestroySpeed(Blocks.STONE.defaultBlockState())==14
                && pick.isCorrectToolForDrops(DreadProcessingContent.STORAGE.get().defaultBlockState()),"Actual level-six pickaxe is wrong");
        for(Item item:List.of(AbyssalniteContent.PICKAXE.get(),WastelandMineralsContent.PICKAXE.get(),Items.NETHERITE_PICKAXE))
            helper.assertTrue(!new ItemStack(item).isCorrectToolForDrops(DreadProcessingContent.STORAGE.get().defaultBlockState()),"Lower tool harvests level-six storage");
        helper.assertTrue(new ItemStack(AbyssalniteContent.PICKAXE.get()).isCorrectToolForDrops(DreadProcessingContent.BRICKS.get().defaultBlockState()),"Level-four brick unnecessarily needs Dreadium");
        var player=helper.makeMockServerPlayerInLevel();
        for(Item tool:List.of(DreadProcessingContent.PICKAXE.get(),DreadProcessingContent.AXE.get(),DreadProcessingContent.SHOVEL.get(),DreadProcessingContent.SWORD.get(),DreadProcessingContent.HOE.get())) {
            var damaged=new ItemStack(tool); damaged.setDamageValue(800); var anvil=new AnvilMenu(1,player.getInventory());
            anvil.getSlot(0).set(damaged); anvil.getSlot(1).set(new ItemStack(DreadProcessingContent.INGOT.get()));
            helper.assertTrue(!anvil.getSlot(2).getItem().isEmpty() && anvil.getSlot(2).getItem().getDamageValue()<800,"Native ingot cannot repair "+tool);
        }
        helper.assertTrue(WastelandProgressionResearch.discover(player,new ItemStack(DreadProcessingContent.SHARD.get()))
                && !WastelandProgressionResearch.discover(player,new ItemStack(DreadProcessingContent.INGOT.get()))
                && EarlyResearch.has(player,EarlyResearch.DREADIUM),"Dreadium research repeats or is absent");
        helper.succeed();
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="dreadium_actual_loot_machine",timeoutTicks=600)
    public static void actualNaturalEliteLootFeedsNativeMachineAndRemainsADreadRendingCarrier(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(DreadlandsContent.DIMENSION); var pos=new BlockPos(26008,150,26008);
        var before=level.getBlockState(pos); var drops=new ArrayList<ItemEntity>();
        java.util.function.Consumer<EntityJoinLevelEvent> capture=event->{ if(event.getLevel()==level && event.getEntity() instanceof ItemEntity item
                && item.position().distanceTo(pos.getCenter())<5 && item.getItem().is(DreadProcessingContent.SHARD.get())) drops.add(item); };
        var player=new net.neoforged.neoforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DreadiumAudit"));
        player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5); var living=new ArrayList<DreadguardEntity>(); NeoForge.EVENT_BUS.addListener(capture);
        try {
            var biome=level.registryAccess().registryOrThrow(Registries.BIOME).get(WastelandContent.id("dreadlands"));
            helper.assertTrue(biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(e->e.type==DreadProcessingContent.GUARD.get()
                    && e.getWeight().asInt()==8 && e.minCount==1 && e.maxCount==1),"Guard has no genuine native biome spawn entry");
            for(int attempt=0;attempt<32 && drops.stream().mapToInt(d->d.getItem().getCount()).sum()<5;attempt++) {
                var guard=DreadProcessingContent.GUARD.get().create(level); living.add(guard); guard.setNoAi(true); guard.setNoGravity(true); guard.moveTo(player.position().add(0,0,2));
                level.addFreshEntity(guard); helper.assertTrue(guard.isAddedToLevel() && guard.hurt(level.damageSources().playerAttack(player),1000),"Native guard death is not admitted/damaged");
            }
            int count=drops.stream().mapToInt(d->d.getItem().getCount()).sum(); helper.assertTrue(count>=5,"Native deaths supply no processing material");
            helper.assertTrue(drops.stream().allMatch(d->d.isAddedToLevel() && !d.isRemoved() && d.getItem().getCount()<=2),"Native shard loot is not source-admitted");
            level.setBlock(pos,WastelandProcessingContent.TRANSMUTATOR.get().defaultBlockState(),3); var machine=(TransmutatorBlockEntity)level.getBlockEntity(pos);
            machine.setItem(0,new ItemStack(DreadProcessingContent.SHARD.get(),5)); machine.setItem(1,new ItemStack(CoraliumContent.PEARL.get()));
            for(int tick=0;tick<1000;tick++) TransmutatorBlockEntity.serverTick(level,pos,machine.getBlockState(),machine);
            helper.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(2).is(DreadProcessingContent.INGOT.get()) && machine.getItem(2).getCount()==5
                    && machine.pendingExperience()==1F,"Actual shards do not form five ingots/one XP without duplication");
            var staff=new ItemStack(DreadlandsProgressionContent.STAFF.get()); var guard=DreadProcessingContent.GUARD.get().create(level); guard.setNoAi(true); guard.moveTo(player.position().add(0,0,2)); living.add(guard);
            helper.assertTrue(dev.abyssalport.item.RendingStaffItem.drain(staff,player,guard)
                    && dev.abyssalport.item.RendingStaffItem.energy(staff,dev.abyssalport.item.RendingStaffItem.Channel.DREAD)==3,"Guard is not a real Dread rending carrier");
            LOGGER.info("DREADIUM WORLD AUDIT: native biome elite, {} admitted shards, 5 actual machine ingots / 1 XP, level-six tool recipes and carrier rending",count);
        } finally {
            NeoForge.EVENT_BUS.unregister(capture); drops.forEach(ItemEntity::discard); living.forEach(DreadguardEntity::discard);
            if(level.getBlockEntity(pos) instanceof TransmutatorBlockEntity machine) machine.clearContent(); level.setBlock(pos,before,3);
        }
        helper.succeed();
    }
}
