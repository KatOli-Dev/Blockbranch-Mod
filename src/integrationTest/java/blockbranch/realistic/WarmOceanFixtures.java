package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.WarmOcean.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class WarmOceanFixtures {
    static Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);
        for(int i=0;i<d.size()-DrainageNetwork.SIDE;i++)for(int step:new int[]{1,DrainageNetwork.SIDE}){
            if(step==1&&i%DrainageNetwork.SIDE==DrainageNetwork.SIDE-1)continue;int j=i+step;
            if((d.nodeGround(i)<56)==(d.nodeGround(j)<56))continue;int ax=d.nodeX(i),az=d.nodeZ(i),bx=d.nodeX(j),bz=d.nodeZ(j);
            if(Math.abs(ax)>1700000||Math.abs(az)>1700000)continue;boolean wet=d.oceanWater(ax,az,d.sample(ax,az))&&d.sample(ax,az).height()<56;
            if(wet==(d.oceanWater(bx,bz,d.sample(bx,bz))&&d.sample(bx,bz).height()<56))continue;
            for(int n=0;n<16;n++){int x=Math.floorDiv(ax+bx,2),z=Math.floorDiv(az+bz,2);if((d.oceanWater(x,z,d.sample(x,z))&&d.sample(x,z).height()<56)==wet){ax=x;az=z;}else{bx=x;bz=z;}}
            int px=wet?ax:bx,pz=wet?az:bz;if(!climate(c.sample(px,pz,d::sample)))continue;
            for(int dz=-64;dz<=64;dz+=2)for(int dx=-64;dx<=64;dx+=2){int x=px+dx,z=pz+dz;if(coralHeight(seed,x,z,d,c)<1||MarineShelf.site(d,x,z).depth()<7||MarineShelf.site(d,x,z).depth()>9)continue;boolean inside=true;for(int oz=-16;oz<=16;oz+=4)for(int ox=-16;ox<=16;ox+=4)inside&=contains(seed,x+ox,z+oz,d,c);if(inside)return new Point(x,z);}
        }throw new AssertionError("No natural warm coral shelf seed="+seed);
    }
    private WarmOceanFixtures() {}
}
