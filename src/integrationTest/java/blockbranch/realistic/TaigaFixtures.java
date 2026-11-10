package blockbranch.realistic;

import static blockbranch.realistic.Taiga.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class TaigaFixtures {
    static Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){
        if(!contains(seed,x,z,d,c))continue;var roots=BorealTrees.roots(seed,x-40,z-40,x+40,z+40,d,c);for(var root:roots)if(root.family()==4&&root.height()>=22&&contains(seed,root.x(),root.z(),d,c)&&roots.stream().filter(r->r.family()==4).count()>12)return new Point(root.x(),root.z());
    }throw new AssertionError("No natural evergreen Taiga stand");}
    private TaigaFixtures() {}
}
