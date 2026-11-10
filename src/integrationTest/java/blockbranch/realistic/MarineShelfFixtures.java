package blockbranch.realistic;

import static blockbranch.realistic.MarineShelf.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class MarineShelfFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var climate=new ClimateField(seed);
        for(int i=0;i<d.size()-DrainageNetwork.SIDE;i++){
            if(Math.abs(d.nodeX(i))>1700000||Math.abs(d.nodeZ(i))>1700000)continue;
            for(int step:new int[]{1,DrainageNetwork.SIDE}){
                int n=i+step;if(step==1&&i%DrainageNetwork.SIDE==DrainageNetwork.SIDE-1)continue;
                if((d.nodeGround(i)<57)==(d.nodeGround(n)<57))continue;
                int ax=d.nodeX(i),az=d.nodeZ(i),bx=d.nodeX(n),bz=d.nodeZ(n);var as=d.sample(ax,az);var bs=d.sample(bx,bz);
                boolean aw=d.oceanWater(ax,az,as)&&as.height()<57,bw=d.oceanWater(bx,bz,bs)&&bs.height()<57;if(aw==bw)continue;
                for(int j=0;j<16;j++){int mx=Math.floorDiv(ax+bx,2),mz=Math.floorDiv(az+bz,2);var s=d.sample(mx,mz);if((d.oceanWater(mx,mz,s)&&s.height()<57)==aw){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
                int cx=aw?ax:bx,cz=aw?az:bz;double temp=climate.sample(cx,cz,d::sample).meanTemperatureC();if(temp<4||temp>27||TemperateOcean.climate(climate.sample(cx,cz,d::sample)))continue;
                for(int oz=-64;oz<=64;oz+=2)for(int ox=-64;ox<=64;ox+=2){int x=cx+ox,z=cz+oz;var s=site(d,x,z);if(s.depth()!=7||cover(seed,x,z,contains(x,z,d),s,climate.sample(x,z,d::sample))!=12)continue;
                    boolean interior=true;for(int dz=-4;dz<=4;dz+=4)for(int dx=-4;dx<=4;dx+=4)if(!contains(x+dx,z+dz,d))interior=false;
                    if(interior)return new Point(x,z);
                }
            }
        }
        throw new AssertionError("No natural gentle sandy shelf seagrass fixture");
    }

    static Transition transition(DrainageNetwork d,long seed){
        var p=fixture(d,seed);
        for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int r=16;r<=16384;r*=2){int ax=Math.floorDiv(p.x(),4)*4,az=Math.floorDiv(p.z(),4)*4,bx=ax+step[0]*r,bz=az+step[1]*r;if(contains(bx,bz,d))continue;
            while(Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4){int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;if(contains(mx,mz,d)){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
            return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No marine shelf/shore/deep boundary");
    }

    static Transition deepTransition(DrainageNetwork d,long seed){
        var p=fixture(d,seed);
        for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int r=16;r<=65536;r*=2){
            int ax=Math.floorDiv(p.x(),4)*4,az=Math.floorDiv(p.z(),4)*4,bx=ax+step[0]*r,bz=az+step[1]*r;var end=site(d,bx,bz);if(!end.ocean()||end.depth()<=200)continue;
            while(Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4){int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;if(eligible(site(d,mx,mz))){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
            if(contains(ax,az,d)&&site(d,bx,bz).ocean()&&site(d,bx,bz).depth()>200)return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No actual adjacent 200 m shelf/deep boundary");
    }
    private MarineShelfFixtures() {}
}
