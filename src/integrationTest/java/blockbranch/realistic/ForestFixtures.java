package blockbranch.realistic;

import static blockbranch.realistic.Forest.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class ForestFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if(!contains(seed,x,z,d,c))continue;
            var roots=WoodlandTrees.roots(seed,x-48,z-48,x+48,z+48,d,c);
            for(var root:roots)if(root.family()==1&&!root.birch()&&root.height()>=20&&contains(seed,root.x(),root.z(),d,c)&&roots.stream().filter(r->r.family()==1).count()>12&&roots.stream().anyMatch(r->r.family()==1&&r.birch()&&Math.hypot(r.x()-root.x(),r.z()-root.z())<24))return new Point(root.x(),root.z());
        }
        throw new AssertionError("No natural closed Forest mature oak fixture");
    }
    private ForestFixtures() {}
}
