package dev.abyssalport.energy;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EnergyDeviceTierTest {
    @Test void sourceValuesIncludeAllFiveTiersWithoutMinecraftClassLoading() {
        int[][] expected = {{1000,500,10_000,4,10,20}, {1500,600,20_000,6,20,30},
                {2000,700,60_000,8,30,40}, {2500,800,240_000,10,40,50}, {3000,900,1_200_000,12,50,60}};
        for (var tier : EnergyDeviceTier.values()) assertArrayEquals(expected[tier.ordinal()], new int[]{
                tier.collectorCapacity(), tier.relayCapacity(), tier.containerCapacity(), tier.range(), tier.drain(), tier.transfer()});
        for (var tier : EnergyDeviceTier.values()) assertEquals(5000 + 2500*tier.ordinal(), tier.pedestalCapacity());
    }
    @Test void higherStoreOverflowRemainsConservedAtSourceCapacities() {
        for (var tier : EnergyDeviceTier.values()) {
            var result = PotentialEnergyMath.add(tier.containerCapacity(), tier.containerCapacity()-3.25F, 20);
            assertEquals(tier.containerCapacity(), result.stored()); assertEquals(16.75F, result.remainder());
        }
    }
}
