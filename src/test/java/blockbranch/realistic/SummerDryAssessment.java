package blockbranch.realistic;

import java.util.*;

/** Pre-implementation assessment. This does not change any production field. */
public final class SummerDryAssessment {
    static boolean climate(int z,double mean,double annual,double background,double beltMin,double beltMax,double dryFactor){
        double belt=Math.abs(z/2097152.);
        if(belt<beltMin||belt>beltMax)return false;
        return SummerDryClimate.summarize(SummerDryClimate.monthly(annual,background),SummerDryClimate.temperatures(z,mean),dryFactor).suitable();
    }
    public static void main(String[] args){
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true,true,true);var c=new ClimateField(seed);
            int dry=0,parent=0,nominalCount=0,marineFetchCount=0;int[] changed=new int[8],horizonChanged=new int[4];var first=new ArrayList<Object>();
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var s=d.sample(x,z);if(s.waterLevel()!=DrainageNetwork.DRY)continue;dry++;
                if(!WarmWoodland.contains(seed,x,z,d,c))continue;parent++;
                double t=c.sample(x,z,d::sample).meanTemperatureC(),annual=DrySeasonPolicy.annualModelRain(c.atmosphericSupply(x,z,d::sample));
                boolean nominal=climate(z,t,annual,.08,.25,.65,2);
                if(nominal)nominalCount++;
                boolean fetch=false;
                for(int west:new int[]{20000,60000}){var sea=d.sample(x-west,z);fetch|=d.habitatOceanWater(x-west,z,sea);}
                if(nominal){
                    int[][] horizons={{19000,60000},{21000,60000},{20000,57000},{20000,63000}};
                    for(int i=0;i<horizons.length;i++){boolean alternative=false;for(int west:horizons[i]){var sea=d.sample(x-west,z);alternative|=d.habitatOceanWater(x-west,z,sea);}if(fetch!=alternative)horizonChanged[i]++;}
                }
                if(!fetch)continue;
                if(nominal)marineFetchCount++;
                for(int i=0;i<8;i++){
                    boolean alternative=climate(z,t,annual*(i==0?.95:i==1?1.05:1),i==2?.07:i==3?.09:.08,i==4?.2375:i==5?.2625:.25,.65,i==6?1.9:i==7?2.1:2);
                    if(nominal!=alternative)changed[i]++;
                }
                if(nominal&&first.size()<8)first.add(Map.of("x",x,"z",z,"height",s.height(),"mean",t,"annualModelRain",annual,"coldest",CoolDesert.season(z,t).coldestC(),"warmest",CoolDesert.season(z,t).warmestC()));
            }
            System.out.println("SUMMER_DRY_ASSESSMENT seed="+seed+" denominator=14400 dryLand="+dry+" warmWoodlandPotential="+parent+" climateWindow="+nominalCount+" marineFetchWindow="+marineFetchCount+" first="+first+" sensitivityChanged="+Arrays.toString(changed)+" horizonSensitivityChanged="+Arrays.toString(horizonChanged));
        }
    }
    private SummerDryAssessment(){}
}
