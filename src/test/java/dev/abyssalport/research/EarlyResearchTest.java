package dev.abyssalport.research;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EarlyResearchTest {
    @Test
    void pagesOnlyRevealAfterTheirOwnDiscovery() {
        for (int page = 0; page <= 2; page++) {
            assertTrue(ResearchMilestones.pageAvailable(page, 0));
        }
        assertFalse(ResearchMilestones.pageAvailable(3, 0));
        assertFalse(ResearchMilestones.pageAvailable(4, 0));
        assertFalse(ResearchMilestones.pageAvailable(5, 0));
        assertFalse(ResearchMilestones.pageAvailable(6, 0));
        assertFalse(ResearchMilestones.pageAvailable(7, 0));
        assertFalse(ResearchMilestones.pageAvailable(8, 0));
        assertFalse(ResearchMilestones.pageAvailable(9, 0));
        assertFalse(ResearchMilestones.pageAvailable(10, 0));
        assertFalse(ResearchMilestones.pageAvailable(11, 0));
        assertFalse(ResearchMilestones.pageAvailable(12, 0));
        assertFalse(ResearchMilestones.pageAvailable(19, 0));
        assertFalse(ResearchMilestones.pageAvailable(20, 0));
        assertTrue(ResearchMilestones.pageAvailable(19, ResearchMilestones.POWERSTONE));
        assertTrue(ResearchMilestones.pageAvailable(19, ResearchMilestones.DREADLANDS_KEY));
        assertFalse(ResearchMilestones.pageAvailable(20, ResearchMilestones.DREADLANDS_KEY));
        assertTrue(ResearchMilestones.pageAvailable(20, ResearchMilestones.DREADLANDS));
        assertFalse(ResearchMilestones.pageAvailable(21, ResearchMilestones.DREADLANDS_KEY));
        assertFalse(ResearchMilestones.pageAvailable(21, 0));
        assertTrue(ResearchMilestones.pageAvailable(21, ResearchMilestones.DREADLANDS));
        assertTrue(ResearchMilestones.pageAvailable(21, ResearchMilestones.DREADLANDS_BOOK));
        assertFalse(ResearchMilestones.pageAvailable(22, 0));
        assertFalse(ResearchMilestones.pageAvailable(22, ResearchMilestones.TRANSMUTATION));
        assertTrue(ResearchMilestones.pageAvailable(22, ResearchMilestones.WASTELAND));
        assertTrue(ResearchMilestones.pageAvailable(22, ResearchMilestones.PROCESSING));
        assertFalse(ResearchMilestones.pageAvailable(23, 0));
        assertFalse(ResearchMilestones.pageAvailable(23, ResearchMilestones.PROCESSING));
        assertTrue(ResearchMilestones.pageAvailable(23, ResearchMilestones.DREADLANDS));
        assertTrue(ResearchMilestones.pageAvailable(23, ResearchMilestones.DREADIUM));
        assertFalse(ResearchMilestones.pageAvailable(24, 0));
        assertFalse(ResearchMilestones.pageAvailable(24, ResearchMilestones.PROCESSING));
        assertTrue(ResearchMilestones.pageAvailable(24, ResearchMilestones.DREADIUM));
        assertTrue(ResearchMilestones.pageAvailable(24, ResearchMilestones.CRYSTALLIZATION));
        assertFalse(ResearchMilestones.pageAvailable(25,0));
        assertFalse(ResearchMilestones.pageAvailable(25,ResearchMilestones.CRYSTALLIZATION));
        assertTrue(ResearchMilestones.pageAvailable(25,ResearchMilestones.CRYSTAL_BAGS));
        assertFalse(ResearchMilestones.pageAvailable(26,0));
        assertFalse(ResearchMilestones.pageAvailable(26,ResearchMilestones.CRYSTAL_BAGS));
        assertTrue(ResearchMilestones.pageAvailable(26,ResearchMilestones.GHOULS));
        assertFalse(ResearchMilestones.pageAvailable(13, 0));
        assertFalse(ResearchMilestones.pageAvailable(14, 0));
        assertTrue(ResearchMilestones.pageAvailable(14, ResearchMilestones.WASTELAND));
        assertTrue(ResearchMilestones.pageAvailable(14, ResearchMilestones.REFINED_CORALIUM));
        assertFalse(ResearchMilestones.pageAvailable(14, ResearchMilestones.GATEWAY));
        assertFalse(ResearchMilestones.pageAvailable(15, 0));
        assertTrue(ResearchMilestones.pageAvailable(15, ResearchMilestones.TRANSMUTATION));
        assertTrue(ResearchMilestones.pageAvailable(15, ResearchMilestones.RENDING));
        assertFalse(ResearchMilestones.pageAvailable(16, ResearchMilestones.RENDING));
        assertTrue(ResearchMilestones.pageAvailable(16, ResearchMilestones.WASTELAND));
        assertTrue(ResearchMilestones.pageAvailable(16, ResearchMilestones.WASTELAND_BOOK));
        assertTrue(ResearchMilestones.pageAvailable(12, ResearchMilestones.OBLIVION));
        assertTrue(ResearchMilestones.pageAvailable(12, ResearchMilestones.GATEWAY));
        assertFalse(ResearchMilestones.pageAvailable(13, ResearchMilestones.GATEWAY));
        assertTrue(ResearchMilestones.pageAvailable(13, ResearchMilestones.WASTELAND));
        assertTrue(ResearchMilestones.pageAvailable(11, ResearchMilestones.TRANSMUTATION));
        assertTrue(ResearchMilestones.pageAvailable(11, ResearchMilestones.OBLIVION));
        assertFalse(ResearchMilestones.pageAvailable(11, ResearchMilestones.CORALIUM));
        assertTrue(ResearchMilestones.pageAvailable(9, ResearchMilestones.CORALIUM));
        assertTrue(ResearchMilestones.pageAvailable(10, ResearchMilestones.CORALIUM));
        assertFalse(ResearchMilestones.pageAvailable(9, ResearchMilestones.ABYSSALNITE));

        assertTrue(ResearchMilestones.pageAvailable(3, ResearchMilestones.RITUAL_GROUND));
        assertFalse(ResearchMilestones.pageAvailable(4, ResearchMilestones.RITUAL_GROUND));
        assertTrue(ResearchMilestones.pageAvailable(4, ResearchMilestones.SHADOW_MATERIALS));
        assertFalse(ResearchMilestones.pageAvailable(5, ResearchMilestones.RITUAL_GROUND));
        assertFalse(ResearchMilestones.pageAvailable(6, ResearchMilestones.RITUAL_GROUND));
        assertTrue(ResearchMilestones.pageAvailable(5, ResearchMilestones.MONOLITH));
        assertFalse(ResearchMilestones.pageAvailable(6, ResearchMilestones.MONOLITH));
        assertTrue(ResearchMilestones.pageAvailable(6, ResearchMilestones.COLLECTOR));
        assertFalse(ResearchMilestones.pageAvailable(7, ResearchMilestones.COLLECTOR));
        assertTrue(ResearchMilestones.pageAvailable(7, ResearchMilestones.DARKLANDS));
        assertTrue(ResearchMilestones.pageAvailable(8, ResearchMilestones.DARKLANDS));
        assertTrue(ResearchMilestones.pageAvailable(8, ResearchMilestones.ABYSSALNITE));
        assertFalse(ResearchMilestones.pageAvailable(7, ResearchMilestones.ABYSSALNITE));
    }
}
