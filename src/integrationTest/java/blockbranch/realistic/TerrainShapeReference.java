// Exact pre-optimization field snapshot at c637d6c, retained only as a compatibility oracle.
package blockbranch.realistic;

/** Immutable, world-coordinate terrain field. No chunk-local randomness or mutable caches. */
public final class TerrainShapeReference {
    // One block is one meter on all axes. Sea level retains the Minecraft
    // datum; physical elevation is surface Y + 1 - SEA_LEVEL (block top).
    public static final int FLOOR = -12288;
    public static final int CEILING = 9216;
    public static final int SEA_LEVEL = 64;
    // Base height field retains definition52; definition79 adds marine cold-season ice over unchanged coastal/inland terrain in DrainageNetwork.
    public static final int SHAPE_VERSION = 82;
    private final long seed;

    public TerrainShapeReference(long seed) { this.seed = seed; }

    /** Highest solid block, inclusive. All wavelengths and amplitudes are meters. */
    public int height(int x,int z) {
        int height=(int)Math.floor(continuousHeight(x,z));
        if(height<=FLOOR||height>=CEILING)throw new IllegalStateException("Terrain exceeds envelope: "+height);
        return height;
    }
    /** Same original field before voxel quantization; used for continuous bank adaptation. */
    double continuousHeight(double x,double z) {
        double wx = x + 32000 * fractal(x, z, 280000, 3, 11);
        double wz = z + 32000 * fractal(x, z, 280000, 3, 29);
        // A million-meter regional wavelength, rather than 12 km "continents".
        double continent = fractal(wx, wz, 1100000, 4, 101) - 0.12;
        double elevation;
        if (continent < 0) {
            double distance = -continent;
            double shelf = smooth(0, 0.03, distance);
            double deep = smooth(0.03, 0.11, distance);
            double basin = -200 * shelf - 5000 * deep;
            double oceanVariation = 650 * fractal(wx, wz, 180000, 3, 131);
            double ridge = 1 - smooth(0.015, 0.16, Math.abs(fractal(wx, wz, 320000, 3, 151)));
            double trenchLine = 1 - smooth(0.003, 0.025, Math.abs(fractal(wx, wz, 210000, 2, 173)));
            // Trenches occur in selected offshore margin belts, not across
            // the middle of every abyssal basin.
            double trenchRegion = smooth(0.12, 0.38, fractal(wx, wz, 430000, 2, 191))
                * (1 - smooth(0.18, 0.32, distance));
            double trench = 5000 * trenchLine * trenchRegion;
            elevation = basin + deep * (oceanVariation + 1700 * ridge - trench
                + 45 * fractal(wx, wz, 12000, 3, 199));
        } else {
            double inland = smooth(0, 0.075, continent);
            double lowland = 1100 * smooth(0, 0.38, continent);
            double belt = 1 - smooth(0.035, 0.19, Math.abs(fractal(wx, wz, 240000, 3, 211)));
            double uplift = smooth(-0.10, 0.38, fractal(wx, wz, 360000, 3, 307));
            double mountains = inland * belt * uplift;
            double ridge = 1 - Math.abs(fractal(wx, wz, 6500, 4, 401));
            // Analytic valley relief, not a physical erosion/drainage simulation.
            double valley = smooth(0.02, 0.24, Math.abs(fractal(wx, wz, 18000, 3, 503)));
            double relief = mountains * (2000 + 5400 * Math.pow(ridge, 1.7) * (0.35 + 0.65 * valley));
            double hills = inland * (50 + 150 * (1 - belt)) * fractal(wx, wz, 7500, 4, 601);
            double detail = inland * (2 + 18 * mountains) * fractal(x, z, 180, 3, 701);
            elevation = lowland + relief + hills + detail;
        }
        return SEA_LEVEL-1+elevation;
    }

    /** Original warped continental coordinate, independent of later coastal uplift. */
    double continentality(double x,double z){
        double wx=x+32000*fractal(x,z,280000,3,11),wz=z+32000*fractal(x,z,280000,3,29);
        return fractal(wx,wz,1100000,4,101)-.12;
    }

