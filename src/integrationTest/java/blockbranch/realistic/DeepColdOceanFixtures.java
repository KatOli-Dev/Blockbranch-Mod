package blockbranch.realistic;

import static blockbranch.realistic.DeepColdOcean.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class DeepColdOceanFixtures {
    static DeepOcean.Point fixture(DrainageNetwork d, long seed) {
        var c = new ClimateField(seed);
        for (int i = 0; i < d.size(); i++) {
            int x = d.nodeX(i), z = d.nodeZ(i);
            if (Math.abs(x) > 1700000 || Math.abs(z) > 1700000 || !contains(x, z, d, c)) continue;
            var s = DeepOcean.site(d, x, z);
            if (s.depth() < 2000 || s.depth() > 6500 || s.slope() > .04) continue;
            int cover = DeepOcean.cover(seed, x, z, true, s);
            for (int[] step : new int[][] {{32,0},{-32,0},{0,32},{0,-32},{64,0},{-64,0},{0,64},{0,-64}}) {
                int wx = x + step[0], wz = z + step[1];
                if (!contains(wx, wz, d, c)) continue;
                int other = DeepOcean.cover(seed, wx, wz, true, DeepOcean.site(d, wx, wz));
                if (other >= 5 && other <= 6 && other != cover) return new DeepOcean.Point(x, z);
            }
        }
        throw new AssertionError("No natural lukewarm deep sediment mosaic seed=" + seed);
    }
    private DeepColdOceanFixtures() {}
}
