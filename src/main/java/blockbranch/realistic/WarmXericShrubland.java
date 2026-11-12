package blockbranch.realistic;

import java.util.function.BiPredicate;

/** Low warm-desert shrub structural analog, not a species or full functional
 * ecosystem. FAO's 100–300 mm semidesert envelope motivates the bounded model
 * rain screen (https://www.fao.org/4/T0122E/t0122e03.htm); IUCN GET T5.2
 * motivates open perennial woody cover and mild winters. Existing silver shrubs
 * stand in for low xeromorphic crowns; no spines, CAM, roots or fire simulation
 * is implied. The atmospheric rain scale and thermal cycle are the published
 * generator conventions, not an observed rainfall/PET calibration.
 */
public final class WarmXericShrubland {
    public static final String KEY="warm_xeric_shrubland";

    static boolean climateSuitable(double annualModelRain,double coldestModelMonth){
        return Double.isFinite(annualModelRain)&&Double.isFinite(coldestModelMonth)
            &&annualModelRain>=100&&annualModelRain<300&&coldestModelMonth>0;
    }
    static boolean eligible(int x,int z,DrainageNetwork d,ClimateField c){
        var climate=c.sample(x,z,d::sample);
        return climateSuitable(DrySeasonPolicy.annualModelRain(c.atmosphericSupply(x,z,d::sample)),
            CoolDesert.season(z,climate.meanTemperatureC()).coldestC())
            &&CoolDesert.physicalSupport(x,z,d,true);
    }
    /** Reuse the bounded selected-quart/physical-block intersection. Its
     * eligibility callback is deliberately this subtype's, not cool desert's. */
    static BiPredicate<Integer,Integer> domain(DrainageNetwork d,ClimateField c,BiPredicate<Integer,Integer> selected){
        return CoolDesert.selectedDomain(selected,(x,z)->eligible(x,z,d,c));
    }
    /** Existing desert roles only: 1 pavement, 2 gravel, 3 sand, 4 fine soil,
     * 5 dead stems, 6 short dry grass, 7 live <1 m shrubs. Shrubs dominate live
     * cover, unlike bunchgrass steppe; broad exposed mineral gaps separate
     * resource patches. Local retention grades patches, never rainfall or soil
     * fertility. Density/patch scales are explicit representation choices,
     * bounded by IUCN's open 10–30% vegetation structure, not a fitted biome.
     * This first low-subshrub subtype excludes tall thornscrub and succulents.
     */
    static int cover(long seed,int x,int z,GroundEcology.Site site){
        double patch=SurfacePatches.sample(seed^0x5845524943534852L,x,z,14);
        double shrubChance=Math.clamp(.18+.28*(patch-.5)+.04*(site.retention()-.5),.06,.30);
        double roll=(OakBirchWoodland.mix(seed^0x5845524943434F56L^x*1297L^z*1237L)>>>11)*0x1.0p-53;
        if(roll<shrubChance)return 7;
        if(roll<shrubChance+.025)return 6;
        if(roll<shrubChance+.035)return 5;
        // Fine soil stays near woody patches; no continuous grass sward or
        // deep sand blanket is inferred from climate alone.
        if(patch>.58)return 4;
        double sediment=SurfacePatches.sample(seed^0x5845524943534F49L,x,z,23);
        return sediment>.64?3:sediment>.36?2:1;
    }
    private WarmXericShrubland(){}
}
