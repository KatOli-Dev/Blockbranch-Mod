package blockbranch.realistic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

/** Analytic candidate-field checks; these do not claim native cave generation. */
public final class UndergroundStructureCheck {
    public static void main(String[] args) throws Exception {
        long[] seeds = {424242, 1, -17};
        long previousHash = 0;
        for (long seed : seeds) {
            var field = new UndergroundStructure(seed);
            int n = 64 * 64 * 64;
            double[] samples = new double[n];
            int voids = 0;
            long hash = 1;
            for (int i = 0; i < n; i++) {
                // Includes negative cell/chunk/section boundaries and depths
                // well below vanilla's original terrain floor.
                int x = -193 + (i & 63) * 4;
                int y = -385 + ((i >>> 6) & 63) * 4;
                int z = -193 + (i >>> 12) * 4;
                double d = field.density(x, y, z, 80);
                require(Double.isFinite(d), "Finite density");
                samples[i] = d;
                if (d < 0) voids++;
                hash = hash * 31 + Double.doubleToLongBits(d);
            }
            double fraction = voids / (double)n;
            require(fraction > .005 && fraction < .25, "Nonempty passages with predominantly solid rock: " + fraction);
            require(seed == seeds[0] || hash != previousHash, "Seed changes geometry");
            previousHash = hash;
            // A second field queried concurrently in reverse order must agree
            // bit-for-bit, so chunk scheduling cannot alter cave geometry.
            var second = new UndergroundStructure(seed);
            IntStream.range(0, n).parallel().forEach(j -> {
                int i = n - 1 - j;
                int x = -193 + (i & 63) * 4, y = -385 + ((i >>> 6) & 63) * 4, z = -193 + (i >>> 12) * 4;
                require(Double.doubleToLongBits(samples[i]) == Double.doubleToLongBits(second.density(x, y, z, 80)), "Order/thread independence");
            });
            for (int surface : new int[]{-9000, -6000, 63, 4000, 9000}) {
                for (int x = -33; x <= 33; x++) for (int z = -17; z <= 17; z++) {
                    for (int d = -1; d <= UndergroundStructure.ROOF_THICKNESS; d++)
                        require(!field.voidAt(x, surface - d, z, surface), "Protected surface/seabed roof");
                    require(!field.voidAt(x, TerrainShape.FLOOR + UndergroundStructure.FLOOR_THICKNESS, z, surface), "Protected basal rock");
                    require(!field.voidAt(x, TerrainShape.FLOOR - 1, z, surface), "No out-of-envelope void");
                }
            }
            int openPairs = 0, open = 0;
            for (int z = -128; z < 128; z++) for (int x = -128; x < 128; x++) {
                if (field.voidAt(x, -256, z, 80)) {
                    open++;
                    if (field.voidAt(x + 1, -256, z, 80) || field.voidAt(x, -256, z + 1, 80)) openPairs++;
                }
            }
            require(open > 0 && openPairs > open * .9, "Voids form passages rather than isolated one-block noise");
            System.out.printf("UNDERGROUND_FIELD seed=%d voidFraction=%.6f connectedPlanePairs=%d/%d hash=%016x%n", seed, fraction, openPairs, open, hash);
        }
        // Explicit deep/negative chunk-seam sample export for developer review.
        var field = new UndergroundStructure(424242);
        var svg = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 512 256\"><title>Analytic cave candidate cross-section; not an in-game capture</title><rect width=\"512\" height=\"256\" fill=\"#55514b\"/>");
        for (int y = 0; y < 256; y++) for (int x = 0; x < 512; x++)
            if (field.voidAt(x - 256, -y - 64, -17, 80)) svg.append("<path d=\"M").append(x).append(' ').append(y).append("h1v1h-1z\" fill=\"#151718\"/>");
        svg.append("</svg>");
        Path out = Path.of("build/underground-structure/cross-section.svg");
        Files.createDirectories(out.getParent()); Files.writeString(out, svg);
        System.out.println("UNDERGROUND_STRUCTURE_ANALYTIC_PASS geometry field; native coverage is separate");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
