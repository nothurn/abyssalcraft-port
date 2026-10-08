package dev.abyssalport.research;

import dev.abyssalport.AbyssalPort;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Player-scoped discovery milestones for the first playable progression tier. */
public final class EarlyResearch {
    public static final int MONOLITH = ResearchMilestones.MONOLITH;
    public static final int RITUAL_GROUND = ResearchMilestones.RITUAL_GROUND;
    public static final int COLLECTOR = ResearchMilestones.COLLECTOR;
    public static final int NETWORK = ResearchMilestones.NETWORK;
    public static final int BASIC_SCROLL = ResearchMilestones.BASIC_SCROLL;
    public static final int SHADOW_MATERIALS = ResearchMilestones.SHADOW_MATERIALS;
    public static final int DARKLANDS = ResearchMilestones.DARKLANDS;
    public static final int ABYSSALNITE = ResearchMilestones.ABYSSALNITE;
    public static final int CORALIUM = ResearchMilestones.CORALIUM;
    public static final int TRANSMUTATION = ResearchMilestones.TRANSMUTATION;
    public static final int OBLIVION = ResearchMilestones.OBLIVION;
    public static final int GATEWAY = ResearchMilestones.GATEWAY;
    public static final int WASTELAND = ResearchMilestones.WASTELAND;
    public static final int REFINED_CORALIUM = ResearchMilestones.REFINED_CORALIUM;
    public static final int RENDING = ResearchMilestones.RENDING;
    public static final int WASTELAND_BOOK = ResearchMilestones.WASTELAND_BOOK;
    public static final int ASORAH = ResearchMilestones.ASORAH;
    public static final int POWERSTONE = ResearchMilestones.POWERSTONE;
    public static final int DREADLANDS_KEY = ResearchMilestones.DREADLANDS_KEY;
    public static final int DREADLANDS = ResearchMilestones.DREADLANDS;
    public static final int DREADLANDS_BOOK = ResearchMilestones.DREADLANDS_BOOK;
    public static final int PROCESSING = ResearchMilestones.PROCESSING;
    public static final int DREADIUM = ResearchMilestones.DREADIUM;
    public static final int CRYSTALLIZATION = ResearchMilestones.CRYSTALLIZATION;
    public static final int CRYSTAL_BAGS = ResearchMilestones.CRYSTAL_BAGS;
    public static final int GHOULS = ResearchMilestones.GHOULS;
    public static final int ALL = ResearchMilestones.ALL;

    private EarlyResearch() {
    }

    public static int discoveries(Player player) {
        return player.getData(AbyssalPort.EARLY_RESEARCH.get()) & ALL;
    }

    public static boolean has(Player player, int milestone) {
        return (discoveries(player) & milestone) == milestone;
    }

    /** Returns true only for a new, valid discovery. Repeated events do not spam chat. */
    public static boolean unlock(ServerPlayer player, int milestone) {
        if (Integer.bitCount(milestone) != 1 || (milestone & ALL) == 0 || has(player, milestone)) {
            return false;
        }
        player.setData(AbyssalPort.EARLY_RESEARCH.get(), discoveries(player) | milestone);
        player.displayClientMessage(Component.translatable("message.abyssalport.discovery",
                Component.translatable("research.abyssalport." + key(milestone))), false);
        return true;
    }

    public static boolean pageAvailable(int page, int discoveries) {
        return ResearchMilestones.pageAvailable(page, discoveries);
    }

    private static String key(int milestone) {
        return switch (milestone) {
            case MONOLITH -> "monolith";
            case RITUAL_GROUND -> "ritual_ground";
            case COLLECTOR -> "collector";
            case NETWORK -> "network";
            case BASIC_SCROLL -> "basic_scroll";
            case SHADOW_MATERIALS -> "shadow_materials";
            case DARKLANDS -> "darklands";
            case ABYSSALNITE -> "abyssalnite";
            case CORALIUM -> "coralium";
            case TRANSMUTATION -> "transmutation";
            case OBLIVION -> "oblivion";
            case GATEWAY -> "gateway";
            case WASTELAND -> "wasteland";
            case REFINED_CORALIUM -> "refined_coralium";
            case RENDING -> "rending";
            case WASTELAND_BOOK -> "wasteland_book";
            case ASORAH -> "asorah";
            case POWERSTONE -> "powerstone";
            case DREADLANDS_KEY -> "dreadlands_key";
            case DREADLANDS -> "dreadlands";
            case DREADLANDS_BOOK -> "dreadlands_book";
            case PROCESSING -> "processing";
            case DREADIUM -> "dreadium";
            case CRYSTALLIZATION -> "crystallization";
            case CRYSTAL_BAGS -> "crystal_bags";
            case GHOULS -> "ghouls";
            default -> throw new IllegalArgumentException("Unknown discovery");
        };
    }
}
