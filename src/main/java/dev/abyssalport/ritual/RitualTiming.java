package dev.abyssalport.ritual;

/*
 * Adapted from ItemNecronomicon#getPercentileAndSeconds in AbyssalCraft.
 * Copyright (c) 2012-2026 Shinoow. Licensed under LGPL-3.0-only.
 * Original source: https://github.com/Shinoow/AbyssalCraft
 */
public final class RitualTiming {
    private RitualTiming() {
    }

    public record Plan(int roundedThreshold, int seconds) {
    }

    /** Finds the original ten-percent timing step; the actual energy debit remains the exact ritual cost. */
    public static Plan forCost(int capacity, int cost) {
        if (capacity <= 0 || cost <= 0 || cost > capacity) {
            throw new IllegalArgumentException("Ritual capacity and cost must be positive; cost must fit capacity");
        }
        int step = capacity / 10;
        if (step == 0) {
            throw new IllegalArgumentException("Ritual capacity must be at least ten energy units");
        }
        int seconds = Math.min(10, (int) (((long) cost + step - 1) / step));
        return new Plan(Math.min(capacity, seconds * step), seconds);
    }
}
