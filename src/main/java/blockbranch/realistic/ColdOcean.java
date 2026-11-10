package blockbranch.realistic;

/** Non-freezing cold surface-climate shelf with existing supported photic vegetation; no water-temperature simulation. */
public final class ColdOcean {
    public static boolean climate(ClimateField.Sample c) {
        return c.meanTemperatureC() < 8;
    }

    public static boolean contains(long seed, int x, int z, DrainageNetwork d, ClimateField c) {
        return surfaceContains(seed, Math.floorDiv(x, 4) * 4, Math.floorDiv(z, 4) * 4, d, c);
    }

    static boolean surfaceContains(long seed, int x, int z, DrainageNetwork d, ClimateField c) {
        return MarineShelf.surfaceContains(x, z, d) && climate(c.sample(x, z, d::sample))
            && !MangroveSwamp.exact(seed, x, z, d, c) && FrozenOcean.surfaceKind(seed,x,z,d,c)==0 && !RoutedRiver.contains(seed,x,z,d,c);
    }

    /** Reuse existing sorted sediment and native photic vegetation without repainting. */
    public static int cover(long seed, int x, int z, DrainageNetwork d, ClimateField c) {
        if (!surfaceContains(seed, x, z, d, c)) return 0;
        var site = MarineShelf.site(d, x, z);
        var climate = c.sample(x, z, d::sample);
        return MarineShelf.cover(seed, x, z, true, site, climate);
    }

    public record Point(int x, int z) {}





    private ColdOcean() {}
}
