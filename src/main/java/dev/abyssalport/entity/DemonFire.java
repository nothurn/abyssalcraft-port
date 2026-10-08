package dev.abyssalport.entity;

import dev.abyssalport.config.MobEcologyConfig;
import dev.abyssalport.content.MobEcologyContent;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Native admission and grief-veto boundary for source Demon fire trails. */
public final class DemonFire {
    private DemonFire() { }
    public static void onPlayerDeath(LivingDeathEvent event){if(event.getEntity() instanceof Player player
        &&event.getSource().getEntity() instanceof CorruptedAnimalEntity animal)animal.recordKilledPlayer(player);}
    public static boolean place(CorruptedAnimalEntity owner,BlockPos pos){
        if(!owner.demon()||!owner.canBurn()||!(owner.level() instanceof ServerLevel level)||!level.hasChunkAt(pos)
            ||!EventHooks.canEntityGrief(level,owner)||!level.isEmptyBlock(pos))return false;
        for(var direction:Direction.values())if(!level.hasChunkAt(pos.relative(direction)))return false;
        var fire=(MobEcologyConfig.mimicFire()?MobEcologyContent.MIMIC_FIRE.get():Blocks.FIRE).defaultBlockState();
        if(!fire.canSurvive(level,pos)||!EventHooks.onEntityDestroyBlock(owner,pos,level.getBlockState(pos)))return false;
        var snapshot=BlockSnapshot.create(level.dimension(),level,pos);if(!level.setBlock(pos,fire,3))return false;
        // Non-player native placement reads snapshot.state as the placed block.
        // Notify with current fire, but retain the original snapshot for rollback.
        if(EventHooks.onBlockPlace(owner,BlockSnapshot.create(level.dimension(),level,pos),Direction.UP)){snapshot.restore();return false;}return true;
    }
    public static void trail(CorruptedAnimalEntity owner){for(int i=0;i<4;i++)place(owner,BlockPos.containing(
        owner.getX()+(i%2*2-1)*.25,owner.getY(),owner.getZ()+(i/2%2*2-1)*.25));}
}
