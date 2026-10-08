package dev.abyssalport;

import dev.abyssalport.block.entity.GatewayAnchorBlockEntity;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.content.*;
import dev.abyssalport.effect.DreadPlagueEffect;
import dev.abyssalport.item.GatewayKeyItem;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.research.GatewayResearch;
import dev.abyssalport.research.WastelandProgressionResearch;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.GatewayRitual;
import dev.abyssalport.ritual.RitualCatalog;
import dev.abyssalport.ritual.RitualGroundFormation;
import dev.abyssalport.world.GatewayTravel;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DreadlandsGameTests {
    private static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    private DreadlandsGameTests() { }

    @GameTest(template="empty", batch="dreadlands_key_tiers")
    public static void keyTierIsImmutableSavedAndCannotSkipDimensionConnections(GameTestHelper helper) {
        var level=helper.getLevel(); var player=helper.makeMockServerPlayerInLevel();
        var key=new ItemStack(DreadlandsContent.KEY.get()); var other=key.copy(); player.setItemInHand(InteractionHand.MAIN_HAND,key);
        DreadlandsContent.KEY.get().use(level,player,InteractionHand.MAIN_HAND);
        helper.assertTrue(GatewayKeyItem.destination(key).equals(WastelandContent.DIMENSION),"First destination differs from base key");
        DreadlandsContent.KEY.get().use(level,player,InteractionHand.MAIN_HAND);
        helper.assertTrue(GatewayKeyItem.destination(key).equals(DreadlandsContent.DIMENSION) && GatewayKeyItem.destination(other).equals(Level.OVERWORLD),"Key selection leaks between stacks");
        var saved=ItemStack.parse(level.registryAccess(),key.save(level.registryAccess())).orElseThrow();
        helper.assertTrue(GatewayKeyItem.destination(saved).equals(DreadlandsContent.DIMENSION) && ((GatewayKeyItem)saved.getItem()).tier()==1,"NBT loses selection/tier");
        player.setShiftKeyDown(true); DreadlandsContent.KEY.get().use(level,player,InteractionHand.MAIN_HAND); player.setShiftKeyDown(false);
        helper.assertTrue(GatewayKeyItem.destination(key).equals(WastelandContent.DIMENSION),"Sneak does not reverse selection");
        var base=new ItemStack(GatewayContent.KEY.get()); GatewayKeyItem.setDestination(base,DreadlandsContent.DIMENSION);
        helper.assertTrue(!GatewayContent.KEY.get().permits(WastelandContent.DIMENSION,DreadlandsContent.DIMENSION)
                && !GatewayContent.KEY.get().permits(DreadlandsContent.DIMENSION,WastelandContent.DIMENSION)
                && DreadlandsContent.KEY.get().permits(WastelandContent.DIMENSION,DreadlandsContent.DIMENSION)
                && DreadlandsContent.KEY.get().permits(DreadlandsContent.DIMENSION,WastelandContent.DIMENSION)
                && !DreadlandsContent.KEY.get().permits(Level.OVERWORLD,DreadlandsContent.DIMENSION)
                && !DreadlandsContent.KEY.get().permits(DreadlandsContent.DIMENSION,Level.OVERWORLD)
                && !DreadlandsContent.KEY.get().permits(Level.NETHER,DreadlandsContent.DIMENSION)
                && !DreadlandsContent.KEY.get().permits(WastelandContent.DIMENSION,null),"Forged destination or key bypasses the progression edge");
        var discovery=helper.makeMockServerPlayerInLevel();
        helper.assertTrue(WastelandProgressionResearch.discover(player,saved) && !WastelandProgressionResearch.discover(player,saved)
                && EarlyResearch.has(player,EarlyResearch.DREADLANDS_KEY) && !EarlyResearch.has(discovery,EarlyResearch.DREADLANDS_KEY),"Key research is not once/player scoped");
        helper.succeed();
    }

    @GameTest(template="empty", batch="dreadlands_native_materials")
    public static void nativeMiningAndSmeltingKeepTheTierFourGateAndRealDrops(GameTestHelper helper) {
        var level=helper.getLevel(); var tool=new ItemStack(AbyssalniteContent.PICKAXE.get());
        for(Block block:List.of(DreadlandsContent.STONE.get(),DreadlandsContent.COBBLESTONE.get(),DreadlandsContent.ELYSIAN_STONE.get(),
                DreadlandsContent.ELYSIAN_COBBLESTONE.get(),DreadlandsContent.DREADED_ORE.get(),DreadlandsContent.ABYSSALNITE_ORE.get())) {
            var state=block.defaultBlockState();
            for(Item weak:List.of(Items.WOODEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.NETHERITE_PICKAXE))
                helper.assertTrue(!new ItemStack(weak).isCorrectToolForDrops(state),"Vanilla tool bypasses tier four: "+block);
            helper.assertTrue(tool.isCorrectToolForDrops(state) && new ItemStack(WastelandMineralsContent.PICKAXE.get()).isCorrectToolForDrops(state),"Existing higher tools cannot harvest "+block);
            var drops=Block.getDrops(state,level,helper.absolutePos(new BlockPos(3,1,3)),null,null,tool);
            helper.assertTrue(drops.size()==1 && drops.getFirst().getCount()==1,"Native loot missing or duplicated");
            if(block==DreadlandsContent.STONE.get()) helper.assertTrue(drops.getFirst().is(DreadlandsContent.COBBLESTONE.get().asItem()),"Natural stone does not drop cobble");
        }
        var input=new SingleRecipeInput(new ItemStack(DreadlandsContent.DREADED_ORE.get()));
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,input,level).orElseThrow();
        helper.assertTrue(recipe.value().assemble(input,level.registryAccess()).is(AbyssalniteContent.INGOT.get()),"Source-informed ore smelting output is wrong");
        helper.succeed();
    }

    @GameTest(template="empty", batch="dreadling_damage")
    public static void nativeMeleeInflictsPlagueOnlyOnASuccessfulNonimmuneHit(GameTestHelper helper) {
        var level=helper.getLevel(); var mob=DreadlandsContent.DREADLING.get().create(level); var cow=EntityType.COW.create(level);
        var peer=DreadlandsContent.DREADLING.get().create(level);
        helper.assertTrue(mob.getMaxHealth()==30 && mob.getAttributeValue(Attributes.ATTACK_DAMAGE)==6 && mob.fireImmune()
                && mob.getBbWidth()==.8F && mob.getBbHeight()==1.5F,"Dreadling source core attributes missing");
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20); cow.setHealth(20);
        helper.assertTrue(mob.doHurtTarget(cow) && cow.getHealth()==14 && cow.hasEffect(DreadlandsContent.PLAGUE)
                && cow.getEffect(DreadlandsContent.PLAGUE).getDuration()==100,"Actual damage does not add five-second plague");
        cow.removeAllEffects(); cow.invulnerableTime=20;
        helper.assertTrue(!mob.doHurtTarget(cow) && !cow.hasEffect(DreadlandsContent.PLAGUE),"Rejected hit still inflicts plague");
        helper.assertTrue(DreadPlagueEffect.immune(peer) && !peer.addEffect(new MobEffectInstance(DreadlandsContent.PLAGUE,100)),"Dreadling is not plague immune");
        var plague=new MobEffectInstance(DreadlandsContent.PLAGUE,100);
        helper.assertTrue(plague.getCures().isEmpty(),"Milk cures the original antidote-only plague");
        cow.invulnerableTime=0; cow.tickCount=24; DreadlandsContent.PLAGUE.get().applyEffectTick(cow,0);
        helper.assertTrue(cow.getHealth()==14,"Plague damages every tick instead of at its interval");
        cow.tickCount=25; DreadlandsContent.PLAGUE.get().applyEffectTick(cow,0);
        helper.assertTrue(cow.getHealth()==13,"Plague interval causes no real damage");
        helper.assertTrue(!dev.abyssalport.entity.DreadlingEntity.checkSpawnRules(DreadlandsContent.DREADLING.get(),level,MobSpawnType.NATURAL,BlockPos.ZERO,level.random),"Natural Dreadling leaks into Overworld");
        mob.discard(); peer.discard(); cow.discard(); helper.succeed();
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="dreadlands_key_infusion",timeoutTicks=400)
    public static void actualSavedInfusionRequiresTenThousandPeAndConsumesTheRealBossAndStone(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION); var pos=new BlockPos(6168,160,6168);
        var prior=prepare(level,pos); var player=helper.makeMockServerPlayerInLevel(); var returnPos=player.position();
        player.teleportTo(level,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        var book=new ItemStack(WastelandProgressionContent.BOOK.get()); var base=new ItemStack(AbyssalPort.NECRONOMICON.get());
        try {
            helper.assertTrue(RitualGroundFormation.form(level,pos,player,book)==RitualGroundFormation.Result.CREATED,"Real nine-cobble higher ground did not form");
            var altar=(RitualAltarBlockEntity)level.getBlockEntity(pos); altar.placeInput(new ItemStack(GatewayContent.KEY.get()));
            Item[] inputs={CoraliumContent.TRANSMUTATION_GEM.get(),WastelandStructuresContent.POWERSTONE_ITEM.get(),WastelandBossContent.EYE.get()};
            int[] places={6,1,4};
            for(int i=0;i<3;i++) ((RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(BasicScrollRitual.pedestalOffsets().get(places[i])))).placeOne(new ItemStack(inputs[i]));
            var definition=RitualCatalog.INSTANCE.get(WastelandContent.id("dreadlands_key")).orElseThrow();
            helper.assertTrue(definition.matches(level,pos) && definition.requirements().bookTier()==1 && definition.requirements().altarTier()==1,"Loaded ritual is not the actual gated unordered infusion");
            player.setItemInHand(InteractionHand.MAIN_HAND,base); NecronomiconItem.setPotentialEnergy(base,5000);
            helper.assertTrue(!altar.start(level,player),"Base book started the higher-key infusion");
            player.setItemInHand(InteractionHand.OFF_HAND,book); NecronomiconItem.setPotentialEnergy(book,9999);
            helper.assertTrue(!altar.start(level,player) && altar.getInput().is(GatewayContent.KEY.get()),"Undercharged attempt consumed or started");
            NecronomiconItem.setPotentialEnergy(book,10000); helper.assertTrue(altar.start(level,player),"Full offhand book cannot start");
            var stand=(RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(BasicScrollRitual.pedestalOffsets().get(4))); var eye=stand.removeOne();
            RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
            helper.assertTrue(!altar.isRunning() && NecronomiconItem.getPotentialEnergy(book)==10000 && altar.getInput().is(GatewayContent.KEY.get()),"Interrupted infusion lost PE/key");
            stand.placeOne(eye); helper.assertTrue(altar.start(level,player),"Restored inputs cannot restart");
            for(int i=0;i<80;i++) RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
            var restored=new RitualAltarBlockEntity(pos,altar.getBlockState()); restored.loadWithComponents(altar.saveWithoutMetadata(level.registryAccess()),level.registryAccess()); level.setBlockEntity(restored);
            helper.assertTrue(restored.remainingTicks()==120,"Mid-infusion NBT loses progress");
            for(int i=0;i<120;i++) RitualAltarBlockEntity.serverTick(level,pos,restored.getBlockState(),restored);
            helper.assertTrue(restored.getInput().is(DreadlandsContent.KEY.get()) && restored.getInput().getCount()==1 && !restored.isRunning()
                    && NecronomiconItem.getPotentialEnergy(book)==0 && NecronomiconItem.getPotentialEnergy(base)==5000,"Infusion output/PE is wrong");
            for(var offset:BasicScrollRitual.pedestalOffsets()) helper.assertTrue(((RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(offset))).getOffering().isEmpty(),"Paid offerings remain");
            GatewayKeyItem.setDestination(restored.getInput(),DreadlandsContent.DIMENSION);
            var forgedBase=new ItemStack(GatewayContent.KEY.get()); GatewayKeyItem.setDestination(forgedBase,DreadlandsContent.DIMENSION);
            helper.assertTrue(GatewayRitual.canComplete(level,pos,restored.getInput()) && !GatewayRitual.canComplete(level,pos,forgedBase)
                    && !GatewayRitual.canComplete(helper.getLevel(),helper.absolutePos(new BlockPos(4,1,4)),restored.getInput()),"Infused/base keys do not respect real connected dimension checks");
            LOGGER.info("DREADLANDS INFUSION AUDIT: native tier-one ground, 10,000 PE, three actual offerings, saved 80/120 ticks, aborted inputs preserved");
        } finally {
            clear(level,pos); prior.forEach((p,state)->level.setBlock(p,state,3));
            player.teleportTo(helper.getLevel(),returnPos.x,returnPos.y,returnPos.z,0,0);
        }
        helper.succeed();
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="dreadlands_world_generation",timeoutTicks=1400)
    public static void nativeNormalTerrainContainsAllThreeDepositsAndRealDreadlingLoot(GameTestHelper helper) {
        var level=helper.getLevel().getServer().getLevel(DreadlandsContent.DIMENSION);
        helper.assertTrue(level!=null && level.dimensionType().coordinateScale()==1 && !level.dimensionType().bedWorks(),"Real Dreadlands dimension missing");
        Map<Block,Integer> counts=new LinkedHashMap<>();
        for(var block:List.of(DreadlandsContent.DREADED_ORE.get(),DreadlandsContent.ABYSSALNITE_ORE.get(),DreadlandsContent.ELYSIAN_STONE.get())) counts.put(block,0);
        int[] surfaceRange={Integer.MAX_VALUE,Integer.MIN_VALUE};
        for(int cx=128;cx<132;cx++) for(int cz=128;cz<132;cz++) {
            var chunk=level.getChunk(cx,cz);
            for(int x=0;x<16;x++) for(int z=0;z<16;z++) {
                int height=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z); surfaceRange[0]=Math.min(surfaceRange[0],height); surfaceRange[1]=Math.max(surfaceRange[1],height);
                for(int y=5;y<=68;y++) { var state=chunk.getBlockState(new BlockPos((cx<<4)+x,y,(cz<<4)+z)); if(counts.containsKey(state.getBlock())) counts.merge(state.getBlock(),1,Integer::sum); }
            }
        }
        for(var entry:counts.entrySet()) helper.assertTrue(entry.getValue()>0,"No natural deposit of "+entry.getKey());
        LOGGER.info("DREADLANDS TERRAIN SAMPLE: Y {}..{}, deposits={}",surfaceRange[0],surfaceRange[1],counts);
        helper.assertTrue(surfaceRange[1]-surfaceRange[0]>5 && surfaceRange[1]>49,"Terrain is empty/flat or lacks the intended highlands: Y "+surfaceRange[0]+".."+surfaceRange[1]);
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).get(WastelandContent.id("dreadlands"));
        helper.assertTrue(biome.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap().stream().anyMatch(s->s.type==DreadlandsContent.DREADLING.get() && s.minCount==1 && s.maxCount==2),"Native biome has no real Dreadling entry");
        var mob=DreadlandsContent.DREADLING.get().create(level); mob.moveTo(2056,160,2056); mob.setNoAi(true); mob.setPersistenceRequired();
        var table=level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,WastelandContent.id("entities/dreadling"))); boolean saw=false;
        for(int i=0;i<64;i++) {
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,mob)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,mob.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,level.damageSources().generic())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            var drops=table.getRandomItems(params);
            for(var drop:drops) {
                if(drop.isEmpty()) continue; // Native item-drop consumer ignores a zero-count roll.
                helper.assertTrue(drop.is(DreadlandsContent.FRAGMENT.get()) && drop.getCount()<=2,"Native Dreadling loot is wrong: "+drop); saw=true;
            }
        }
        helper.assertTrue(saw,"Native Dreadling table never yields its fragment"); mob.discard();
        LOGGER.info("DREADLANDS WORLD AUDIT: 16 native chunks, surface Y {}..{}, deposits={}, real fragment loot",surfaceRange[0],surfaceRange[1],counts);
        helper.succeed();
    }

    @GameTest(template="empty",templateNamespace="abyssalport_worldtest",batch="dreadlands_native_gateway",timeoutTicks=500)
    public static void actualInfusedKeyPortalRitualTravelsAndSavedReturnKeepsItsPair(GameTestHelper helper) {
        var aw=helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION);
        var dl=helper.getLevel().getServer().getLevel(DreadlandsContent.DIMENSION);
        var pos=new BlockPos(6280,160,6280); var prior=prepare(aw,pos); var player=helper.makeMockServerPlayerInLevel(); var returnPos=player.position();
        player.teleportTo(aw,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,0,0);
        // The portal ritual itself remains tier zero, as in NecronomiconPortalRitual.
        // Give the artificial elevated source the same supported arrival area that
        // the native destination search requires; do not hide an unsafe return.
        for(var ground:BlockPos.betweenClosed(pos.offset(-2,0,-2),pos.offset(2,0,2))) aw.setBlock(ground,WastelandContent.STONE.get().defaultBlockState(),3);
        aw.setBlock(pos,AbyssalPort.RITUAL_ALTAR.get().defaultBlockState(),3);
        var altar=(RitualAltarBlockEntity)aw.getBlockEntity(pos); var key=new ItemStack(DreadlandsContent.KEY.get()); GatewayKeyItem.setDestination(key,DreadlandsContent.DIMENSION);
        altar.placeInput(key.copy());
        for(int i=0;i<8;i++) {
            var standPos=pos.offset(BasicScrollRitual.pedestalOffsets().get(i)); aw.setBlock(standPos,AbyssalPort.RITUAL_PEDESTAL.get().defaultBlockState(),3);
            if(i<4) ((RitualPedestalBlockEntity)aw.getBlockEntity(standPos)).placeOne(new ItemStack(AbyssalPort.SHADOW_GEM.get()));
        }
        var book=new ItemStack(AbyssalPort.NECRONOMICON.get()); NecronomiconItem.setPotentialEnergy(book,1000); player.setItemInHand(InteractionHand.MAIN_HAND,book);
        var cow=EntityType.COW.create(aw); cow.setNoAi(true); cow.setNoGravity(true); cow.setHealth(7); UUID entityId=cow.getUUID();
        GatewayAnchorBlockEntity[] anchor={null};
        Runnable cleanup=()->{
            for(var level:List.of(aw,dl)) if(level.getEntity(entityId)!=null) level.getEntity(entityId).discard();
            cow.discard();
            if(anchor[0]!=null && anchor[0].partner()!=null) dl.removeBlock(anchor[0].partner(),false);
            clear(aw,pos); prior.forEach((p,state)->aw.setBlock(p,state,3));
            player.teleportTo(helper.getLevel(),returnPos.x,returnPos.y,returnPos.z,0,0);
        };
        try {
            helper.assertTrue(altar.start(aw,player),"Actual 1,000-PE infused-key portal did not start");
            for(int i=0;i<40;i++) RitualAltarBlockEntity.serverTick(aw,pos,altar.getBlockState(),altar);
            anchor[0]=(GatewayAnchorBlockEntity)aw.getBlockEntity(pos);
            helper.assertTrue(anchor[0]!=null && anchor[0].intact() && anchor[0].destination().equals(DreadlandsContent.DIMENSION)
                    && NecronomiconItem.getPotentialEnergy(book)==0,"Portal outcome/dimension or cost is wrong");
            int returned=player.getInventory().items.stream().filter(stack->stack.is(DreadlandsContent.KEY.get()) && DreadlandsContent.DIMENSION.equals(GatewayKeyItem.destination(stack))).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(returned==1,"Portal did not return exactly the configured infused key");
            cow.moveTo(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5); aw.addFreshEntity(cow);
            aw.getBlockState(pos.above()).entityInside(aw,pos.above(),cow); cow.baseTick();
            helper.assertTrue(cow.getRemovalReason()==net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION,"Native processor did not transfer Wasteland to Dreadlands");
        } catch(RuntimeException failure) { cleanup.run(); throw failure; }
        helper.startSequence().thenWaitUntil(()->helper.assertTrue(dl.getEntity(entityId) instanceof Cow,"Native Dreadlands entity not tracked after portal ticket"))
            .thenExecute(()->{
                try {
                    var arrived=(Cow)dl.getEntity(entityId); var partner=(GatewayAnchorBlockEntity)dl.getBlockEntity(anchor[0].partner());
                    helper.assertTrue(arrived.getHealth()==7 && arrived.isNoAi() && arrived.isOnPortalCooldown()
                            && partner.intact() && partner.destination().equals(WastelandContent.DIMENSION)
                            && GatewayTravel.safeArrival(dl,partner.getBlockPos(),arrived),"Transfer loses state or safe reciprocal arrival");
                    var restored=new GatewayAnchorBlockEntity(partner.getBlockPos(),partner.getBlockState());
                    restored.loadWithComponents(partner.saveWithoutMetadata(dl.registryAccess()),dl.registryAccess()); dl.setBlockEntity(restored);
                    var originalPortal=partner.portalId(); arrived.setPortalCooldown(0);
                    var backPos=restored.getBlockPos(); arrived.moveTo(backPos.getX()+.5,backPos.getY()+1,backPos.getZ()+.5);
                    var back=restored.getBlockPos().above(); dl.getBlockState(back).entityInside(dl,back,arrived); arrived.baseTick();
                    helper.assertTrue(originalPortal.equals(anchor[0].partnerId()),"Reload/return recreated the linked gateway");
                } catch(RuntimeException failure) { cleanup.run(); throw failure; }
            }).thenWaitUntil(()->helper.assertTrue(aw.getEntity(entityId) instanceof Cow && aw.getEntity(entityId).position().distanceToSqr(GatewayTravel.arrival(pos))<.01,
                    "Saved Dreadlands gateway did not return to the exact source"))
            .thenExecute(()->{ cleanup.run(); LOGGER.info("DREADLANDS GATEWAY AUDIT: paid real infused-key ritual, returned selected key, native collision/cooldown, saved pair, exact return"); })
            .thenSucceed();
    }

    private static Map<BlockPos,BlockState> prepare(ServerLevel level,BlockPos pos) {
        var prior=new HashMap<BlockPos,BlockState>();
        for(var cursor:BlockPos.betweenClosed(pos.offset(-4,0,-4),pos.offset(4,3,4))) { prior.put(cursor.immutable(),level.getBlockState(cursor)); level.setBlock(cursor,Blocks.AIR.defaultBlockState(),3); }
        level.setBlock(pos,WastelandContent.COBBLESTONE.get().defaultBlockState(),3);
        for(var offset:BasicScrollRitual.pedestalOffsets()) level.setBlock(pos.offset(offset),WastelandContent.COBBLESTONE.get().defaultBlockState(),3);
        return prior;
    }
    private static void clear(ServerLevel level,BlockPos pos) {
        if(level.getBlockEntity(pos) instanceof RitualAltarBlockEntity altar) altar.removeInput();
        for(var offset:BasicScrollRitual.pedestalOffsets()) if(level.getBlockEntity(pos.offset(offset)) instanceof RitualPedestalBlockEntity stand) stand.removeOne();
    }
}
