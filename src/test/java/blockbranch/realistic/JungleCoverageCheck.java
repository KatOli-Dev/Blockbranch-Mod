package blockbranch.realistic;
import java.util.*;

/** Same three-seed grid; report tropical reassignment separately from placeholder gains. */
public final class JungleCoverageCheck {
    public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){
        var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var before=new TreeMap<String,Integer>();var after=new TreeMap<String,Integer>();var changed=new TreeMap<String,Integer>();int land=0,oldGap=0,newGap=0;
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            String old=PlacementBaseline.definition24(seed,x,z,d,c);boolean jungle=Jungle.contains(seed,x,z,d,c);String now=jungle?"jungle":old;
            if(jungle&&!Set.of("placeholder","warm-woodland","humid-woodland").contains(old))throw new AssertionError("Unexpected prior habitat changed "+old);
            if(jungle)changed.merge(old,1,Integer::sum);before.merge(old,1,Integer::sum);after.merge(now,1,Integer::sum);
            var t=d.sample(x,z);if(t.waterLevel()<=t.height()+1){land++;if(old.equals("placeholder"))oldGap++;if(now.equals("placeholder"))newGap++;}else if(jungle)throw new AssertionError("Water displaced");
        }
        if(!before.toString().equals(java.util.Map.of(424242L,"{boreal=905, cold-steppe=357, deep-ocean=36928, gravel-desert=137, humid-woodland=102, lake=1, marine-shelf=3407, meadow=8390, placeholder=1674, river=11, river-channel=29, savanna=157, scree=850, shore=42, steppe=739, temperate-woodland=2070, tundra=79, warm-woodland=766}",1L,"{boreal=909, cold-steppe=177, deep-ocean=37328, gravel-desert=120, humid-woodland=199, lake=3, marine-shelf=3121, meadow=8341, placeholder=1440, river=12, river-channel=28, savanna=203, scree=724, shore=62, steppe=942, temperate-woodland=1909, tundra=39, warm-woodland=1087}",-17L,"{boreal=784, cold-steppe=447, deep-ocean=37231, gravel-desert=107, humid-woodland=255, lake=2, marine-shelf=3919, meadow=8298, placeholder=1324, reedbed=2, river=5, river-channel=29, savanna=52, scree=608, shore=78, steppe=834, temperate-woodland=2159, tundra=59, warm-woodland=451}").get(seed)))throw new AssertionError("Frozen committed definition-24 coverage changed: "+before);
        if(changed.isEmpty()||!before.containsKey("humid-woodland")||after.getOrDefault("humid-woodland",0)<=0||after.getOrDefault("warm-woodland",0)<=0)throw new AssertionError("Missing tropical Jungle or prior specific woodlands");
        System.out.printf(Locale.ROOT,"JUNGLE_COVERAGE seed=%d land=%d changes=%s landGapBeforePct=%.3f landGapAfterPct=%.3f before=%s after=%s%n",seed,land,changed,100.0*oldGap/land,100.0*newGap/land,before,after);
    }System.out.println("JUNGLE_COVERAGE_BOUNDED_TROPICAL_REALLOCATION_PASS");}
    private JungleCoverageCheck(){}
}
