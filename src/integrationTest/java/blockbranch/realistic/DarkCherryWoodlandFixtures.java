package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.DarkCherryWoodland.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class DarkCherryWoodlandFixtures {
    static Point fixture(DrainageNetwork d,long seed,int kind){
        var c=new ClimateField(seed);
        for(int z=-1700000;z<=1700000;z+=16000)for(int x=-1700000;x<=1700000;x+=16000){
            if(kind(seed,x,z,d,c)!=kind)continue;
            for(var r:WoodlandTrees.roots(seed,x-64,z-64,x+64,z+64,d,c))if(r.family()==family(kind)&&r.height()>=(kind==CHERRY?14:21)&&kind(seed,r.x(),r.z(),d,c)==kind)return new Point(r.x(),r.z());
        }
        throw new AssertionError("No natural dark/cherry stand kind="+kind+" seed="+seed);
    }

    public static View view(int x,int y,int z,boolean elevated,int kind,java.util.function.IntBinaryOperator ground){
        double cx=x+(elevated?-38:-18)+.5,cz=z+(elevated?50:28)+.5;int local=ground.applyAsInt((int)Math.floor(cx),(int)Math.floor(cz));
        double cy=elevated?Math.max(y,local)+(kind==CHERRY?34:43):local+2,target=y+(kind==CHERRY?6:10);
        return new View(cx,cy,cz,(float)Math.toDegrees(Math.atan2(cx-x-.5,z+.5-cz)),(float)-Math.toDegrees(Math.atan2(target-(cy+1.62),Math.hypot(x+.5-cx,z+.5-cz))));
    }
    private DarkCherryWoodlandFixtures() {}
}
