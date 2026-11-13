package blockbranch.realistic;

import java.util.function.BiPredicate;

/** Partial seasonally cold montane needleleaf structure, restricted to existing
 * ordinary forest owners. FAO GEZ supplies the4–8 >10C-month temperate domain
 * and approximate800m mountain convention, not a conifer classifier:
 * https://www.fao.org/4/ad652e/ad652e07.htm . Pan et al.(2013), Table1,
 * doi:10.1146/annurev-ecolsys-110512-135914 supplies1000–2500mm as a broad
 * temperate mountain context; wetter conifer forests are not ecologically
 * excluded by this bounded first representation. IUCN GET T2.1 motivates
 * cold-season needleleaf structure, not deterministic climate-to-species routing:
 * https://jrfep.github.io/Ecosystem-profiles-comments/EFGs/T2.1 .
 * Rain and the idealized thermal cycle are inherited uncalibrated model units.
 * A separately salted existing composition province retains broadleaf stands;
 * it expresses missing floristics/history, never measured ecosystem frequency.
 * Soil acidity, disturbance, nutrients and seasonal phenology remain absent. */
public final class MontaneForest {
    public static final String KEY="montane_needleleaf_forest";
    public static final int FLAG=64,FAMILY=20;
    static boolean climateSuitable(int z,int elevation,double mean,double rain){
        var season=CoolDesert.season(z,mean);
        return elevation>=800&&rain>=1000&&rain<=2500&&season.coldestC()<0
            &&season.warmMonths()>=4&&season.warmMonths()<=8;
    }
    static boolean eligible(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var s=d.sample(x,z);var climate=c.sample(x,z,d::sample);
        if(!climateSuitable(z,s.height()+1-TerrainShape.SEA_LEVEL,climate.meanTemperatureC(),DrySeasonPolicy.annualModelRain(c.atmosphericSupply(x,z,d::sample)))
            ||!Taiga.provinceSelected(seed^0x4D4F4E54414E454CL,x,z))return false;
        // Current swamp banks top out at603m, below this800m envelope;
        // wind cores exceed.45 slope while woodland allows at most.35, and
        // wind shoulders exclude woodland. Explicit owner guards retain those
        // physical reservations independently of future eligibility envelopes.
        if(LandVegetation.surfaceReserved(d,x,z)||ColdUplands.surfaceKind(seed,x,z,d,c)>0
            ||WindsweptUplands.surfaceMaterialKind(seed,x,z,d,c)>0||Badlands.surfaceKind(seed,x,z,d,c)>0
            ||RedwoodForest.surfaceContains(seed,x,z,d,c)||Swamp.surfaceSoilContains(seed,x,z,d,c))return false;
        // Recheck competing physical ownership between quart anchors. No grass,
        // specialist forest, water or exposed rock becomes forest from climate.
        if(!OakBirchWoodland.surfacePotential(seed,x,z,d,c))return false;
        if(Forest.surfacePotential(seed,x,z,d,c))return true;
        return TemperateForestVariants.surfaceKind(seed,x,z,d,c)==0
            &&!DappledForest.surfaceContains(seed,x,z,d,c)
            &&DarkCherryWoodland.surfaceKind(seed,x,z,d,c)==0;
    }
    static BiPredicate<Integer,Integer> domain(long seed,DrainageNetwork d,ClimateField c,BiPredicate<Integer,Integer> selectedQuart){
        return CoolDesert.selectedDomain(selectedQuart,(x,z)->eligible(seed,x,z,d,c));
    }
    private MontaneForest(){}
}
