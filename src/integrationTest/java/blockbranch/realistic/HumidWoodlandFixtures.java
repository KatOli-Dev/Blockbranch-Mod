package blockbranch.realistic;

import static blockbranch.realistic.HumidWoodland.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class HumidWoodlandFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);
        for(int z=-1700000;z<=1700000;z+=16000)for(int x=-1700000;x<=1700000;x+=16000){
            if(!contains(seed,x,z,d,c))continue;
            for(var r:HumidTrees.roots(seed,x-32,z-32,x+32,z+32,d,c))if(r.height()>=19)return new Point(r.x(),r.z());
        }
        throw new AssertionError("No humid subtropical oak-fern woodland found");
    }

    static Transition transition(DrainageNetwork d,long seed,boolean climateOnly){
        var start=fixture(d,seed);var c=new ClimateField(seed);
        java.util.function.BiPredicate<Integer,Integer> selected=climateOnly?(x,z)->climateSuitable(c.sample(x,z,d::sample)):(x,z)->contains(seed,x,z,d,c);
        for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}})for(int r=256;r<=524288;r*=2){
            int bx=start.x()+step[0]*r,bz=start.z()+step[1]*r;if(Math.abs(bx)>1900000||Math.abs(bz)>1900000||selected.test(bx,bz))continue;
            int ax=start.x(),az=start.z();
            for(int i=0;i<32&&Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4;i++){
                int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;
                if(selected.test(mx,mz)){ax=mx;az=mz;}else{bx=mx;bz=mz;}
            }
            return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No humid woodland transition");
    }
    private HumidWoodlandFixtures() {}
}
