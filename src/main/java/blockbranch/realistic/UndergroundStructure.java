package blockbranch.realistic;

/**
 * Initial immutable cave geometry field, sampled in world coordinates.
 * Used by versioned sparse profiles when explicitly enabled. Positive density is rock;
 * negative density is a candidate void. This is a geometric proxy, not erosion.
 */
public final class UndergroundStructure {
    public static final int VERSION = 1;
    public static final int ROOF_THICKNESS = 24;
    public static final int FLOOR_THICKNESS = 32;
    private final long seed;

    public UndergroundStructure(long seed) { this.seed = seed; }

    /**
     * Retains a solid roof below the supplied final surface/seabed and a solid
     * basal layer. Water, entrances, materials and biomes are separate decisions.
     */
    public double density(int x, int y, int z, int surfaceY) {
        int roof = surfaceY - ROOF_THICKNESS;
        int bottom = TerrainShape.FLOOR + FLOOR_THICKNESS;
        if (y >= roof || y <= bottom) return 1;
        // Two intersecting smooth fields produce winding passages. A slower
        // field varies their thickness without imposing chunk or quart grids.
        double width = .065 + .035 * (.5 + .5 * noise(x, y, z, 137, 11));
        double a = noise(x, y, z, 53, 29) + .18 * noise(x, y, z, 23, 31);
        double b = noise(x, y, z, 61, 43) + .18 * noise(x, y, z, 29, 47);
        double passage = Math.max(Math.abs(a), Math.abs(b)) - width;
        // Larger voids occupy selected regions, rather than filling every
        // passage with a cavern. Rock remains the majority of the volume.
        double chamber = .58 - noise(x, y, z, 89, 71);
        double geometry = Math.min(passage, chamber);
        // Smoothly close voids against the protected roof and basal layers.
        double boundary = 1 - Math.min(1, Math.min(roof - y, y - bottom) / 16.0);
        return geometry + boundary;
    }

    public boolean voidAt(int x, int y, int z, int surfaceY) {
        return density(x, y, z, surfaceY) < 0;
    }

    private double noise(int x, int y, int z, int scale, long salt) {
        int gx = Math.floorDiv(x, scale), gy = Math.floorDiv(y, scale), gz = Math.floorDiv(z, scale);
        double tx = fade(Math.floorMod(x, scale) / (double) scale);
        double ty = fade(Math.floorMod(y, scale) / (double) scale);
        double tz = fade(Math.floorMod(z, scale) / (double) scale);
        double near = lerp(ty, lerp(tx, value(gx, gy, gz, salt), value(gx + 1, gy, gz, salt)),
            lerp(tx, value(gx, gy + 1, gz, salt), value(gx + 1, gy + 1, gz, salt)));
        double far = lerp(ty, lerp(tx, value(gx, gy, gz + 1, salt), value(gx + 1, gy, gz + 1, salt)),
            lerp(tx, value(gx, gy + 1, gz + 1, salt), value(gx + 1, gy + 1, gz + 1, salt)));
        return lerp(tz, near, far);
    }

    private double value(int x, int y, int z, long salt) {
        long h = seed ^ x * 0x9E3779B97F4A7C15L ^ y * 0xD6E8FEB86659FD93L
            ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53 * 2 - 1;
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
}
