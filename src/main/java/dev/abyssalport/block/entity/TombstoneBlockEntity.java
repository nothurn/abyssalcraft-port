package dev.abyssalport.block.entity;

import dev.abyssalport.content.GraveyardContent;
import dev.abyssalport.entity.GhoulEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

/** Saved, night-only spawn timer; rejected/obstructed attempts never leave a ghost. */
public final class TombstoneBlockEntity extends BlockEntity {
    public static final int COOLDOWN=200,MAX_NEARBY=5,RADIUS=15;
    private int timer;
    public TombstoneBlockEntity(BlockPos pos,BlockState state) { super(GraveyardContent.TOMBSTONE_ENTITY.get(),pos,state); }
    public int activeTicks() { return timer; }
    public static void serverTick(Level level,BlockPos pos,BlockState state,TombstoneBlockEntity tombstone) {
        if(!(level instanceof ServerLevel server)||server.isDay()||server.getDifficulty()==Difficulty.PEACEFUL||!server.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        if(++tombstone.timer<COOLDOWN) { if(tombstone.timer%20==0) tombstone.setChanged(); return; }
        tombstone.timer=0; tombstone.setChanged();
        if(server.getEntitiesOfClass(GhoulEntity.class,new AABB(pos).inflate(RADIUS),mob->mob.isAlive()).size()>=MAX_NEARBY) return;
        var type=GraveyardContent.raisedBy(state);
        var ghoul=type.create(server); if(ghoul==null) return;
        BlockPos spawn=null;
        // Bounded loaded neighbourhood, full support plus actual entity collision.
        // Do not reproduce the old fallback teleport fifteen blocks into a wall.
        for(int dy:new int[]{1,0,2}) {
            for(int radius=0;radius<=4&&spawn==null;radius++) for(int dx=-radius;dx<=radius&&spawn==null;dx++) for(int dz=-radius;dz<=radius&&spawn==null;dz++) {
                if(Math.max(Math.abs(dx),Math.abs(dz))!=radius) continue;
                var candidate=pos.offset(dx,dy,dz);
                if(!server.hasChunkAt(candidate)||!server.hasChunkAt(candidate.below())||!server.hasChunkAt(candidate.above())
                        ||!server.getBlockState(candidate.below()).isFaceSturdy(server,candidate.below(),Direction.UP)) continue;
                ghoul.moveTo(candidate.getX()+.5,candidate.getY(),candidate.getZ()+.5,server.random.nextFloat()*360,0);
                if(server.noCollision(ghoul)&&!server.containsAnyLiquid(ghoul.getBoundingBox())) { spawn=candidate; break; }
            }
            if(spawn!=null) break;
        }
        if(spawn==null) { ghoul.discard(); return; }
        EventHooks.finalizeMobSpawn(ghoul,server,server.getCurrentDifficultyAt(spawn),MobSpawnType.SPAWNER,null);
        if(ghoul.isSpawnCancelled()||ghoul.isRemoved()||!server.noCollision(ghoul)||!server.addFreshEntity(ghoul)) { ghoul.discard(); return; }
        server.broadcastEntityEvent(ghoul,(byte)23);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.saveAdditional(tag,registries); tag.putInt("Timer",timer); }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.loadAdditional(tag,registries); timer=Math.clamp(tag.getInt("Timer"),0,COOLDOWN-1); }
}
