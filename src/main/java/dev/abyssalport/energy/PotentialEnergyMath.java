package dev.abyssalport.energy;

/*
 * Adapted from PEUtils#addEnergy and PEUtils#consumeEnergy in AbyssalCraft.
 * Copyright (c) 2012-2026 Shinoow. Licensed under LGPL-3.0-only.
 * Original source: https://github.com/Shinoow/AbyssalCraft
 */
public final class PotentialEnergyMath {
    private PotentialEnergyMath() {
    }

    /** For add, remainder is overflow; for consume, remainder is energy actually consumed. */
    public record Transfer(float stored, float remainder) {
    }

    public static Transfer add(float capacity, float stored, float incoming) {
        validate(capacity, stored, incoming);
        float accepted = Math.min(incoming, capacity - stored);
        return new Transfer(stored + accepted, incoming - accepted);
    }

    public static Transfer consume(float capacity, float stored, float requested) {
        validate(capacity, stored, requested);
        float consumed = Math.min(requested, stored);
        return new Transfer(stored - consumed, consumed);
    }

    private static void validate(float capacity, float stored, float delta) {
        if (!Float.isFinite(capacity) || !Float.isFinite(stored) || !Float.isFinite(delta)
                || capacity <= 0 || stored < 0 || stored > capacity || delta < 0) {
            throw new IllegalArgumentException("Potential energy values must be finite and within capacity");
        }
    }
}
