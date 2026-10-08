package dev.abyssalport;

import dev.abyssalport.block.entity.EnergyContainerBlockEntity;
import dev.abyssalport.block.entity.PrototypeEffigyBlockEntity;
import dev.abyssalport.block.entity.RitualAltarBlockEntity;
import dev.abyssalport.block.entity.RitualPedestalBlockEntity;
import dev.abyssalport.content.CoraliumContent;
import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandMobsContent;
import dev.abyssalport.content.WastelandProgressionContent;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.item.RendingStaffItem;
import dev.abyssalport.menu.EnergyContainerMenu;
import dev.abyssalport.recipe.BookUpgradeRecipe;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.research.WastelandProgressionResearch;
import dev.abyssalport.ritual.BasicScrollRitual;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastelandProgressionGameTests {
    private WastelandProgressionGameTests() { }

    @GameTest(template = "empty", batch = "wasteland_upgrade_recipe")
    public static void realUpgradeRecipePreservesEnergyComponentsAndNetworkRoundtrip(GameTestHelper helper) {
        var level = helper.getLevel();
        var oldBook = new ItemStack(AbyssalPort.NECRONOMICON.get()); NecronomiconItem.setPotentialEnergy(oldBook, 4321.5F);
        oldBook.set(DataComponents.CUSTOM_NAME, Component.literal("My field notes"));
        var data = oldBook.get(DataComponents.CUSTOM_DATA).copyTag(); data.putString("PrivateNote", "retained"); oldBook.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        var input = CraftingInput.of(2, 2, List.of(new ItemStack(WastelandProgressionContent.SKIN.get()), new ItemStack(WastelandProgressionContent.SKIN.get()), new ItemStack(WastelandProgressionContent.SKIN.get()), oldBook));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow().value();
        helper.assertTrue(recipe instanceof BookUpgradeRecipe, "Upgrade fell back to a component-losing vanilla recipe");
        var result = recipe.assemble(input, level.registryAccess());
        helper.assertTrue(result.is(WastelandProgressionContent.BOOK.get()) && result.getCount() == 1 && NecronomiconItem.capacity(result) == 10_000
                && NecronomiconItem.getPotentialEnergy(result) == 4321.5F && "My field notes".equals(result.getHoverName().getString())
                && result.get(DataComponents.CUSTOM_DATA).copyTag().getString("PrivateNote").equals("retained"), "Upgrade loses book identity or PE");
        helper.assertTrue(NecronomiconItem.getPotentialEnergy(oldBook) == 4321.5F && oldBook.getCount() == 1
                && recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Craft preview mutates the old book or returns a second book");
        var serializer = WastelandProgressionContent.BOOK_UPGRADE.get();
        var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        try {
            serializer.streamCodec().encode(buffer, (BookUpgradeRecipe) recipe);
            var synced = serializer.streamCodec().decode(buffer);
            helper.assertTrue(synced.matches(input, level) && ItemStack.matches(result, synced.assemble(input, level.registryAccess())), "Client recipe sync loses component-preserving behavior");
        } finally { buffer.release(); }
        var mirrored = CraftingInput.of(2, 2, List.of(new ItemStack(WastelandProgressionContent.SKIN.get()), new ItemStack(WastelandProgressionContent.SKIN.get()), oldBook, new ItemStack(WastelandProgressionContent.SKIN.get())));
        helper.assertTrue(recipe.matches(mirrored, level), "Original mirrored shaped recipe is not supported");
        var saved = ItemStack.parse(level.registryAccess(), result.save(level.registryAccess())).orElseThrow();
        NecronomiconItem.setPotentialEnergy(saved, 9500);
        helper.assertTrue(NecronomiconItem.getPotentialEnergy(saved) == 9500 && NecronomiconItem.tier(saved) == 1, "Tier capacity or PE is not persistent");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "wasteland_upgrade_materials")
    public static void actualStaffSkinRecipesAndResearchAreReachableAndPlayerScoped(GameTestHelper helper) {
        var level = helper.getLevel();
        var ingredients = new ArrayList<ItemStack>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        for (int slot : new int[]{1, 4, 5, 6}) ingredients.set(slot, new ItemStack(AbyssalPort.SHADOW_SHARD.get()));
        ingredients.set(2, new ItemStack(CoraliumContent.OBLIVION_SHARD.get()));
        var input = CraftingInput.of(3, 3, ingredients);
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
        helper.assertTrue(recipe.value().assemble(input, level.registryAccess()).is(WastelandProgressionContent.RENDING_STAFF.get()), "Staff materials/layout are disconnected");
        ingredients = new ArrayList<>(); for (int slot = 0; slot < 9; slot++) ingredients.add(new ItemStack(slot == 4 ? WastelandProgressionContent.ESSENCE.get() : WastelandMobsContent.PLAGUED_FLESH.get()));
        input = CraftingInput.of(3, 3, ingredients);
        recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
        helper.assertTrue(recipe.value().assemble(input, level.registryAccess()).is(WastelandProgressionContent.SKIN.get()), "Skin lacks its actual flesh/essence recipe");
        var player = helper.makeMockServerPlayerInLevel(); var other = helper.makeMockServerPlayerInLevel();
        player.setData(AbyssalPort.EARLY_RESEARCH.get(), 0); other.setData(AbyssalPort.EARLY_RESEARCH.get(), 0);
        helper.assertTrue(WastelandProgressionResearch.discover(player, new ItemStack(WastelandProgressionContent.BOOK.get()))
                && !WastelandProgressionResearch.discover(player, new ItemStack(WastelandProgressionContent.BOOK.get()))
                && EarlyResearch.has(player, EarlyResearch.WASTELAND_BOOK) && !EarlyResearch.has(other, EarlyResearch.WASTELAND_BOOK), "Book discovery repeats or leaks between players");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "wasteland_book_devices")
    public static void devicesEffigyAndInventoryTransfersRespectBothBookCapacities(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pos, AbyssalPort.ENERGY_CONTAINER.get().defaultBlockState(), 3);
        var container = (EnergyContainerBlockEntity) level.getBlockEntity(pos);
        var book = new ItemStack(WastelandProgressionContent.BOOK.get()); NecronomiconItem.setPotentialEnergy(book, 9990);
        container.addEnergy(100); container.items().setStackInSlot(1, book);
        EnergyContainerBlockEntity.serverTick(level, pos, container.getBlockState(), container);
        helper.assertTrue(NecronomiconItem.getPotentialEnergy(book) == 10_000 && container.energy() == 90, "Container clamps advanced book to 5,000 or loses PE");
        var base = new ItemStack(AbyssalPort.NECRONOMICON.get()); NecronomiconItem.setPotentialEnergy(base, 4990);
        container.items().setStackInSlot(1, base); EnergyContainerBlockEntity.serverTick(level, pos, container.getBlockState(), container);
        helper.assertTrue(NecronomiconItem.getPotentialEnergy(base) == 5000 && container.energy() == 80, "Basic book capacity changed");
        NecronomiconItem.setPotentialEnergy(book, 8500); container.items().setStackInSlot(1, ItemStack.EMPTY); container.items().setStackInSlot(0, book);
        EnergyContainerBlockEntity.serverTick(level, pos, container.getBlockState(), container);
        helper.assertTrue(NecronomiconItem.getPotentialEnergy(book) == 8480 && container.energy() == 100, "Advanced book cannot discharge");
        helper.assertTrue(container.transferToBook(book) == 100 && NecronomiconItem.getPotentialEnergy(book) == 8580
                && container.transferFromBook(book) == 100 && NecronomiconItem.getPotentialEnergy(book) == 8480, "Manual transfer rejects advanced book");
        var player = helper.makeMockServerPlayerInLevel(); player.setItemInHand(InteractionHand.OFF_HAND, book);
        helper.assertTrue(PrototypeEffigyBlockEntity.chargeHeldBook(player) && NecronomiconItem.getPotentialEnergy(book) == 8485, "Effigy cannot charge advanced offhand book");
        container.items().setStackInSlot(0, ItemStack.EMPTY); player.getInventory().clearContent(); player.getInventory().setItem(9, book);
        var menu = new EnergyContainerMenu(2, player.getInventory(), container);
        helper.assertTrue(!menu.quickMoveStack(player, 2).isEmpty() && container.items().getStackInSlot(0).is(WastelandProgressionContent.BOOK.get()), "Shift-click cannot insert advanced book");
        var restored = new EnergyContainerBlockEntity(pos, container.getBlockState()); restored.loadWithComponents(container.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        helper.assertTrue(restored.energy() == 100 && NecronomiconItem.getPotentialEnergy(restored.items().getStackInSlot(0)) == 8485, "Container save loses advanced book energy");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "rending_safety", timeoutTicks=100)
    public static void nativeStaffUseHonorsWallsCooldownDamageVetoAndOutputSpawnVeto(GameTestHelper helper) {
        var level=helper.getLevel(); var origin=helper.absolutePos(new BlockPos(2,1,1));
        var forced=new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for(int x=(origin.getX()-1)>>4;x<=(origin.getX()+1)>>4;x++) for(int z=origin.getZ()>>4;z<=(origin.getZ()+4)>>4;z++) {
            var chunk=new net.minecraft.world.level.ChunkPos(x,z);
            if(!level.getForcedChunks().contains(chunk.toLong())) { forced.add(chunk); level.setChunkForced(x,z,true); }
        }
        // Entity-section visibility is asynchronous; a just-generated fixture may still be HIDDEN.
        helper.runAfterDelay(20,()->{
            try { runNativeStaffSafety(helper); }
            finally { forced.forEach(chunk->level.setChunkForced(chunk.x,chunk.z,false)); }
        });
    }

    private static void runNativeStaffSafety(GameTestHelper helper) {
        var level = helper.getLevel(); var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL); var staff = new ItemStack(WastelandProgressionContent.RENDING_STAFF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        var origin = helper.absolutePos(new BlockPos(2, 1, 1)); player.moveTo(origin.getX()+.5, origin.getY(), origin.getZ()+.5, 0, 0);
        // Flat batches may leave terrain in a neighbouring fixture. Own the short ray corridor explicitly.
        var priorRayBlocks=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(var block:BlockPos.betweenClosed(origin.offset(-1,0,0),origin.offset(1,2,4))) {
            var position=block.immutable(); priorRayBlocks.put(position,level.getBlockState(position));
            level.setBlock(position,Blocks.AIR.defaultBlockState(),3);
        }
        var shadow = AbyssalPort.SHADOW_CREATURE.get().create(level); shadow.setNoAi(true); shadow.setNoGravity(true); shadow.moveTo(origin.getX()+.5, origin.getY(), origin.getZ()+3.5, 0, 0); level.addFreshEntity(shadow);
        player.setXRot((float) Math.toDegrees(Math.atan2(player.getEyeY()-shadow.getBoundingBox().getCenter().y, 3)));
        // LivingEntity.getViewYRot reads yHeadRot, not getYRot. A mock's independently
        // ticked head must be aimed too; moveTo's body yaw alone is not a valid ray fixture.
        player.setYHeadRot(0);
        helper.assertTrue(player.getViewYRot(1F)==0,"Fixture head does not face its southward ray target");
        var wall = origin.south().above(); level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        try {
            helper.assertTrue(level.getEntity(shadow.getUUID())==shadow,"Fixture target was not admitted to the visible native entity lookup");
            staff.getItem().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(RendingStaffItem.energy(staff, RendingStaffItem.Channel.SHADOW) == 0, "Server targeting drains through a wall");
            level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
            shadow.setInvulnerable(true); staff.getItem().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(RendingStaffItem.energy(staff, RendingStaffItem.Channel.SHADOW) == 0, "Denied damage generated energy");
            shadow.setInvulnerable(false); staff.getItem().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(RendingStaffItem.energy(staff, RendingStaffItem.Channel.SHADOW) == 1,
                    "Native right-click ray did not drain a visible target: "+rendingRayDiagnostic(level,player,shadow,staff));
            staff.getItem().use(level, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(RendingStaffItem.energy(staff, RendingStaffItem.Channel.SHADOW) == 1, "Repeated use bypassed cooldown");
            var blocked = EntityType.COW.create(level); blocked.moveTo(shadow.position());
            player.getCooldowns().removeCooldown(staff.getItem());
            helper.assertTrue(!RendingStaffItem.drain(staff, player, blocked), "Unrelated animals can be drained");
            var data = new CompoundTag(); data.putInt("energyShadow", 200); staff.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            Consumer<EntityJoinLevelEvent> veto = event -> { if (event.getEntity() instanceof ItemEntity item && item.getItem().is(AbyssalPort.SHADOW_GEM.get())) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(veto);
            try { helper.assertTrue(!RendingStaffItem.redeem(staff, player) && RendingStaffItem.energy(staff, RendingStaffItem.Channel.SHADOW) == 200, "Cancelled output discarded stored energy"); }
            finally { NeoForge.EVENT_BUS.unregister(veto); }
            player.getInventory().setItem(7, ItemStack.EMPTY);
            helper.assertTrue(RendingStaffItem.redeem(staff, player) && player.getInventory().getItem(7).is(AbyssalPort.SHADOW_GEM.get())
                    && RendingStaffItem.energy(staff, RendingStaffItem.Channel.SHADOW) == 0 && !RendingStaffItem.redeem(staff, player), "Rending reward is missing or duplicated");
        } finally { shadow.discard(); priorRayBlocks.forEach((position,state)->level.setBlock(position,state,3)); }
        helper.succeed();
    }

    private static String rendingRayDiagnostic(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.LivingEntity target,ItemStack staff) {
        var start=player.getEyePosition(); var direction=player.getViewVector(1F).scale(50);
        var block=level.clip(new net.minecraft.world.level.ClipContext(start,start.add(direction),net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        var end=block.getType()==net.minecraft.world.phys.HitResult.Type.MISS?start.add(direction):block.getLocation();
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,player,start,end,player.getBoundingBox().expandTowards(direction).inflate(1),
                entity->entity instanceof net.minecraft.world.entity.LivingEntity && entity.isAlive() && !entity.isSpectator() && entity.isPickable(),0);
        return "target="+target.getUUID()+" box="+target.getBoundingBox()+" hp="+target.getHealth()+" removed="+target.isRemoved()
                +" player="+player.position()+" eye="+start+" yaw="+player.getYRot()+" headYaw="+player.getViewYRot(1F)+" pitch="+player.getXRot()+" vector="+direction
                +" clipped="+target.getBoundingBox().clip(start,end)+" candidate="+level.getEntities(player,player.getBoundingBox().expandTowards(direction).inflate(1)).contains(target)
                +" los="+player.hasLineOfSight(target)+" cooldown="+player.getCooldowns().isOnCooldown(staff.getItem())
                +" block="+block.getType()+":"+block.getBlockPos()+" hit="+(hit==null?null:hit.getEntity().getType()+":"+hit.getEntity().getUUID());
    }

    @GameTest(template = "empty", batch = "wasteland_book_ritual")
    public static void advancedOffhandBookCanCompleteAnExistingBasicRitual(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = helper.absolutePos(new BlockPos(4, 1, 4));
        level.setBlock(pos, AbyssalPort.RITUAL_ALTAR.get().defaultBlockState(), 3);
        for (int index = 0; index < 8; index++) {
            var pedestalPos = pos.offset(BasicScrollRitual.pedestalOffsets().get(index));
            level.setBlock(pedestalPos, AbyssalPort.RITUAL_PEDESTAL.get().defaultBlockState(), 3);
            if (index < 4) ((RitualPedestalBlockEntity) level.getBlockEntity(pedestalPos)).placeOne(new ItemStack(Items.BOOK));
        }
        var player = helper.makeMockServerPlayerInLevel(); player.moveTo(pos.getX()+.5, pos.getY()+1, pos.getZ()+.5);
        var book = new ItemStack(WastelandProgressionContent.BOOK.get()); NecronomiconItem.setPotentialEnergy(book, 7500);
        player.setItemInHand(InteractionHand.OFF_HAND, book);
        var altar = (RitualAltarBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(altar.start(level, player), "Altar rejects advanced offhand book");
        for (int tick = 0; tick < BasicScrollRitual.DURATION_TICKS; tick++) RitualAltarBlockEntity.serverTick(level, pos, altar.getBlockState(), altar);
        helper.assertTrue(!altar.isRunning() && NecronomiconItem.getPotentialEnergy(book) == 7400, "Existing ritual does not conserve advanced book PE");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "abyssalport_worldtest", batch = "wasteland_rending")
    public static void hundredSuccessfulWastelandDrainsProduceAnEssenceWithoutDuplicatingIt(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(WastelandContent.DIMENSION);
        var player = new net.neoforged.neoforge.common.util.FakePlayer(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "RendingAudit"));
        var staff = new ItemStack(WastelandProgressionContent.RENDING_STAFF.get()); player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        player.moveTo(1000, 140, 1000); var target = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(level); target.moveTo(1000, 140, 1002); target.setNoAi(true);
        helper.assertTrue(RendingStaffItem.energy(staff, RendingStaffItem.Channel.ABYSSAL) == 0, "New staff starts charged");
        for (int count = 0; count < 100; count++) {
            target.setHealth(target.getMaxHealth()); target.invulnerableTime = 0; player.getCooldowns().removeCooldown(staff.getItem());
            helper.assertTrue(RendingStaffItem.drain(staff, player, target), "Actual Abyssal target refused a successful drain at "+count);
        }
        helper.assertTrue(RendingStaffItem.energy(staff, RendingStaffItem.Channel.ABYSSAL) == 100
                && RendingStaffItem.energy(staff, RendingStaffItem.Channel.SHADOW) == 0, "Rending channel mixed or miscounted");
        var saved = ItemStack.parse(level.registryAccess(), staff.save(level.registryAccess())).orElseThrow();
        helper.assertTrue(RendingStaffItem.redeem(saved, player) && !RendingStaffItem.redeem(saved, player)
                && player.getInventory().countItem(WastelandProgressionContent.ESSENCE.get()) == 1, "Saved staff does not redeem exactly one essence");
        var outside = WastelandMobsContent.ABYSSAL_ZOMBIE.get().create(helper.getLevel());
        var outsidePlayer = helper.makeMockServerPlayerInLevel(); outsidePlayer.moveTo(outside.position());
        helper.assertTrue(!RendingStaffItem.drain(new ItemStack(WastelandProgressionContent.RENDING_STAFF.get()), outsidePlayer, outside), "Abyssal rending ignores its dimension gate");
        helper.succeed();
    }
}
