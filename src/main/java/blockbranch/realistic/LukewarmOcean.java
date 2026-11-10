package blockbranch.realistic;

/** Surface-climate marine shelf with photic seagrass meadows; no water-temperature simulation. */
public final class LukewarmOcean {
    public static boolean climate(ClimateField.Sample c) {
        return c.meanTemperatureC() >= 18 && c.meanTemperatureC() < 24;
    }

    public static boolean contains(long seed, int x, int z, DrainageNetwork d, ClimateField c) {
        return surfaceContains(seed, Math.floorDiv(x, 4) * 4, Math.floorDiv(z, 4) * 4, d, c);
    }

    static boolean surfaceContains(long seed, int x, int z, DrainageNetwork d, ClimateField c) {
        return MarineShelf.surfaceContains(x, z, d) && climate(c.sample(x, z, d::sample))
            && !MangroveSwamp.exact(seed, x, z, d, c);
    }

    /** Preserve the sorted bed; vary short native grass density at meadow and tuft scales. */
    public static int cover(long seed, int x, int z, DrainageNetwork d, ClimateField c) {
        if (!surfaceContains(seed, x, z, d, c)) return 0;
        var site = MarineShelf.site(d, x, z);
        var climate = c.sample(x, z, d::sample);
        int base = MarineShelf.cover(seed, x, z, true, site, climate);
        if ((base != 7 && base != 12) || !MarineShelf.plantSuitable(site, climate)) return base;
        // Both codes have the same sandy substrate. Treat the old and added
        // populations together so overlapping colonies cannot create a carpet.
        return seagrass(seed, x, z) ? 12 : 7;
    }

    static boolean seagrass(long seed, int x, int z) {
        var site = new GroundEcology.Site(.75, 0, 0, 0, 0, false);
        double meadow = GroundEcology.colonyStrength(seed, 0x4C554B4553454147L, x, z, 40, 9, 18, site);
        // Reuse fully jittered, rotated, variable-radius foliage colonies at a
        // second scale: small tufts and soft open gaps within the broad meadow.
        double tuft = GroundEcology.colonyStrength(seed, 0x5345415455465453L, x, z, 12, 2, 7, site);
        double chance = (OakBirchWoodland.mix(seed ^ 0x4C554B454D454144L
            ^ x * 0x165667B19E3779F9L ^ z * 0xD1B54A32D192ED03L) >>> 11) * 0x1.0p-53;
        return chance < (.16 + .52 * meadow) * (.25 + .75 * tuft);
    }

    public record Point(int x, int z) {}





    private LukewarmOcean() {}
}
