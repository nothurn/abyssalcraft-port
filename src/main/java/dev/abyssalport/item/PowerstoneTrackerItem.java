package dev.abyssalport.item;

import dev.abyssalport.content.WastelandContent;
import dev.abyssalport.content.WastelandStructuresContent;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;

/** The native eye supplies synced item rendering, flight and the original 80% return chance. */
public final class PowerstoneTrackerItem extends Item {
    public PowerstoneTrackerItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.dimension().equals(WastelandContent.DIMENSION)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.abyssalport.tracker_dimension"), true);
            return InteractionResultHolder.fail(stack);
        }
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() == HitResult.Type.BLOCK && level.getBlockState(hit.getBlockPos()).is(WastelandStructuresContent.POWERSTONE.get())) return InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel server) {
            var target = findUnclaimedStronghold(server, player.blockPosition());
            if (target == null) {
                player.displayClientMessage(Component.translatable("message.abyssalport.tracker_missing"), true);
                return InteractionResultHolder.fail(stack);
            }
            var eye = new EyeOfEnder(level, player.getX(), player.getY(.5), player.getZ());
            eye.setItem(stack); eye.signalTo(target);
            if (!server.addFreshEntity(eye) || eye.isRemoved()) return InteractionResultHolder.fail(stack);
            stack.consume(1, player);
            player.getCooldowns().addCooldown(this, 20);
            server.gameEvent(GameEvent.PROJECTILE_SHOOT, eye.position(), GameEvent.Context.of(player));
            server.playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, .5F, .45F);
            player.awardStat(Stats.ITEM_USED.get(this)); player.swing(hand, true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    /** Native locate(skipKnown=true) claims a start while searching, unlike the original
     * tracker. Read the same ring/start data without mutating its explored references. */
    public static BlockPos findUnclaimedStronghold(ServerLevel level, BlockPos origin) {
        if (!level.dimension().equals(WastelandContent.DIMENSION)) return null;
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(WastelandStructuresContent.STRONGHOLD);
        var state = level.getChunkSource().getGeneratorState();
        for (var placement : state.getPlacementsForStructure(structure)) {
            if (!(placement instanceof ConcentricRingsStructurePlacement rings)) continue;
            var positions = state.getRingPositionsFor(rings);
            if (positions == null) continue;
            for (var candidate : positions.stream().sorted(java.util.Comparator.comparingDouble(pos -> pos.getMiddleBlockPosition(32).distSqr(origin))).toList()) {
                var chunk = level.getChunk(candidate.x, candidate.z, ChunkStatus.STRUCTURE_STARTS);
                var start = chunk.getStartForStructure(structure.value());
                if (start != null && start.isValid() && start.canBeReferenced()) {
                    // The original locator points to Y=32. Use this actual central chamber
                    // floor instead, so the final eye descends to the generated structure.
                    return start.getPieces().getFirst().getBoundingBox().getCenter().atY(start.getPieces().getFirst().getBoundingBox().minY()+1);
                }
            }
        }
        return null;
    }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> text, TooltipFlag flag) {
        text.add(Component.translatable("tooltip.abyssalport.powerstone_tracker").withStyle(ChatFormatting.GRAY));
    }
}
