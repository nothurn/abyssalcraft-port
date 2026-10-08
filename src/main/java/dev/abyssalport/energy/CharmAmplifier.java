package dev.abyssalport.energy;

/** Source charm factors; generic charms use the non-matching branch. */
public enum CharmAmplifier {
    NONE("none"), RANGE("range"), POWER("power"), DURATION("duration");

    private final String id;
    CharmAmplifier(String id) { this.id = id; }
    public String id() { return id; }

    public float factor(boolean matchingDeity) {
        return switch (this) {
            case NONE -> 0;
            case RANGE -> matchingDeity ? 6 : 4;
            case POWER -> matchingDeity ? 2.5F : 1.5F;
            case DURATION -> matchingDeity ? 4 : 2;
        };
    }

    public static CharmAmplifier fromId(String id) {
        for (var value : values()) if (value.id.equals(id)) return value;
        return NONE;
    }
}
