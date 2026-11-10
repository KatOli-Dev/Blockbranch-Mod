package blockbranch.realistic;

import java.util.function.BiFunction;

/** World-coordinate scree habitat and debris cover; no chunk-local random state. */
public final class AlpineScree {
    public static final int MIN_ELEVATION = 2200, MAX_ELEVATION = 4200;
    public static final double MIN_SLOPE = 0.30, MAX_SLOPE = 1.20;

    /** A 64 m relief measure selects debris slopes, excluding flats and near-vertical cliffs.
     * Elevation is an initial treeline proxy, not a latitude or climate simulation.
     * Biome identity uses the 4 m palette grid; surfaceContains evaluates debris per block.
     */
    public static boolean contains(int x, int z, BiFunction<Integer, Integer, DrainageNetwork.Sample> terrain) {
        return surfaceContains(Math.floorDiv(x, 4) * 4, Math.floorDiv(z, 4) * 4, terrain);
    }

    /** Debris follows the actual block's slope; the biome palette remains quart-sized. */
    static boolean surfaceContains(int x, int z, BiFunction<Integer, Integer, DrainageNetwork.Sample> terrain) {
        var center = terrain.apply(x, z);
        int elevation = center.height() + 1 - TerrainShape.SEA_LEVEL;
        if (center.waterLevel() > center.height() || elevation < MIN_ELEVATION || elevation > MAX_ELEVATION) return false;
        double dx = (terrain.apply(x + 32, z).height() - terrain.apply(x - 32, z).height()) / 64.0;
        double dz = (terrain.apply(x, z + 32).height() - terrain.apply(x, z - 32).height()) / 64.0;
        double slope = Math.hypot(dx, dz);
        return slope >= MIN_SLOPE && slope <= MAX_SLOPE;
    }

    /** Metre-scale exposed bedrock ribs interrupt broad 1–3 m debris aprons. */
    public static int depth(long seed, int x, int z, boolean habitat) {
        if (!habitat) return 0;
        double patch = SurfacePatches.sample(seed ^ 0x5343524545444550L, x, z, 18);
        return patch < .28 ? 0 : patch < .46 ? 1 : patch < .62 ? 2 : 3;
    }
    private AlpineScree() {}
}
