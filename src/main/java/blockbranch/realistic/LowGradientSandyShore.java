package blockbranch.realistic;

import java.util.function.BiFunction;

/** A static beach/backshore adjoining an actual ocean crossing and gently shelving seabed. */
public final class LowGradientSandyShore {
    public record Site(int height,int water,boolean oceanCatchment,boolean oceanWater,boolean river,boolean sandyBackshore) {
        public Site(int height,int water,boolean catchment,boolean ocean,boolean river){this(height,water,catchment,ocean,river,true);}
        public int elevation() { return height+1-TerrainShape.SEA_LEVEL; }
        public boolean dry() { return water==DrainageNetwork.DRY; }
    }
    static Site site(DrainageNetwork drainage,int x,int z) {
        var sample=drainage.sample(x,z);
        int elevation=sample.height()+1-TerrainShape.SEA_LEVEL;
        if(elevation<-16 || elevation>4 || sample.closed() || (sample.waterLevel()!=DrainageNetwork.DRY && sample.waterLevel()!=TerrainShape.SEA_LEVEL))
            return new Site(sample.height(),sample.waterLevel(),!sample.closed(),false,false);
        return new Site(sample.height(),sample.waterLevel(),!sample.closed(),drainage.oceanWater(x,z,sample),drainage.channelAt(x,z).edge()>=0,sample.sandyBackshore());
    }
    public static boolean eligible(Site site) {
        return (!site.dry()||site.sandyBackshore()) && site.oceanCatchment() && !site.river() && site.elevation()>=-8 && site.elevation()<=4
            && (site.dry() || site.oceanWater());
    }
    public static boolean contains(int x,int z,BiFunction<Integer,Integer,Site> terrain) {
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,terrain);
    }
    /** 1=bare beach/shelf sand; 2=grass and root-bound upper sand on the dry backshore. */
    public static int cover(long seed,int x,int z,boolean habitat,Site site) {
        if(!habitat || !eligible(site)) return 0;
        if(!site.dry() || site.elevation()<0) return 1;
        if(SurfacePatches.sample(seed ^ 0x53484F5245504C54L,x,z,18)>.37) return 1;
        return (mix(seed ^ x*0x165667B19E3779F9L ^ z*0xD1B54A32D192ED03L)&3)==0 ? 2 : 1;
    }
    public record ShorePoint(int x,int z) {}

    private static long mix(long value) {
        value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;
        value=(value^(value>>>27))*0x94D049BB133111EBL;
        return value^(value>>>31);
    }
    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,BiFunction<Integer,Integer,Site> terrain) {
        var center=terrain.apply(x,z);
        if(!eligible(center)) return false;
        double dx=(terrain.apply(x+32,z).height()-terrain.apply(x-32,z).height())/64.0;
        double dz=(terrain.apply(x,z+32).height()-terrain.apply(x,z-32).height())/64.0;
        if(Math.hypot(dx,dz)>0.06) return false;
        boolean wet=false,dry=false,shelf=false;
        for(int oz=-1;oz<=1;oz++) for(int ox=-1;ox<=1;ox++) {
            if(ox==0 && oz==0) continue;
            var neighbor=terrain.apply(x+ox*128,z+oz*128);
            if(neighbor.oceanWater() && !neighbor.river()) {
                wet=true;
                var offshore=terrain.apply(x+ox*256,z+oz*256);
                if(offshore.oceanWater() && offshore.elevation()>=-16) shelf=true;
            }
            if(neighbor.dry() && neighbor.oceanCatchment() && neighbor.elevation()>=0 && !neighbor.river()) dry=true;
        }
        return wet && dry && shelf;
    }
    private LowGradientSandyShore() {}
}
