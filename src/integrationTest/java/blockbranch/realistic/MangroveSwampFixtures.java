package blockbranch.realistic;

import static blockbranch.realistic.MangroveSwamp.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class MangroveSwampFixtures {
    /** Coast crossings searched deterministically;second site belongs to a separate coastline. */
    public static Point fixture(DrainageNetwork d,long seed,boolean second){var c=new ClimateField(seed);int accepted=0;
        for(int i=0;i<d.size()-DrainageNetwork.SIDE;i++){
            if(Math.abs(d.nodeX(i))>1700000||Math.abs(d.nodeZ(i))>1700000)continue;
            for(int step:new int[]{1,DrainageNetwork.SIDE}){if(step==1&&i%DrainageNetwork.SIDE==DrainageNetwork.SIDE-1)continue;int j=i+step;if((d.nodeGround(i)<63)==(d.nodeGround(j)<63))continue;
                int ax=d.nodeX(i),az=d.nodeZ(i),bx=d.nodeX(j),bz=d.nodeZ(j);boolean aw=d.oceanWater(ax,az,d.sample(ax,az)),bw=d.oceanWater(bx,bz,d.sample(bx,bz));if(aw==bw||!climateSuitable(c.sample((ax+bx)/2,(az+bz)/2,d::sample)))continue;
                for(int k=0;k<14;k++){int mx=Math.floorDiv(ax+bx,2),mz=Math.floorDiv(az+bz,2);if(d.oceanWater(mx,mz,d.sample(mx,mz))==aw){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
                int x=aw?ax:bx,z=aw?az:bz;if(!contains(seed,x,z,d,c))continue;var roots=MangroveTrees.roots(seed,x-32,z-32,x+32,z+32,d,c);if(roots.size()<5)continue;
                if(second&&accepted++<3)continue;for(var r:roots)if(r.height()>=12)return new Point(r.x(),r.z());
            }
        }throw new AssertionError("No natural sheltered Mangrove stand");
    }
    private MangroveSwampFixtures() {}
}
