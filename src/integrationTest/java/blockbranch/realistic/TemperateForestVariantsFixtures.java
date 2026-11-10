package blockbranch.realistic;

import static blockbranch.realistic.TemperateForestVariants.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class TemperateForestVariantsFixtures {
    static Point fixture(DrainageNetwork d,long seed,int kind){
        var c=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if(kind(seed,x,z,d,c)!=kind)continue;
            var roots=WoodlandTrees.roots(seed,x-64,z-64,x+64,z+64,d,c);
            for(var r:roots)if((kind==OLD_BIRCH?r.family()==BIRCH_FAMILY&&r.birch()&&r.height()>=24:r.family()==1&&!r.birch()&&r.height()>=17)&&kind(seed,r.x(),r.z(),d,c)==kind)return new Point(r.x(),r.z());
        }
        throw new AssertionError("No natural temperate variant kind="+kind+" seed="+seed);
    }

    public static View view(int x,int y,int z,boolean elevated,int kind){
        double cx=x+(elevated?-38:-18)+.5,cz=z+(elevated?50:28)+.5,cy=y+(elevated?43:2),target=y+(kind==OLD_BIRCH?12:2);
        return new View(cx,cy,cz,(float)Math.toDegrees(Math.atan2(cx-x-.5,z+.5-cz)),(float)-Math.toDegrees(Math.atan2(target-(cy+1.62),Math.hypot(x+.5-cx,z+.5-cz))));
    }
    private TemperateForestVariantsFixtures() {}
}
