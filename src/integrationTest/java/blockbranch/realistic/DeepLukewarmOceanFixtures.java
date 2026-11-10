package blockbranch.realistic;

import static blockbranch.realistic.DeepLukewarmOcean.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class DeepLukewarmOceanFixtures {
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

    static MarineShelf.Transition shelfTransition(DrainageNetwork d, long seed) {
        var p = blockbranch.realistic.LukewarmOceanFixtures.fixture(d, seed);
        var c = new ClimateField(seed);
        for (int[] step : new int[][] {{1,0},{-1,0},{0,1},{0,-1}})
            for (int radius = 16; radius <= 65536; radius *= 2) {
                int ax = Math.floorDiv(p.x(),4)*4, az = Math.floorDiv(p.z(),4)*4;
                int bx = ax + step[0]*radius, bz = az + step[1]*radius;
                if (!contains(bx,bz,d,c)) continue;
                while (Math.max(Math.abs(ax-bx),Math.abs(az-bz)) > 4) {
                    int mx = Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4, mz = Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;
                    if (MarineShelf.eligible(MarineShelf.site(d,mx,mz))) { ax=mx; az=mz; }
                    else { bx=mx; bz=mz; }
                }
                if (LukewarmOcean.contains(seed,ax,az,d,c) && contains(bx,bz,d,c))
                    return new MarineShelf.Transition(ax,az,bx,bz);
            }
        throw new AssertionError("No natural adjacent lukewarm shelf/deep boundary seed="+seed);
    }
    private DeepLukewarmOceanFixtures() {}
}
