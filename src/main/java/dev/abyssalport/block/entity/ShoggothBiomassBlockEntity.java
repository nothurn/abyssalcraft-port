package dev.abyssalport.block.entity;

import dev.abyssalport.AbyssalPort;
import dev.abyssalport.content.ShoggothLairContent;
import dev.abyssalport.content.ShoggothEcologyContent;
import dev.abyssalport.entity.ShoggothEntity;
import java.util.LinkedHashSet;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

/** Source-informed 400-tick node; count only entities actually admitted to the native level. */
public final class ShoggothBiomassBlockEntity extends BlockEntity {
    public static final int COOLDOWN=400,PLAYER_RADIUS=16,MOB_RADIUS=32,MAX_NEARBY=6,EXHAUSTION=5;
    private int cooldown,spawned;
    public ShoggothBiomassBlockEntity(BlockPos pos,BlockState state) { super(ShoggothLairContent.BIOMASS_ENTITY.get(),pos,state); }
    public int activeTicks() { return cooldown; }
    public int admittedCount() { return spawned; }
    public static void serverTick(Level level,BlockPos pos,BlockState state,ShoggothBiomassBlockEntity node) {
        if(!(level instanceof ServerLevel server)||node.isRemoved()||!server.getBlockState(pos).is(ShoggothLairContent.BIOMASS.get())
                ||server.getDifficulty()==Difficulty.PEACEFUL||!server.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        if(node.spawned>=EXHAUSTION) { server.setBlock(pos,ShoggothLairContent.MONOLITH_STONE.get().defaultBlockState(),3); return; }
        if(++node.cooldown<COOLDOWN) { if(node.cooldown%20==0) node.setChanged(); return; }
        node.cooldown=server.random.nextInt(10); node.setChanged(); resetNeighbourhood(server,pos);
        if(server.getNearestPlayer(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,PLAYER_RADIUS,false)==null
                ||server.getEntitiesOfClass(ShoggothEntity.class,new AABB(pos).inflate(MOB_RADIUS),Entity::isAlive).size()>=MAX_NEARBY) return;
        var mob=ShoggothEcologyContent.biomassType(server,server.random).create(server); if(mob==null) return;
        var feet=findStandingSpace(server,pos,mob);
        if(feet==null) { mob.discard(); return; }
        EventHooks.finalizeMobSpawn(mob,server,server.getCurrentDifficultyAt(feet),MobSpawnType.SPAWNER,null);
        if(mob.isSpawnCancelled()||mob.isRemoved()||!server.noCollision(mob)||server.containsAnyLiquid(mob.getBoundingBox())||!server.addFreshEntity(mob)) {
            mob.discard(); return;
        }
        node.spawned++; node.setChanged();
        server.sendParticles(ParticleTypes.LARGE_SMOKE,mob.getX(),mob.getY()+.5,mob.getZ(),4,.2,.2,.2,.01);
        if(node.spawned>=EXHAUSTION) server.setBlock(pos,ShoggothLairContent.MONOLITH_STONE.get().defaultBlockState(),3);
    }
    private static BlockPos findStandingSpace(ServerLevel level,BlockPos origin,ShoggothEntity mob) {
        for(int dy:new int[]{1,0,2}) for(int radius=0;radius<=4;radius++) for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius) continue;
            var feet=origin.offset(dx,dy,dz);
            if(!level.hasChunkAt(feet.below())||!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP)) continue;
            mob.moveTo(feet.getX()+.5,feet.getY(),feet.getZ()+.5,level.random.nextFloat()*360,0);
            var bounds=mob.getBoundingBox();
            if(!level.hasChunkAt(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ))
                ||!level.hasChunkAt(BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ))||!level.getWorldBorder().isWithinBounds(bounds)) continue;
            if(level.noCollision(mob)&&!level.containsAnyLiquid(bounds)) return feet;
        }
        return null;
    }
    private static void resetNeighbourhood(ServerLevel level,BlockPos origin) {
        // Exactly two local hops through existing biomass, never recursive flood
        // fill and never force-load a neighbour chunk to find a block entity.
        var first=new LinkedHashSet<BlockPos>(); var reset=new LinkedHashSet<BlockPos>();
        neighbours(level,origin,first); reset.addAll(first);
        for(var pos:first) neighbours(level,pos,reset);
        for(var pos:reset) if(level.getBlockEntity(pos) instanceof ShoggothBiomassBlockEntity neighbour) {
            neighbour.cooldown=level.random.nextInt(30); neighbour.setChanged();
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE,origin.getX()+.5,origin.getY()+1,origin.getZ()+.5,2,.15,.1,.15,.005);
    }
    private static void neighbours(ServerLevel level,BlockPos origin,java.util.Set<BlockPos> result) {
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) if(dx!=0||dz!=0) {
            var pos=origin.offset(dx,0,dz);
            if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof ShoggothBiomassBlockEntity) result.add(pos);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); tag.putInt("Cooldown",cooldown); tag.putInt("SpawnedShoggoths",spawned);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); cooldown=Math.clamp(tag.getInt("Cooldown"),0,COOLDOWN-1); spawned=Math.clamp(tag.getInt("SpawnedShoggoths"),0,EXHAUSTION);
    }
}
