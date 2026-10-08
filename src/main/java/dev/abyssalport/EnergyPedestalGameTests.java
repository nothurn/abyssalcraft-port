package dev.abyssalport;

import dev.abyssalport.block.EnergyPedestalBlock;
import dev.abyssalport.block.entity.*;
import dev.abyssalport.content.*;
import dev.abyssalport.content.EnergyDeviceContent.Kind;
import dev.abyssalport.energy.*;
import dev.abyssalport.item.*;
import dev.abyssalport.ritual.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnergyPedestalGameTests {
    private EnergyPedestalGameTests() { }
    private static ItemStack charged(Item item, float energy) {
        var stack = new ItemStack(item); PotentialEnergyItems.add(stack, energy); return stack;
    }
    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "pedestal-audit"));
        player.setGameMode(GameType.SURVIVAL); return player;
    }
    private static void emptyPedestals(GraveyardGameTests.Fixture fixture) {
        fixture.saved.keySet().forEach(pos -> {
            if (fixture.level.getBlockEntity(pos) instanceof EnergyPedestalBlockEntity pedestal) pedestal.removeItem();
        });
    }
    @GameTest(template="empty", batch="pe_pedestal_singleton")
    public static void SingletonChargesNativeContainerKindsPreservesSyncAndStopsAtItemCapacity(GameTestHelper helper) {
        try (var fixture = new GraveyardGameTests.Fixture(helper)) {
            try {
                for (var tier : EnergyDeviceContent.REACHABLE) {
                    var block = EnergyDeviceContent.block(Kind.PEDESTAL, tier);
                    fixture.level.setBlock(fixture.node, block.defaultBlockState(), 2);
                    var pedestal = (EnergyPedestalBlockEntity) fixture.level.getBlockEntity(fixture.node);
                    var containers=new ArrayList<Item>(); containers.add(AbyssalPort.NECRONOMICON.get());
                    for (var itemTier:EnergyDeviceContent.REACHABLE) for (var kind:Kind.values()) containers.add(EnergyDeviceContent.item(kind,itemTier));
                    for (var item : containers) {
                        pedestal.consumeEnergy(pedestal.energy()); pedestal.addEnergy(53.75F); var held = charged(item, 17.25F);
                        held.set(DataComponents.CUSTOM_NAME, Component.literal("Charged keepsake"));
                        var tag = held.get(DataComponents.CUSTOM_DATA).copyTag(); tag.putString("PrivateNote", "retain");
                        held.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        helper.assertTrue(pedestal.placeOne(held) && held.isEmpty(), "Pedestal does not consume exactly one actual source item");
                        EnergyPedestalBlockEntity.serverTick(fixture.level, fixture.node, pedestal.getBlockState(), pedestal);
                        helper.assertTrue(pedestal.energy()==33.75F && PotentialEnergyItems.stored(pedestal.item())==37.25F, "Pedestal exceeds the native 20 PE/tick charge");
                        for (int tick = 1; tick < 3; tick++) EnergyPedestalBlockEntity.serverTick(fixture.level, fixture.node, pedestal.getBlockState(), pedestal);
                        helper.assertTrue(pedestal.energy() == 0 && PotentialEnergyItems.stored(pedestal.item()) == 71,
                                "Pedestal drains its item or loses fractional PE instead of charging it");
                        EnergyPedestalBlockEntity.serverTick(fixture.level, fixture.node, pedestal.getBlockState(), pedestal);
                        helper.assertTrue(pedestal.energy()==0 && PotentialEnergyItems.stored(pedestal.item())==71, "Empty pedestal reverses the transfer direction");
                        var saved = pedestal.saveWithFullMetadata(fixture.level.registryAccess());
                        var restored = new EnergyPedestalBlockEntity(fixture.node, pedestal.getBlockState()); restored.loadWithComponents(saved, fixture.level.registryAccess());
                        helper.assertTrue(restored.capacity() == tier.pedestalCapacity() && restored.energy() == 0
                                && ItemStack.matches(restored.item(), pedestal.item()) && pedestal.getUpdateTag(fixture.level.registryAccess()).contains("Item"), "Native save/client item payload loses tier/components");
                        var removed = pedestal.removeItem();
                        helper.assertTrue(removed.getHoverName().getString().equals("Charged keepsake")
                                && removed.get(DataComponents.CUSTOM_DATA).copyTag().getString("PrivateNote").equals("retain"), "Singleton strips personal components");
                    }
                    pedestal.consumeEnergy(pedestal.energy()); pedestal.addEnergy(30);
                    var held = charged(AbyssalPort.NECRONOMICON.get(), 4996.75F); pedestal.placeOne(held);
                    EnergyPedestalBlockEntity.serverTick(fixture.level, fixture.node, pedestal.getBlockState(), pedestal);
                    helper.assertTrue(pedestal.energy()==26.75F && PotentialEnergyItems.stored(pedestal.item())==5000, "Item cap loses pedestal overflow");
                    EnergyPedestalBlockEntity.serverTick(fixture.level, fixture.node, pedestal.getBlockState(), pedestal);
                    helper.assertTrue(pedestal.energy()==26.75F && PotentialEnergyItems.stored(pedestal.item())==5000, "Full item keeps consuming pedestal PE");
                    pedestal.removeItem();
                    var corrupt = charged(AbyssalPort.NECRONOMICON.get(), 100); corrupt.setCount(2);
                    helper.assertTrue(!pedestal.placeOne(corrupt) && corrupt.getCount()==2 && pedestal.item().isEmpty(), "Stacked balance is split and cloned on insertion");
                    var decor = new ItemStack(Items.DIAMOND, 3); pedestal.placeOne(decor);
                    helper.assertTrue(decor.getCount()==2 && pedestal.item().getCount()==1, "Ordinary display item does not remain a singleton");
                    EnergyPedestalBlockEntity.serverTick(fixture.level, fixture.node, pedestal.getBlockState(), pedestal);
                    helper.assertTrue(pedestal.energy()==26.75F, "Decoration creates or removes energy"); pedestal.removeItem();
                }
                helper.succeed();
            } finally { emptyPedestals(fixture); }
        }
    }
    @GameTest(template="empty", batch="pe_pedestal_actual_use_break")
    public static void NativeHandInsertionRetrievalTiltAndBreakReturnExactItemAndChargedDeviceOnce(GameTestHelper helper) {
        try (var fixture = new GraveyardGameTests.Fixture(helper)) {
            var player = player(helper); player.moveTo(fixture.node.getCenter().add(-2, 0, 0));
            var captured = new ArrayList<ItemEntity>();
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> capture = event -> {
                if (event.getLevel()==fixture.level && event.getEntity() instanceof ItemEntity item && item.position().distanceTo(fixture.node.getCenter())<3) captured.add(item);
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(capture);
            try {
                for (var tier : EnergyDeviceContent.REACHABLE) {
                    var block = (EnergyPedestalBlock) EnergyDeviceContent.block(Kind.PEDESTAL, tier);
                    player.setYRot(45); var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(block),
                            new BlockHitResult(fixture.node.below().getCenter(), Direction.UP, fixture.node.below(), false));
                    helper.assertTrue(block.getStateForPlacement(context).getValue(EnergyPedestalBlock.TILTED), "Diagonal native placement ignores tilted property");
                    player.setYRot(0); helper.assertTrue(!block.getStateForPlacement(context).getValue(EnergyPedestalBlock.TILTED), "Cardinal native placement invents tilt");
                    fixture.level.setBlock(fixture.node, block.defaultBlockState(), 2); var state = fixture.level.getBlockState(fixture.node);
                    var pedestal = (EnergyPedestalBlockEntity) fixture.level.getBlockEntity(fixture.node); pedestal.addEnergy(721.5F);
                    var held = charged(AbyssalPort.NECRONOMICON.get(), 100); held.set(DataComponents.CUSTOM_NAME, Component.literal("Personal book")); var expected = held.copy();
                    player.setItemInHand(InteractionHand.MAIN_HAND, held); var hit = new BlockHitResult(fixture.node.getCenter(), Direction.UP, fixture.node, false);
                    helper.assertTrue(state.useItemOn(held, fixture.level, player, InteractionHand.MAIN_HAND, hit).consumesAction()
                            && held.isEmpty() && ItemStack.matches(pedestal.item(), expected), "Actual book interaction uses manual transfer instead of the singleton");
                    state.useWithoutItem(fixture.level, player, hit);
                    helper.assertTrue(pedestal.item().isEmpty() && ItemStack.matches(player.getInventory().getItem(0), expected), "Empty-hand native retrieval loses/duplicates book");
                    player.getInventory().clearContent(); player.setGameMode(GameType.ADVENTURE); var locked = expected.copy(); player.setItemInHand(InteractionHand.MAIN_HAND, locked);
                    state.useItemOn(locked, fixture.level, player, InteractionHand.MAIN_HAND, hit);
                    helper.assertTrue(pedestal.item().isEmpty() && ItemStack.matches(locked, expected), "Adventure protection is bypassed");
                    player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); pedestal.placeOne(expected.copy());
                    for (int slot=0; slot<36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
                    state.useWithoutItem(fixture.level, player, hit);
                    helper.assertTrue(ItemStack.matches(pedestal.item(), expected) && captured.isEmpty(), "Full inventory spills/removes the singleton instead of keeping it");
                    player.getInventory().clearContent();
                    fixture.level.setBlock(fixture.node, state.setValue(EnergyPedestalBlock.TILTED, true), 2);
                    helper.assertTrue(fixture.level.getBlockEntity(fixture.node)==pedestal && captured.isEmpty(), "A property-only update breaks the singleton");
                    helper.assertTrue(fixture.level.destroyBlock(fixture.node, true), "Actual native pedestal break failed");
                    helper.assertTrue(captured.size()==2 && captured.stream().filter(item->ItemStack.matches(item.getItem(), expected)).count()==1
                            && captured.stream().filter(item->item.getItem().is(block.asItem()) && PotentialEnergyItems.stored(item.getItem())==721.5F).count()==1,
                            "Native break loses/duplicates the exact displayed item or charged pedestal: " + captured.stream().map(ItemEntity::getItem).toList());
                    captured.forEach(ItemEntity::discard); captured.clear();
                }
                helper.succeed();
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(capture); captured.forEach(ItemEntity::discard); emptyPedestals(fixture); player.getInventory().clearContent(); }
        }
    }
    @GameTest(template="empty", batch="pe_pedestal_statue_role")
    public static void StatueChargesBothCollectorRolesAtEveryTierButMixedAdjacencyAndReservoirsCannotReceive(GameTestHelper helper) {
        try (var fixture = new GraveyardGameTests.Fixture(helper)) {
            fixture.level.setBlock(fixture.node, StatueContent.block(StatueContent.Deity.CTHULHU).defaultBlockState(), 2);
            var statue = (DeityStatueBlockEntity) fixture.level.getBlockEntity(fixture.node);
            var pos = fixture.node.east(3); var neighbor = pos.east();
            for (var tier : EnergyDeviceContent.REACHABLE) for (var kind : List.of(Kind.COLLECTOR, Kind.PEDESTAL)) {
                fixture.level.setBlock(pos, EnergyDeviceContent.block(kind, tier).defaultBlockState(), 2);
                var receiver = (EnergyStorageBlockEntity) fixture.level.getBlockEntity(pos);
                receiver.addEnergy(receiver.capacity()-2.5F); statue.refreshCollectors(fixture.level);
                helper.assertTrue(statue.collectorPositions().contains(pos), "Source collector role misses a reachable tier/pedestal");
                statue.tickEnergy(fixture.level, new StatueEnergyGameTests.FixedRandom(0));
                helper.assertTrue(receiver.energy()==receiver.capacity(), "Statue cannot fill fractional pedestal/collector remainder");
                receiver.consumeEnergy(5); fixture.level.setBlock(neighbor, EnergyDeviceContent.block(kind==Kind.COLLECTOR?Kind.PEDESTAL:Kind.COLLECTOR, tier).defaultBlockState(), 2);
                statue.tickEnergy(fixture.level, new StatueEnergyGameTests.FixedRandom(0));
                helper.assertTrue(receiver.energy()==receiver.capacity()-5, "Mixed collector/pedestal adjacency fails isolation");
                fixture.level.setBlock(neighbor, Blocks.AIR.defaultBlockState(), 2);
                statue.tickEnergy(fixture.level, new StatueEnergyGameTests.FixedRandom(0));
                helper.assertTrue(receiver.energy()==receiver.capacity(), "Removing adjacent collector does not resume charging");
            }
            fixture.level.setBlock(pos, AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(), 2); statue.refreshCollectors(fixture.level);
            statue.tickEnergy(fixture.level, new StatueEnergyGameTests.FixedRandom(0));
            helper.assertTrue(!statue.collectorPositions().contains(pos) && ((EnergyStorageBlockEntity)fixture.level.getBlockEntity(pos)).energy()==0, "Every store is incorrectly given collector eligibility"); helper.succeed();
        }
    }
    @GameTest(template="empty", batch="pe_pedestal_live_network", timeoutTicks=140)
    public static void RegisteredLiveTickerDrainsInputThroughReservoirRelayAndChargesPedestalBookWithoutManualTicks(GameTestHelper helper) {
        var fixture = new GraveyardGameTests.Fixture(helper);
        try {
            fixture.level.setBlock(fixture.node, EnergyDeviceContent.block(Kind.CONTAINER, EnergyDeviceTier.DREADLANDS).defaultBlockState(), 2);
            var reservoir = (EnergyContainerBlockEntity) fixture.level.getBlockEntity(fixture.node);
            var input = charged(EnergyDeviceContent.item(Kind.COLLECTOR,EnergyDeviceTier.DREADLANDS), 73.75F); reservoir.items().setStackInSlot(0,input);
            fixture.level.setBlock(fixture.node.east(), AbyssalPort.ENERGY_RELAY.get().defaultBlockState().setValue(dev.abyssalport.block.EnergyRelayBlock.FACING, Direction.EAST), 2);
            var relay = (EnergyRelayBlockEntity) fixture.level.getBlockEntity(fixture.node.east());
            fixture.level.setBlock(fixture.node.east(5), EnergyDeviceContent.block(Kind.PEDESTAL, EnergyDeviceTier.DREADLANDS).defaultBlockState(), 2);
            var pedestal = (EnergyPedestalBlockEntity) fixture.level.getBlockEntity(fixture.node.east(5)); pedestal.placeOne(new ItemStack(AbyssalPort.NECRONOMICON.get()));
            helper.runAfterDelay(120, () -> {
                try {
                    helper.assertTrue(PotentialEnergyItems.stored(input)==0 && PotentialEnergyItems.stored(pedestal.item())>0
                            && PotentialEnergyItems.stored(input)+PotentialEnergyItems.stored(pedestal.item())+pedestal.energy()+relay.energy()+reservoir.energy()==73.75F,
                            "Native live connected network loses PE or does not charge book: "+PotentialEnergyItems.stored(pedestal.item())+"/"+pedestal.energy()+"/"+relay.energy()+"/"+reservoir.energy()); helper.succeed();
                } finally { reservoir.items().setStackInSlot(0,ItemStack.EMPTY); emptyPedestals(fixture); fixture.close(); }
            });
        } catch (RuntimeException error) { emptyPedestals(fixture); fixture.close(); throw error; }
    }
    @GameTest(template="empty", batch="pe_canonical_reservoir_chain")
    public static void NativeTakingCraftedCollectorRelayAndPedestalConsumesExactSourceMaterialsAndRejectsOldDarkstone(GameTestHelper helper) {
        try(var fixture=new GraveyardGameTests.Fixture(helper)) {
            var player=player(helper);fixture.level.setBlock(fixture.node,Blocks.CRAFTING_TABLE.defaultBlockState(),2);
            try {
                for(var kind:List.of(Kind.COLLECTOR,Kind.RELAY,Kind.PEDESTAL)) for(boolean mirror:new boolean[]{false,true}) {
                    player.getInventory().clearContent();var craft=new net.minecraft.world.inventory.CraftingMenu(13,player.getInventory(),
                            net.minecraft.world.inventory.ContainerLevelAccess.create(fixture.level,fixture.node));
                    for(int slot=0;slot<9;slot++) {
                        Item item=ShoggothLairContent.MONOLITH_STONE.get().asItem();
                        if(slot==4)item=AbyssalPort.SHADOW_GEM.get();
                        else if(kind==Kind.RELAY && slot%3==(mirror?0:2) && slot!=4)item=AbyssalPort.SHADOW_SHARD.get();
                        else if(kind==Kind.PEDESTAL && slot==1)item=CoraliumContent.PEARL.get();
                        // The relay has shards only in its upper/lower corners, not the middle edge.
                        if(kind==Kind.RELAY && slot/3==1 && slot!=4)item=ShoggothLairContent.MONOLITH_STONE.get().asItem();
                        craft.getSlot(slot+1).set(new ItemStack(item));
                    }
                    var expected=EnergyDeviceContent.item(kind,EnergyDeviceTier.BASE);
                    helper.assertTrue(craft.getSlot(0).getItem().is(expected) && craft.getSlot(0).getItem().getCount()==1,"Native source device recipe does not assemble: "+kind);
                    helper.assertTrue(!craft.quickMoveStack(player,0).isEmpty() && player.getInventory().countItem(expected)==1,"Native crafting loses or duplicates device: "+kind);
                    for(int slot=1;slot<=9;slot++)helper.assertTrue(craft.getSlot(slot).getItem().isEmpty(),"Actual device take leaves an unspent ingredient: "+kind);
                }
                var old=new net.minecraft.world.inventory.CraftingMenu(14,player.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(fixture.level,fixture.node));
                for(int slot=0;slot<9;slot++)old.getSlot(slot+1).set(new ItemStack(slot==4?AbyssalPort.SHADOW_GEM.get():AbyssalPort.DARKSTONE_ITEM.get()));
                helper.assertTrue(old.getSlot(0).getItem().isEmpty(),"Prototype Darkstone still bypasses actual Monolith Stone access");
                for(int slot=1;slot<=9;slot++)old.getSlot(slot).set(ItemStack.EMPTY);helper.succeed();
            } finally {player.getInventory().clearContent();}
        }
    }
    @GameTest(template="empty", batch="pe_canonical_reservoir_chain")
    public static void NativeCraftAndPaidInfusionKeepOnlySourcePotEnergyAndCannotUsePrototypeBypass(GameTestHelper helper) {
        var level = helper.getLevel(); var id = ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID, "energy_container");
        helper.assertTrue(level.getRecipeManager().byKey(id).isEmpty(), "Cheap prototype reservoir craft is still loaded");
        var parts = new ArrayList<ItemStack>();
        for (int slot=0; slot<9; slot++) parts.add(new ItemStack(slot==1?CoraliumContent.PEARL.get():slot==4?AbyssalPort.SHADOW_GEM.get():ShoggothLairContent.MONOLITH_STONE.get().asItem()));
        var input = CraftingInput.of(3, 3, parts); var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
        var source = recipe.value().assemble(input, level.registryAccess()); helper.assertTrue(source.is(EnergyDeviceContent.item(Kind.PEDESTAL, EnergyDeviceTier.BASE)), "Canonical seven-stone/pearl/shadow pedestal craft is unreachable");
        var definition = RitualCatalog.INSTANCE.get(id).orElseThrow();
        helper.assertTrue(definition.energyCost()==100 && definition.durationTicks()==20 && definition.preserveEnergy() && !definition.preserveComponents()
                && definition.offerings().equals(Map.of(AbyssalPort.ENERGY_COLLECTOR_ITEM.get(), 4, AbyssalPort.SHADOW_SHARD.get(), 4)), "Loaded canonical reservoir infusion differs from the original");
        try (var fixture = new GraveyardGameTests.Fixture(helper)) {
            var pos=fixture.node; var player=helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL); player.moveTo(pos.getCenter().add(0,1,0));
            level.setBlock(pos, AbyssalPort.RITUAL_ALTAR.get().defaultBlockState(), 2);
            for (var offset:BasicScrollRitual.pedestalOffsets()) level.setBlock(pos.offset(offset), AbyssalPort.RITUAL_PEDESTAL.get().defaultBlockState(), 2);
            var altar=(RitualAltarBlockEntity)level.getBlockEntity(pos);
            try {
                EnergyStorageItem.addEnergy(source, 4321.75F); var tag=source.get(DataComponents.CUSTOM_DATA).copyTag(); tag.putString("PrivateNote","not-original-copy-target");
                source.set(DataComponents.CUSTOM_DATA,CustomData.of(tag)); source.set(DataComponents.CUSTOM_NAME,Component.literal("Not a preserved tag"));
                altar.placeInput(source); var center=altar.getInput().copy(); int slot=0;
                for(var entry:definition.offerings().entrySet())for(int count=0;count<entry.getValue();count++)
                    ((RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(BasicScrollRitual.pedestalOffsets().get(slot++)))).placeOne(new ItemStack(entry.getKey()));
                var book=charged(AbyssalPort.NECRONOMICON.get(),99); player.setItemInHand(InteractionHand.MAIN_HAND,book);
                helper.assertTrue(!altar.start(level,player) && ItemStack.matches(center,altar.getInput()),"Unpaid infusion consumes centre");
                PotentialEnergyItems.add(book,101); helper.assertTrue(altar.start(level,player),"Canonical paid infusion cannot start");
                RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
                var saved=altar.saveWithFullMetadata(level.registryAccess()); altar.loadWithComponents(saved,level.registryAccess());
                for(int tick=1;tick<20;tick++)RitualAltarBlockEntity.serverTick(level,pos,altar.getBlockState(),altar);
                var result=altar.getInput(); helper.assertTrue(!altar.isRunning() && result.is(AbyssalPort.ENERGY_CONTAINER_ITEM.get()) && result.getCount()==1
                        && PotentialEnergyItems.stored(result)==4321.75F && PotentialEnergyItems.stored(book)==100
                        && !result.has(DataComponents.CUSTOM_NAME) && !result.get(DataComponents.CUSTOM_DATA).copyTag().contains("PrivateNote"), "Original PotEnergy-only copy/debit/save duplicates components or loses centre energy");
                for(var offset:BasicScrollRitual.pedestalOffsets())helper.assertTrue(((RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(offset))).getOffering().isEmpty(),"Paid reservoir recipe leaves duplicate offerings");
                helper.succeed();
            } finally {
                altar.removeInput(); for(var offset:BasicScrollRitual.pedestalOffsets()) ((RitualPedestalBlockEntity)level.getBlockEntity(pos.offset(offset))).removeOne(); player.getInventory().clearContent();
            }
        }
    }
    @GameTest(template="empty", templateNamespace="abyssalport_worldtest", batch="pe_pedestal_normal_world_live", timeoutTicks=140)
    public static void RealNormalWorldTicksRunConnectedPedestalRelayReservoir(GameTestHelper helper) {
        RegisteredLiveTickerDrainsInputThroughReservoirRelayAndChargesPedestalBookWithoutManualTicks(helper);
    }
}
