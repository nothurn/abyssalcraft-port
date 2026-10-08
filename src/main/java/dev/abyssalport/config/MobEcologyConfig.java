package dev.abyssalport.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Native common config; source defaults from AbyssalCraft InitHandler (Shinoow, LGPL-3.0). */
public final class MobEcologyConfig {
    private static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue HARDCORE=BUILDER.define("hardcoreMobs",false);
    public static final ModConfigSpec.BooleanValue EVIL_NEW_MOON=BUILDER.define("evilAnimalsOnlyNewMoon",true);
    public static final ModConfigSpec.BooleanValue DEMON_ON_DEATH=BUILDER.define("evilAnimalsSpawnDemonsOnDeath",true);
    public static final ModConfigSpec.BooleanValue DEMON_FIRE=BUILDER.define("demonAnimalsBurnInOverworld",false);
    public static final ModConfigSpec.BooleanValue MIMIC_FIRE=BUILDER.define("demonsUseMimicFire",true);
    public static final ModConfigSpec.BooleanValue DISABLE_DISRUPTIONS=BUILDER.comment("Disable statue disruptions; tolerance still clears at the source threshold.").define("disableStatueDisruptions",false);
    public static final ModConfigSpec.IntValue EVIL_WEIGHT=BUILDER.comment("Biome weights apply when loading a world.").defineInRange("evilAnimalSpawnWeight",15,0,100);
    public static final ModConfigSpec.IntValue DEMON_WEIGHT=BUILDER.defineInRange("demonAnimalSpawnWeight",15,0,100);
    public static final ModConfigSpec.IntValue OFFSPRING_WEIGHT=BUILDER.defineInRange("shubOffspringSpawnWeight",5,0,50);
    public static final ModConfigSpec SPEC=BUILDER.build();
    private MobEcologyConfig() { }
    public static boolean hardcore() { return SPEC.isLoaded()&&HARDCORE.get(); }
    public static boolean newMoonOnly() { return !SPEC.isLoaded()||EVIL_NEW_MOON.get(); }
    public static boolean demonOnDeath() { return !SPEC.isLoaded()||DEMON_ON_DEATH.get(); }
    public static boolean overworldFire() { return SPEC.isLoaded()&&DEMON_FIRE.get(); }
    public static boolean mimicFire() { return !SPEC.isLoaded()||MIMIC_FIRE.get(); }
    public static boolean disruptionsDisabled() { return SPEC.isLoaded()&&DISABLE_DISRUPTIONS.get(); }
    public static int weight(String group) { return switch(group) {
        case "evil" -> SPEC.isLoaded()?EVIL_WEIGHT.get():15;
        case "demon" -> SPEC.isLoaded()?DEMON_WEIGHT.get():15;
        case "offspring" -> SPEC.isLoaded()?OFFSPRING_WEIGHT.get():5;
        default -> throw new IllegalArgumentException("Unknown animal ecology group: "+group);
    }; }
}
