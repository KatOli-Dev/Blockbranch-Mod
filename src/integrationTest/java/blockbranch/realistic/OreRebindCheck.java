package blockbranch.realistic;

/** A profile is rebound when a perennial sheet lowers its rock datum. A rebind whose bounds
 * exceed the index limits must give the same (unindexed, direct-path) layer as a fresh bind,
 * never reuse the earlier index, which covers only its own bounds and silently omits ore
 * outside them. Compares the derived index itself: block generation needs a live server. */
public final class OreRebindCheck {
    public static void main(String[] args)throws Exception{
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
        var index=OreLayer.class.getDeclaredField("index");index.setAccessible(true);
        int cases=0,indexed=0;
        for(long seed:new long[]{424242,1,-17})for(int cx:new int[]{-1,0,1})for(int cz:new int[]{0,5}){
            var bound=new OreLayer(seed,cx,cz,OreLayer.DEFINITION,0,0).bind(0,500);
            if(index.get(bound)==null)throw new AssertionError("Vacuous: ordinary bounds must be indexed");indexed++;
            var rebound=bound.bind(-60,2000);              // exceeds the index limits: direct path
            var reference=new OreLayer(seed,cx,cz,OreLayer.DEFINITION,0,0).bind(-60,2000);
            if(index.get(reference)!=null)throw new AssertionError("Vacuous: oversized bounds must be unindexed");
            if(index.get(rebound)!=index.get(reference))throw new AssertionError("Stale index kept after rebinding beyond the limits, seed "+seed+" column "+cx+","+cz);
            if(bound.bind(0,500)!=bound)throw new AssertionError("Unchanged bounds must return the existing indexed layer");
            var rebuilt=bound.bind(10,480);                // in-limit rebind builds a new exact index
            if(index.get(rebuilt)==null||index.get(rebuilt)==index.get(bound))throw new AssertionError("In-limit rebind must build its own index");
            cases++;
        }
        System.out.println("PASS ore rebind beyond the index limits equals a fresh bind: "+cases+" cases ("+indexed+" indexed ordinary binds)");
    }
    private OreRebindCheck(){}
}
