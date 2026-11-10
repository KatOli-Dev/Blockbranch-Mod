package blockbranch.realistic;

/** Thin static sediment veneers over the existing frozen-ocean substrate.
 * Slope/concavity are retention proxies, not a glacial transport simulation. */
final class FrozenSeabed {
    /** Existing aquatic plant codes own both their plants and supporting bed. */
    static int replacement(int existing, int frozen) {
        return existing >= 10 && existing <= 12 || frozen == 0 ? existing : frozen;
    }
    /** Call only after the existing shelf/deep physical substrate gate passes. */
    static int frozenCover(long seed, int x, int z, DrainageNetwork drainage, ClimateField climate, boolean icebergs) {
        if (FrozenOcean.surfaceKind(seed, x, z, drainage, climate) == 0
                || icebergs && IcebergField.habitat(seed, x, z, drainage, climate)) return 0;
        return cover(seed, x, z, drainage);
    }
    static int cover(long seed, int x, int z, int depth, double slope, double retention) {
        if (depth < 2) return 0;
        // Continuous block-resolution fields; no biome/quart mask is reused
        // to draw individual materials. Small-scale variation only softens edges.
        double exposure = .85 * slope - .12 * Math.clamp(retention, -1, 1) + .30 * sorting(seed, x, z);
        if (exposure > .43) return 9; // Existing stone/deepslate, never an invented rock unit.
        if (exposure > .14) return 8; // Coarse residual gravel on exposed/sloping bed.
        double fines = fines(seed, x, z);
        if (depth <= 40 && retention < .4) return fines > .20 ? 6 : 7;
        return fines > .12 ? 6 : 5; // Clay-rich patches within unconsolidated fines.
    }

    static double sorting(long seed, int x, int z) {
        return SurfacePatches.signed(seed ^ 0x46524F5A454E4244L, x, z, 48)
            + .12 * SurfacePatches.signed(seed ^ 0x46524F5A454E4544L, x, z, 7);
    }

    static double fines(long seed, int x, int z) {
        return SurfacePatches.signed(seed ^ 0x46524F5A46494E45L, x, z, 36)
            + .10 * SurfacePatches.signed(seed ^ 0x46524F5A46494E45L, x, z, 5);
    }

    static int cover(long seed, int x, int z, DrainageNetwork drainage) {
        var s = drainage.sample(x, z);
        int west = drainage.sample(x - 32, z).height(), east = drainage.sample(x + 32, z).height();
        int north = drainage.sample(x, z - 32).height(), south = drainage.sample(x, z + 32).height();
        double slope = Math.hypot((east - west) / 64., (south - north) / 64.);
        double retention = ((west + east + north + south) / 4. - s.height()) / 8.;
        return cover(seed, x, z, s.waterLevel() - s.height() - 1, slope, retention);
    }

    private FrozenSeabed() {}
}
