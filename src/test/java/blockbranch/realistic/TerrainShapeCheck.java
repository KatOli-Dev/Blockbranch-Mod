package blockbranch.realistic;

/** Dependency-free checks for the terrain field, including chunk and coordinate edges. */
public final class TerrainShapeCheck {
    public static void main(String[] args) {
        for (long seed : new long[]{0, 1, -17, 424242, Long.MIN_VALUE}) {
            var shape = new TerrainShape(seed);
            var repeated = new TerrainShape(seed);
            var other = new TerrainShape(seed + 1);
            int ocean = 0, mountain = 0, changed = 0, samples = 0, deep = 0, coastChanges = 0;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            long totalDepth = 0;
            for (int z = -1900000; z <= 1900000; z += 15000) for (int x = -1900000; x <= 1900000; x += 15000) {
                int height = shape.height(x, z);
                require(height == repeated.height(x, z), "Determinism");
                require(height >= TerrainShape.FLOOR + 32 && height < TerrainShape.CEILING, "Terrain envelope");
                require(Math.abs(height - shape.height(x + 1, z)) <= 32, "X continuity");
                require(Math.abs(height - shape.height(x, z - 1)) <= 32, "Z continuity");
                int elevation = height + 1 - TerrainShape.SEA_LEVEL;
                min = Math.min(min, elevation); max = Math.max(max, elevation);
                if (elevation < 0) { ocean++; totalDepth -= elevation; if (elevation < -200) deep++; }
                if (elevation > 3000) mountain++;
                if ((elevation < 0) != (shape.height(x + 10000, z) + 1 < TerrainShape.SEA_LEVEL)) coastChanges++;
                if (height != other.height(x, z)) changed++;
                samples++;
            }
            double meanDepth = totalDepth / (double)ocean;
            require(ocean > samples * 0.60 && ocean < samples * 0.85, "Ocean coverage at meter scale");
            require(meanDepth > 3000 && meanDepth < 4300, "Mean ocean depth: " + meanDepth);
            require(deep > ocean * 0.85, "Most ocean deeper than 200 m");
            require(min < -8000 && min > -11500, "Occasional deep trenches");
            require(max > 6500 && max < 9000, "High mountain elevations");
            require(mountain > samples / 200 && mountain < samples / 10, "Regional mountain coverage");
            require(coastChanges < samples / 12, "Continental horizontal scale");
            require(changed > samples * 9 / 10, "Seed sensitivity");
            require(shape.height(0, 0) != other.height(0, 0), "Seed-dependent world origin");
            var spawn = shape.spawnPoint();
            require(shape.height(spawn.x(), spawn.z()) >= TerrainShape.SEA_LEVEL + 4, "Dry spawn");
            require(shape.height(spawn.x(), spawn.z()) <= TerrainShape.SEA_LEVEL + 250, "Lowland spawn");
            require(spawn.equals(repeated.spawnPoint()), "Stable spawn search");
            // Includes both sides of chunk seams, zero, and packing boundaries.
            for (int x : new int[]{-2097152, -2097137, -17, -16, -1, 0, 15, 16, 2097150})
                for (int z : new int[]{-2097152, -17, -1, 0, 15, 2097150}) {
                    int h = shape.height(x, z);
                    require(Math.abs(h - shape.height(x + 1, z)) <= 32, "Boundary continuity");
                    require(h == repeated.height(x, z), "Boundary determinism");
                }
            System.out.printf("Terrain seed=%d samples=%d ocean=%.1f%% meanDepth=%.0fm deep=%.1f%% elevations=[%d,%d]m mountains=%d%n",
                seed, samples, ocean * 100.0 / samples, meanDepth, deep * 100.0 / ocean, min, max, mountain);
        }
        System.out.println("REALISTIC_SHAPE_PASS");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
