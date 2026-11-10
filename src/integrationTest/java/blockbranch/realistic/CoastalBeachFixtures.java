package blockbranch.realistic;

import static blockbranch.realistic.CoastalBeach.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class CoastalBeachFixtures {
    /** Search real ocean crossings, then require a dry interior and wet shore in view. */
    static Point fixture(DrainageNetwork d, long seed, boolean snow) {
        var c = new ClimateField(seed);
        for (int i = 0; i < d.size() - DrainageNetwork.SIDE; i++) {
            if (Math.abs(d.nodeX(i)) > 2050000 || Math.abs(d.nodeZ(i)) > 2050000) continue;
            if (snow && Math.abs(d.nodeZ(i)) < 1700000) continue;
            for (int step : new int[] {1, DrainageNetwork.SIDE}) {
                if (step == 1 && i % DrainageNetwork.SIDE == DrainageNetwork.SIDE - 1) continue;
                int j = i + step;
                if ((d.nodeGround(i) < 63) == (d.nodeGround(j) < 63)) continue;
                int ax = d.nodeX(i), az = d.nodeZ(i), bx = d.nodeX(j), bz = d.nodeZ(j);
                boolean aw = d.oceanWater(ax, az, d.sample(ax, az));
                if (aw == d.oceanWater(bx, bz, d.sample(bx, bz))) continue;
                for (int n = 0; n < 16; n++) {
                    int mx = Math.floorDiv(ax + bx, 2), mz = Math.floorDiv(az + bz, 2);
                    if (d.oceanWater(mx, mz, d.sample(mx, mz)) == aw) { ax = mx; az = mz; }
                    else { bx = mx; bz = mz; }
                }
                int cx = aw ? bx : ax, cz = aw ? bz : az;
                for (int oz = -48; oz <= 48; oz += 8) for (int ox = -48; ox <= 48; ox += 8) {
                    int x = Math.floorDiv(cx + ox, 4) * 4, z = Math.floorDiv(cz + oz, 4) * 4;
                    if (!contains(x, z, d, c, snow)) continue;
                    boolean interior = true;
                    for (int dz = -8; dz <= 8; dz += 4) for (int dx = -8; dx <= 8; dx += 4)
                        if (!contains(x + dx, z + dz, d, c, snow)) interior = false;
                    if (interior) return new Point(x, z);
                }
            }
        }
        throw new AssertionError("No natural " + (snow ? "snowy " : "") + "beach interior seed=" + seed);
    }
    private CoastalBeachFixtures() {}
}
