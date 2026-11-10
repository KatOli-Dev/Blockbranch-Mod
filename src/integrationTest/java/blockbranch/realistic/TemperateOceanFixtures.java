package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.TemperateOcean.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class TemperateOceanFixtures {
    static Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);
        for(int i=0;i<d.size()-DrainageNetwork.SIDE;i++){if(Math.abs(d.nodeX(i))>1700000||Math.abs(d.nodeZ(i))>1700000)continue;for(int step:new int[]{1,DrainageNetwork.SIDE}){if(step==1&&i%DrainageNetwork.SIDE==DrainageNetwork.SIDE-1)continue;int n=i+step;if((d.nodeGround(i)<56)==(d.nodeGround(n)<56))continue;int ax=d.nodeX(i),az=d.nodeZ(i),bx=d.nodeX(n),bz=d.nodeZ(n);boolean wet=d.oceanWater(ax,az,d.sample(ax,az))&&d.sample(ax,az).height()<56;if(wet==(d.oceanWater(bx,bz,d.sample(bx,bz))&&d.sample(bx,bz).height()<56))continue;
            for(int k=0;k<16;k++){int x=Math.floorDiv(ax+bx,2),z=Math.floorDiv(az+bz,2);var s=d.sample(x,z);if((d.oceanWater(x,z,s)&&s.height()<56)==wet){ax=x;az=z;}else{bx=x;bz=z;}}
            int x=wet?ax:bx,z=wet?az:bz;if(!climate(c.sample(x,z,d::sample)))continue;for(int oz=-96;oz<=96;oz+=2)for(int ox=-96;ox<=96;ox+=2)if(kelpHeight(seed,x+ox,z+oz,d,c)>=4)return new Point(x+ox,z+oz);
        }}throw new AssertionError("No natural temperate rocky kelp shelf");
    }
    private TemperateOceanFixtures() {}
}
