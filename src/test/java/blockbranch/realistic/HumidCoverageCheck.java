package blockbranch.realistic;
import java.util.*;

/** Same coarse survey and frozen complete definition-17 baseline; narrow water margins are under-resolved. */
public final class HumidCoverageCheck {
    public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){
        var d=new DrainageNetwork(new TerrainShape(seed),seed);var climate=new ClimateField(seed);
        var before=new TreeMap<String,Integer>();var after=new TreeMap<String,Integer>();var gains=new TreeMap<String,Integer>();var gaps=new TreeMap<String,Integer>();var candidates=new TreeMap<String,Integer>();
        int samples=0,land=0,oldLandGap=0,newLandGap=0;
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            samples++;var t=d.sample(x,z);boolean dryLand=!(t.waterLevel()!=DrainageNetwork.DRY&&t.height()+1<t.waterLevel());if(dryLand)land++;
            String old=PlacementBaseline.previous(seed,x,z,d,climate),now=PlacementBaseline.definition18(seed,x,z,d,climate);
            before.merge(old,1,Integer::sum);after.merge(now,1,Integer::sum);
            if(!old.equals("placeholder")&&!old.equals(now)&&!(old.equals("meadow")&&now.equals("humid-woodland")))throw new AssertionError("Existing habitat replaced "+seed+" "+x+","+z+" "+old+" -> "+now);
            if(dryLand&&old.equals("placeholder"))oldLandGap++;
            if(!old.equals(now)){
                if(!dryLand||LandVegetation.reserved(d,x,z))throw new AssertionError("Reserved/wet terrain changed");
                if(!now.equals("humid-woodland")||!PlacementBaseline.humidBeforeJungle(seed,x,z,d,climate))throw new AssertionError("Gain outside humid woodland");
                gains.merge(old+"->"+now,1,Integer::sum);
            }
            if(!dryLand||!now.equals("placeholder"))continue;newLandGap++;
            var c=climate.sample(x,z,d::sample);double slope=SlopeCorrection.slope(d,x,z);String reason=LandVegetation.reserved(d,x,z)?"reserved":slope>.4?"steep-over-0.4":c.meanTemperatureC()<-6?"very-cold-open":c.moisture()<.12?"arid":c.meanTemperatureC()<5&&c.moisture()<.32?"cold-dry":c.moisture()>.85?"very-wet":c.meanTemperatureC()>19?"warm-other":"temperate-other";gaps.merge(reason,1,Integer::sum);
            if(!LandVegetation.reserved(d,x,z)){
                if(c.meanTemperatureC()>=-14&&c.meanTemperatureC()<-6&&c.moisture()>=.20&&c.moisture()<=.85&&slope<=.35)candidates.merge("cold-dwarf-shrub-tundra",1,Integer::sum);
                if(c.meanTemperatureC()>19&&c.meanTemperatureC()<=29&&c.moisture()>.72&&c.moisture()<=.95&&slope<=.35)candidates.merge("warm-humid-woodland",1,Integer::sum);
            }
        }
        System.out.printf(Locale.ROOT,"HUMID_REMAINING_WATER seed=%d wetSamples=%d unassignedWetSamples=%d wholeSampleUnassignedWet=%.2f%%%n",seed,samples-land,after.get("placeholder")-newLandGap,100.0*(after.get("placeholder")-newLandGap)/samples);
        String expected=seed==424242?"{boreal=916, cold-steppe=362, gravel-desert=137, meadow=8491, placeholder=42352, river=11, scree=751, shore=44, steppe=738, temperate-woodland=2075, warm-woodland=767}":seed==1?"{boreal=921, cold-steppe=185, gravel-desert=123, meadow=8435, placeholder=42336, river=12, scree=630, shore=55, steppe=944, temperate-woodland=1913, warm-woodland=1090}":"{boreal=798, cold-steppe=450, gravel-desert=107, meadow=8473, placeholder=42769, reedbed=2, river=7, scree=511, shore=72, steppe=837, temperate-woodland=2165, warm-woodland=453}";
        if(!before.toString().equals(expected))throw new AssertionError("Frozen eleven-habitat baseline changed "+seed+" "+before);
        System.out.printf(Locale.ROOT,"HUMID_WOODLAND_COVERAGE seed=%d samples=%d spacingM=16000 land=%d beforeLandGap=%d afterLandGap=%d beforeLandPlaceholder=%.2f%% afterLandPlaceholder=%.2f%% wholeSamplePlaceholder=%.2f%% gains=%s habitats=%s remainingGapReasons=%s ecologicalCandidates=%s%n",seed,samples,land,oldLandGap,newLandGap,100.0*oldLandGap/land,100.0*newLandGap/land,100.0*after.get("placeholder")/samples,gains,after,gaps,candidates);
    }System.out.println("HUMID_WOODLAND_COVERAGE_PASS (frozen definition-17; only warm wet meadow and placeholder gains; no area quotas)");}
    private HumidCoverageCheck(){}
}
