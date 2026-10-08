package dev.abyssalport.ritual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RitualTimingTest {
    @Test
    void preservesOriginalTenStepTimingAtBoundaries() {
        assertEquals(new RitualTiming.Plan(500, 1), RitualTiming.forCost(5_000, 1));
        assertEquals(new RitualTiming.Plan(500, 1), RitualTiming.forCost(5_000, 500));
        assertEquals(new RitualTiming.Plan(1_000, 2), RitualTiming.forCost(5_000, 501));
        assertEquals(new RitualTiming.Plan(1_500, 3), RitualTiming.forCost(5_000, 1_200));
        assertEquals(new RitualTiming.Plan(5_000, 10), RitualTiming.forCost(5_000, 5_000));
    }

    @Test
    void rejectsImpossibleRituals() {
        assertThrows(IllegalArgumentException.class, () -> RitualTiming.forCost(5_000, 0));
        assertThrows(IllegalArgumentException.class, () -> RitualTiming.forCost(5_000, 5_001));
        assertThrows(IllegalArgumentException.class, () -> RitualTiming.forCost(9, 1));
    }
}
