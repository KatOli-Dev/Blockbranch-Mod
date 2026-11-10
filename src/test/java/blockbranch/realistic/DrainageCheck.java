package blockbranch.realistic;

import java.util.concurrent.*;

/** Graph invariants plus block-space checks independent of Minecraft APIs. */
public final class DrainageCheck {
    public static void main(String[] args) throws Exception {
        long[] seeds = args.length == 0 ? new long[]{424242, 1, -17}
            : java.util.Arrays.stream(args).mapToLong(Long::parseLong).toArray();
        for (long seed : seeds) check(seed);
        System.out.println("REALISTIC_DRAINAGE_PASS");
    }
    private static void check(long seed) throws Exception {
        long start = System.nanoTime();
        var shape = new TerrainShape(seed);
        var network = new DrainageNetwork(shape, seed);
        int[] color = new int[network.size()];
        int land = 0, closed = 0, lakes = 0, rivers = 0;
        int firstLake = -1, oceanRiver = -1, closedRiver = -1;
        // Every parent chain must terminate. DFS colors detect cycles without
        // assuming any particular ordering of the implementation's flood queue.
        for (int i = 0; i < network.size(); i++) {
            int current = i;
            while (current >= 0 && color[current] == 0) { color[current] = 1; current = network.parent(current); }
            require(current < 0 || color[current] == 2, "Drainage cycle");
            current = i;
            while (current >= 0 && color[current] == 1) { color[current] = 2; current = network.parent(current); }
            int p = network.parent(i);
            if (p >= 0) {
                land++;
                require(network.outlet(i) == network.outlet(p), "Connected outlet label");
                require(network.waterLevel(p) <= network.waterLevel(i), "Water rises downstream");
                require(network.accumulation(p) > network.accumulation(i), "Accumulation at confluences");
            } else require(network.outlet(i) == i, "Terminal label");
            if (network.closed(i)) {
                closed++;
                if (p < 0) {
                    lakes++;
                    if (firstLake < 0) firstLake = i;
                    var center = network.sample(network.nodeX(i), network.nodeZ(i));
                    require(center.closed() && center.basin() == i, "Closed terminal footprint");
                    require(center.waterLevel() == network.waterLevel(i), "Local lake level");
                    require(center.waterLevel() > center.height() + 1, "Lake has water depth");
                    require(center.waterLevel() != TerrainShape.SEA_LEVEL, "Not a global-sea-level lake");
                    require(network.sample(network.nodeX(i) + 1, network.nodeZ(i)).waterLevel() == center.waterLevel(), "Flat lake surface");
                }
            }
            if (network.river(i)) {
                rivers++;
                if (network.closed(i)) closedRiver = i; else oceanRiver = i;
            }
        }
        require(lakes > 0 && oceanRiver >= 0 && closedRiver >= 0, "Both outlet types with tributaries");
        require(closed > land / 100 && closed < land * 0.50, "Regional closed catchments");
        // Trace each representative river to its actual terminal, and inspect
        // continuous channel samples through multiple chunk and mesh boundaries.
        for (int source : new int[]{oceanRiver, closedRiver}) {
            int current = source, steps = 0;
            while (network.parent(current) >= 0) {
                if (network.river(current) && steps < 16) {
                    int previous = Integer.MAX_VALUE;
                    for (int k = 0; k <= 128; k++) {
                        double t = k / 128.0;
                        int x = (int)Math.round(network.edgeX(current, t)), z = (int)Math.round(network.edgeZ(current, t));
                        var sample = network.sample(x, z);
                        require(sample.waterLevel() != DrainageNetwork.DRY, "Continuous wet river at " + x + "," + z);
                        require(sample.waterLevel() <= previous, "Block-space downstream water profile");
                        require(sample.height() + 1 < sample.waterLevel(), "Carved channel depth");
                        require(sample.equals(network.sample(x, z)), "Stable repeated query");
                        previous = sample.waterLevel();
                    }
                }
                current = network.parent(current);
                require(++steps < network.size(), "Terminal reachability");
            }
            require(current == network.outlet(source), "River reaches its labeled outlet");
            require(network.closed(current) == network.closed(source), "Outlet type preserved");
        }
        // Probe many separate confluences and sharp bends, not just the two
        // representative long routes. This also exercises curved projections.
        for (int i = 0; i < network.size(); i += 127) if (network.river(i)) {
            int previous = Integer.MAX_VALUE;
            for (int k = 0; k <= 32; k++) {
                double t = k / 32.0;
                int x = (int)Math.round(network.edgeX(i, t)), z = (int)Math.round(network.edgeZ(i, t));
                var sample = network.sample(x, z);
                require(sample.waterLevel() != DrainageNetwork.DRY, "Wet bend at node " + i + " t=" + t + " xy=" + x + "," + z + " sample=" + sample + " levels=" + network.waterLevel(i) + "," + network.waterLevel(network.parent(i)));
                require(sample.waterLevel() <= previous, "Confluence profile at node " + i + " t=" + t);
                previous = sample.waterLevel();
            }
        }
        // Reconstructing the immutable graph must not depend on chunk/query
        // order. Parallel readers must see the same final samples.
        var repeated = new DrainageNetwork(new TerrainShape(seed), seed);
        for (int i = 0; i < network.size(); i += 997) {
            require(network.parent(i) == repeated.parent(i), "Seeded graph determinism");
            require(network.waterLevel(i) == repeated.waterLevel(i), "Seeded water determinism");
        }
        try (var pool = Executors.newFixedThreadPool(4)) {
            int lake = firstLake;
            var tasks = new java.util.ArrayList<Callable<DrainageNetwork.Sample>>();
            for (int i = 0; i < 32; i++) tasks.add(() -> network.sample(network.nodeX(lake), network.nodeZ(lake)));
            var expected = repeated.sample(network.nodeX(lake), network.nodeZ(lake));
            for (var future : pool.invokeAll(tasks)) require(future.get().equals(expected), "Concurrent deterministic sample");
        }
        for (int x : new int[]{-2097152, -17, -16, -1, 0, 15, 16, 2097151})
            for (int z : new int[]{-2097152, -1, 0, 2097151}) {
                var sample = network.sample(x, z);
                require(sample.height() > TerrainShape.FLOOR && sample.height() < TerrainShape.CEILING, "Bounds at packing edge");
                require(sample.equals(repeated.sample(x, z)), "Boundary/query-order determinism");
            }
        System.out.printf("Drainage seed=%d nodes=%d rivers=%d terminalLakes=%d closedLand=%.1f%% elapsed=%.2fs%n",
            seed, network.size(), rivers, lakes, closed * 100.0 / land, (System.nanoTime() - start) / 1e9);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
