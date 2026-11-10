package blockbranch.realistic;

import static blockbranch.realistic.ColdTundra.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class ColdTundraFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var c=new ClimateField(seed);var ecology=new GroundEcology.Field(d::sample,c);
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if(!retained(x,z,d,c))continue;
            for(int dz=-32;dz<=32;dz++)for(int dx=-32;dx<=32;dx++){
                int wx=x+dx,wz=z+dz;if(!retained(wx,wz,d,c)||cover(seed,wx,wz,ecology.sample(wx,wz))!=11)continue;
                boolean interior=true;for(int oz=-16;oz<=16;oz+=8)for(int ox=-16;ox<=16;ox+=8)if(!retained(wx+ox,wz+oz,d,c))interior=false;
                if(interior)return new Point(wx,wz);
            }
        }
        throw new AssertionError("No cold dwarf-shrub tundra found");
    }

    static Transition transition(DrainageNetwork d,long seed,boolean climateOnly){
        var start=fixture(d,seed);var c=new ClimateField(seed);java.util.function.BiPredicate<Integer,Integer> selected=climateOnly?(x,z)->climateSuitable(c.sample(x,z,d::sample)):(x,z)->contains(x,z,d,c);
        for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}})for(int r=256;r<=524288;r*=2){
            int bx=start.x()+step[0]*r,bz=start.z()+step[1]*r;if(Math.abs(bx)>2000000||Math.abs(bz)>2000000||selected.test(bx,bz))continue;int ax=start.x(),az=start.z();
            for(int i=0;i<32&&Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4;i++){
                int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;if(selected.test(mx,mz)){ax=mx;az=mz;}else{bx=mx;bz=mz;}
            }
            return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No cold tundra transition");
    }
    private ColdTundraFixtures() {}
}
