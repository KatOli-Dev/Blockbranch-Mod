package blockbranch.realistic;

import java.util.LinkedHashMap;
import java.util.function.BiPredicate;

/** Regional seasonally dry broadleaf forest analog. Only the selector's existing
 * warm-woodland owner can split; climate alone never converts savanna to forest.
 * The precipitation envelope is deliberately narrow: dry forests wetter than
 * 1600 mm/year (e.g. Santa Rosa, doi:10.1002/ecs2.3824) remain unrepresented.
 * Neither terrain retention nor drainage is interpreted as soil fertility.
 * Static growing-season canopy/litter does not simulate deciduous phenology. */
public final class DryForest {
    public static final String KEY="dry_forest";
    public static final int FAMILY=18;
    public static final int FLAG=4;
    /** A bind owns immutable seed/fields and bounded positive/negative caches.
     * Repeated vertical/material queries must not recompute twelve months.
     * Separate physical and quart keys retain exact block clipping at edges. */
    static BiPredicate<Integer,Integer> domain(long seed,DrainageNetwork d,ClimateField c,BiPredicate<Integer,Integer> selectedQuart){
        var blocks=new LinkedHashMap<Long,Boolean>(256,.75f,true);
        var quarts=new LinkedHashMap<Long,Boolean>(256,.75f,true);
        return (x,z)->{
            long key=(x&0xffffffffL)|((long)z<<32);
            synchronized(blocks){var value=blocks.get(key);if(value!=null)return value;}
            int qx=x>>2,qz=z>>2;long qkey=(qx&0xffffffffL)|((long)qz<<32);Boolean selected;
            synchronized(quarts){selected=quarts.get(qkey);}
            if(selected==null){selected=selectedQuart.test(qx,qz);synchronized(quarts){quarts.put(qkey,selected);if(quarts.size()>2048)quarts.remove(quarts.keySet().iterator().next());}}
            boolean value=selected&&eligible(seed,x,z,d,c);
            synchronized(blocks){blocks.put(key,value);if(blocks.size()>8192)blocks.remove(blocks.keySet().iterator().next());}
            return value;
        };
    }
    static boolean eligible(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var climate=c.sample(x,z,d::sample);
        return climate.meanTemperatureC()>23&&climate.meanTemperatureC()<=30
            &&WarmWoodland.surfaceContains(seed,x,z,d,c)&&DrySeasonPolicy.potential(x,z,d,c).regionalEnvelope();
    }
    static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){return cover(seed,x,z,site,clear,false);}
    /** With patchy litter the same gap rolls are kept; only the former uniform 86% litter roll becomes irregular. */
    static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear,boolean patchy){
        if(!clear)return 8;
        double roll=(OakBirchWoodland.mix(seed^0x445259464F524553L^x*1297L^z*1237L)>>>11)*0x1.0p-53;
        // Existing rooted-soil/litter roles; grasses and ferns remain rare,
        // rather than applying the old open oak/pine floor beneath closed crowns.
        if(!site.shaded()&&roll<.025)return 11;
        if(site.shaded()&&site.wetness()>.55&&roll<.008)return 10;
        return (patchy?LeafLitter.present(seed,x,z,site):roll<.86)?9:8;
    }
    private DryForest(){}
}
