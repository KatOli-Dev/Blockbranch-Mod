package blockbranch.realistic;

import java.util.function.BiFunction;

/** Conservative topographic support proxy for newly admitted slopes, not an erosion or soil simulation. */
public final class SlopeStability {
    public record Site(int height,boolean eligible) {}
    public static boolean supportsSoil(int x,int z,int height,double dx,double dz,BiFunction<Integer,Integer,Site> terrain){
        for(int oz=-8;oz<=8;oz+=8)for(int ox=-8;ox<=8;ox+=8){
            if(ox==0&&oz==0)continue;
            var nearby=terrain.apply(x+ox,z+oz);double rise=nearby.height()-height;
            // A 16 m footprint must remain dry and avoid local scarps or abrupt convex/concave breaks.
            // The 1.5 m plane residual allows integer-height quantization on an otherwise smooth slope.
            if(!nearby.eligible()||Math.abs(rise)/Math.hypot(ox,oz)>0.45||Math.abs(rise-dx*ox-dz*oz)>1.5)return false;
        }
        return true;
    }
    private SlopeStability(){}
}
