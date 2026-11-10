package blockbranch.realistic;

import static blockbranch.realistic.OldGrowthTaiga.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class OldGrowthTaigaFixtures {
    static Point fixture(DrainageNetwork d,long seed,int kind) {
        var c=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){
            if(kind(seed,x,z,d,c)!=kind)continue;
            var roots=BorealTrees.roots(seed,x-48,z-48,x+48,z+48,d,c);
            for(var r:roots)if(r.family()==family(kind)&&r.height()>=26&&kind(seed,r.x(),r.z(),d,c)==kind
                &&roots.stream().filter(q->q.family()==family(kind)).count()>=6)return new Point(r.x(),r.z());
        }
        throw new AssertionError("No natural old-growth taiga fixture kind="+kind+" seed="+seed);
    }

    public static View view(int x,int y,int z,boolean elevated) {
        double cx=x+(elevated?-38:-18)+.5,cz=z+(elevated?50:28)+.5,cy=y+(elevated?43:2);
        double dx=x+.5-cx,dz=z+.5-cz;
        return new View(cx,cy,cz,(float)Math.toDegrees(Math.atan2(-dx,dz)),
            (float)-Math.toDegrees(Math.atan2(y+13-(cy+1.62),Math.hypot(dx,dz))));
    }
    private OldGrowthTaigaFixtures() {}
}
