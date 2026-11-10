package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.ColdUplands.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class ColdUplandsFixtures {
    static Point fixture(DrainageNetwork d,long seed,int kind){
        var c=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=8000)for(int x=-1900000;x<=1900000;x+=8000){
            if(kind(seed,x,z,d,c)!=kind)continue;
            for(int radius:new int[]{0,128,256,512})for(int dz=-radius;dz<=radius;dz+=128)for(int dx=-radius;dx<=radius;dx+=128){
                if(radius>0&&Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;int wx=x+dx,wz=z+dz;
                if(kind(seed,wx,wz,d,c)!=kind||interiorCount(seed,wx,wz,kind,d,c)<60)continue;
                if(kind==GRAVELLY){for(var r:roots(seed,wx-64,wz-64,wx+64,wz+64,d,c))if(r.height()>=10&&interiorCount(seed,r.x(),r.z(),kind,d,c)>=60)return new Point(r.x(),r.z());}
                else return new Point(wx,wz);
            }
        }
        throw new AssertionError("No natural cold upland interior seed="+seed+" kind="+kind);
    }

    public static MountainPeaks.View view(int x,int y,int z,int kind,boolean elevated,java.util.function.IntBinaryOperator ground){
        if(kind==FROZEN&&elevated){
            var camera=MountainPeaks.overview(x,y,z,ground);int sx=x,sz=z,top=y;
            for(int dz=-192;dz<=192;dz+=16)for(int dx=-192;dx<=192;dx+=16){
                if(dx*dx+dz*dz>192*192)continue;int h=ground.applyAsInt(x+dx,z+dz);
                if(h>top){sx=x+dx;sz=z+dz;top=h;}
            }
            // Summit anchors do not need a65m aerial: it excludes the visible lower flank.
            double cy=Math.max(ground.applyAsInt((int)Math.floor(camera.x()),(int)Math.floor(camera.z()))+8,Math.min(camera.y(),top+8));
            double pitch=-Math.toDegrees(Math.atan2(top+.5-(cy+1.62),Math.hypot(sx+.5-camera.x(),sz+.5-camera.z())));
            return new MountainPeaks.View(camera.x(),cy,camera.z(),camera.yaw(),(float)pitch);
        }
        var v=blockbranch.realistic.WindsweptUplandsFixtures.view(x,y,z,elevated,ground);return new MountainPeaks.View(v.x(),v.y(),v.z(),v.yaw(),v.pitch());
    }
    private ColdUplandsFixtures() {}
}
