package blockbranch.realistic;

/** No native bootstrap: version contracts and exact preservation of geometry/old ownership. */
public final class SurfaceOwnershipCheck {
    public static void main(String[] args) {
        require(SurfaceOwnershipSchema.schema(257, 1) == 258 && SurfaceOwnershipSchema.baseSchema(258, 1) == 257, "Complete foundation");
        for (int saved : new int[]{82, 141, 255, 256, 257}) require(SurfaceOwnershipSchema.baseSchema(saved, 0) == saved, "Legacy passthrough");
        require(!SurfaceOwnershipSchema.matches(258, 0) && !SurfaceOwnershipSchema.matches(257, 1) && !SurfaceOwnershipSchema.matches(259, 1), "Mismatched/future definitions reject");
        for (int base : new int[]{82, 255, 256, 258}) {
            try { SurfaceOwnershipSchema.schema(base, 1); throw new AssertionError("Incomplete foundation"); }
            catch (IllegalArgumentException expected) {}
        }
        int samples = 0, addedMarine = 0;
        for (long seed : new long[]{424242, 1, -17}) {
            var shape = new TerrainShape(seed);
            var old = new DrainageNetwork(shape, seed, true, true, true);
            var now = new DrainageNetwork(shape, seed, true, true, true, true);
            var climate = new ClimateField(seed);
            for (int z = -1904000; z < 1920000; z += 32000) for (int x = -1904000; x < 1920000; x += 32000) {
                var a = old.sample(x, z); var b = now.sample(x, z);
                require(a.equals(b), "Surface/region/water geometry unchanged");
                require(old.channelAt(x, z).equals(now.channelAt(x, z)), "Routes unchanged");
                require(old.oceanWater(x, z, a) == now.oceanWater(x, z, b), "Structural ocean contract unchanged");
                require(old.habitatOceanWater(x, z, a) == old.oceanWater(x, z, a), "Gate disabled retains ownership");
                if (now.habitatOceanWater(x, z, b) && !old.oceanWater(x, z, a)) {
                    require(!b.closed() && b.waterLevel() == 64 && b.height() < 63 && shape.height(x, z) < 63, "Added marine follows actual open-basin water");
                    require(!now.terminalLakeWater(x, z, b), "Never promote a terminal lake"); addedMarine++;
                }
                if (b.closed() || b.waterLevel() != 64) require(!now.habitatOceanWater(x, z, b), "Closed/inland datums excluded");
                if (ColdRockland.eligible(x, z, now, climate)) {
                    require(climate.sample(x, z, now::sample).meanTemperatureC() < -6 && b.waterLevel() == DrainageNetwork.DRY, "Thermal, not elevation-only domain");
                    int depth = ColdRockland.debrisDepth(seed, x, z, now);
                    require(depth >= 0 && depth <= 2, "Supported debris bound");
                    if (SlopeCorrection.slope(now, x, z) > 1.2) require(depth == 0, "Cliffs retain stone");
                }
                samples++;
            }
        }
        require(addedMarine >= 99, "All audited unowned sea-water cases are admitted");
        System.out.println("SURFACE_OWNERSHIP_SCHEMA_GEOMETRY_LEGACY_CONTRACT_PASS samples=" + samples + " addedMarine=" + addedMarine);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private SurfaceOwnershipCheck() {}
}
