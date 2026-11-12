package blockbranch.realistic;

/** Source-inspired regional seasonal broadleaf structure, using the established
 * dry-forest identity and growing-season canopy. FAO's Asian moist-deciduous
 * description supplies mean20–30C, model rain1300–2000mm and4–5 dry months:
 * https://www.fao.org/4/ad908e/AD908E01.htm (section1.1.1d).
 * The <100mm convention is borrowed from IUCN T1.2/existing DrySeasonPolicy,
 * not defined by that FAO regional paragraph. This hybrid is not FAO's global
 * tropical classifier (all months>18C, P<=2T), observed rainfall/PET or a
 * nutrient/phenology model. Only existing forest owners may split; savanna
 * cannot become closed forest from climate alone. */
public final class SeasonalForestExpansion {
    public static final int FLAG=32;
    static boolean climateSuitable(int z,ClimateField.Sample c,DrySeasonPolicy.Potential rain){
        return c.meanTemperatureC()>=20&&c.meanTemperatureC()<=30
            &&CoolDesert.season(z,c.meanTemperatureC()).coldestC()>0
            &&rain.annualModelRainMm()>=1300&&rain.annualModelRainMm()<=2000
            &&rain.dryMonths()>=4&&rain.dryMonths()<=5&&rain.longestDryRun()==rain.dryMonths();
    }
    static boolean eligible(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var climate=c.sample(x,z,d::sample);
        if(climate.meanTemperatureC()<20||climate.meanTemperatureC()>30)return false;
        if(!climateSuitable(z,climate,DrySeasonPolicy.potential(x,z,d,c)))return false;
        return WarmWoodland.surfaceContains(seed,x,z,d,c)
            ||TropicalForestVariants.surfaceKind(seed,x,z,d,c)==TropicalForestVariants.SPARSE;
    }
    private SeasonalForestExpansion(){}
}
