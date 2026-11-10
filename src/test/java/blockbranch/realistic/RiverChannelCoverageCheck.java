package blockbranch.realistic;
import java.util.*;

/** Same coarse survey and frozen complete definition-20 baseline; narrow water margins are under-resolved. */
public final class RiverChannelCoverageCheck {
    public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){
        var d=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);
        var before=new TreeMap<String,Integer>();var after=new TreeMap<String,Integer>();var gains=new TreeMap<String,Integer>();var gaps=new TreeMap<String,Integer>();var candidates=new TreeMap<String,Integer>();var detail=new TreeMap<String,Integer>();var waterGroups=new TreeMap<String,Integer>();
        int samples=0,land=0,oldLandGap=0,newLandGap=0;
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            samples++;var t=d.sample(x,z);boolean dryLand=!(t.waterLevel()!=DrainageNetwork.DRY&&t.height()+1<t.waterLevel());if(dryLand)land++;
            String old=PlacementBaseline.definition20(seed,x,z,d,climate),now=PlacementBaseline.definition21(seed,x,z,d,climate);
            before.merge(old,1,Integer::sum);after.merge(now,1,Integer::sum);
            if(!dryLand&&now.equals("placeholder")){
                int depth=t.waterLevel()-t.height()-1;
                String group=t.closed()?"closed-inland-water":t.waterLevel()!=TerrainShape.SEA_LEVEL?"open-inland-water-above-marine-datum":depth<=200?"marine-datum-shallow-0-200m":"marine-datum-deep-over-200m";
                waterGroups.merge(group,1,Integer::sum);
            }
            if(!old.equals("placeholder")&&!old.equals(now))throw new AssertionError("Existing habitat replaced "+seed+" "+x+","+z+" "+old+" -> "+now);
            if(dryLand&&old.equals("placeholder"))oldLandGap++;
            if(!old.equals(now)){
                if(dryLand||!RiverChannel.eligible(RiverChannel.site(d,x,z)))throw new AssertionError("River gain is not confirmed routed water");
                if(!now.equals("river-channel")||!RiverChannel.contains(x,z,d))throw new AssertionError("Gain outside routed channel");
                gains.merge(old+"->"+now,1,Integer::sum);
            }
            if(!dryLand||!now.equals("placeholder"))continue;newLandGap++;
            var c=climate.sample(x,z,d::sample);double slope=SlopeCorrection.slope(d,x,z);String reason=LandVegetation.reserved(d,x,z)?"reserved":slope>.4?"steep-over-0.4":c.meanTemperatureC()<-6?"very-cold-open":c.moisture()<.12?"arid":c.meanTemperatureC()<5&&c.moisture()<.32?"cold-dry":c.moisture()>.85?"very-wet":c.meanTemperatureC()>19?"warm-other":"temperate-other";gaps.merge(reason,1,Integer::sum);
            String why=LandVegetation.reserved(d,x,z)?"protected-or-routed-margin":slope>.4?"regional-steep-preserved-bare":slope>.35?"0.35-0.40-outside-current-soil-habitats":slope>.2&&!LowlandMeadow.contains(x,z,(wx,wz)->LowlandMeadow.site(d,wx,wz))?"local-soil-support-rejected":c.meanTemperatureC()<-14?"very-cold-beyond-tundra":c.meanTemperatureC()<-6?(c.moisture()<.2?"cold-arid-open":c.moisture()>.85?"cold-very-wet-open":"tundra-local-guard"):c.moisture()>.85?"humid-temperate-or-extremely-wet":c.moisture()<.12?"arid-coastal-or-climate-guard":c.meanTemperatureC()>19?"warm-open-stand-transition-or-climate-limit":c.meanTemperatureC()<3?"cool-climate-gap":"temperate-soil-or-climate-transition";
            detail.merge(why,1,Integer::sum);
            if(!LandVegetation.reserved(d,x,z)){
                if(c.meanTemperatureC()>=-14&&c.meanTemperatureC()<-6&&c.moisture()>=.20&&c.moisture()<=.85&&slope<=.35)candidates.merge("cold-dwarf-shrub-tundra",1,Integer::sum);
                if(c.meanTemperatureC()>19&&c.meanTemperatureC()<=29&&c.moisture()>.72&&c.moisture()<=.95&&slope<=.35)candidates.merge("warm-humid-woodland",1,Integer::sum);
            }
        }
        System.out.printf(Locale.ROOT,"RIVER_CHANNEL_REMAINING_WATER seed=%d wetSamples=%d unassignedWetSamples=%d wholeSampleUnassignedWet=%.2f%%%n",seed,samples-land,after.get("placeholder")-newLandGap,100.0*(after.get("placeholder")-newLandGap)/samples);
        if(oldLandGap!=newLandGap)throw new AssertionError("Land assignment coverage changed");
        System.out.printf("RIVER_CHANNEL_REMAINING_DETAIL seed=%d land=%s water=%s%n",seed,detail,waterGroups);
        String expected=seed==424242?"{boreal=916, cold-steppe=362, gravel-desert=137, humid-woodland=102, lake=1, meadow=8408, placeholder=42246, river=11, scree=751, shore=44, steppe=738, temperate-woodland=2075, tundra=86, warm-woodland=767}":seed==1?"{boreal=921, cold-steppe=185, gravel-desert=123, humid-woodland=198, lake=3, meadow=8352, placeholder=42177, river=12, scree=630, shore=55, steppe=944, temperate-woodland=1913, tundra=41, warm-woodland=1090}":"{boreal=798, cold-steppe=450, gravel-desert=107, humid-woodland=254, lake=2, meadow=8308, placeholder=42614, reedbed=2, river=7, scree=511, shore=72, steppe=837, temperate-woodland=2165, tundra=64, warm-woodland=453}";
        if(!before.toString().equals(expected))throw new AssertionError("Frozen fourteen-habitat baseline changed "+seed+" "+before);
        System.out.printf(Locale.ROOT,"RIVER_CHANNEL_COVERAGE seed=%d samples=%d spacingM=16000 land=%d beforeLandGap=%d afterLandGap=%d beforeLandPlaceholder=%.2f%% afterLandPlaceholder=%.2f%% wholeSamplePlaceholder=%.2f%% gains=%s habitats=%s remainingGapReasons=%s ecologicalCandidates=%s%n",seed,samples,land,oldLandGap,newLandGap,100.0*oldLandGap/land,100.0*newLandGap/land,100.0*after.get("placeholder")/samples,gains,after,gaps,candidates);
    }System.out.println("RIVER_CHANNEL_COVERAGE_PASS (frozen definition-20; only previously unassigned confirmed routed-water gains; no area quotas)");}
    private RiverChannelCoverageCheck(){}
}
