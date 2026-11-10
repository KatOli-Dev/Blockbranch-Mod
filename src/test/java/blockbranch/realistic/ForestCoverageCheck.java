package blockbranch.realistic;
import java.util.*;

/** Forest consumes only closed interiors of the previous oak-birch domain, retaining specific open woodland. */
public final class ForestCoverageCheck {
    public static void main(String[]args){int i=0;for(long seed:new long[]{424242,1,-17}){
        var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var before=new TreeMap<String,Integer>();var after=new TreeMap<String,Integer>();int changed=0;
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){String old=PlacementBaseline.definition30(seed,x,z,d,c);boolean forest=Forest.potential(seed,x,z,d,c);String now=BirchForest.contains(seed,x,z,d,c)?"birch-forest":forest?"forest":old;
            require(!forest||old.equals("temperate-woodland"),"Only existing woodland interior refined");require(!forest||!OakBirchWoodland.contains(seed,x,z,d,c)&&!Plains.potential(seed,x,z,d,c),"Exclusive woodland/grassland partition");
            if(forest)changed++;before.merge(old,1,Integer::sum);after.merge(now,1,Integer::sum);
        }
        require(changed==new int[]{1311,1231,1373}[i]&&after.getOrDefault("temperate-woodland",0)==new int[]{759,678,786}[i++],"Audited closed Forest versus retained dappled woodland");
        for(var e:before.entrySet())if(!e.getKey().equals("temperate-woodland"))require(Objects.equals(e.getValue(),after.get(e.getKey())),"Other habitats and hydrology coverage unchanged "+e.getKey());
        System.out.printf("FOREST_COVERAGE seed=%d changed=%d before=%s after=%s%n",seed,changed,before,after);
    }System.out.println("FOREST_CLOSED_STAND_PARTITION_COVERAGE_PASS");}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
