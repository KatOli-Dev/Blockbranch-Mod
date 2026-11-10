package blockbranch.realistic;

import static blockbranch.realistic.LukewarmOcean.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class LukewarmOceanFixtures {
    static Point fixture(DrainageNetwork d, long seed) {
        var c = new ClimateField(seed);
        for (int i = 0; i < d.size() - DrainageNetwork.SIDE; i++) {
            if (Math.abs(d.nodeX(i)) > 1700000 || Math.abs(d.nodeZ(i)) > 1700000) continue;
            for (int step : new int[] {1, DrainageNetwork.SIDE}) {
                if (step == 1 && i % DrainageNetwork.SIDE == DrainageNetwork.SIDE - 1) continue;
                int j = i + step;
                if ((d.nodeGround(i) < 57) == (d.nodeGround(j) < 57)) continue;
                int ax = d.nodeX(i), az = d.nodeZ(i), bx = d.nodeX(j), bz = d.nodeZ(j);
                boolean aw = below(d, ax, az);
                if (aw == below(d, bx, bz)) continue;
                for (int n = 0; n < 16; n++) {
                    int mx = Math.floorDiv(ax + bx, 2), mz = Math.floorDiv(az + bz, 2);
                    if (below(d, mx, mz) == aw) { ax = mx; az = mz; }
                    else { bx = mx; bz = mz; }
                }
                int cx = aw ? ax : bx, cz = aw ? az : bz;
                if (!climate(c.sample(cx, cz, d::sample))) continue;
                for (int oz = -64; oz <= 64; oz += 2) for (int ox = -64; ox <= 64; ox += 2) {
                    int x = cx + ox, z = cz + oz;
                    var site = MarineShelf.site(d, x, z);
                    if (cover(seed, x, z, d, c) != 12 || site.depth() < 5
                            || MarineShelf.cover(seed, x, z, true, site, c.sample(x, z, d::sample)) != 7) continue;
                    boolean interior = true;
                    for (int dz = -8; dz <= 8; dz += 4) for (int dx = -8; dx <= 8; dx += 4)
                        if (!contains(seed, x + dx, z + dz, d, c)) interior = false;
                    if (interior) return new Point(x, z);
                }
            }
        }
        throw new AssertionError("No natural lukewarm sandy seagrass shelf seed=" + seed);
    }

    private static boolean below(DrainageNetwork d, int x, int z) {
        var s = d.sample(x, z);
        return d.oceanWater(x, z, s) && s.height() < 57;
    }
    private LukewarmOceanFixtures() {}
}
