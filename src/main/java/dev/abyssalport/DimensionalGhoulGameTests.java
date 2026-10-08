package dev.abyssalport;

import dev.abyssalport.content.*;
import dev.abyssalport.entity.GhoulEntity;
import dev.abyssalport.effect.*;
import dev.abyssalport.block.GhoulHeadBlock;
import dev.abyssalport.research.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DimensionalGhoulGameTests {
    private DimensionalGhoulGameTests() { }

    @GameTest(template="empty",batch="graveyard_native_palette")
    public static void EveryPackedGravePaletteNameIsActuallyRegisteredBeforeNativeTemplateLoading(GameTestHelper helper) {
        for(String material:List.of("stone","dark","abyssal","coralium","dread","elysian")) for(String size:List.of("small","medium","large")) {
            String path="/data/abyssalport/structure/graveyard/"+material+"_"+size+".nbt";
            try(var input=DimensionalGhoulGameTests.class.getResourceAsStream(path)) {
                helper.assertTrue(input!=null,"Actual grave template is not packed: "+path);
                var nbt=net.minecraft.nbt.NbtIo.readCompressed(input,net.minecraft.nbt.NbtAccounter.create(1_000_000));
                var palette=nbt.getList("palette",net.minecraft.nbt.Tag.TAG_COMPOUND);
                for(var value:palette) {
                    String name=((net.minecraft.nbt.CompoundTag)value).getString("Name");
                    helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.ResourceLocation.parse(name)),
                        "Native template would silently turn an unknown palette block into AIR: "+path+" / "+name);
                }
            } catch(java.io.IOException error) { throw new GameTestAssertException("Native compressed grave data is unreadable: "+path+" / "+error); }
        }
        helper.succeed();
    }

    @GameTest(template="empty",batch="dread_soil_lifecycle",timeoutTicks=100)
    public static void NativeDreadSoilHasSourceDecayDimSpreadPlantsBonemealAndSilkLoot(GameTestHelper helper) {
        var fixture=new GraveyardGameTests.Fixture(helper);
        // Let native light/chunk status catch up before checking actual decay.
        fixture.level.setBlock(fixture.node,DreadlandsContent.GRASS.get().defaultBlockState(),2);
        fixture.level.setBlock(fixture.node.above(),Blocks.STONE.defaultBlockState(),2);
        helper.runAfterDelay(20,()-> {
        try {
            var level=fixture.level; var pos=fixture.node; var grass=DreadlandsContent.GRASS.get().defaultBlockState();
            level.setBlock(pos,grass,2); level.setBlock(pos.above(),Blocks.STONE.defaultBlockState(),2);
            grass.randomTick(level,pos,net.minecraft.util.RandomSource.create(8844));
            helper.assertTrue(level.getBlockState(pos).is(DreadlandsContent.DIRT.get()),"Covered, dark Dread Grass does not return its own dirt");
            level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),2); level.setBlock(pos,grass,2);
            for(var soil:List.of(DreadlandsContent.DIRT.get(),DreadlandsContent.GRASS.get())) {
                level.setBlock(pos,soil.defaultBlockState(),2);
                helper.assertTrue(Blocks.SHORT_GRASS.defaultBlockState().canSurvive(level,pos.above()),"Native plain plants cannot use actual Dread soil");
                helper.assertTrue(!Blocks.SUGAR_CANE.defaultBlockState().canSurvive(level,pos.above()),"Native beach plant survives without source water condition");
                level.setBlock(pos.east(),Blocks.WATER.defaultBlockState(),2);
                helper.assertTrue(Blocks.SUGAR_CANE.defaultBlockState().canSurvive(level,pos.above()),"Native beach plant cannot use watered Dread soil"); level.setBlock(pos.east(),Blocks.AIR.defaultBlockState(),2);
            }
            level.setBlock(pos,grass,2); level.setBlock(pos.east(),DreadlandsContent.DIRT.get().defaultBlockState(),2);
            var random=net.minecraft.util.RandomSource.create(917788);
            for(int attempt=0;attempt<128;attempt++) grass.randomTick(level,pos,random);
            helper.assertTrue(level.getBlockState(pos.east()).is(DreadlandsContent.GRASS.get()),"Native grass has an invented daylight threshold or fails to spread over its own dirt");
            helper.assertTrue(level.noCollision(EntityType.COW.create(level),new net.minecraft.world.phys.AABB(pos.above()))&&grass.isFaceSturdy(level,pos,Direction.UP),"Registered soil has no supported walking/spawn surface");
            DreadlandsContent.GRASS.get().performBonemeal(level,net.minecraft.util.RandomSource.create(7755),pos,grass);
            helper.assertTrue(!level.getBlockState(pos.above()).isAir(),"Native bonemeal does not grow any real plant above Dread Grass");
            var plain=Block.getDrops(grass,level,pos,null,null,new ItemStack(Items.DIAMOND_SHOVEL));
            var silk=new ItemStack(Items.DIAMOND_SHOVEL); silk.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
            var careful=Block.getDrops(grass,level,pos,null,null,silk);
            helper.assertTrue(plain.size()==1&&plain.getFirst().is(DreadlandsContent.DIRT_ITEM.get())&&careful.size()==1&&careful.getFirst().is(DreadlandsContent.GRASS_ITEM.get()),"Native normal/Silk Dread soil harvest changes source returns");
            helper.succeed();
        } finally { fixture.close(); }
        });
    }

    @GameTest(template="empty",batch="dimensional_ghoul_rules")
    public static void NativeVariantStatsImmunitiesAndAcceptedHitDiseasesUseRealEntities(GameTestHelper helper) {
        var level=helper.getLevel(); var depths=GraveyardContent.DEPTHS_GHOUL.get().create(level); var dreaded=GraveyardContent.DREADED_GHOUL.get().create(level);
        var cow=EntityType.COW.create(level); var abyssal=WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level); var guard=DreadProcessingContent.GUARD.get().create(level);
        try {
            for(var mob:List.of(depths,dreaded)) helper.assertTrue(mob.getMaxHealth()==35&&mob.getAttributeValue(Attributes.ATTACK_DAMAGE)==6
                &&mob.getAttributeValue(Attributes.FOLLOW_RANGE)==42&&mob.isInvertedHealAndHarm(),"Dimensional Ghoul source attributes/classification differ");
            // Modern vanilla's breath tag also includes the undead tag. The
            // source guarantees Depths breathing, not a Dreaded drowning rule.
            helper.assertTrue(depths.canBreatheUnderwater()&&!depths.fireImmune()&&dreaded.fireImmune(),"Variant breath/fire rules were conflated: depths breath="+depths.canBreatheUnderwater()+", depths fire="+depths.fireImmune()+", dreaded fire="+dreaded.fireImmune());
            helper.assertTrue(CoraliumPlagueEffect.immune(depths)&&!depths.addEffect(new MobEffectInstance(WastelandContent.CORALIUM_PLAGUE,100))
                &&DreadPlagueEffect.immune(dreaded)&&!dreaded.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100)),"New carrier lacks native plague immunity");
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100);
            helper.assertTrue(depths.doHurtTarget(cow)&&cow.getHealth()==94&&!cow.hasEffect(WastelandContent.CORALIUM_PLAGUE),"Default Coralium infection incorrectly spreads outside Wasteland");
            cow.invulnerableTime=0;
            helper.assertTrue(dreaded.doHurtTarget(cow)&&cow.getHealth()==88&&cow.getEffect(DreadlandsContent.PLAGUE).getDuration()==100,"Actual Dreaded melee fails to apply six damage/100-tick plague");
            cow.removeAllEffects(); cow.setInvulnerable(true);
            helper.assertTrue(!dreaded.doHurtTarget(cow)&&!cow.hasEffect(DreadlandsContent.PLAGUE),"Rejected native melee still infects");
            dreaded.doHurtTarget(guard); helper.assertTrue(!guard.hasEffect(DreadlandsContent.PLAGUE),"Dreaded melee infects its immune allied carrier");
            helper.assertTrue(depths.getType().is(net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE,WastelandContent.id("abyssal_rending_targets")))
                &&dreaded.getType().is(net.minecraft.tags.TagKey.create(Registries.ENTITY_TYPE,WastelandContent.id("dread_rending_targets"))),"Dimensional Ghoul is not connected to its rending channel");
            for(String name:new String[]{"abyssal_wasteland","abyssal_plateau","dreadlands","darklands_forest"}) {
                var biome=level.registryAccess().registryOrThrow(Registries.BIOME).get(WastelandContent.id(name));
                var type=name.equals("dreadlands")?dreaded.getType():depths.getType();
                var entries=biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().filter(entry->entry.type==type).toList();
                helper.assertTrue(entries.size()==1&&entries.getFirst().getWeight().asInt()==(name.equals("darklands_forest")?60:1),"Native biome lacks one exact Ghoul spawn entry: "+name);
            }
            helper.succeed();
        } finally { depths.discard(); dreaded.discard(); cow.discard(); abyssal.discard(); guard.discard(); }
    }

    @GameTest(template="empty",batch="depths_ghoul_saved_variants")
    public static void BabyAndFourFlavoursPersistWithoutDoubleSizeOrRepeatedSpeedBoost(GameTestHelper helper) {
        var level=helper.getLevel(); var original=GraveyardContent.DEPTHS_GHOUL.get().create(level); var loaded=GraveyardContent.DEPTHS_GHOUL.get().create(level);
        try {
            for(int type=0;type<=3;type++) {
                original.setGhoulType(type); original.setBaby(true); original.setBaby(true);
                helper.assertTrue(Math.abs(original.getAttributeValue(Attributes.MOVEMENT_SPEED)-.345)<1e-9
                    &&Math.abs(original.getBbWidth()-.45)<1e-5&&Math.abs(original.getBbHeight()-.85)<1e-5,"Baby dimensions/speed are double scaled");
                var saved=original.saveWithoutId(new net.minecraft.nbt.CompoundTag()); loaded.load(saved); loaded.load(saved);
                helper.assertTrue(loaded.isBaby()&&loaded.getGhoulType()==type&&Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED)-.345)<1e-9,"Repeated native load loses or duplicates the baby modifier/flavour");
                if(type>0) helper.assertTrue(loaded.getName().getString().equals(List.of("Pete","Mr. Wilson","Dr. Orange").get(type-1)),"Native named Depths variant label is not preserved");
                loaded.setCustomName(Component.literal("Saved personal name")); helper.assertTrue(loaded.getName().getString().equals("Saved personal name"),"A rare variant overrides a real custom name"); loaded.setCustomName(null);
                loaded.setBaby(false); loaded.setBaby(false);
                helper.assertTrue(Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED)-.23)<1e-9&&Math.abs(loaded.getBbHeight()-1.7)<1e-5,"Returning adult loses source size/speed");
            }
            var malformed=original.saveWithoutId(new net.minecraft.nbt.CompoundTag()); malformed.putInt("GhoulType",Integer.MAX_VALUE); loaded.load(malformed);
            helper.assertTrue(loaded.getGhoulType()==3,"Malformed flavour escapes native texture/loot bounds"); malformed.putInt("GhoulType",-100); loaded.load(malformed);
            helper.assertTrue(loaded.getGhoulType()==0,"Negative saved flavour has no safe default");
            var seen=new HashSet<Integer>();
            for(int seed=0;seed<200;seed++) {
                original.getRandom().setSeed(seed);
                var group=EventHooks.finalizeMobSpawn(original,level,level.getCurrentDifficultyAt(original.blockPosition()),MobSpawnType.NATURAL,null);
                var next=GraveyardContent.DEPTHS_GHOUL.get().create(level);
                try { EventHooks.finalizeMobSpawn(next,level,level.getCurrentDifficultyAt(next.blockPosition()),MobSpawnType.NATURAL,group);
                    helper.assertTrue(next.isBaby()==original.isBaby(),"A native natural pack loses its shared age flag");
                    seen.add(original.getGhoulType());
                } finally { next.discard(); }
            }
            helper.assertTrue(seen.equals(Set.of(0,1,2,3)),"Native initialization never selects all four reachable flavour variants: "+seen); helper.succeed();
        } finally { original.discard(); loaded.discard(); }
    }

    @GameTest(template="empty",batch="dimensional_ghoul_food")
    public static void DimensionalFleshConsumesOnceAndAppliesOnlyItsMatchingDisease(GameTestHelper helper) {
        var player=helper.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setData(AbyssalPort.EARLY_RESEARCH.get(),0);
        try {
            for(boolean dread:new boolean[]{false,true}) {
                player.removeAllEffects(); player.getFoodData().setFoodLevel(10);
                var item=(dread?GraveyardContent.DREADED_FLESH:GraveyardContent.DEPTHS_FLESH).get(); var stack=new ItemStack(item,2);
                var disease=dread?DreadlandsContent.PLAGUE:WastelandContent.CORALIUM_PLAGUE;
                item.finishUsingItem(stack,helper.getLevel(),player);
                helper.assertTrue(stack.getCount()==1&&player.getFoodData().getFoodLevel()==12&&player.getEffect(disease).getDuration()==600
                    &&player.getEffect(MobEffects.HUNGER).getAmplifier()==1&&player.getEffect(MobEffects.CONFUSION).getDuration()==600
                    &&!player.hasEffect(dread?WastelandContent.CORALIUM_PLAGUE:DreadlandsContent.PLAGUE),"Consumed dimensional flesh changes source nutrition/effects or invents cross-infection");
                GhoulResearch.discover(player,stack); helper.assertTrue(EarlyResearch.has(player,EarlyResearch.GHOULS),"Actual dimensional remains have no discovery path");
            }
            helper.succeed();
        } finally { player.removeAllEffects(); }
    }

    @GameTest(template="empty",batch="dimensional_ghoul_tombstones",timeoutTicks=120)
    public static void FourDimensionalTombstonesRaiseActualMatchingVariantsAndNativeHeadsWork(GameTestHelper helper) {
        var fixture=new GraveyardGameTests.Fixture(helper);
        helper.runAfterDelay(20,()-> {
            try {
                for(var stone:List.of(GraveyardContent.ABYSSAL_STONE.get(),GraveyardContent.CORALIUM_STONE.get(),GraveyardContent.DREADSTONE.get(),GraveyardContent.ELYSIAN_STONE.get())) {
                    fixture.clearMobs(); var block=fixture.place(stone); fixture.advance(block,200);
                    helper.assertTrue(fixture.admitted()==1&&fixture.mobs.getFirst().getType()==GraveyardContent.raisedBy(stone.defaultBlockState()),"Native saved grave raises the wrong or an unadmitted Ghoul type");
                }
                fixture.clearMobs(); var player=helper.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                for(var block:List.of(GraveyardContent.DEPTHS_HEAD.get(),GraveyardContent.PETE_HEAD.get(),GraveyardContent.WILSON_HEAD.get(),GraveyardContent.ORANGE_HEAD.get())) {
                    fixture.level.setBlock(fixture.node,block.defaultBlockState().setValue(GhoulHeadBlock.FACING,Direction.EAST),2);
                    var drops=Block.getDrops(fixture.level.getBlockState(fixture.node),fixture.level,fixture.node,null);
                    helper.assertTrue(drops.size()==1&&drops.getFirst().is(block.asItem())&&player.getEquipmentSlotForItem(drops.getFirst())==EquipmentSlot.HEAD,"Native head loses exact placed trophy or wearable slot");
                }
                helper.succeed();
            } finally { fixture.close(); }
        });
    }

    @GameTest(template="empty",batch="depths_ghoul_trophy_loot")
    public static void FourLoadedLootTablesKeepFleshAndRarePlayerOnlyMatchingHeads(GameTestHelper helper) {
        var level=helper.getLevel(); var player=helper.makeMockServerPlayerInLevel(); var mob=GraveyardContent.DEPTHS_GHOUL.get().create(level);
        var heads=List.of(GraveyardContent.DEPTHS_HEAD.get(),GraveyardContent.PETE_HEAD.get(),GraveyardContent.WILSON_HEAD.get(),GraveyardContent.ORANGE_HEAD.get());
        try {
            for(int type=0;type<=3;type++) {
                mob.setGhoulType(type); var table=level.getServer().reloadableRegistries().getLootTable(mob.getLootTable());
                var builder=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,mob).withParameter(LootContextParams.ORIGIN,mob.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(player));
                var environmental=builder.create(LootContextParamSets.ENTITY);
                for(int seed=0;seed<128;seed++) for(var output:table.getRandomItems(environmental,seed))
                    helper.assertTrue(output.isEmpty()||output.is(GraveyardContent.DEPTHS_FLESH.get())&&output.getCount()<=2,"A non-player death gives a rare head or wrong flesh quantity");
                var playerKill=builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER,player).create(LootContextParamSets.ENTITY); int trophies=0;
                for(int seed=0;seed<512;seed++) for(var output:table.getRandomItems(playerKill,seed)) {
                    if(output.isEmpty()) continue;
                    if(output.is(heads.get(type).asItem())) { trophies++; helper.assertTrue(output.getCount()==1,"A single rare kill duplicates its trophy"); }
                    else helper.assertTrue(output.is(GraveyardContent.DEPTHS_FLESH.get())&&output.getCount()<=2,"Flavour selects another Ghoul's trophy");
                }
                helper.assertTrue(trophies>0&&trophies<64,"Loaded player-only rare chance is missing or promoted to a guaranteed trophy: "+trophies);
            }
            helper.succeed();
        } finally { mob.discard(); }
    }
}
