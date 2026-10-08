package dev.abyssalport.energy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

/** Bounded native admission shared by source spawn/swarm/conversion disruptions.
 * Derived rules: AbyssalCraft disruption classes, Shinoow, LGPL-3.0.
 * Fixes stale-height placement and unsafe fallback/removal in the reference.
 */
public final class DisruptionEntities {
    private static final Set<UUID> CONVERTING=new HashSet<>();
    private DisruptionEntities() { }
    public static boolean loaded(ServerLevel level,AABB box) {
        if(!Double.isFinite(box.minX)||!Double.isFinite(box.minY)||!Double.isFinite(box.minZ)
                ||!Double.isFinite(box.maxX)||!Double.isFinite(box.maxY)||!Double.isFinite(box.maxZ)
                ||box.minY<level.getMinBuildHeight()||box.maxY>level.getMaxBuildHeight()
                ||!level.getWorldBorder().isWithinBounds(box))return false;
        for(int x=BlockPos.containing(box.minX,0,0).getX()>>4;x<=BlockPos.containing(Math.nextDown(box.maxX),0,0).getX()>>4;x++)
            for(int z=BlockPos.containing(0,0,box.minZ).getZ()>>4;z<=BlockPos.containing(0,0,Math.nextDown(box.maxZ)).getZ()>>4;z++)
                if(!level.getChunkSource().hasChunk(x,z))return false;
        return true;
    }
    public static boolean clear(ServerLevel level,Entity entity,AABB box) {
        return loaded(level,box)&&level.noCollision(entity,box)&&!level.containsAnyLiquid(box);
    }
    public static Mob spawn(ServerLevel level,BlockPos anchor,EntityType<?> type,RandomSource random,Consumer<Mob> configure) {
        var made=type.create(level);if(!(made instanceof Mob mob)){if(made!=null)made.discard();return null;}
        boolean admitted=false;
        try {
            // Ten source-sized candidates, plus a checked near-statue fallback. Never
            // insert into solid/liquid space simply because all candidates failed.
            for(int attempt=0;attempt<11;attempt++) {
                int x=anchor.getX()+(attempt==10?random.nextInt(9)-4:random.nextInt(32)-16);
                int z=anchor.getZ()+(attempt==10?random.nextInt(9)-4:random.nextInt(32)-16);
                var column=new BlockPos(x,anchor.getY(),z);if(!level.hasChunkAt(column))continue;
                int y=level.canSeeSky(column)?level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z):anchor.getY()+random.nextInt(32)-16;
                if(y<level.getMinBuildHeight()+1||y>=level.getMaxBuildHeight()-3)continue;
                var pos=new BlockPos(x,y,z);mob.moveTo(x+.5,y,z+.5,random.nextFloat()*360,0);
                if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)
                        ||!clear(level,mob,mob.getBoundingBox()))continue;
                EventHooks.finalizeMobSpawn(mob,level,level.getCurrentDifficultyAt(pos),MobSpawnType.EVENT,null);
                if(mob.isSpawnCancelled())return null;
                configure.accept(mob);
                // Finalizers/configuration may alter size or position. Revalidate before join.
                if(!clear(level,mob,mob.getBoundingBox()))return null;
                admitted=level.addFreshEntity(mob)&&mob.isAddedToLevel()&&!mob.isRemoved();
                return admitted?mob:null;
            }
            return null;
        } finally {if(!admitted)mob.discard();}
    }
    public static boolean convert(ServerLevel level,Mob victim,EntityType<? extends Mob> type,Consumer<Mob> configure) {
        if(!victim.isAlive()||victim.level()!=level||!CONVERTING.add(victim.getUUID()))return false;
        Mob replacement=null;boolean committed=false;
        try(var possessions=new ConversionPossessions(victim)) {
            // Riding/leash links need their own native migration. Possessions no
            // longer exempt an otherwise valid animal from the actual disruption.
            if(victim.isPassenger()||victim.isVehicle()||victim.isLeashed())return false;
            if(!EventHooks.canLivingConvert(victim,type,ignored->{}))return false;
            replacement=type.create(level);if(replacement==null)return false;
            replacement.copyPosition(victim);replacement.setNoAi(victim.isNoAi());replacement.setPersistenceRequired();
            if(victim.hasCustomName()){replacement.setCustomName(victim.getCustomName());replacement.setCustomNameVisible(victim.isCustomNameVisible());}
            EventHooks.finalizeMobSpawn(replacement,level,level.getCurrentDifficultyAt(victim.blockPosition()),MobSpawnType.CONVERSION,null);
            if(replacement.isSpawnCancelled())return false;
            configure.accept(replacement);
            if(!possessions.unchanged(level)||!clear(level,replacement,replacement.getBoundingBox()))return false;
            if(!level.addFreshEntity(replacement)||!replacement.isAddedToLevel()||replacement.isRemoved()
                    ||!possessions.unchanged(level)||!possessions.admit(level)
                    ||!replacement.isAlive()||replacement.level()!=level
                    ||!clear(level,replacement,replacement.getBoundingBox()))return false;
            possessions.commit();committed=true;
            // Post means conversion succeeded, including the complete possession
            // transaction. A rejected item join must not emit a success callback.
            EventHooks.onLivingConvert(victim,replacement);return true;
        } finally {CONVERTING.remove(victim.getUUID());if(replacement!=null&&!committed)replacement.discard();}
    }
}
