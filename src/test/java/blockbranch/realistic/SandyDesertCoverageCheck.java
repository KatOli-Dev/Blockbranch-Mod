package blockbranch.realistic;
import java.util.*;

/** Frozen pre-sand coverage: distinguish intentional gravel refinement from placeholder gains. */
public final class SandyDesertCoverageCheck {
    public static void main(String[]args){for(long seed:new long[]{424242,1,-17}){
        var d=new DrainageNetwork(new TerrainShape(seed),seed);var c=new ClimateField(seed);var before=new TreeMap<String,Integer>();var after=new TreeMap<String,Integer>();var changed=new TreeMap<String,Integer>();int land=0,oldGap=0,newGap=0;
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            String old=PlacementBaseline.definition25(seed,x,z,d,c);boolean sand=SandyDesert.contains(seed,x,z,d,c);String now=sand?"desert":old;
            if(sand&&!Set.of("gravel-desert","placeholder").contains(old))throw new AssertionError("Unexpected prior habitat changed "+old);
            if(sand)changed.merge(old,1,Integer::sum);before.merge(old,1,Integer::sum);after.merge(now,1,Integer::sum);
            var t=d.sample(x,z);if(t.waterLevel()<=t.height()+1){land++;if(old.equals("placeholder"))oldGap++;if(now.equals("placeholder"))newGap++;}else if(sand)throw new AssertionError("Water displaced");
        }
        if(changed.isEmpty()||after.getOrDefault("gravel-desert",0)<20)throw new AssertionError("Sandy and gravel desert both retained");
        for(var e:before.entrySet())if(!Set.of("gravel-desert","placeholder").contains(e.getKey())&&!Objects.equals(e.getValue(),after.get(e.getKey())))throw new AssertionError("Prior coverage changed "+e.getKey());
        System.out.printf(Locale.ROOT,"SANDY_DESERT_COVERAGE seed=%d land=%d changes=%s landGapBeforePct=%.3f landGapAfterPct=%.3f before=%s after=%s%n",seed,land,changed,100.0*oldGap/land,100.0*newGap/land,before,after);
    }System.out.println("SANDY_DESERT_BOUNDED_SUBSTRATE_REALLOCATION_PASS");}
    private SandyDesertCoverageCheck(){}
}
