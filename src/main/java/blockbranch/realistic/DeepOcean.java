package blockbranch.realistic;

/** Confirmed deep marine water and thin basin sediment; no bathymetry, circulation or biological simulation. */
public final class DeepOcean {
    public record Site(int height,int water,boolean ocean,boolean lake,boolean river,double slope){
        public int depth(){return water==DrainageNetwork.DRY?0:water-height-1;}
    }
    static Site site(DrainageNetwork d,int x,int z){
        var s=d.sample(x,z);boolean ocean=d.oceanWater(x,z,s);
        if(!ocean||s.waterLevel()-s.height()-1<=200)return new Site(s.height(),s.waterLevel(),ocean,d.terminalLakeWater(x,z,s),false,0);
        double dx=(d.sample(x+32,z).height()-d.sample(x-32,z).height())/64.0,dz=(d.sample(x,z+32).height()-d.sample(x,z-32).height())/64.0;
        return new Site(s.height(),s.waterLevel(),true,false,false,Math.hypot(dx,dz));
    }
    public static boolean eligible(Site s){return s.ocean()&&!s.lake()&&!s.river()&&s.water()==TerrainShape.SEA_LEVEL&&s.depth()>200;}
    public static boolean contains(int x,int z,DrainageNetwork d){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,d);
    }
    /** Existing aquatic codes only: 5 fines,6 clay,8 coarse gravel,9 exposed original rock. */
    public static int cover(long seed,int x,int z,boolean habitat,Site s){
        if(!habitat||!eligible(s))return 0;
        if(s.slope()>.2)return 9;
        if(s.slope()>.08)return 8;
        return materialPatch(seed,x,z)>.62?6:5;
    }
    static double materialPatch(long seed,int x,int z){return SurfacePatches.sample(seed^0x444545504245444CL,x,z,192);}
    public record Point(int x,int z){}


    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,DrainageNetwork d){
        var s=d.sample(x,z);
        return s.waterLevel()==TerrainShape.SEA_LEVEL&&s.waterLevel()-s.height()-1>200&&d.oceanWater(x,z,s)&&!d.terminalLakeWater(x,z,s);
    }
    private DeepOcean(){}
}
