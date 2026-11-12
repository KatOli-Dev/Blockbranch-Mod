package blockbranch.realistic;
import java.util.function.BiFunction;
import static blockbranch.realistic.OakBirchWoodland.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class OakBirchWoodlandFixtures {
    static WoodlandPoint fixture(DrainageNetwork drainage,long seed) {return fixture(drainage,seed,(x,z)->true);}
    /** {@code owned} lets a smoke require the generated biome at the root too: habitat membership alone no longer implies ownership where another biome takes precedence. */
    static WoodlandPoint fixture(DrainageNetwork drainage,long seed,java.util.function.BiPredicate<Integer,Integer> owned) {
        var climate=new ClimateField(seed);
        for(int z=-1700000;z<=1700000;z+=2000) for(int x=-1700000;x<=1700000;x+=2000) {
            int e=drainage.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;
            if(e<60 || e>900)continue; // Preserve the original regression witness.
            if(!contains(seed,x,z,drainage,climate)) continue;
            var roots=WoodlandTrees.roots(seed,x-32,z-32,x+32,z+32,drainage,climate);
            for(var root:roots) {
                if(root.family()!=0 || root.birch() || root.height()<14 || !owned.test(root.x(),root.z())) continue;
                boolean nearbyBirch=roots.stream().anyMatch(other->other.family()==0&&other.birch() && Math.hypot(other.x()-root.x(),other.z()-root.z())<32);
                if(nearbyBirch) return new WoodlandPoint(root.x(),root.z());
            }
        }
        throw new AssertionError("No mesic mixed oak-birch stand found");
    }
    private OakBirchWoodlandFixtures() {}
}
