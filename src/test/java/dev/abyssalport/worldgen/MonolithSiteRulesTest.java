package dev.abyssalport.worldgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MonolithSiteRulesTest {
    @Test
    void spacingIsHorizontalAndAcceptsTheBoundary() {
        assertTrue(MonolithSpacing.tooClose(0, 0, 13, 0));
        assertFalse(MonolithSpacing.tooClose(0, 0, 14, 0));
        assertTrue(MonolithSpacing.tooClose(0, 0, 9, 9));
    }
}
