package blockbranch.realistic;

import static blockbranch.realistic.WarmWoodland.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class WarmWoodlandFixtures {
    static Point fixture(DrainageNetwork drainage,long seed) {return fixture(drainage,seed,(x,z)->true);}
    /** {@code owned} lets a smoke require the generated biome at the root too: habitat membership alone no longer implies ownership where another biome takes precedence. */
    static Point fixture(DrainageNetwork drainage,long seed,java.util.function.BiPredicate<Integer,Integer> owned) {
        var climate=new ClimateField(seed);
        for(int z=-1700000;z<=1700000;z+=16000)for(int x=-1700000;x<=1700000;x+=16000) {
            if(!contains(seed,x,z,drainage,climate))continue;
            var roots=WarmTrees.roots(seed,x-32,z-32,x+32,z+32,drainage,climate);
            for(var root:roots)if(owned.test(root.x(),root.z()) && root.pine() && root.height()>=14 && roots.stream().anyMatch(other->!other.pine() && Math.hypot(root.x()-other.x(),root.z()-other.z())<32))return new Point(root.x(),root.z());
        }
        throw new AssertionError("No warm mixed oak-pine woodland found");
    }

    static Transition transition(DrainageNetwork drainage,long seed,boolean climateOnly) {return transition(drainage,seed,climateOnly,(x,z)->true);}
    /** With {@code owned}, the edge is where the generated biome (not only habitat membership) stops. */
    static Transition transition(DrainageNetwork drainage,long seed,boolean climateOnly,java.util.function.BiPredicate<Integer,Integer> owned) {
        var start=fixture(drainage,seed,owned);var climate=new ClimateField(seed);
        java.util.function.BiPredicate<Integer,Integer> selected=climateOnly ? (x,z)->climateSuitable(climate.sample(x,z,drainage::sample)) : (x,z)->contains(seed,x,z,drainage,climate)&&owned.test(x,z);
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}})for(int r=256;r<=524288;r*=2) {
            int bx=start.x()+d[0]*r,bz=start.z()+d[1]*r;if(Math.abs(bx)>1900000 || Math.abs(bz)>1900000 || selected.test(bx,bz))continue;
            int ax=start.x(),az=start.z();
            for(int i=0;i<32 && Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4;i++) {
                int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;
                if(selected.test(mx,mz)){ax=mx;az=mz;}else{bx=mx;bz=mz;}
            }
            return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No adjacent warm woodland transition");
    }
    private WarmWoodlandFixtures() {}
}
