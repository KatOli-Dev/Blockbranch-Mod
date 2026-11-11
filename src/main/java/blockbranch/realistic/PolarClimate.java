package blockbranch.realistic;

/** New-world geography convention, not a fitted Earth latitude/temperature curve.
 * Preserve the existing warm/midlatitude climate through 60% of the finite map's
 * half-span. A C2 taper supplies genuinely polar lowlands at the two edges.
 * The chosen 36 C edge cooling gives a sea-level mean of -32 C before the old
 * bounded +/-5 C macro anomaly. With the existing 20 C land half-range and +4 C
 * warm-spell screen, even its warm edge is below freezing. This establishes an
 * idealized thermal opportunity, never snowfall, perennial balance or ice extent. */
public final class PolarClimate {
    public static final double START=.60,EDGE_COOLING_C=36;
    public static double cooling(double latitude){return EDGE_COOLING_C*smooth((latitude-START)/(1-START));}
    static double smooth(double t){t=Math.clamp(t,0,1);return t*t*t*(t*(t*6-15)+10);}
    public static double latitude(int z){return Math.min(1,Math.abs((double)z)/2097152.);}
    private PolarClimate(){}
}
