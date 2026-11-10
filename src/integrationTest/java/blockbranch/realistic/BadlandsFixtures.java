package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.Badlands.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class BadlandsFixtures {
    static Point fixture(DrainageNetwork d,long seed,int kind){
        var c=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){
            if(kind==ERODED){
                if(d.eroded().envelope(x,z)<.99)continue;
                Point best=null;double relief=12;
                for(int dz=-64;dz<=64;dz+=2)for(int dx=-64;dx<=64;dx+=2){
                    int wx=x+dx,wz=z+dz;
                    if(d.eroded().envelope(wx,wz)<.99||d.eroded().incision(wx,wz)>5)continue;
                    int h=d.sample(wx,wz).height();double contrast=0;
                    for(int[] n:new int[][]{{16,0},{-16,0},{0,16},{0,-16}})contrast+=h-d.sample(wx+n[0],wz+n[1]).height();
                    contrast/=4;
                    if(contrast>relief&&interiorCount(seed,wx,wz,kind,d,c)>=60){best=new Point(wx,wz);relief=contrast;}
                }
                if(best!=null)return best;continue;
            }
            if(kind(seed,x,z,d,c)!=kind||interiorCount(seed,x,z,kind,d,c)<60)continue;
            if(kind==WOODED){for(var root:roots(seed,x-48,z-48,x+48,z+48,d,c))if(root.height()>=11&&interiorCount(seed,root.x(),root.z(),kind,d,c)>=60)return new Point(root.x(),root.z());}
            else return new Point(x,z);
        }
        throw new AssertionError("No natural badlands interior seed="+seed+" kind="+kind);
    }

    public static MountainPeaks.View view(int x,int y,int z,boolean elevated,java.util.function.IntBinaryOperator ground){
        return view(x,y,z,elevated,ground,BARE);
    }

    public static MountainPeaks.View view(int x,int y,int z,boolean elevated,java.util.function.IntBinaryOperator ground,int kind){
        var v=blockbranch.realistic.WindsweptUplandsFixtures.view(x,y,z,elevated,ground);
        // A hoodoo anchor is a remnant TOP, unlike an ordinary hillside anchor.
        // Aim at its body, preserving the ground camera's actual eye height/XY.
        float pitch=kind==ERODED&&!elevated?(float)-Math.toDegrees(Math.atan2(y-15-(v.y()+1.62),Math.hypot(x+.5-v.x(),z+.5-v.z()))):v.pitch();
        return new MountainPeaks.View(v.x(),v.y(),v.z(),v.yaw(),pitch);
    }
    private BadlandsFixtures() {}
}
