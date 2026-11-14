package blockbranch.realistic;

/** Cold bare steep ground just below {@link AlpineScree#MIN_ELEVATION} that no established owner claimed.
 * Block materials already treat these cells as stone; only the biome label changes, from the grassland fallback to
 * {@code alpine_scree_slope}. Slope uses the same 64 m relief measure as {@link AlpineScree}. The 5 C mean-temperature
 * bound keeps warm mountain slopes (which can reach this elevation near the tropics) out of the cold label. Flats,
 * cliffs steeper than {@link AlpineScree#MAX_SLOPE}, water, and ground below {@link #MIN_ELEVATION} are never claimed. */
public final class ScreeBelt {
    public static final int MIN_ELEVATION = 1500;
    public static final double MAX_MEAN_TEMPERATURE_C = 5;

    /** Biome identity uses the 4 m palette grid, like {@link AlpineScree#contains}. */
    public static boolean contains(int x, int z, DrainageNetwork d, ClimateField c) {
        x = Math.floorDiv(x, 4) * 4; z = Math.floorDiv(z, 4) * 4;
        var center = d.sample(x, z);
        int elevation = center.height() + 1 - TerrainShape.SEA_LEVEL;
        if (center.waterLevel() > center.height() || elevation < MIN_ELEVATION || elevation >= AlpineScree.MIN_ELEVATION) return false;
        double dx = (d.sample(x + 32, z).height() - d.sample(x - 32, z).height()) / 64.0;
        double dz = (d.sample(x, z + 32).height() - d.sample(x, z - 32).height()) / 64.0;
        double slope = Math.hypot(dx, dz);
        return slope >= AlpineScree.MIN_SLOPE && slope <= AlpineScree.MAX_SLOPE
            && c.sample(x, z, d::sample).meanTemperatureC() < MAX_MEAN_TEMPERATURE_C;
    }
    private ScreeBelt() {}
}
