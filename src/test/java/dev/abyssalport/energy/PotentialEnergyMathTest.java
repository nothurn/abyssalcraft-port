package dev.abyssalport.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PotentialEnergyMathTest {
    @Test
    void addReportsOverflowWithoutExceedingCapacity() {
        assertEquals(new PotentialEnergyMath.Transfer(5_000, 300),
                PotentialEnergyMath.add(5_000, 4_800, 500));
    }

    @Test
    void consumeReportsActualAmount() {
        assertEquals(new PotentialEnergyMath.Transfer(0, 200),
                PotentialEnergyMath.consume(5_000, 200, 500));
    }

    @Test
    void rejectsNegativeAndNonFiniteTransfers() {
        assertThrows(IllegalArgumentException.class, () -> PotentialEnergyMath.add(5_000, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> PotentialEnergyMath.consume(5_000, 0, Float.NaN));
    }
}
