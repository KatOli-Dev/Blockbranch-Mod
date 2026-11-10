package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.SavannaUplands.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class SavannaUplandsFixtures {
    static Point fixture(DrainageNetwork d,long seed,int kind){
        var c=new ClimateField(seed);
        for(int z=-800000;z<=800000;z+=8000)for(int x=-1800000;x<=1800000;x+=8000){
            if(kind(seed,x,z,d,c)!=kind)continue;
            // Refine a bounded neighbourhood of actual candidates; rare faces may
            // cross the coarse search point close to their edge.
            for(int radius:new int[]{0,128,256,512})for(int dz=-radius;dz<=radius;dz+=128)for(int dx=-radius;dx<=radius;dx+=128){
                if(radius>0&&Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                int wx=x+dx,wz=z+dz;if(kind(seed,wx,wz,d,c)!=kind)continue;
                for(var r:roots(seed,wx-64,wz-64,wx+64,wz+64,d,c)){
                    if(r.height()<10||kind(seed,r.x(),r.z(),d,c)!=kind)continue;
                    if(interiorCount(seed,r.x(),r.z(),kind,d,c)>=60)return new Point(r.x(),r.z());
                }
            }
        }
        throw new AssertionError("No natural interior savanna upland witness seed="+seed+" kind="+kind);
    }

    public static View view(int x,int y,int z,boolean elevated,java.util.function.IntBinaryOperator ground){var v=blockbranch.realistic.WindsweptUplandsFixtures.view(x,y,z,elevated,ground);return new View(v.x(),v.y(),v.z(),v.yaw(),v.pitch());}
    private SavannaUplandsFixtures() {}
}
