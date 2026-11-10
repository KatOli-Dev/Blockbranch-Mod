package blockbranch.realistic;
import java.util.*;

/** Same3seed coarse audit:birch refines closed Forest only,not the retained dappled woodland or other habitats. */
public final class BirchCoverageCheck {
    public static void main(String[]args){int i=0;for(long seed:new long[]{424242,1,-17}){
        var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var before=new TreeMap<String,Integer>();var after=new TreeMap<String,Integer>();int birch=0;
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){String old=PlacementBaseline.definition31(seed,x,z,d,c);boolean selected=BirchForest.contains(seed,x,z,d,c);String now=selected?"birch-forest":old;
            require(!selected||old.equals("forest")&&!Forest.contains(seed,x,z,d,c)&&!OakBirchWoodland.contains(seed,x,z,d,c),"Only closed Forest composition changes");if(selected)birch++;before.merge(old,1,Integer::sum);after.merge(now,1,Integer::sum);
        }
        require(birch==new int[]{242,188,159}[i]&&after.get("forest")==new int[]{1069,1043,1214}[i]&&after.get("temperate-woodland")==new int[]{759,678,786}[i++],"Explicit audited forest/birch/specific woodland partition");
        for(var e:before.entrySet())if(!e.getKey().equals("forest"))require(Objects.equals(e.getValue(),after.get(e.getKey())),"Other habitat coverage unchanged "+e.getKey());
        System.out.printf("BIRCH_COVERAGE seed=%d birch=%d before=%s after=%s%n",seed,birch,before,after);
    }System.out.println("BIRCH_EXCLUSIVE_CLOSED_FOREST_PARTITION_PASS");}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
