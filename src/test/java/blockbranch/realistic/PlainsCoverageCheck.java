package blockbranch.realistic;
import java.util.*;

/** Comparable definition28 survey: only intentional meadow refinement, never forest/water/gap replacement. */
public final class PlainsCoverageCheck {
    public static void main(String[]args){int index=0;for(long seed:new long[]{424242,1,-17}){
        var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var before=new TreeMap<String,Integer>();var after=new TreeMap<String,Integer>();int changed=0;
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            String old=PlacementBaseline.definition28(seed,x,z,d,c);boolean selected=Plains.potential(seed,x,z,d,c);String now=SunflowerPlains.contains(seed,x,z,d,c)?"sunflower-plains":selected?"plains":old;
            require(!selected||old.equals("meadow"),"Only previous specific meadow refined,not "+old);require(!selected||!LowlandMeadow.contains(x,z,d,c),"Exclusive production meadow identity");
            if(selected)changed++;before.merge(old,1,Integer::sum);after.merge(now,1,Integer::sum);
        }
        require(changed==new int[]{4606,4682,4531}[index],"Audited broad Plains domain stable");require(after.getOrDefault("meadow",0)==new int[]{3784,3659,3767}[index++],"Substantial specific meadow retained");
        for(var e:before.entrySet())if(!e.getKey().equals("meadow"))require(Objects.equals(e.getValue(),after.get(e.getKey())),"Prior habitat and hydrology coverage retained "+e.getKey());
        System.out.printf("PLAINS_COVERAGE seed=%d changed=%d before=%s after=%s%n",seed,changed,before,after);
    }System.out.println("PLAINS_EXCLUSIVE_MEADOW_PARTITION_COVERAGE_PASS");}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private PlainsCoverageCheck(){}
}
