package blockbranch.realistic;

import static blockbranch.realistic.TropicalForestVariants.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class TropicalForestVariantsFixtures {
    static Point fixture(DrainageNetwork d,long seed,int kind){
        var c=new ClimateField(seed);var ecology=new GroundEcology.Field(d::sample,c);
        for(int z=-800000;z<=800000;z+=8000)for(int x=-1700000;x<=1700000;x+=8000){
            if(kind(seed,x,z,d,c)!=kind)continue;
            if(kind==SPARSE){for(var r:TropicalVariantTrees.roots(seed,x-64,z-64,x+64,z+64,d,c))if(r.family()==12&&r.height()>=18)return new Point(r.x(),r.z());}
            else for(int dz=-32;dz<=32;dz++)for(int dx=-32;dx<=32;dx++)if(surfaceKind(seed,x+dx,z+dz,d,c)==BAMBOO&&BambooGroves.accepts(seed,x+dx,z+dz,ecology.sample(x+dx,z+dz)))return new Point(x+dx,z+dz);
        }
        throw new AssertionError("No natural tropical variant kind="+kind+" seed="+seed);
    }

    public static View view(int x,int y,int z,boolean elevated,int kind,java.util.function.IntBinaryOperator ground){
        if(kind==3)return new View(x-17.5,y+(elevated?44:5),z+28.5,-147,elevated?27:7);
        double cx=x+(elevated?-34:-18)+.5,cz=z+(elevated?44:28)+.5;
        int local=ground.applyAsInt((int)Math.floor(cx),(int)Math.floor(cz));double cy=elevated?Math.max(y,local)+(kind==BAMBOO?24:34):local+2,target=y+(kind==BAMBOO?5:8);
        return new View(cx,cy,cz,(float)Math.toDegrees(Math.atan2(cx-x-.5,z+.5-cz)),(float)-Math.toDegrees(Math.atan2(target-(cy+1.62),Math.hypot(x+.5-cx,z+.5-cz))));
    }
    private TropicalForestVariantsFixtures() {}
}
