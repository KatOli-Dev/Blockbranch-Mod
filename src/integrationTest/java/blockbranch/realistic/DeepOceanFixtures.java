package blockbranch.realistic;

import static blockbranch.realistic.DeepOcean.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class DeepOceanFixtures {
    static Point fixture(DrainageNetwork d,long seed){
        var climate=new ClimateField(seed);
        for(int i=0;i<d.size();i++){
            int x=d.nodeX(i),z=d.nodeZ(i);if(Math.abs(x)>1700000||Math.abs(z)>1700000||TemperateDeepOcean.contains(x,z,d,climate))continue;var s=site(d,x,z);
            if(s.depth()<2000||s.depth()>6500||s.slope()>.04||!contains(x,z,d))continue;
            int code=cover(seed,x,z,true,s);boolean contrast=false;
            for(int[] step:new int[][]{{32,0},{-32,0},{0,32},{0,-32},{64,0},{-64,0},{0,64},{0,-64}}){var n=site(d,x+step[0],z+step[1]);int c=cover(seed,x+step[0],z+step[1],contains(x+step[0],z+step[1],d),n);if(c>=5&&c<=6&&c!=code)contrast=true;}
            if(contrast)return new Point(x,z);
        }
        throw new AssertionError("No natural deep sediment mosaic fixture");
    }

    public static MarineShelf.Transition transition(DrainageNetwork d,long seed){return blockbranch.realistic.MarineShelfFixtures.deepTransition(d,seed);}
    private DeepOceanFixtures() {}
}
