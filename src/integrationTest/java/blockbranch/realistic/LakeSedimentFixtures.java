package blockbranch.realistic;

import static blockbranch.realistic.LakeSediment.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class LakeSedimentFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);
        for(int i=0;i<d.size();i++){
            if(d.parent(i)!=-1||!d.closed(i)||Math.abs(d.nodeX(i))>1800000||Math.abs(d.nodeZ(i))>1800000)continue;
            for(int angle=0;angle<48;angle++){
                double a=angle*Math.PI/24,radius=d.lakeShoreRadius(i,a);
                for(double r=radius*.3;r<radius;r+=2){
                    int x=d.nodeX(i)+(int)(Math.cos(a)*r),z=d.nodeZ(i)+(int)(Math.sin(a)*r);
                    var site=site(d,x,z);if(site.depth()>=4&&cover(seed,x,z,contains(x,z,d),site,c.sample(x,z,d::sample))>=10)return new Point(x,z);
                }
            }
        }
        throw new AssertionError("No shallow lit inland-lake plant fixture");
    }

    static Transition transition(DrainageNetwork d,long seed){
        var start=fixture(d,seed);
        for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int r=16;r<=16384;r*=2){
            int ax=Math.floorDiv(start.x(),4)*4,az=Math.floorDiv(start.z(),4)*4,bx=ax+step[0]*r,bz=az+step[1]*r;if(contains(bx,bz,d))continue;
            for(int n=0;n<32&&Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4;n++){
                int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;
                if(contains(mx,mz,d)){ax=mx;az=mz;}else{bx=mx;bz=mz;}
            }
            return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No lake/reed/dry transition");
    }
    private LakeSedimentFixtures() {}
}
