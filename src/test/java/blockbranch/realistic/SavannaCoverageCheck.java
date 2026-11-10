package blockbranch.realistic;
import java.util.*;

/** Comparable three-seed survey; never treats coverage as a quota or a biome-size measurement. */
public final class SavannaCoverageCheck {
    public static void main(String[]args){
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var before=new TreeMap<String,Integer>();var after=new TreeMap<String,Integer>();int land=0,oldGap=0,newGap=0,gain=0;
            for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
                var sample=d.sample(x,z);boolean dry=sample.waterLevel()<=sample.height()+1;String old=PlacementBaseline.actual(seed,x,z,d,c);boolean selected=Savanna.contains(seed,x,z,d,c);String now=selected?"savanna":old;
                if(selected&&!old.equals("placeholder"))throw new AssertionError("Savanna changed an existing habitat "+old);
                if(selected&&!dry)throw new AssertionError("Savanna displaced water");
                before.merge(old,1,Integer::sum);after.merge(now,1,Integer::sum);if(selected)gain++;
                if(dry){land++;if(old.equals("placeholder"))oldGap++;if(now.equals("placeholder"))newGap++;}
            }
            if(gain==0||newGap>=oldGap)throw new AssertionError("No actual warm open grassland gained");
            System.out.printf(Locale.ROOT,"SAVANNA_COVERAGE seed=%d land=%d gains=%d oldLandGap=%d newLandGap=%d oldLandGapPct=%.3f newLandGapPct=%.3f before=%s after=%s%n",seed,land,gain,oldGap,newGap,100.0*oldGap/land,100.0*newGap/land,before,after);
        }
        System.out.println("SAVANNA_COVERAGE_PRIOR_SEVENTEEN_DOMAINS_PASS");
    }
    private SavannaCoverageCheck(){}
}
