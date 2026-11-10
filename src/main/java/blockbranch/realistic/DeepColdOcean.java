package blockbranch.realistic;

/** Surface-climate identity above an unchanged aphotic basin; no warm-deep-water inference. */
public final class DeepColdOcean {
    public static boolean contains(int x, int z, DrainageNetwork d, ClimateField c) {
        x = Math.floorDiv(x, 4) * 4; z = Math.floorDiv(z, 4) * 4;
        return DeepOcean.contains(x, z, d) && ColdOcean.climate(c.sample(x, z, d::sample)) && SeaIcePolicy.potential(z,c.sample(x,z,d::sample).meanTemperatureC()).freezingMarginC()<=0;
    }



    static MarineShelf.Transition shelfTransition(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);
        for(int i=0;i<d.size()-DrainageNetwork.SIDE;i++)for(int step:new int[]{1,DrainageNetwork.SIDE}){
            if(step==1&&i%DrainageNetwork.SIDE==DrainageNetwork.SIDE-1)continue;
            if((d.nodeGround(i)<TerrainShape.SEA_LEVEL-201)==(d.nodeGround(i+step)<TerrainShape.SEA_LEVEL-201))continue;
            int ax=Math.floorDiv(d.nodeX(i),4)*4,az=Math.floorDiv(d.nodeZ(i),4)*4,bx=Math.floorDiv(d.nodeX(i+step),4)*4,bz=Math.floorDiv(d.nodeZ(i+step),4)*4;
            boolean shallow=MarineShelf.contains(ax,az,d);
            if(shallow==MarineShelf.contains(bx,bz,d))continue;
            while(Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4){
                int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;
                if(MarineShelf.contains(mx,mz,d)==shallow){ax=mx;az=mz;}else{bx=mx;bz=mz;}
            }
            if(ColdOcean.contains(seed,ax,az,d,c)&&contains(bx,bz,d,c))return new MarineShelf.Transition(ax,az,bx,bz);
            if(ColdOcean.contains(seed,bx,bz,d,c)&&contains(ax,az,d,c))return new MarineShelf.Transition(bx,bz,ax,az);
        }throw new AssertionError("No natural cold shelf/deep boundary seed="+seed);
    }

    static MarineShelf.Transition thermalTransition(DrainageNetwork d, long seed, boolean upper) {
        var c = new ClimateField(seed); double threshold = 8;
        for (int i=0;i<d.size()-DrainageNetwork.SIDE;i++) {
            if (Math.abs(d.nodeX(i))>1700000 || Math.abs(d.nodeZ(i))>1700000) continue;
            for (int step : new int[] {1,DrainageNetwork.SIDE}) {
                if (step==1 && i%DrainageNetwork.SIDE==DrainageNetwork.SIDE-1) continue;
                int j=i+step, ax=Math.floorDiv(d.nodeX(i),4)*4, az=Math.floorDiv(d.nodeZ(i),4)*4, bx=Math.floorDiv(d.nodeX(j),4)*4, bz=Math.floorDiv(d.nodeZ(j),4)*4;
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

    private DeepColdOcean() {}
}
