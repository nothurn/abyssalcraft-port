package dev.abyssalport;

import java.util.*;
import com.mojang.authlib.GameProfile;
import dev.abyssalport.content.*;
import dev.abyssalport.item.CrystalBagItem;
import dev.abyssalport.menu.CrystalBagMenu;
import dev.abyssalport.recipe.CrystalBagUpgradeRecipe;
import dev.abyssalport.research.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(AbyssalPort.MOD_ID)
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class CrystalBagGameTests {
    private static ItemStack carbon(int count) { return new ItemStack(CrystallizationContent.material("carbon").crystal().get(),count); }
    private static ServerPlayer player(GameTestHelper helper,String name) {
        return new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),new GameProfile(UUID.randomUUID(),name));
    }
    private static ItemStack craft(GameTestHelper helper,ServerPlayer player,List<ItemStack> inputs) {
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(2,1,2)); var previous=level.getBlockState(pos);
        level.setBlock(pos,Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        try {
            var menu=new CraftingMenu(7,player.getInventory(),ContainerLevelAccess.create(level,pos));
            for(int index=0;index<9;index++) menu.getSlot(index+1).set(inputs.get(index));
            var preview=menu.getSlot(0).getItem().copy(); helper.assertTrue(!preview.isEmpty(),"Actual native crafting produced no bag");
            menu.quickMoveStack(player,0);
            for(int index=1;index<=9;index++) helper.assertTrue(menu.getSlot(index).getItem().isEmpty(),"Bag crafting left unspent source ingredients");
            helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),"Bag recipe still has an output after its inputs were spent");
            ItemStack actual=ItemStack.EMPTY; int outputs=0;
            for(int index=0;index<36;index++) if(player.getInventory().getItem(index).is(preview.getItem())) {
                outputs+=player.getInventory().getItem(index).getCount(); actual=player.getInventory().removeItem(index,1);
            }
            helper.assertTrue(outputs==1 && ItemStack.matches(actual,preview),"Native result duplicated or lost bag components");
            return actual;
        } finally { level.setBlock(pos,previous,3); }
    }
    private static List<ItemStack> upgrade(ItemStack bag,Item skin) {
        var inputs=new ArrayList<ItemStack>(); for(int slot=0;slot<9;slot++) inputs.add(slot==4?bag:new ItemStack(skin)); return inputs;
    }
    private static int stored(ItemStack bag,Item item) {
        return bag.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).nonEmptyStream().filter(stack->stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template="empty",batch="crystal_bag_source_chain")
    public static void RealCraftingUsesGoldLeatherAndBothSkinsWithoutEmptyingOrDuplicatingBags(GameTestHelper helper) {
        var player=player(helper,"BagCraftAudit");
        var ingredients=new ArrayList<ItemStack>();
        for(int slot=0;slot<9;slot++) ingredients.add(new ItemStack(slot==0 || slot==2?Items.STRING:slot==4?Items.GOLD_INGOT:Items.LEATHER));
        var small=craft(helper,player,ingredients);
        helper.assertTrue(small.is(CrystalBagContent.SMALL.get()) && CrystalBagItem.capacity(small)==18,"Source small bag is not reachable");
        var items=new ArrayList<ItemStack>(); for(int slot=0;slot<18;slot++) items.add(carbon(64));
        items.set(17,new ItemStack(CrystallizationContent.material("sulfur").clusterItem().get(),11));
        small.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items)); small.set(DataComponents.CUSTOM_NAME,Component.literal("Packed crystals"));
        var medium=craft(helper,player,upgrade(small,WastelandProgressionContent.SKIN.get()));
        var large=craft(helper,player,upgrade(medium,DreadlandsProgressionContent.SKIN.get()));
        helper.assertTrue(large.is(CrystalBagContent.LARGE.get()) && CrystalBagItem.capacity(large)==54
                && stored(large,carbon(1).getItem())==17*64 && large.getHoverName().getString().equals("Packed crystals"),"Filled upgrades lost contents or custom name");
        helper.assertTrue(ItemStack.matches(large.get(DataComponents.CONTAINER).getStackInSlot(17),items.get(17)),"Upgrade moved or lost the last occupied slot");
        var saved=ItemStack.parse(helper.getLevel().registryAccess(),large.save(helper.getLevel().registryAccess())).orElseThrow();
        helper.assertTrue(ItemStack.matches(large,saved) && CrystalBagItem.hasValidContents(saved),"Native saved bag does not retain its contents");
        helper.assertTrue(EarlyResearch.has(player,EarlyResearch.CRYSTAL_BAGS) && !WastelandProgressionResearch.discover(player,large),"Crafted bags do not unlock one player-scoped discovery");
        helper.succeed();
    }

    @GameTest(template="empty",batch="crystal_bag_native_clicks")
    public static void NativeRightClickShiftClickAndHeldSlotLockPersistExactlyOneBag(GameTestHelper helper) {
        var player=helper.makeMockServerPlayerInLevel(); player.getInventory().selected=3;
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(player.connection.getConnection());
        var bag=new ItemStack(CrystalBagContent.SMALL.get()); player.setItemInHand(InteractionHand.MAIN_HAND,bag);
        bag.getItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(player.containerMenu instanceof CrystalBagMenu,"Native right-click did not open the bag");
        var menu=(CrystalBagMenu)player.containerMenu;
        helper.assertTrue(menu.slots.size()==54 && menu.capacity()==18 && menu.stillValid(player),"Wrong native slot count or hand binding");
        menu.setCarried(carbon(16)); menu.clicked(17,0,ClickType.PICKUP,player);
        helper.assertTrue(menu.getCarried().isEmpty() && stored(bag,carbon(1).getItem())==16,"Actual cursor insertion failed to persist");
        player.getInventory().setItem(9,carbon(40)); menu.quickMoveStack(player,18);
        helper.assertTrue(player.getInventory().getItem(9).isEmpty() && stored(bag,carbon(1).getItem())==56,"Shift-click loses crystals");
        menu.setCarried(new ItemStack(Items.DIRT)); menu.clicked(1,0,ClickType.PICKUP,player);
        helper.assertTrue(menu.getSlot(1).getItem().isEmpty() && menu.getCarried().is(Items.DIRT),"Non-crystal accepted by a native slot"); menu.setCarried(ItemStack.EMPTY);
        int locked=18+27+3;
        for(var click:List.of(ClickType.PICKUP,ClickType.QUICK_MOVE,ClickType.THROW,ClickType.CLONE,ClickType.SWAP)) menu.clicked(locked,0,click,player);
        menu.clicked(17,3,ClickType.SWAP,player);
        helper.assertTrue(player.getMainHandItem()==bag && menu.getCarried().isEmpty() && stored(bag,carbon(1).getItem())==56,"Native clicks moved or recursively inserted the held bag");
        helper.assertTrue(menu.getSlot(17).getItem().getCount()==56,"Native shift-click did not merge the matching existing crystal stack");
        menu.quickMoveStack(player,17); helper.assertTrue(stored(bag,carbon(1).getItem())==0 && player.getInventory().countItem(carbon(1).getItem())==56,"Native extraction did not persist exactly once");
        for(int slot=menu.capacity();slot<menu.slots.size();slot++) if(menu.getSlot(slot).getItem().is(carbon(1).getItem())) menu.quickMoveStack(player,slot);
        helper.assertTrue(stored(bag,carbon(1).getItem())==56 && player.getInventory().countItem(carbon(1).getItem())==0,"Reinsertion lost or duplicated native crystals");
        player.closeContainer();
        var restored=ItemStack.parse(helper.getLevel().registryAccess(),bag.save(helper.getLevel().registryAccess())).orElseThrow();
        player.setItemInHand(InteractionHand.MAIN_HAND,restored); restored.getItem().use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(((CrystalBagMenu)player.containerMenu).getSlot(0).getItem().getCount()==56,"Native reopen lost saved inventory"); player.closeContainer();
        helper.succeed();
    }

    @GameTest(template="empty",batch="crystal_bag_offhand_ownership")
    public static void OffhandBagCannotBeSwappedAndAReplacementCannotInheritAnOpenInventory(GameTestHelper helper) {
        var player=player(helper,"BagOwnershipAudit"); var first=new ItemStack(CrystalBagContent.MEDIUM.get()); var second=new ItemStack(CrystalBagContent.LARGE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND,second); player.setItemInHand(InteractionHand.OFF_HAND,first);
        var menu=new CrystalBagMenu(1,player.getInventory(),first,InteractionHand.OFF_HAND,40);
        menu.setCarried(carbon(9)); menu.clicked(35,0,ClickType.PICKUP,player); menu.clicked(35,40,ClickType.SWAP,player);
        helper.assertTrue(player.getOffhandItem()==first && stored(first,carbon(1).getItem())==9 && stored(second,carbon(1).getItem())==0,"Offhand swap bypassed the lock or different bags share inventory");
        var equalReplacement=first.copy(); player.setItemInHand(InteractionHand.OFF_HAND,equalReplacement);
        helper.assertTrue(!menu.stillValid(player),"Equal replacement retained the previous bag's live inventory");
        menu.quickMoveStack(player,35); menu.clicked(35,0,ClickType.PICKUP,player);
        helper.assertTrue(menu.getCarried().isEmpty() && stored(first,carbon(1).getItem())==9 && stored(equalReplacement,carbon(1).getItem())==9,"Stale native menu removed items from an orphaned bag");
        player.setItemInHand(InteractionHand.MAIN_HAND,second);
        var main=new CrystalBagMenu(2,player.getInventory(),second,InteractionHand.MAIN_HAND,player.getInventory().selected);
        player.getInventory().selected=1; helper.assertTrue(!main.stillValid(player),"Changing the selected hotbar slot retained bag access");
        helper.succeed();
    }

    @GameTest(template="empty",batch="crystal_bag_codec_and_validation")
    public static void NativeRecipeAndOpeningCodecsRejectMalformedOrNestedStorageWithoutDiscardingData(GameTestHelper helper) {
        var level=helper.getLevel(); var recipe=level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AbyssalPort.MOD_ID,"medium_crystal_bag")).orElseThrow().value();
        helper.assertTrue(recipe instanceof CrystalBagUpgradeRecipe,"Filled bag upgrade is a destructive plain crafting recipe");
        var encoded=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try {
            CrystalBagContent.UPGRADE.get().streamCodec().encode(encoded,(CrystalBagUpgradeRecipe)recipe);
            var decoded=CrystalBagContent.UPGRADE.get().streamCodec().decode(encoded);
            var input=CraftingInput.of(3,3,upgrade(new ItemStack(CrystalBagContent.SMALL.get()),WastelandProgressionContent.SKIN.get()));
            helper.assertTrue(decoded.matches(input,level) && decoded.assemble(input,level.registryAccess()).is(CrystalBagContent.MEDIUM.get()),"Native synchronized upgrade recipe does not work");
        } finally { encoded.release(); }
        var malformed=new ItemStack(CrystalBagContent.SMALL.get()); var items=new ArrayList<ItemStack>(); for(int slot=0;slot<19;slot++) items.add(ItemStack.EMPTY); items.set(18,carbon(1));
        malformed.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items));
        var player=helper.makeMockServerPlayerInLevel(); player.setItemInHand(InteractionHand.MAIN_HAND,malformed); var saved=malformed.copy();
        malformed.getItem().use(level,player,InteractionHand.MAIN_HAND);
        helper.assertTrue(!(player.containerMenu instanceof CrystalBagMenu) && ItemStack.matches(malformed,saved)
                && !((CrystalBagUpgradeRecipe)recipe).matches(CraftingInput.of(3,3,upgrade(malformed,WastelandProgressionContent.SKIN.get())),level),"Oversized bag data was discarded or laundered through an upgrade");
        malformed.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(CrystalBagContent.SMALL.get()))));
        helper.assertTrue(!CrystalBagItem.hasValidContents(malformed) && !CrystalBagItem.accepts(malformed) && !malformed.getItem().canFitInsideContainerItems(),"Recursive bag storage accepted");
        for(int size:List.of(18,36,54)) {
            var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            try {
                buffer.writeVarInt(size); buffer.writeEnum(InteractionHand.OFF_HAND); buffer.writeVarInt(40);
                var menu=CrystalBagContent.MENU.get().create(6,player.getInventory(),buffer);
                helper.assertTrue(menu.capacity()==size && menu.slots.size()==size+36,"Native client menu-opening data lost its tier");
            } finally { buffer.release(); }
        }
        helper.succeed();
    }
}
