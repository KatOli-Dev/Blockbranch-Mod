package blockbranch.realistic;

import static blockbranch.realistic.MountainPeaks.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class MountainPeaksFixtures {
    static Point fixture(DrainageNetwork d,long seed,int kind){
        var c=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if(kind(seed,x,z,d,c)!=kind)continue;
            for(int radius:new int[]{0,128,256,512})for(int dz=-radius;dz<=radius;dz+=128)for(int dx=-radius;dx<=radius;dx+=128){
                if(radius>0&&Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                int wx=x+dx,wz=z+dz;if(kind(seed,wx,wz,d,c)!=kind||interiorCount(seed,wx,wz,kind,d,c)<60)continue;
                boolean inner=true;for(int oz=-8;oz<=8;oz+=4)for(int ox=-8;ox<=8;ox+=4)if(kind(seed,wx+ox,wz+oz,d,c)!=kind)inner=false;
                if(inner)return new Point(wx,wz);
            }
        }
        throw new AssertionError("No natural summit interior seed="+seed+" kind="+kind);
    }

    public static View view(int x,int y,int z,boolean elevated,java.util.function.IntBinaryOperator ground){
        double gx=ground.applyAsInt(x+32,z)-ground.applyAsInt(x-32,z),gz=ground.applyAsInt(x,z+32)-ground.applyAsInt(x,z-32),length=Math.hypot(gx,gz);
        if(length<.01){gx=1;gz=0;length=1;}
        double cx,cy,cz,tx,ty,tz;
        if(elevated){cx=x-gx/length*90+.5;cz=z-gz/length*90+.5;cy=Math.max(y,ground.applyAsInt((int)Math.floor(cx),(int)Math.floor(cz)))+65;tx=x+gx/length*32+.5;tz=z+gz/length*32+.5;ty=ground.applyAsInt((int)Math.floor(tx),(int)Math.floor(tz))+.5;}
        else{cx=x+.5;cz=z+.5;cy=y+2;tx=x-gx/length*64+.5;tz=z-gz/length*64+.5;ty=ground.applyAsInt((int)Math.floor(tx),(int)Math.floor(tz))+.5;}
        return new View(cx,cy,cz,(float)Math.toDegrees(Math.atan2(cx-tx,tz-cz)),(float)-Math.toDegrees(Math.atan2(ty-(cy+1.62),Math.hypot(tx-cx,tz-cz))));
    }
    private MountainPeaksFixtures() {}
}