    /** Landform-scale hollows/shoulders with small surface relief. Continuous seeded
     * geometry, not an erosion simulation. Gentle plains retain low roughness. */
    double surfaceRelief(int x,int z,double slope) {return surfaceReliefStrength(x,z,smooth(.035,.35,slope));}
    double surfaceReliefStrength(int x,int z,double strength) {
        double u=.8*x+.6*z,v=-.6*x+.8*z;
        // The old regional mountains jump from kilometre-scale slopes to tiny
        // detail. Warped intermediate ridges and ravines supply the missing
        // shoulders between them; this is analytic relief, not simulated erosion.
        double warp=45*fractal(u,v,310,2,983);
        double ridge=1-2*Math.abs(fractal(u+warp,v-warp,185,3,997));
        double shoulder=fractal(u,v,420,3,911);
        double medium=fractal(u+warp,v-warp,72,2,937),fine=fractal(u,v,19,2,953);
        double bound=120*strength;
        return Math.clamp(strength*(120*shoulder+65*(ridge-.55)+22*medium+.6*fine),-bound,bound);
    }

    /** Bounded short-scale bank/bed variation, not erosion or sediment transport. */
    double bankFraction(int x,int z){return .5*Math.tanh(2*fractal(.8*x+.6*z,-.6*x+.8*z,63,3,971));}

    /** Reach-scale variation sampled on the centreline: both banks see one width. */
    double channelWidthScale(double x,double z){return 1+.38*Math.tanh(2.2*fractal(x,z,260,3,1013));}
    double channelShift(double x,double z){return Math.tanh(3*fractal(x,z,340,3,1051));}

    /** Independent continuous field for asymmetric cut banks and depositional shoulders. */
    double bankForm(double x,double z){return Math.tanh(2*fractal(x,z,190,3,1031));}

    // Broad regional designation only; climate and biomes remain deferred.
    boolean closedBasinRegion(int x, int z) {
        return fractal(x, z, 420000, 3, 809) > 0.12;
    }

    public record SpawnPoint(int x, int z) {}

    /** Find dry, gently sloping lowland without allocating candidate chunks. */
    public SpawnPoint spawnPoint() {
        int bestX = 0, bestZ = 0, bestHeight = Integer.MIN_VALUE;
        for (int ring = 0; ring <= 240; ring++) {
            for (int dz = -ring; dz <= ring; dz++) for (int dx = -ring; dx <= ring; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                int x = dx * 8192, z = dz * 8192;
                int h = height(x, z);
                if (h > bestHeight) { bestHeight = h; bestX = x; bestZ = z; }
                if (h < SEA_LEVEL + 4 || h > SEA_LEVEL + 250) continue;
                if (Math.abs(h - height(x + 16, z)) <= 4 && Math.abs(h - height(x, z + 16)) <= 4)
                    return new SpawnPoint(x, z);
            }
        }
        return new SpawnPoint(bestX, bestZ);
    }

    private double fractal(double x, double z, double scale, int octaves, long salt) {
        double sum = 0, weight = 1, total = 0;
        for (int i = 0; i < octaves; i++) {
            sum += weight * noise(x / scale, z / scale, salt + i * 31L);
            total += weight;
            weight *= 0.5;
            scale *= 0.5;
        }
        return sum / total;
    }

    private double noise(double x, double z, long salt) {
        // Seeded fractional phases avoid forcing every octave through zero at
        // the world origin, and avoid a seed-independent landscape there.
        x += (mix(seed ^ salt) >>> 12) * 0x1.0p-40;
        z += (mix(seed ^ salt ^ 0xD1B54A32D192ED03L) >>> 12) * 0x1.0p-40;
        int ix = (int)Math.floor(x), iz = (int)Math.floor(z);
        double dx = x - ix, dz = z - iz;
        double u = fade(dx), v = fade(dz);
        return 1.4142135623730951 * lerp(v,
            lerp(u, gradient(ix, iz, dx, dz, salt), gradient(ix + 1, iz, dx - 1, dz, salt)),
            lerp(u, gradient(ix, iz + 1, dx, dz - 1, salt), gradient(ix + 1, iz + 1, dx - 1, dz - 1, salt)));
    }

    private double gradient(int x, int z, double dx, double dz, long salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = mix(h);
        return switch ((int)h & 7) {
            case 0 -> dx;
            case 1 -> -dx;
            case 2 -> dz;
            case 3 -> -dz;
            case 4 -> (dx + dz) * 0.7071067811865476;
            case 5 -> (dx - dz) * 0.7071067811865476;
            case 6 -> (-dx + dz) * 0.7071067811865476;
            default -> (-dx - dz) * 0.7071067811865476;
        };
    }

    private static long mix(long h) {
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double smooth(double a, double b, double value) {
        double t = Math.clamp((value - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
