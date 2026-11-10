package blockbranch.realistic;

import static blockbranch.realistic.Savanna.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class SavannaFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);
        for(int z=-500000;z<=500000;z+=8000)for(int x=-1800000;x<=1800000;x+=8000){
            if(!contains(seed,x,z,d,c))continue;
            for(var r:SavannaTrees.roots(seed,x-32,z-32,x+32,z+32,d,c))if(r.height()>=9)return new Point(r.x(),r.z());
        }
        throw new AssertionError("No natural warm acacia savanna fixture");
    }
    private SavannaFixtures() {}
}
