package dev.abyssalport.energy;

import dev.abyssalport.content.StatueContent.Deity;
import java.util.Arrays;
import java.util.List;
import net.minecraft.util.RandomSource;

/** Actual addDisruptions catalogue, adapted from AbyssalCraft (Shinoow, LGPL-3.0).
 * Copyright (c) 2012-2026 Shinoow. Commented-out registrations are not effects.
 */
public enum DisruptionCatalogue {
    LIGHTNING("lightning"), FIRE("fire"), SHOGGOTH("spawnShoggoth"),
    POISON("poisonPotion"), SLOWNESS("slownessPotion"), WEAKNESS("weaknessPotion"),
    WITHER("witherPotion"), CORALIUM("coraliumPotion"), POTENTIAL_ENERGY("potentialEnergy"),
    FREEZE("freeze"), SHADOW_SWARM("swarmShadow"), FIRE_RAIN("fireRain"), DISPLACE("displace"),
    RANDOM_TELEPORT("randomTeleport"), DRAIN_PE("potentialEnergyDrain"),
    SHEEP_SWARM("swarmSheep",Deity.SHUB_NIGGURATH), ANIMAL_CORRUPTION("animalCorruption",Deity.SHUB_NIGGURATH),
    OOZE("ooze"), RANDOM_SWARM("randomSwarm"), RANDOM_SPAWN("randomSpawn"),
    OFFSPRING("spawnShubOffspring",Deity.SHUB_NIGGURATH),
    INVISIBLE_HASTUR("invisibleSwarmHastur",Deity.HASTUR),
    INVISIBLE_NYARLATHOTEP("invisibleSwarmNyarlathotep",Deity.NYARLATHOTEP),
    SACRIFICE_JZAHAR("sacrificeCorruptionJzahar",Deity.JZAHAR),
    SACRIFICE_YOG("sacrificeCorruptionYogSothoth",Deity.YOG_SOTHOTH),
    FAMINE_AZATHOTH("famineAzathoth",Deity.AZATHOTH),
    FAMINE_SHUB("famineShuNiggurath",Deity.SHUB_NIGGURATH);

    private final String sourceId;
    private final Deity deity;
    DisruptionCatalogue(String id) { this(id,null); }
    DisruptionCatalogue(String id,Deity deity) { sourceId=id;this.deity=deity; }
    public String sourceId() { return sourceId; }
    public Deity deity() { return deity; }
    public String translationKey() { return "disruption.abyssalport."+sourceId; }
    public static List<DisruptionCatalogue> eligible(Deity deity) {
        return Arrays.stream(values()).filter(effect->effect.deity==null||effect.deity==deity).toList();
    }
    public static DisruptionCatalogue choose(Deity deity,RandomSource random) {
        var effects=eligible(deity);return effects.get(random.nextInt(effects.size()));
    }
}
