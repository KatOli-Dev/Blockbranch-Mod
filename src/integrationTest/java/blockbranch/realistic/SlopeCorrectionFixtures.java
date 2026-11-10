package blockbranch.realistic;

import static blockbranch.realistic.SlopeCorrection.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class SlopeCorrectionFixtures {
    public static Point fixture(Kind kind,DrainageNetwork d,long seed){
        var c=new ClimateField(seed);
        for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
            if(!added(kind,seed,x,z,d,c))continue;
            if(kind==Kind.BOREAL && d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL<=1800)continue; // Exercise newly admitted upland canopy in native fixtures.
            if(kind==Kind.MEADOW){
                if(!OakBirchWoodland.climateSuitable(c.sample(x,z,d::sample))||WoodlandTrees.roots(seed,x-64,z-64,x+64,z+64,d,c).size()<3)continue;
            }
            if(kind==Kind.BOREAL){
                for(var root:BorealTrees.roots(seed,x-24,z-24,x+24,z+24,d,c))if(!root.birch()&&added(kind,seed,root.x(),root.z(),d,c))return new Point(root.x(),root.z());
                continue;
            }
            return new Point(x,z);
        }
        throw new AssertionError("No corrected terrain witness "+kind);
    }
    private SlopeCorrectionFixtures() {}
}
