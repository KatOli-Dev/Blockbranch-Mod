package blockbranch.realistic;

import java.util.function.BiFunction;

/** Coarse shallow flanks and low banks of moderately graded inland river reaches. */
public final class GravelRiverMargin {
    public record Site(int height,int water,DrainageNetwork.Channel channel) {}
    static Site site(DrainageNetwork drainage,int x,int z) {
        var terrain=drainage.sample(x,z);
        return new Site(terrain.height(),terrain.waterLevel(),drainage.channelAt(x,z));
    }
    public static boolean eligible(Site site) {
        var c=site.channel();
        if(c.edge()<0 || c.waterLevel()<TerrainShape.SEA_LEVEL+8 || c.waterLevel()>TerrainShape.SEA_LEVEL+1500
            || c.halfWidth()<8 || c.halfWidth()>96 || c.gradient()<0.0005 || c.gradient()>0.02) return false;
        double lateral=c.distance()/c.halfWidth();
        int above=site.height()+1-c.waterLevel();
        return lateral>=0.5 && lateral<=1.3 && above>=-4 && above<=3
            && (site.water()==DrainageNetwork.DRY || site.water()==c.waterLevel());
    }
    public static boolean contains(int x,int z,BiFunction<Integer,Integer,Site> terrain) {
        return eligible(terrain.apply(Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4));
    }
    static boolean surfaceContains(int x,int z,BiFunction<Integer,Integer,Site> terrain){return eligible(terrain.apply(x,z));}
    /** 1=interlocked lower shingle; 2=loose gravel; 3=soil pocket; 4=soil with pioneer grass. */
    public static int cover(long seed,int x,int z,boolean habitat,Site site) {
        if(!habitat || !eligible(site)) return 0;
        int above=site.height()+1-site.channel().waterLevel();
        if(above<=0) return 1;
        if(above<=1) return 2;
        if(SurfacePatches.sample(seed ^ 0x5348494E474C45L,x,z,12)>.44) return 2;
        return ((mix(seed ^ x*0x165667B19E3779F9L ^ z*0xD1B54A32D192ED03L)&1)==0) ? 3 : 4;
    }
    public record BankPoint(int x,int z) {}

    private static long mix(long value) {
        value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;
        value=(value^(value>>>27))*0x94D049BB133111EBL;
        return value^(value>>>31);
    }
    private GravelRiverMargin() {}
}
