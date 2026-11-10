package blockbranch.realistic;

import static blockbranch.realistic.StonyShore.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class StonyShoreFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        for(int i=0;i<d.size()-DrainageNetwork.SIDE;i++)for(int step:new int[]{1,DrainageNetwork.SIDE}){
            if(step==1&&i%DrainageNetwork.SIDE==DrainageNetwork.SIDE-1)continue;int j=i+step;
            if(Math.abs(d.nodeX(i))>1700000||Math.abs(d.nodeZ(i))>1700000||(d.nodeGround(i)<63)==(d.nodeGround(j)<63))continue;
            int ax=d.nodeX(i),az=d.nodeZ(i),bx=d.nodeX(j),bz=d.nodeZ(j);boolean wet=d.oceanWater(ax,az,d.sample(ax,az));
            if(wet==d.oceanWater(bx,bz,d.sample(bx,bz)))continue;
            for(int n=0;n<16;n++){int x=Math.floorDiv(ax+bx,2),z=Math.floorDiv(az+bz,2);if(d.oceanWater(x,z,d.sample(x,z))==wet){ax=x;az=z;}else{bx=x;bz=z;}}
            int px=wet?bx:ax,pz=wet?bz:az;
            for(int dz=-64;dz<=64;dz+=8)for(int dx=-64;dx<=64;dx+=8){int x=px+dx,z=pz+dz;var s=d.sample(x,z);
                if(!contains(x,z,d)||!surfaceContains(x,z,d)||s.waterLevel()!=DrainageNetwork.DRY||s.height()-s.regionalHeight()<18||SlopeCorrection.slope(d,x,z)<.45)continue;
                int interior=0;for(int oz=-32;oz<=32;oz+=8)for(int ox=-32;ox<=32;ox+=8)if(contains(x+ox,z+oz,d))interior++;
                if(interior>=50&&view(x,s.height(),z,false,(wx,wz)->d.sample(wx,wz).height())!=null)return new Point(x,z);
            }
        }
        throw new AssertionError("No natural formed Stony Shore seed="+seed);
    }

    public static MountainPeaks.View view(int x,int y,int z,boolean elevated,java.util.function.IntBinaryOperator ground){
        double dx=ground.applyAsInt(x+32,z)-ground.applyAsInt(x-32,z),dz=ground.applyAsInt(x,z+32)-ground.applyAsInt(x,z-32),length=Math.hypot(dx,dz);
        if(length<1)throw new IllegalArgumentException("Stony Shore camera requires actual cliff relief");dx/=length;dz/=length;
        // A water-side viewpoint includes the shoreline silhouette and cove,
        // rather than standing on the rise and filling the frame with rock.
        double cx=0,cz=0;boolean found=false;
        search:for(int side:new int[]{1,-1})for(int along:new int[]{48,80,112})for(int back:new int[]{64,96,128}){
            double px=x-dx*back-dz*along*side+.5,pz=z-dz*back+dx*along*side+.5;
            int h=ground.applyAsInt((int)Math.floor(px),(int)Math.floor(pz));
            if(h!=Integer.MIN_VALUE&&h<TerrainShape.SEA_LEVEL-1){cx=px;cz=pz;found=true;break search;}
        }
        if(!found)return null;
        double cy=TerrainShape.SEA_LEVEL+(elevated?85:2);
        double yaw=Math.toDegrees(Math.atan2(cx-x-.5,z+.5-cz)),pitch=-Math.toDegrees(Math.atan2(y+4-(cy+1.62),Math.hypot(x+.5-cx,z+.5-cz)));
        return new MountainPeaks.View(cx,cy,cz,(float)yaw,(float)pitch);
    }
    private StonyShoreFixtures() {}
}
