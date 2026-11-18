package blockbranch.realistic;

import java.util.function.BiPredicate;

/** Coastal winter-rain evergreen oak-woodland structural analog. Only an
 * existing warm-woodland owner can split. Neither low soil fertility nor fire
 * history can be inferred from the current fields; no grassland conversion,
 * pyric classifier, simulated fire regime or evergreen physiology is claimed.
 * FAO describes 15–18m oak canopies and 3–5m shrubs in its European regional
 * profile: https://www.fao.org/4/ad652e/ad652e21.htm
 * Heights/spacing below are voxel cohorts, not a fit to measured stand plots. */
public final class SummerDryWoodland {
    public static final String KEY="summer_dry_woodland";
    public static final int FLAG=8,FAMILY=19;
    static boolean eligible(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        return SummerDryClimate.coastalRegime(x,z,d)&&WarmWoodland.surfaceContains(seed,x,z,d,c)
            &&SummerDryClimate.potential(x,z,d,c).suitable();
    }
    /** Independently bounded physical/quart caches preserve exact ground
     * clipping without repeating the monthly calculation in vertical reads. */
    static BiPredicate<Integer,Integer> domain(long seed,DrainageNetwork d,ClimateField c,BiPredicate<Integer,Integer> selectedQuart){
        return SelectedDomain.of(selectedQuart,(x,z)->eligible(seed,x,z,d,c));
    }
    /** Existing rooted/litter/short-grass roles; a woody understory rather than
     * a fern carpet. The static green canopy does not implement leaf phenology. */
    static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){return cover(seed,x,z,site,clear,false);}
    /** With patchy litter the same gap rolls are kept; only the former uniform 80% litter roll becomes irregular. */
    static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear,boolean patchy){
        if(!clear)return 8;
        double roll=(OakBirchWoodland.mix(seed^0x53554D4D4552464CL^x*1297L^z*1237L)>>>11)*0x1.0p-53;
        if(!site.shaded()&&roll<.06)return 12;
        return (patchy?LeafLitter.present(seed,x,z,site):roll<.80)?9:8;
    }
    private SummerDryWoodland(){}
}
