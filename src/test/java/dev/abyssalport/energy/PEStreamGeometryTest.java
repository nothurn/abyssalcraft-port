package dev.abyssalport.energy;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import dev.abyssalport.energy.PEStreamGeometry.Point;

class PEStreamGeometryTest {
    @Test void actualSharedClientGeometryIsBoundedInAllSixDirectionsAndHonorsParticleDensity() {
        var from=new Point(12.5,120.5,-.5);
        for(var to:new Point[]{new Point(16.5,120.5,-.5),new Point(8.5,120.5,-.5),new Point(12.5,124.5,-.5),
                new Point(12.5,116.5,-.5),new Point(12.5,120.5,3.5),new Point(12.5,120.5,-4.5)}) {
            var full=PEStreamGeometry.line(from,to,1);
            assertEquals(16,full.size()); assertEquals(from,full.getFirst().position()); assertEquals(to,full.getLast().position());
            assertEquals(8,PEStreamGeometry.line(from,to,2).size()); assertEquals(4,PEStreamGeometry.line(from,to,4).size());
            for(var sample:full) { assertTrue(sample.position().finite()); assertTrue(sample.velocity().finite()); assertEquals(.15,sample.velocity().y()); }
        }
        assertEquals(64,PEStreamGeometry.line(from,new Point(44.5,120.5,-.5),1).size());
    }
    @Test void zeroInvalidAndOversizedLinesAllocateNoParticles() {
        var origin=new Point(0,0,0);
        for(var endpoint:new Point[]{origin,new Point(Double.NaN,0,0),new Point(Double.POSITIVE_INFINITY,0,0),new Point(0,0,32.1),new Point(Double.MAX_VALUE,0,0)})
            assertTrue(PEStreamGeometry.line(origin,endpoint,1).isEmpty());
        assertTrue(PEStreamGeometry.line(origin,new Point(1,0,0),0).isEmpty());
        assertTrue(PEStreamGeometry.line(origin,new Point(1,0,0),5).isEmpty());
    }
}
