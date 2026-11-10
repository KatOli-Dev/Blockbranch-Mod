package blockbranch.realistic;

import static blockbranch.realistic.TemperateDeepOcean.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class TemperateDeepOceanFixtures {
    static DeepOcean.Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);
        for(int i=0;i<d.size();i++){int x=d.nodeX(i),z=d.nodeZ(i);if(Math.abs(x)>1700000||Math.abs(z)>1700000||!contains(x,z,d,c))continue;var s=DeepOcean.site(d,x,z);if(s.depth()<2000||s.depth()>6500||s.slope()>.04)continue;int code=DeepOcean.cover(seed,x,z,true,s);
            for(int[] step:new int[][]{{32,0},{-32,0},{0,32},{0,-32},{64,0},{-64,0},{0,64},{0,-64}}){int wx=x+step[0],wz=z+step[1];var n=DeepOcean.site(d,wx,wz);int next=DeepOcean.cover(seed,wx,wz,DeepOcean.contains(wx,wz,d),n);if(next>=5&&next<=6&&next!=code)return new DeepOcean.Point(x,z);}
        }throw new AssertionError("No temperate deep sediment-mosaic fixture");
    }

    static MarineShelf.Transition transition(DrainageNetwork d,long seed){var p=blockbranch.realistic.TemperateOceanFixtures.fixture(d,seed);var c=new ClimateField(seed);
        for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int radius=16;radius<=65536;radius*=2){int ax=Math.floorDiv(p.x(),4)*4,az=Math.floorDiv(p.z(),4)*4,bx=ax+step[0]*radius,bz=az+step[1]*radius;if(!contains(bx,bz,d,c))continue;
            while(Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4){int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;if(MarineShelf.eligible(MarineShelf.site(d,mx,mz))){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
            if(TemperateOcean.contains(seed,ax,az,d,c)&&contains(bx,bz,d,c))return new MarineShelf.Transition(ax,az,bx,bz);
        }throw new AssertionError("No adjacent temperate Ocean/Deep Ocean boundary");
    }
    private TemperateDeepOceanFixtures() {}
}
