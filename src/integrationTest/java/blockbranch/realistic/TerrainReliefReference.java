/** Frozen dd0c2e5 terrain-relief cache implementation; QA only. */
package blockbranch.realistic;

import java.util.LinkedHashMap;
import java.util.function.BiFunction;

/** Continuous strength from the original landform slope, never from its own modified surface. */
final class TerrainReliefReference {
    private record Gradient(double x,double z) {}
    private final BiFunction<Integer,Integer,DrainageNetwork.Sample> structural;
    private final LinkedHashMap<Long,Double> bankFractions=new LinkedHashMap<>(512,.75f,true);
    private final LinkedHashMap<Long,Double> offsets=new LinkedHashMap<>(512,.75f,true);
    private final LinkedHashMap<Long,Gradient> gradients=new LinkedHashMap<>(512,.75f,true);
    TerrainReliefReference(BiFunction<Integer,Integer,DrainageNetwork.Sample> structural){this.structural=structural;}
    /** Repeated biome/material probes reuse exact offsets; bounded, per-world and order independent. */
    double offset(int x,int z,TerrainShape shape){
        long key=(x&0xffffffffL)|((long)z<<32);
        synchronized(offsets){var found=offsets.get(key);if(found!=null)return found;}
        double value=shape.surfaceReliefStrength(x,z,strength(x,z));
        synchronized(offsets){offsets.put(key,value);if(offsets.size()>16384)offsets.remove(offsets.keySet().iterator().next());}return value;
    }
    double bankFraction(int x,int z,TerrainShape shape){
        long key=(x&0xffffffffL)|((long)z<<32);
        synchronized(bankFractions){var found=bankFractions.get(key);if(found!=null)return found;}
        double value=shape.bankFraction(x,z);
        synchronized(bankFractions){bankFractions.put(key,value);if(bankFractions.size()>16384)bankFractions.remove(bankFractions.keySet().iterator().next());}return value;
    }
    /** Interpolate bounded response rather than applying a steep response to
     * interpolated opposing gradients. This bounds its spatial rate of change. */
    double strength(int x,int z){
        int gx=Math.floorDiv(x,64),gz=Math.floorDiv(z,64);
        double tx=smooth(Math.floorMod(x,64)/64.),tz=smooth(Math.floorMod(z,64)/64.);
        return blend(coherentResponse(gx,gz),coherentResponse(gx+1,gz),coherentResponse(gx,gz+1),coherentResponse(gx+1,gz+1),tx,tz);
    }
    // Large new relief belongs to a coherent steep landform. A one-cell
    // buffer keeps the64m response interpolation out of soil-supporting
    // slopes/plateau interiors; their original fine terrain remains intact.
    private double coherentResponse(int gx,int gz){
        double value=response(corner(gx,gz));if(value==0)return 0;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){if(dx==0&&dz==0)continue;value=Math.min(value,response(corner(gx+dx,gz+dz)));if(value==0)return 0;}
        return value;
    }
    private static double response(Gradient g){double t=Math.clamp((Math.hypot(g.x,g.z)-.35)/.20,0,1);return t*t*(3-2*t);}
    double slope(int x,int z){
        int gx=Math.floorDiv(x,64),gz=Math.floorDiv(z,64);
        double tx=smooth(Math.floorMod(x,64)/64.),tz=smooth(Math.floorMod(z,64)/64.);
        var a=corner(gx,gz);var b=corner(gx+1,gz);var c=corner(gx,gz+1);var d=corner(gx+1,gz+1);
        return Math.hypot(blend(a.x,b.x,c.x,d.x,tx,tz),blend(a.z,b.z,c.z,d.z,tx,tz));
    }
    private Gradient corner(int gx,int gz){
        long key=(gx&0xffffffffL)|((long)gz<<32);
        synchronized(gradients){var found=gradients.get(key);if(found!=null)return found;}
        int x=gx*64,z=gz*64;
        var value=new Gradient((structural.apply(x+16,z).height()-structural.apply(x-16,z).height())/32.,(structural.apply(x,z+16).height()-structural.apply(x,z-16).height())/32.);
        synchronized(gradients){gradients.put(key,value);if(gradients.size()>4096)gradients.remove(gradients.keySet().iterator().next());}return value;
    }
    private static double smooth(double t){return t*t*t*(t*(t*6-15)+10);}
    private static double blend(double a,double b,double c,double d,double x,double z){return (a+(b-a)*x)*(1-z)+(c+(d-c)*x)*z;}
}
