package blockbranch.realistic;
import java.util.function.BiFunction;
import static blockbranch.realistic.DrySteppe.*;

/** Development-only fixture selection and camera helpers; no production registration. */
public final class DrySteppeFixtures {
    static SteppePoint fixture(DrainageNetwork drainage,long seed) {
        var climate=new ClimateField(seed);
        for(int z=-1700000;z<=1700000;z+=2000) for(int x=-1700000;x<=1700000;x+=2000) {
            int elevation=drainage.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;
            if(elevation<30 || elevation>1400)continue; // Keep the original regression witness.
            if(!contains(x,z,drainage,climate)) continue;
            for(int oz=-32;oz<=32;oz+=4) for(int ox=-32;ox<=32;ox+=4) {
                int wx=x+ox,wz=z+oz;
                if(cover(seed,wx,wz,contains(wx,wz,drainage,climate),true)!=4) continue;
                boolean interior=true;
                for(int dz=-4;dz<=4;dz+=4) for(int dx=-4;dx<=4;dx+=4) if(!contains(wx+dx,wz+dz,drainage,climate)) interior=false;
                if(interior) return new SteppePoint(wx,wz);
            }
        }
        throw new AssertionError("No dry continental scrub-steppe found");
    }

    static Transition transition(DrainageNetwork drainage,long seed,boolean climateOnly) {
        var start=fixture(drainage,seed);var climate=new ClimateField(seed);
        java.util.function.BiPredicate<Integer,Integer> selected=climateOnly
            ? (x,z)->climateSuitable(climate.sample(x,z,drainage::sample)) : (x,z)->contains(x,z,drainage,climate);
        for(int[] direction:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,1},{1,-1},{-1,-1}}) for(int radius=256;radius<=524288;radius*=2) {
            int bx=start.x()+direction[0]*radius,bz=start.z()+direction[1]*radius;
            if(Math.abs(bx)>1900000 || Math.abs(bz)>1900000 || selected.test(bx,bz)) continue;
            int ax=start.x(),az=start.z();
            for(int iteration=0;iteration<32 && Math.max(Math.abs(ax-bx),Math.abs(az-bz))>4;iteration++) {
                int mx=Math.floorDiv(Math.floorDiv(ax+bx,2),4)*4,mz=Math.floorDiv(Math.floorDiv(az+bz,2),4)*4;
                if(selected.test(mx,mz)) { ax=mx;az=mz; } else { bx=mx;bz=mz; }
            }
            return new Transition(ax,az,bx,bz);
        }
        throw new AssertionError("No steppe transition found");
    }
    private DrySteppeFixtures() {}
}
