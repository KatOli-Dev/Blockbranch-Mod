package blockbranch.realistic;

/** Continuous material variation, independent of terrain height and chunk boundaries. */
final class SurfacePatches {
    private static final double SKEW = (Math.sqrt(3) - 1) / 2;
    private static final double UNSKEW = (3 - Math.sqrt(3)) / 6;
    private static final double[] GX = new double[32], GZ = new double[32];
    static {
        for (int i = 0; i < GX.length; i++) {
            double angle = i * (2 * Math.PI / GX.length);
            GX[i] = Math.cos(angle); GZ[i] = Math.sin(angle);
        }
    }

    static double sample(long seed, int x, int z, double scale) {
        return .5 + .6 * signed(seed, x, z, scale);
    }

    /** Compact radial gradient kernels avoid the axis-flat edges of square value interpolation.
     * A differently oriented second wavelength softens the triangular sampling structure.
     * The scale factor retains the existing material patch sizes.
     */
    static double signed(long seed, int x, int z, double scale) {
        double u = x / (2.3 * scale) + 12.345, v = z / (2.3 * scale) - 7.891;
        return .75 * noise(seed, u, v)
            + .25 * noise(seed ^ 0xD1B54A32D192ED03L, 1.6 * u + .8 * v + 7.3, .8 * u - 1.6 * v + 2.8);
    }

    private static double noise(long seed, double x, double z) {
        double skew = (x + z) * SKEW;
        int ix = (int)Math.floor(x + skew), iz = (int)Math.floor(z + skew);
        double unskew = (ix + iz) * UNSKEW, dx = x - ix + unskew, dz = z - iz + unskew;
        int ox = dx > dz ? 1 : 0, oz = 1 - ox;
        return 70 * (corner(seed, ix, iz, dx, dz)
            + corner(seed, ix + ox, iz + oz, dx - ox + UNSKEW, dz - oz + UNSKEW)
            + corner(seed, ix + 1, iz + 1, dx - 1 + 2 * UNSKEW, dz - 1 + 2 * UNSKEW));
    }

    private static double corner(long seed, int x, int z, double dx, double dz) {
        double weight = .5 - dx * dx - dz * dz;
        if (weight <= 0) return 0;
        int gradient = (int)OakBirchWoodland.mix(seed ^ x * 0x9E3779B97F4A7C15L
            ^ z * 0xC2B2AE3D27D4EB4FL) & 31;
        weight *= weight;
        return weight * weight * (GX[gradient] * dx + GZ[gradient] * dz);
    }

    private SurfacePatches() {}
}
