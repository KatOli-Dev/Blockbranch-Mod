package blockbranch.realistic;

/** Natural seed-424242 review witnesses. These constants select cameras, never terrain. */
public final class CoastalBeachScenes {
    public record Scene(int x, int z, double seaX, double seaZ, boolean snow) {}
    public record Transect(double x, double z, double width) {}
    public record View(double x, double y, double z, double yaw, double pitch) {
        public double[] coordinates() { return new double[] {x, y, z, yaw, pitch}; }
    }

    /** Fitted outward normals come from actual ocean-water boundary probes. */
    public static Scene review(long seed, boolean snow) {
        if (seed != 424242) return null;
        return snow
            ? new Scene(1552700, -1970060, .32234624304111836, .9466218355802261, true)
            : new Scene(281724, -520572, -.9952479936977335, .09737264010303939, false);
    }

    private static final java.util.Map<DrainageNetwork,Scene[]> NATURAL=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    /** Additional-seed review uses the first natural biome witness, not a scenic search. */
    public static Scene forWorld(long seed,boolean snow,DrainageNetwork d,ClimateField c){
        var fixed=review(seed,snow);if(fixed!=null)return fixed;
        synchronized(NATURAL){var pair=NATURAL.computeIfAbsent(d,unused->new Scene[2]);int i=snow?1:0;
            if(pair[i]==null){var p=blockbranch.realistic.CoastalBeachFixtures.fixture(d,seed,snow);var shape=new TerrainShape(seed);
                double dx=shape.continuousHeight(p.x()+32,p.z())-shape.continuousHeight(p.x()-32,p.z()),dz=shape.continuousHeight(p.x(),p.z()+32)-shape.continuousHeight(p.x(),p.z()-32),length=Math.hypot(dx,dz);
                if(length<1e-10)throw new AssertionError("Ambiguous natural beach normal");
                pair[i]=new Scene(p.x(),p.z(),-dx/length,-dz/length,snow);
            }return pair[i];
        }
    }

    static Transect transect(Scene scene, DrainageNetwork d, ClimateField c, double along) {
        double x = scene.x() - scene.seaZ() * along, z = scene.z() + scene.seaX() * along;
        double lo = -256, hi = 256;
        if (ocean(d, x + scene.seaX() * lo, z + scene.seaZ() * lo)
                || !ocean(d, x + scene.seaX() * hi, z + scene.seaZ() * hi))
            throw new AssertionError("Review transect must cross an actual ocean shore");
        for (int n = 0; n < 14; n++) {
            double mid = (lo + hi) / 2;
            if (ocean(d, x + scene.seaX() * mid, z + scene.seaZ() * mid)) hi = mid;
            else lo = mid;
        }
        x += scene.seaX() * hi; z += scene.seaZ() * hi;
        int width = 0;
        for (int distance = 0; distance <= 256; distance += 4) {
            int wx = (int)Math.round(x - scene.seaX() * distance), wz = (int)Math.round(z - scene.seaZ() * distance);
            if (CoastalBeach.surfaceContains(wx, wz, d, c, scene.snow())) width = distance;
            else if (width > 0) break;
        }
        if (width < 48 || width >= 240) throw new AssertionError("Bounded natural beach/backshore transition required");
        return new Transect(x, z, width);
    }

    public static View camera(Scene scene, DrainageNetwork d, ClimateField c, boolean elevated) {
        var t = transect(scene, d, c, elevated ? 0 : -36);
        double nx = scene.seaX(), nz = scene.seaZ(), x, z, y, tx, tz, pitch;
        if (elevated) {
            // Stand over the sea and look inland: all three bands are in view.
            x = t.x() + nx * 24 + nz * 48;
            z = t.z() + nz * 24 - nx * 48;
            y = TerrainShape.SEA_LEVEL + 44;
            tx = t.x() - nx * t.width() * .22 - nz * 28;
            tz = t.z() - nz * t.width() * .22 + nx * 28;
            pitch = Math.toDegrees(Math.atan2(y + 1.62 - TerrainShape.SEA_LEVEL, Math.hypot(tx - x, tz - z)));
        } else {
            // Dry backshore, looking mostly along the coast. The inland edge
            // stays beside the camera rather than behind an offshore viewpoint.
            x = Math.floor(t.x() - nx * (t.width() - 16)) + .5;
            z = Math.floor(t.z() - nz * (t.width() - 16)) + .5;
            int wx = (int)Math.floor(x), wz = (int)Math.floor(z);
            if (d.sample(wx, wz).waterLevel() != DrainageNetwork.DRY
                    || !CoastalBeach.surfaceContains(wx, wz, d, c, scene.snow()))
                throw new AssertionError("Review ground camera must stand on its natural dry beach");
            y = d.sample(wx, wz).height() + 1;
            tx = t.x() - nx * (t.width() - 48) - nz * 96;
            tz = t.z() - nz * (t.width() - 48) + nx * 96;
            pitch = 8;
        }
        return new View(x, y, z, Math.toDegrees(Math.atan2(-(tx - x), tz - z)), pitch);
    }

    static boolean ocean(DrainageNetwork d, double x, double z) {
        int wx = (int)Math.round(x), wz = (int)Math.round(z);
        return d.oceanWater(wx, wz, d.sample(wx, wz));
    }

    private CoastalBeachScenes() {}
}
