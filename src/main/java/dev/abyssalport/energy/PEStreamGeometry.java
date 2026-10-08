package dev.abyssalport.energy;

import java.util.ArrayList;
import java.util.List;

/** Bounded drifting line, not a travelling bolt. Source PEStreamMessage/
 * ParticleUtil supplies the line behaviour; this layout/density is independent.
 * Shinoow, LGPL-3.0. Pure arithmetic is shared with the actual client.
 */
public final class PEStreamGeometry {
    public static final double MAX_LENGTH = 32;
    public static final int MAX_SAMPLES = 64;
    public record Point(double x, double y, double z) {
        public boolean finite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
    }
    public record Sample(Point position, Point velocity) { }
    private PEStreamGeometry() { }
    public static List<Sample> line(Point from, Point to, int stride) {
        if (!from.finite() || !to.finite() || stride < 1 || stride > 4) return List.of();
        double dx=to.x-from.x, dy=to.y-from.y, dz=to.z-from.z;
        double length=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (!Double.isFinite(length) || length < .01 || length > MAX_LENGTH) return List.of();
        int count=Math.min(MAX_SAMPLES, Math.max(2, (int)Math.ceil(length*4)));
        var velocity=new Point(dx/length*.1, .15, dz/length*.1);
        var points=new ArrayList<Sample>();
        for(int index=0; index<count; index+=stride) {
            double t=index/(double)(count-1);
            points.add(new Sample(new Point(from.x+dx*t,from.y+dy*t,from.z+dz*t),velocity));
        }
        return List.copyOf(points);
    }
}
