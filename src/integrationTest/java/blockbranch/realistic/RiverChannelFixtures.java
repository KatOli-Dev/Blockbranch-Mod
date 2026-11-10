package blockbranch.realistic;

import static blockbranch.realistic.RiverChannel.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class RiverChannelFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var climate=new ClimateField(seed);
        for(int i=0;i<d.size();i++){
            if(!d.river(i)||Math.abs(d.nodeX(i))>1800000||Math.abs(d.nodeZ(i))>1800000)continue;
            for(double t:new double[]{.15,.3,.5,.7,.85}){
                double x=d.edgeX(i,t),z=d.edgeZ(i,t);var center=d.channelAt((int)x,(int)z);if(center.edge()<0||center.gradient()>.0005||center.halfWidth()<24)continue;var temperature=climate.sample((int)x,(int)z,d::sample).meanTemperatureC();if(temperature<4||temperature>27)continue;double dx=d.edgeX(i,t+.001)-d.edgeX(i,t-.001),dz=d.edgeZ(i,t+.001)-d.edgeZ(i,t-.001),length=Math.hypot(dx,dz),width=d.riverHalfWidth(i)*(1-t)+d.riverHalfWidth(d.parent(i))*t;
                for(double side:new double[]{-1,1})for(double lateral=.55;lateral<=.9;lateral+=.025){
                    int wx=(int)Math.round(x-dz/length*width*lateral*side),wz=(int)Math.round(z+dx/length*width*lateral*side);var s=site(d,wx,wz);
                    if(s.depth()<4||cover(seed,wx,wz,contains(wx,wz,d),s,climate.sample(wx,wz,d::sample))<10)continue;
                    boolean interior=true;for(int oz=-4;oz<=4;oz+=4)for(int ox=-4;ox<=4;ox+=4)if(!contains(wx+ox,wz+oz,d))interior=false;
                    if(interior)return new Point(wx,wz);
                }
            }
        }
        throw new AssertionError("No natural sheltered shallow river plant fixture");
    }

    static Transition transition(DrainageNetwork d,long seed){
        var p=fixture(d,seed);
        for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int r=16;r<=16384;r*=2){
            int ax=Math.floorDiv(p.x(),4)*4,az=Math.floorDiv(p.z(),4)*4,bx=ax+step[0]*r,bz=az+step[1]*r;if(contains(bx,bz,d))continue;
            while(Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4){int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;if(contains(mx,mz,d)){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
            return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No routed channel/margin boundary");
    }
    private RiverChannelFixtures() {}
}
