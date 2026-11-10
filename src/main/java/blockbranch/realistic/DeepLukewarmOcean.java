package blockbranch.realistic;

/** Surface-climate identity above an unchanged aphotic basin; no warm-deep-water inference. */
public final class DeepLukewarmOcean {
    public static boolean contains(int x, int z, DrainageNetwork d, ClimateField c) {
        x = Math.floorDiv(x, 4) * 4; z = Math.floorDiv(z, 4) * 4;
        return DeepOcean.contains(x, z, d) && LukewarmOcean.climate(c.sample(x, z, d::sample));
    }





    static MarineShelf.Transition thermalTransition(DrainageNetwork d, long seed, boolean upper) {
        var c = new ClimateField(seed); double threshold = upper ? 24 : 18;
        for (int i=0;i<d.size()-DrainageNetwork.SIDE;i++) {
            if (Math.abs(d.nodeX(i))>1700000 || Math.abs(d.nodeZ(i))>1700000) continue;
            for (int step : new int[] {1,DrainageNetwork.SIDE}) {
                if (step==1 && i%DrainageNetwork.SIDE==DrainageNetwork.SIDE-1) continue;
                int j=i+step, ax=d.nodeX(i), az=d.nodeZ(i), bx=d.nodeX(j), bz=d.nodeZ(j);
                if (!DeepOcean.contains(ax,az,d) || !DeepOcean.contains(bx,bz,d)) continue;
                boolean low=c.sample(ax,az,d::sample).meanTemperatureC()<threshold;
                if (low==(c.sample(bx,bz,d::sample).meanTemperatureC()<threshold)) continue;
                while (Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4) {
                    int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4, mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;
                    if ((c.sample(mx,mz,d::sample).meanTemperatureC()<threshold)==low) { ax=mx; az=mz; }
                    else { bx=mx; bz=mz; }
                }
                if (!DeepOcean.contains(ax,az,d) || !DeepOcean.contains(bx,bz,d)) continue;
                if (contains(ax,az,d,c) && !contains(bx,bz,d,c)) return new MarineShelf.Transition(ax,az,bx,bz);
                if (contains(bx,bz,d,c) && !contains(ax,az,d,c)) return new MarineShelf.Transition(bx,bz,ax,az);
            }
        }
        throw new AssertionError("No natural deep marine climate boundary "+threshold+" seed="+seed);
    }

    private DeepLukewarmOcean() {}
}
