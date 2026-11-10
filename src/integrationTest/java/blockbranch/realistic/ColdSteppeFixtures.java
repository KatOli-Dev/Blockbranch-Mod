package blockbranch.realistic;

import static blockbranch.realistic.ColdSteppe.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class ColdSteppeFixtures {
    static Point fixture(DrainageNetwork drainage,long seed){
        var climate=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if(!contains(x,z,drainage,climate))continue;
            for(int dz=-32;dz<=32;dz+=4)for(int dx=-32;dx<=32;dx+=4){
                int wx=x+dx,wz=z+dz;if(cover(seed,wx,wz,contains(wx,wz,drainage,climate),true)!=4)continue;
                boolean interior=true;for(int oz=-16;oz<=16;oz+=8)for(int ox=-16;ox<=16;ox+=8)if(!contains(wx+ox,wz+oz,drainage,climate))interior=false;
                if(interior)return new Point(wx,wz);
            }
        }
        throw new AssertionError("No cold continental fescue steppe found");
    }

    static Transition transition(DrainageNetwork drainage,long seed,boolean climateOnly){
        var p=fixture(drainage,seed);var climate=new ClimateField(seed);java.util.function.BiPredicate<Integer,Integer> selected=climateOnly?(x,z)->climateSuitable(climate.sample(x,z,drainage::sample)):(x,z)->contains(x,z,drainage,climate);
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}})for(int r=256;r<=524288;r*=2){
            int bx=p.x()+d[0]*r,bz=p.z()+d[1]*r;if(Math.abs(bx)>2000000 || Math.abs(bz)>2000000 || selected.test(bx,bz))continue;
            int ax=p.x(),az=p.z();for(int i=0;i<32 && Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4;i++){
                int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;if(selected.test(mx,mz)){ax=mx;az=mz;}else{bx=mx;bz=mz;}
            }
            return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No adjacent cold steppe transition");
    }
    private ColdSteppeFixtures() {}
}
