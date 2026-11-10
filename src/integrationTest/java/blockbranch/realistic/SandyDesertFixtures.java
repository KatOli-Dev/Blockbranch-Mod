package blockbranch.realistic;

import static blockbranch.realistic.SandyDesert.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class SandyDesertFixtures {
    static Point fixture(DrainageNetwork d,long seed){return fixture(d,seed,Integer.MIN_VALUE);}

    static Point fixture(DrainageNetwork d,long seed,int minGround){return fixture(d,seed,minGround,Integer.MAX_VALUE);}

    static Point fixture(DrainageNetwork d,long seed,int minGround,int maxGround){
        var c=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if((d.sample(x,z).height()<minGround||d.sample(x,z).height()>maxGround)||!contains(seed,x,z,d,c))continue;
            for(int dz=-64;dz<=64;dz++)for(int dx=-64;dx<=64;dx++){int wx=x+dx,wz=z+dz;if(d.sample(wx,wz).height()>=minGround&&d.sample(wx,wz).height()<=maxGround&&contains(seed,wx,wz,d,c)&&cover(seed,wx,wz,true,d)==10)return new Point(wx,wz);}
        }
        throw new AssertionError("No natural sandy Desert cactus fixture");
    }
    private SandyDesertFixtures() {}
}
