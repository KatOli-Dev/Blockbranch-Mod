package blockbranch.realistic;

import it.unimi.dsi.fastutil.longs.Long2DoubleLinkedOpenHashMap;

/** Attenuates only the inherited bank-scale field, never basin/ridge/trench
 * geometry. Depth 1–3 km, gradient .02–.10 and residual amplitude .10 are
 * generator conventions, not measured thresholds or area/trace-count targets. */
final class DeepBasinRelief {
    private final TerrainShape shape;
    private final Long2DoubleLinkedOpenHashMap corners=new Long2DoubleLinkedOpenHashMap(512);
    DeepBasinRelief(TerrainShape shape){this.shape=shape;}
    static double gradient(TerrainShape shape,int x,int z){
        return Math.hypot((shape.continuousHeight(x+128,z)-shape.continuousHeight(x-128,z))/256,
            (shape.continuousHeight(x,z+128)-shape.continuousHeight(x,z-128))/256);
    }
    static double response(double depth,double gradient){return smooth((depth-1000)/2000)*(1-smooth((gradient-.02)/.08));}
    double strength(int x,int z){
        double depth=TerrainShape.SEA_LEVEL-1-shape.continuousHeight(x,z);
        if(depth<=1000)return 0;
        int gx=Math.floorDiv(x,256),gz=Math.floorDiv(z,256);
        double tx=smooth(Math.floorMod(x,256)/256.),tz=smooth(Math.floorMod(z,256)/256.);
        double a=corner(gx,gz),b=corner(gx+1,gz),c=corner(gx,gz+1),d=corner(gx+1,gz+1);
        return response(depth,0)*((a+(b-a)*tx)*(1-tz)+(c+(d-c)*tx)*tz);
    }
    private double corner(int gx,int gz){
        long key=(gx&0xffffffffL)|((long)gz<<32);
        synchronized(corners){double found=corners.getAndMoveToLast(key);if(found!=0||corners.containsKey(key))return found;}
        // Use scalar responses, not opposing vectors that can cancel at a crest.
        // One cell of rugged-neighbour buffering preserves narrow structural
        // features. Quintic 256 m interpolation has zero first/second derivatives
        // on cell boundaries and bounds response rate to 1.875/256 per axis.
        double response=1;
        for(int dz=-1;dz<=1&&response>0;dz++)for(int dx=-1;dx<=1;dx++)response=Math.min(response,response(3000,gradient(shape,(gx+dx)*256,(gz+dz)*256)));
        synchronized(corners){corners.putAndMoveToLast(key,response);if(corners.size()>4096)corners.removeFirstDouble();}
        return response;
    }
    DrainageNetwork.Sample apply(DrainageNetwork d,int x,int z,DrainageNetwork.Sample old){
        if(old.waterLevel()!=TerrainShape.SEA_LEVEL||old.height()>=TerrainShape.SEA_LEVEL-200||!d.habitatOceanWater(x,z,old))return old;
        double strength=strength(x,z);if(strength==0)return old;
        if(d.terminalLakeWater(x,z,old)||d.channelAt(x,z).edge()>=0)return old;
        // Offshore depth makes the original wet cap exactly 26. Replace only
        // that rounded contribution after all original shaping; climate retains
        // the original detailed reference heights/datums, including remote probes.
        double bank=52*shape.bankFraction(x,z);
        int delta=(int)Math.round(bank*(1-.90*strength))-(int)Math.round(bank);
        if(delta==0)return old;
        return new DrainageNetwork.Sample(old.height()+delta,old.waterLevel(),old.basin(),old.closed(),old.bedrockCoast(),old.regionalHeight(),old.regionalWaterLevel(),old.sandyBackshore());
    }
    private static double smooth(double value){double t=Math.clamp(value,0,1);return t*t*t*(t*(t*6-15)+10);}
}
