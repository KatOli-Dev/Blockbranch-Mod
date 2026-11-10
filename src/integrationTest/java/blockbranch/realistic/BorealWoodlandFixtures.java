package blockbranch.realistic;
import java.util.function.BiFunction;
import static blockbranch.realistic.BorealWoodland.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class BorealWoodlandFixtures {
    static Point fixture(DrainageNetwork drainage,long seed) {
        var climate=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=2000) for(int x=-1900000;x<=1900000;x+=2000) {
            int elevation=drainage.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;
            if(elevation<40 || elevation>1800)continue; // Keep the original regression witness.
            if(!contains(seed,x,z,drainage,climate)||Taiga.contains(seed,x,z,drainage,climate)||SnowyTaiga.contains(seed,x,z,drainage,climate)) continue;
            var roots=BorealTrees.roots(seed,x-32,z-32,x+32,z+32,drainage,climate);
            for(var root:roots) if(root.family()==0&&!root.birch() && root.height()>=16 && roots.stream().anyMatch(other->other.birch() && Math.hypot(other.x()-root.x(),other.z()-root.z())<32)) return new Point(root.x(),root.z());
        }
        throw new AssertionError("No cold mesic spruce-birch woodland found");
    }

    static Transition transition(DrainageNetwork drainage,long seed,boolean climateOnly) {
        var start=fixture(drainage,seed);var climate=new ClimateField(seed);
        java.util.function.BiPredicate<Integer,Integer> selected=climateOnly ? (x,z)->climateSuitable(climate.sample(x,z,drainage::sample)) : (x,z)->contains(seed,x,z,drainage,climate);
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}}) for(int r=256;r<=524288;r*=2) {
            int bx=start.x()+d[0]*r,bz=start.z()+d[1]*r;
            if(Math.abs(bx)>2000000 || Math.abs(bz)>2000000 || selected.test(bx,bz)) continue;
            int ax=start.x(),az=start.z();
            for(int i=0;i<32 && Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4;i++) {
                int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;
                if(selected.test(mx,mz)) { ax=mx;az=mz; } else { bx=mx;bz=mz; }
            }
            return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No adjacent boreal transition");
    }
    private BorealWoodlandFixtures() {}
}
