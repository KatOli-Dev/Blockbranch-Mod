package blockbranch.realistic;
import java.util.*;

/** Same56644point three-seed survey: sunflower composition refines Plains only; all prior habitats retain coverage. */
public final class SunflowerCoverageCheck {
    public static void main(String[]args){int i=0;for(long seed:new long[]{424242,1,-17}){
        var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var after=new TreeMap<String,Integer>();int flowers=0,plains=0,meadow=0,domain=0;
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            String old=PlacementBaseline.definition28(seed,x,z,d,c);boolean parent=Plains.potential(seed,x,z,d,c),sf=SunflowerPlains.contains(seed,x,z,d,c),plain=Plains.contains(seed,x,z,d,c);require(!sf||parent&&old.equals("meadow"),"Only old Plains domain refined");require(parent==(sf||plain)&&!(sf&&plain),"No gaps or overlaps in broad grassland partition");
            if(parent)domain++;if(sf)flowers++;if(plain)plains++;if(old.equals("meadow")&&!parent)meadow++;
            after.merge(sf?"sunflower-plains":plain?"plains":old,1,Integer::sum);
        }
        require(flowers==new int[]{887,848,775}[i]&&plains==new int[]{3719,3834,3756}[i]&&meadow==new int[]{3784,3659,3767}[i],"Audited sunflower/Plains/specific meadow split");i++;
        System.out.printf("SUNFLOWER_COVERAGE seed=%d domain=%d sunflower=%d plains=%d retainedMeadow=%d after=%s%n",seed,domain,flowers,plains,meadow,after);
    }System.out.println("SUNFLOWER_EXCLUSIVE_PLAINS_REFINEMENT_COVERAGE_PASS");}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SunflowerCoverageCheck(){}
}
