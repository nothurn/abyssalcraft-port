package dev.abyssalport.worldgen;

/** Pure horizontal distance rule shared by the world lookup and fast unit tests. */
public final class MonolithSpacing {
    public static final int MIN_BLOCKS = 14;

    private MonolithSpacing() {
    }

    public static boolean tooClose(int firstX, int firstZ, int secondX, int secondZ) {
        long dx = (long) firstX - secondX;
        long dz = (long) firstZ - secondZ;
        return dx * dx + dz * dz < (long) MIN_BLOCKS * MIN_BLOCKS;
    }
}
