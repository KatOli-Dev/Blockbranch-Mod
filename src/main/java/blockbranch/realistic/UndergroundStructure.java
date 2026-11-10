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

    /** {@link #voidAt(int,int,int,int)} for a block of {@code section}; other blocks use the direct path. */
    public boolean voidAt(int x, int y, int z, int surfaceY, Section section) {
        return section == null || !section.covers(x, y, z) ? voidAt(x, y, z, surfaceY) : sectionDensity(x, y, z, surfaceY, section) < 0;
    }

    /** Same expression as {@link #density}, reading lattice values from the section tables. */
    private double sectionDensity(int x, int y, int z, int surfaceY, Section s) {
        int roof = surfaceY - ROOF_THICKNESS;
        int bottom = TerrainShape.FLOOR + FLOOR_THICKNESS;
        if (y >= roof || y <= bottom) return 1;
        int lx = x - s.x0, ly = y - s.y0, lz = z - s.z0;
        double width = .065 + .035 * (.5 + .5 * s.noise(0, lx, ly, lz));
        double a = s.noise(1, lx, ly, lz) + .18 * s.noise(2, lx, ly, lz);
        double b = s.noise(3, lx, ly, lz) + .18 * s.noise(4, lx, ly, lz);
        double passage = Math.max(Math.abs(a), Math.abs(b)) - width;
        double chamber = .58 - s.noise(5, lx, ly, lz);
        double geometry = Math.min(passage, chamber);
        double boundary = 1 - Math.min(1, Math.min(roof - y, y - bottom) / 16.0);
        return geometry + boundary;
    }

    // (scale, salt) of each noise term in density(), in the order sectionDensity reads them.
    private static final int[] SCALES = {137, 53, 23, 61, 29, 89};
    private static final long[] SALTS = {11, 29, 31, 43, 47, 71};

    /** Lattice tables for one 16-block section; single-threaded scratch. */
    public Section section(int x0, int y0, int z0) { return new Section(x0, y0, z0); }

    /**
     * Each density() noise hashes 8 lattice corners per block, 48 hashes in all. Every scale is
     * at least 16, so a section spans at most 2 lattice cells per axis and 27 corners per term.
     * The tables hold the identical value() results and fade weights; noise() interpolates in
     * the same order, so densities match bit for bit. Terms are filled on first use.
     */
    public final class Section {
        private final int x0, y0, z0;
        private final int[][] cell = new int[SCALES.length][];     // 3*16 local -> cell offset
        private final double[][] weight = new double[SCALES.length][]; // 3*16 local -> fade weight
        private final double[][] corners = new double[SCALES.length][];

        private Section(int x0, int y0, int z0) { this.x0 = x0; this.y0 = y0; this.z0 = z0; }

        boolean covers(int x, int y, int z) { return (x - x0) >>> 4 == 0 && (y - y0) >>> 4 == 0 && (z - z0) >>> 4 == 0; }

        private double noise(int term, int lx, int ly, int lz) {
            var c = corners[term];
            if (c == null) c = fill(term);
            int[] g = cell[term]; double[] w = weight[term];
            int ix = g[lx], iy = g[16 + ly], iz = g[32 + lz];
            double tx = w[lx], ty = w[16 + ly], tz = w[32 + lz];
            int i = (iz * 3 + iy) * 3 + ix;
            double near = lerp(ty, lerp(tx, c[i], c[i + 1]), lerp(tx, c[i + 3], c[i + 4]));
            double far = lerp(ty, lerp(tx, c[i + 9], c[i + 10]), lerp(tx, c[i + 12], c[i + 13]));
            return lerp(tz, near, far);
        }

        private double[] fill(int term) {
            int scale = SCALES[term]; long salt = SALTS[term];
            int gx = Math.floorDiv(x0, scale), gy = Math.floorDiv(y0, scale), gz = Math.floorDiv(z0, scale);
            int[] g = new int[48]; double[] w = new double[48];
            int[] origin = {x0, y0, z0}, base = {gx, gy, gz};
            for (int axis = 0; axis < 3; axis++) for (int l = 0; l < 16; l++) {
                int v = origin[axis] + l;
                g[axis * 16 + l] = Math.floorDiv(v, scale) - base[axis];
                w[axis * 16 + l] = fade(Math.floorMod(v, scale) / (double) scale);
            }
            var c = new double[27];
            for (int dz = 0; dz < 3; dz++) for (int dy = 0; dy < 3; dy++) for (int dx = 0; dx < 3; dx++)
                c[(dz * 3 + dy) * 3 + dx] = value(gx + dx, gy + dy, gz + dz, salt);
            cell[term] = g; weight[term] = w;
            return corners[term] = c;
        }
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
