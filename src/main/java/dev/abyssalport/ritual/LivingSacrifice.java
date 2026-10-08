package dev.abyssalport.ritual;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;

/** Original food categories, excluding children, undead and players. */
public final class LivingSacrifice {
    private LivingSacrifice() { }

    public static boolean eligible(Mob mob) {
        return mob.isAlive() && !mob.isBaby() && !mob.getType().is(EntityTypeTags.UNDEAD)
                && (mob instanceof Animal || mob instanceof AmbientCreature || mob instanceof WaterAnimal
                    || mob instanceof Spider || mob instanceof Villager);
    }

    public static AABB area(BlockPos altar) {
        return new AABB(altar).inflate(4);
    }

    public static Optional<Mob> select(ServerLevel level, BlockPos altar) {
        return level.getEntitiesOfClass(Mob.class, area(altar), LivingSacrifice::eligible).stream()
                .min(Comparator.comparingDouble((Mob mob) -> mob.distanceToSqr(altar.getX() + 0.5,
                        altar.getY() + 0.5, altar.getZ() + 0.5)).thenComparing(Mob::getUUID));
    }
}
