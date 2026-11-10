package blockbranch.realistic;

import it.unimi.dsi.fastutil.longs.Long2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/** Continuous strength from the original landform slope, never from its own modified surface. */
final class TerrainRelief {
    private record Gradient(double x,double z) {}
    @FunctionalInterface interface SampleReader {DrainageNetwork.Sample sample(int x,int z);}
    private final SampleReader structural;
    private final Long2DoubleLinkedOpenHashMap bankFractions=new Long2DoubleLinkedOpenHashMap(512);
    private final Long2DoubleLinkedOpenHashMap offsets=new Long2DoubleLinkedOpenHashMap(512);
    private final Long2ObjectLinkedOpenHashMap<Gradient> gradients=new Long2ObjectLinkedOpenHashMap<>(512);
    TerrainRelief(SampleReader structural){this.structural=structural;}
    /** Repeated biome/material probes reuse exact offsets; bounded, per-world and order independent. */
    double offset(int x,int z,TerrainShape shape){
        long key=(x&0xffffffffL)|((long)z<<32);
        synchronized(offsets){double found=offsets.getAndMoveToLast(key);if(found!=0||offsets.containsKey(key))return found;}
        double value=shape.surfaceReliefStrength(x,z,strength(x,z));
        synchronized(offsets){offsets.putAndMoveToLast(key,value);if(offsets.size()>16384)offsets.removeFirstDouble();}return value;
    }
    double bankFraction(int x,int z,TerrainShape shape){
        long key=(x&0xffffffffL)|((long)z<<32);
        synchronized(bankFractions){double found=bankFractions.getAndMoveToLast(key);if(found!=0||bankFractions.containsKey(key))return found;}
        double value=shape.bankFraction(x,z);
        synchronized(bankFractions){bankFractions.putAndMoveToLast(key,value);if(bankFractions.size()>16384)bankFractions.removeFirstDouble();}return value;
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
    private static double response(Gradient g){double t=Math.clamp((PlanarMath.length(g.x,g.z)-.35)/.20,0,1);return t*t*(3-2*t);}
    double slope(int x,int z){
        int gx=Math.floorDiv(x,64),gz=Math.floorDiv(z,64);
        double tx=smooth(Math.floorMod(x,64)/64.),tz=smooth(Math.floorMod(z,64)/64.);
        var a=corner(gx,gz);var b=corner(gx+1,gz);var c=corner(gx,gz+1);var d=corner(gx+1,gz+1);
        return PlanarMath.length(blend(a.x,b.x,c.x,d.x,tx,tz),blend(a.z,b.z,c.z,d.z,tx,tz));
    }
    private Gradient corner(int gx,int gz){
        long key=(gx&0xffffffffL)|((long)gz<<32);
        synchronized(gradients){var found=gradients.getAndMoveToLast(key);if(found!=null)return found;}
        int x=gx*64,z=gz*64;
        var value=new Gradient((structural.sample(x+16,z).height()-structural.sample(x-16,z).height())/32.,(structural.sample(x,z+16).height()-structural.sample(x,z-16).height())/32.);
        synchronized(gradients){gradients.putAndMoveToLast(key,value);if(gradients.size()>4096)gradients.removeFirst();}return value;
    }
    private static double smooth(double t){return t*t*t*(t*(t*6-15)+10);}
    private static double blend(double a,double b,double c,double d,double x,double z){return (a+(b-a)*x)*(1-z)+(c+(d-c)*x)*z;}
}
