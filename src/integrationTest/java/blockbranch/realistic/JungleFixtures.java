package blockbranch.realistic;

import static blockbranch.realistic.Jungle.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class JungleFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);
        for(int z=-600000;z<=600000;z+=8000)for(int x=-1800000;x<=1800000;x+=8000){
            if(!contains(seed,x,z,d,c)||c.sample(x,z,d::sample).meanTemperatureC()<23.1||c.sample(x,z,d::sample).moisture()<.67)continue;
            for(var root:JungleTrees.roots(seed,x-32,z-32,x+32,z+32,d,c))if(root.height()>=25)return new Point(root.x(),root.z());
        }
        throw new AssertionError("No natural tropical Jungle fixture");
    }
    private JungleFixtures() {}
}
