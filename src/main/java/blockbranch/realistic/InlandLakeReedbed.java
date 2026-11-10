package blockbranch.realistic;

import java.util.function.BiFunction;

/** Shallow emergent vegetation in terminal lakes, without changing their water budget or shape. */
public final class InlandLakeReedbed {
    public record Site(int height, int water, boolean lake) {
        public int depth() { return water == DrainageNetwork.DRY ? 0 : water - height - 1; }
    }
    static Site site(DrainageNetwork drainage,int x,int z) {
        var sample=drainage.sample(x,z);
        return new Site(sample.height(),sample.waterLevel(),drainage.terminalLakeWater(x,z,sample));
    }
    public static boolean shallow(Site site) {
        return site.lake() && site.water()>TerrainShape.SEA_LEVEL && site.water()<=TerrainShape.SEA_LEVEL+600
            && site.depth()>=1 && site.depth()<=3;
    }
    public static boolean contains(int x,int z,BiFunction<Integer,Integer,Site> terrain) {
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,terrain);
    }
    /** 0=unchanged; 1=two metres of mud; 2..4=mud and that many metres of reed stems. */
    public static int cover(long seed,int x,int z,boolean habitat,Site site) {
        if(!habitat || !shallow(site)) return 0;
        var ecology=new GroundEcology.Site(.85,0,0,0,0,false);
        double colony=GroundEcology.colonyStrength(seed,0x52454544434F4CL,x,z,32,9,22,ecology);
        double stalk=(mix(seed^0x5354414C4B53414CL^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
        if(site.depth()>2 || stalk>.68*colony) return 1;
        // Static height cohorts vary independently of the elliptical stand edges.
        // Soft local cohorts and individual variation avoid level or trimmed tops.
        double growth=SurfacePatches.sample(seed^0x5245454447524F57L,x,z,4);
        double maturity=(mix(seed^0x5245454448454947L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
        return site.depth()+(maturity<Math.clamp(.15+.55*growth,.15,.7)?1:2);
    }
    public record ShorePoint(int x,int z) {}

    private static long mix(long v) {
        v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;
        v=(v^(v>>>27))*0x94D049BB133111EBL;
        return v^(v>>>31);
    }
    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,BiFunction<Integer,Integer,Site> terrain) {
        var center=terrain.apply(x,z);
        if(!shallow(center)) return false;
        double dx=(terrain.apply(x+16,z).height()-terrain.apply(x-16,z).height())/32.0;
        double dz=(terrain.apply(x,z+16).height()-terrain.apply(x,z-16).height())/32.0;
        return Math.hypot(dx,dz)<=0.25;
    }
    private InlandLakeReedbed() {}
}
