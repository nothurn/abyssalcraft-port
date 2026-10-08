package dev.abyssalport.block.entity;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.ritual.BasicScrollRitual;
import dev.abyssalport.ritual.RitualCatalog;
import dev.abyssalport.ritual.RitualDefinition;
import dev.abyssalport.ritual.LivingSacrifice;
import dev.abyssalport.ritual.GatewayRitual;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.research.CoraliumResearch;
import net.minecraft.world.item.Items;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;

public final class RitualAltarBlockEntity extends BlockEntity {
    private static final double USER_RANGE_SQUARED = 8 * 8;
    private UUID ritualUser;
    private int remainingTicks;
    private ResourceLocation ritualId;
    private String ritualSignature;
    private ItemStack input = ItemStack.EMPTY;
    private UUID sacrificeId;
    private boolean sacrificeDeathConfirmed;
    // Retain one selected mob for at most the active ritual, so removal cannot be mistaken for death.
    private Mob sacrifice;

    public RitualAltarBlockEntity(BlockPos pos, BlockState state) {
        super(AbyssalPort.RITUAL_ALTAR_ENTITY.get(), pos, state);
    }

    public boolean isRunning() {
        return remainingTicks > 0;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public ItemStack getInput() { return input; }

    public boolean placeInput(ItemStack held) {
        if (isRunning() || !input.isEmpty() || held.isEmpty()) return false;
        input = held.split(1);
        syncInput();
        return true;
    }

    public ItemStack removeInput() {
        ItemStack removed = input;
        input = ItemStack.EMPTY;
        syncInput();
        return removed;
    }

    private void syncInput() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    public boolean start(ServerLevel level, ServerPlayer player) {
        if (isRunning() || player.level() != level || player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) > USER_RANGE_SQUARED) {
            return false;
        }
        RitualDefinition definition = RitualCatalog.INSTANCE.find(level, worldPosition).orElse(null);
        if (definition == null) {
            return false;
        }
        ItemStack book = heldBook(player, definition);
        if (book.isEmpty() || NecronomiconItem.getPotentialEnergy(book) < definition.energyCost()) {
            return false;
        }
        if (definition.gateway() && !GatewayRitual.permitted(level, worldPosition, player, input)) return false;
        Mob selected = definition.livingSacrifice() ? LivingSacrifice.select(level, worldPosition).orElse(null) : null;
        if (definition.livingSacrifice() && selected == null) {
            player.displayClientMessage(Component.translatable("message.abyssalport.sacrifice_missing"), true);
            return false;
        }
        sacrifice = selected;
        sacrificeId = selected == null ? null : selected.getUUID();
        sacrificeDeathConfirmed = false;
        if (selected != null) {
            selected.addEffect(new MobEffectInstance(MobEffects.GLOWING, definition.durationTicks(), 0));
            player.displayClientMessage(Component.translatable("message.abyssalport.sacrifice_selected",
                    selected.getDisplayName()), false);
        }
        ritualUser = player.getUUID();
        ritualId = definition.id();
        ritualSignature = definition.signature();
        remainingTicks = definition.durationTicks();
        setChanged();
        player.displayClientMessage(Component.translatable("message.abyssalport.ritual_started",
                definition.outcomeName()), true);
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RitualAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel server) || !altar.isRunning()) {
            return;
        }
        RitualDefinition definition = altar.ritualId == null ? null
                : RitualCatalog.INSTANCE.get(altar.ritualId).orElse(null);
        if (definition == null || !definition.signature().equals(altar.ritualSignature)) {
            altar.stop();
            return;
        }
        ServerPlayer player = altar.findUser(server);
        if (player == null || !definition.matches(server, pos)
                || NecronomiconItem.getPotentialEnergy(heldBook(player, definition)) < definition.energyCost()
                || definition.gateway() && !GatewayRitual.permitted(server, pos, player, altar.input)) {
            altar.stop();
            return;
        }
        if (definition.livingSacrifice() && !altar.checkSacrifice(server)) {
            player.displayClientMessage(Component.translatable("message.abyssalport.sacrifice_interrupted"), true);
            altar.stop();
            return;
        }
        if (--altar.remainingTicks > 0) {
            altar.setChanged();
            return;
        }
        if (definition.livingSacrifice() && !altar.sacrificeDeathConfirmed) {
            player.displayClientMessage(Component.translatable("message.abyssalport.sacrifice_interrupted"), true);
            altar.stop();
            return;
        }
        ItemStack book = heldBook(player, definition);
        dev.abyssalport.entity.AbyssalDragonEntity summoned = null;
        if (definition.summonType() != null) {
            summoned = dev.abyssalport.ritual.SummoningRitual.spawn(server, pos, definition);
            // Spawn hooks may veto or modify the ground/book. Recheck after the hook, before any debit.
            book = heldBook(player, definition);
            if (summoned == null || !summoned.isAlive() || summoned.isRemoved() || !definition.matches(server, pos) || book.isEmpty()) {
                if (summoned != null) summoned.discard();
                player.displayClientMessage(Component.translatable("message.abyssalport.summon_blocked"), true);
                altar.stop(); return;
            }
        }
        if (NecronomiconItem.consumePotentialEnergy(book, definition.energyCost()) == definition.energyCost()) {
            ItemStack returnedKey = definition.gateway() ? altar.input.copy() : ItemStack.EMPTY;
            ItemStack result = definition.resultStack(altar.input);
            definition.consumeOfferings(server, pos);
            if (summoned != null) {
                net.minecraft.advancements.CriteriaTriggers.SUMMONED_ENTITY.trigger(player, summoned);
            } else if (definition.gateway()) {
                GatewayRitual.complete(server, pos, player, returnedKey);
            } else if (definition.altarInput() != Items.AIR) {
                altar.input = result;
                altar.syncInput();
            } else {
                server.addFreshEntity(new ItemEntity(server, pos.getX() + 0.5, pos.getY() + 1.15,
                        pos.getZ() + 0.5, result));
            }
            CoraliumResearch.discover(player, definition.resultStack());
            dev.abyssalport.research.WastelandProgressionResearch.discover(player, result);
            if (definition.result() == AbyssalPort.BASIC_SCROLL.get()) {
                EarlyResearch.unlock(player, EarlyResearch.BASIC_SCROLL);
            }
            player.displayClientMessage(definition.gateway() ? Component.translatable("message.abyssalport.gateway_created")
                    : Component.translatable("message.abyssalport.ritual_complete", definition.outcomeName()), true);
        } else if (summoned != null) {
            summoned.discard();
        }
        altar.stop();
    }

    /** Only an observed death is proof. Unload, discard, dimension change and missing UUID cancel. */
    private boolean checkSacrifice(ServerLevel server) {
        if (sacrificeId == null) return false;
        if (sacrificeDeathConfirmed) return true;
        if (sacrifice == null) {
            Entity found = server.getEntity(sacrificeId);
            if (!(found instanceof Mob mob)) return false;
            sacrifice = mob;
        }
        if (sacrifice.level() != server || !sacrifice.getUUID().equals(sacrificeId)
                || !LivingSacrifice.area(worldPosition).intersects(sacrifice.getBoundingBox())) return false;
        if (sacrifice.isRemoved()) {
            if (sacrifice.getRemovalReason() != Entity.RemovalReason.KILLED) return false;
        } else if (!sacrifice.isDeadOrDying()) {
            return LivingSacrifice.eligible(sacrifice);
        }
        // LivingDeathEvent may be cancelled while health is still zero. Vanilla sets DYING only after that hook.
        if (!sacrifice.isDeadOrDying() || sacrifice.getPose() != Pose.DYING) return false;
        sacrificeDeathConfirmed = true;
        sacrifice = null;
        setChanged();
        return true;
    }

    private ServerPlayer findUser(ServerLevel level) {
        if (ritualUser == null) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(ritualUser);
        return player != null && player.level() == level
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= USER_RANGE_SQUARED ? player : null;
    }

    private static ItemStack heldBook(ServerPlayer player, RitualDefinition definition) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (NecronomiconItem.isBook(held) && definition.requirements().acceptsBook(held)
                    && NecronomiconItem.getPotentialEnergy(held) >= definition.energyCost()) {
                return held;
            }
        }
        return ItemStack.EMPTY;
    }

    private void stop() {
        ritualUser = null;
        remainingTicks = 0;
        ritualId = null;
        ritualSignature = null;
        sacrificeId = null;
        sacrifice = null;
        sacrificeDeathConfirmed = false;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!input.isEmpty()) tag.put("AltarInput", input.save(registries));
        if (ritualUser != null && remainingTicks > 0) {
            tag.putUUID("RitualUser", ritualUser);
            tag.putInt("RemainingTicks", remainingTicks);
            if (sacrificeId != null) {
                tag.putUUID("SacrificeId", sacrificeId);
                tag.putBoolean("SacrificeDeathConfirmed", sacrificeDeathConfirmed);
            }
            if (ritualId != null && ritualSignature != null) {
                tag.putString("RitualId", ritualId.toString());
                tag.putString("RitualSignature", ritualSignature);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = tag.contains("AltarInput") ? ItemStack.parseOptional(registries, tag.getCompound("AltarInput")) : ItemStack.EMPTY;
        ritualUser = tag.hasUUID("RitualUser") ? tag.getUUID("RitualUser") : null;
        ritualId = null;
        ritualSignature = null;
        sacrificeId = tag.hasUUID("SacrificeId") ? tag.getUUID("SacrificeId") : null;
        sacrificeDeathConfirmed = sacrificeId != null && tag.getBoolean("SacrificeDeathConfirmed");
        sacrifice = null;
        remainingTicks = ritualUser == null ? 0 : Math.clamp(tag.getInt("RemainingTicks"), 0, 1200);
        if (remainingTicks > 0) {
            if (tag.contains("RitualId")) {
                ritualId = ResourceLocation.tryParse(tag.getString("RitualId"));
                ritualSignature = tag.getString("RitualSignature");
            } else {
                // Old saves only ever contained the original Basic Scroll ritual.
                ritualId = BasicScrollRitual.ID;
                ritualSignature = BasicScrollRitual.legacySignature();
                remainingTicks = Math.min(remainingTicks, BasicScrollRitual.DURATION_TICKS);
            }
            if (ritualId == null || ritualSignature == null || ritualSignature.isEmpty()) {
                ritualUser = null;
                remainingTicks = 0;
            }
        }
    }
}
