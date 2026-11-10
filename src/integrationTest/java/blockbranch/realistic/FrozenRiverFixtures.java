package blockbranch.realistic;
import java.util.*;
import static blockbranch.realistic.FrozenRiver.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class FrozenRiverFixtures {
    static Point fixture(DrainageNetwork d,long seed){var c=new ClimateField(seed);for(int i=0;i<d.size();i++){if(!d.river(i)||Math.abs(d.nodeX(i))>1700000||Math.abs(d.nodeZ(i))>1700000)continue;for(double t:new double[]{.2,.5,.8}){int x=Math.floorDiv((int)Math.round(d.edgeX(i,t)),4)*4,z=Math.floorDiv((int)Math.round(d.edgeZ(i,t)),4)*4;if(contains(x,z,d,c))return new Point(x,z);}}throw new AssertionError("No natural cold routed ice fixture");}
    private FrozenRiverFixtures() {}
}
