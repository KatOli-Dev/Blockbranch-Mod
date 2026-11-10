package blockbranch.realistic;

/** Ice-free cold rock/debris, not a glacier or a high-elevation synonym for tundra. */
public final class ColdRockland {
    public static final String KEY = "cold_rockland";
    // The -6 C boundary is the existing generator's cold-open-domain proxy,
    // not a universal scientific treeline or a criterion for glaciation.
    public static boolean eligible(int x, int z, DrainageNetwork d, ClimateField c) {
        var s = d.sample(x, z);
        return s.waterLevel() == DrainageNetwork.DRY && !s.bedrockCoast() && d.channelAt(x, z).edge() < 0
            && c.sample(x, z, d::sample).meanTemperatureC() < -6;
    }
    public static boolean contains(int x, int z, DrainageNetwork d, ClimateField c) {
        return eligible(Math.floorDiv(x, 4) * 4, Math.floorDiv(z, 4) * 4, d, c);
    }
    /** Only the selector's final unassigned domain may acquire this owner. Exposed
     * cliffs retain stone; supported patches receive 0–2 m of existing gravel roles.
     * No persistent palette codes, new terrain heights or invented fertile soil. */
    public static int debrisDepth(long seed, int x, int z, DrainageNetwork d) {
        double slope = SlopeCorrection.slope(d, x, z);
        if (slope > 1.2) return 0;
        double patch = SurfacePatches.sample(seed ^ 0x434F4C44524F434BL, x, z, 21);
        return patch < .45 ? 0 : patch < .78 || slope > .65 ? 1 : 2;
    }
    /** Static snow retention on cold, non-arid, sheltered ground. Never claims
     * glacial mass balance, glacier flow or an all-land ice-sheet representation. */
    public static boolean retainsSnow(int x, int z, DrainageNetwork d, ClimateField c, GroundEcology.Field e) {
        var q = c.sample(x, z, d::sample); var site = e.sample(x, z);
        return q.meanTemperatureC() < -10 && q.moisture() >= .30 && SlopeCorrection.slope(d, x, z) <= .9
            && site.exposure() < .65;
    }
    private ColdRockland() {}
}
