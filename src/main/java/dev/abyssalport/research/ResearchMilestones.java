package dev.abyssalport.research;

/** Pure discovery IDs and page visibility rules, independent of game runtime. */
public final class ResearchMilestones {
    public static final int MONOLITH = 1;
    public static final int RITUAL_GROUND = 1 << 1;
    public static final int COLLECTOR = 1 << 2;
    public static final int NETWORK = 1 << 3;
    public static final int BASIC_SCROLL = 1 << 4;
    public static final int SHADOW_MATERIALS = 1 << 5;
    public static final int DARKLANDS = 1 << 6;
    public static final int ABYSSALNITE = 1 << 7;
    public static final int CORALIUM = 1 << 8;
    public static final int TRANSMUTATION = 1 << 9;
    public static final int OBLIVION = 1 << 10;
    public static final int GATEWAY = 1 << 11;
    public static final int WASTELAND = 1 << 12;
    public static final int REFINED_CORALIUM = 1 << 13;
    public static final int RENDING = 1 << 14;
    public static final int WASTELAND_BOOK = 1 << 15;
    public static final int ASORAH = 1 << 16;
    public static final int POWERSTONE = 1 << 17;
    public static final int DREADLANDS_KEY = 1 << 18;
    public static final int DREADLANDS = 1 << 19;
    public static final int DREADLANDS_BOOK = 1 << 20;
    public static final int PROCESSING = 1 << 21;
    public static final int DREADIUM = 1 << 22;
    public static final int CRYSTALLIZATION = 1 << 23;
    public static final int CRYSTAL_BAGS = 1 << 24;
    public static final int GHOULS = 1 << 25;
    public static final int ALL = MONOLITH | RITUAL_GROUND | COLLECTOR | NETWORK | BASIC_SCROLL
            | SHADOW_MATERIALS | DARKLANDS | ABYSSALNITE | CORALIUM | TRANSMUTATION | OBLIVION | GATEWAY | WASTELAND | REFINED_CORALIUM | RENDING | WASTELAND_BOOK | ASORAH | POWERSTONE | DREADLANDS_KEY | DREADLANDS | DREADLANDS_BOOK | PROCESSING | DREADIUM | CRYSTALLIZATION | CRYSTAL_BAGS | GHOULS;

    private ResearchMilestones() {
    }

    public static boolean pageAvailable(int page, int discoveries) {
        return switch (page) {
            case 3 -> (discoveries & RITUAL_GROUND) != 0;
            case 4 -> (discoveries & SHADOW_MATERIALS) != 0;
            case 5 -> (discoveries & MONOLITH) != 0;
            case 6 -> (discoveries & COLLECTOR) != 0;
            case 7 -> (discoveries & DARKLANDS) != 0;
            case 8 -> (discoveries & (DARKLANDS | ABYSSALNITE)) != 0;
            case 9 -> (discoveries & CORALIUM) != 0;
            case 10 -> (discoveries & CORALIUM) != 0;
            case 11 -> (discoveries & (TRANSMUTATION | OBLIVION)) != 0;
            case 12 -> (discoveries & (OBLIVION | GATEWAY | WASTELAND)) != 0;
            case 13 -> (discoveries & WASTELAND) != 0;
            case 14 -> (discoveries & (WASTELAND | REFINED_CORALIUM)) != 0;
            case 15 -> (discoveries & (TRANSMUTATION | OBLIVION | RENDING)) != 0;
            case 16 -> (discoveries & (WASTELAND | WASTELAND_BOOK)) != 0;
            case 17 -> (discoveries & (WASTELAND_BOOK | ASORAH)) != 0;
            case 18 -> (discoveries & (WASTELAND | POWERSTONE)) != 0;
            case 19 -> (discoveries & (POWERSTONE | DREADLANDS_KEY)) != 0;
            case 20 -> (discoveries & DREADLANDS) != 0;
            case 21 -> (discoveries & (DREADLANDS | DREADLANDS_BOOK)) != 0;
            case 22 -> (discoveries & (WASTELAND | PROCESSING)) != 0;
            case 23 -> (discoveries & (DREADLANDS | DREADIUM)) != 0;
            case 24 -> (discoveries & (DREADIUM | CRYSTALLIZATION)) != 0;
            case 25 -> (discoveries & CRYSTAL_BAGS) != 0;
            case 26 -> (discoveries & GHOULS) != 0;
            default -> true;
        };
    }
}
