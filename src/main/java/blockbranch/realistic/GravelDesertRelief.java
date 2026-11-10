package blockbranch.realistic;

import java.util.LinkedHashMap;

/** Shallow, static undulations on buffered gravel-desert interiors. No drainage simulation. */
final class GravelDesertRelief {
    static final int MAX_OFFSET=3;
    private final long seed;
    private final DrainageNetwork reference;
    private final ClimateField climate;
    private final LinkedHashMap<Long,Double> interiors=new LinkedHashMap<>(512,.75f,true);
    private record Tile(float[] clearance,int[] waterLevels) {}
    private final LinkedHashMap<Long,Tile> tiles=new LinkedHashMap<>(256,.75f,true);
    private final long[] eligibilityKeys=new long[1<<17];
    private final byte[] eligibilityFlags=new byte[eligibilityKeys.length];
    GravelDesertRelief(long seed,DrainageNetwork reference){this.seed=seed;this.reference=reference;climate=new ClimateField(seed);}
    private double interior(int gx,int gz){
        long key=(gx&0xffffffffL)|((long)gz<<32);
        synchronized(interiors){var found=interiors.get(key);if(found!=null)return found;}
        double value=1;
        for(int dz=-1;dz<=1&&value>0;dz++)for(int dx=-1;dx<=1;dx++){
            int x=(gx+dx)*64,z=(gz+dz)*64;var old=reference.sample(x,z);
            double sx=(reference.sample(x+32,z).height()-reference.sample(x-32,z).height())/64.;
            double sz=(reference.sample(x,z+32).height()-reference.sample(x,z-32).height())/64.;
            if(old.waterLevel()!=DrainageNetwork.DRY||old.bedrockCoast()||old.height()<TerrainShape.SEA_LEVEL+32||Math.hypot(sx,sz)>.14
                ||reference.channelAt(x,z).edge()>=0||!GravelDesert.surfaceContains(x,z,reference,climate)){value=0;break;}
        }
        synchronized(interiors){interiors.put(key,value);if(interiors.size()>4096)interiors.remove(interiors.keySet().iterator().next());}return value;
    }
    double envelope(int x,int z){
        var old=reference.sample(x,z);
        if(old.waterLevel()!=DrainageNetwork.DRY||old.bedrockCoast()||old.height()<TerrainShape.SEA_LEVEL+32)return 0;
        int gx=Math.floorDiv(x,64),gz=Math.floorDiv(z,64);
        double tx=fade(Math.floorMod(x,64)/64.),tz=fade(Math.floorMod(z,64)/64.);
        double a=interior(gx,gz)*(1-tx)+interior(gx+1,gz)*tx,b=interior(gx,gz+1)*(1-tx)+interior(gx+1,gz+1)*tx;
        double support=a*(1-tz)+b*tz;
        if(support==0)return 0;
        var tile=tile(Math.floorDiv(x,16),Math.floorDiv(z,16));
        double slope=SlopeCorrection.slope(reference,x,z);
        return support*tile.clearance[(z&15)*16+(x&15)]*(1-fade(Math.clamp((slope-.08)/.06,0,1)));
    }
    private Tile tile(int tx,int tz){
        long key=(tx&0xffffffffL)|((long)tz<<32);
        synchronized(tiles){var found=tiles.get(key);if(found!=null)return found;}
        // Read every reference column, including an eight-block halo. Tiny wet or
        // specialist pockets must taper relief even when the broad grid misses them.
        byte[] distance=new byte[32*32];int[] water=new int[32*32];boolean hasWater=false;
        for(int z=0;z<32;z++)for(int x=0;x<32;x++){
            int wx=tx*16+x-8,wz=tz*16+z-8;var old=reference.sample(wx,wz);
            water[z*32+x]=old.waterLevel();hasWater|=old.waterLevel()!=DrainageNetwork.DRY;
            distance[z*32+x]=(byte)(eligible(wx,wz,old)?8:0);
        }
        clearance(distance);
        float[] weights=new float[256];int[] nearbyWater=hasWater?centeredWaterLevels(water):new int[256];
        if(!hasWater)java.util.Arrays.fill(nearbyWater,DrainageNetwork.DRY);
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            weights[z*16+x]=(float)fade(distance[(z+8)*32+x+8]/8.);
        }
        var built=new Tile(weights,nearbyWater);
        synchronized(tiles){tiles.put(key,built);if(tiles.size()>256)tiles.remove(tiles.keySet().iterator().next());}return built;
    }
    private boolean eligible(int x,int z,DrainageNetwork.Sample old){
        long key=(x&0xffffffffL)|((long)z<<32);int slot=(int)OakBirchWoodland.mix(key)&(eligibilityFlags.length-1);
        synchronized(eligibilityFlags){if(eligibilityFlags[slot]!=0&&eligibilityKeys[slot]==key)return eligibilityFlags[slot]==1;}
        boolean value=old.waterLevel()==DrainageNetwork.DRY&&!old.bedrockCoast()&&old.height()>=TerrainShape.SEA_LEVEL+32
            &&GravelDesert.surfaceContains(x,z,reference,climate)&&Badlands.surfaceKind(seed,x,z,reference,climate)==0;
        // The reference is immutable. Cache exact true/false results, including halo
        // overlap; eviction and concurrent duplicate misses can affect only cost.
        synchronized(eligibilityFlags){eligibilityKeys[slot]=key;eligibilityFlags[slot]=(byte)(value?1:2);}return value;
    }
    static int[] centeredWaterLevels(int[] water){
        int[] levels=new int[256];java.util.Arrays.fill(levels,DrainageNetwork.DRY);
        // Every output column uses the same centered radius, including at tile edges.
        for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int wz=z;wz<=z+16;wz++)for(int wx=x;wx<=x+16;wx++)levels[z*16+x]=Math.max(levels[z*16+x],water[wz*32+wx]);
        return levels;
    }
    /** Capped Chebyshev clearance, with a complete halo so tile order cannot affect output. */
    static void clearance(byte[] distance){
        for(int z=0;z<32;z++)for(int x=0;x<32;x++){
            int i=z*32+x,v=distance[i];
            if(x>0)v=Math.min(v,1+distance[i-1]);
            if(z>0){v=Math.min(v,1+distance[i-32]);if(x>0)v=Math.min(v,1+distance[i-33]);if(x<31)v=Math.min(v,1+distance[i-31]);}
            distance[i]=(byte)v;
        }
        for(int z=31;z>=0;z--)for(int x=31;x>=0;x--){
            int i=z*32+x,v=distance[i];
            if(x<31)v=Math.min(v,1+distance[i+1]);
            if(z<31){v=Math.min(v,1+distance[i+32]);if(x>0)v=Math.min(v,1+distance[i+31]);if(x<31)v=Math.min(v,1+distance[i+33]);}
            distance[i]=(byte)v;
        }
    }
    DrainageNetwork.Sample apply(int x,int z,DrainageNetwork.Sample old){
        double support=envelope(x,z);if(support==0)return old;
        double undulation=4.8*SurfacePatches.signed(seed^0x47524156454C5246L,x,z,20)
            +1.2*SurfacePatches.signed(seed^0x47524156454C5243L,x,z,48);
        int offset=(int)Math.round(support*Math.clamp(undulation,-MAX_OFFSET,MAX_OFFSET));
        int nearbyWater=tile(Math.floorDiv(x,16),Math.floorDiv(z,16)).waterLevels[(z&15)*16+(x&15)];
        if(nearbyWater!=DrainageNetwork.DRY)offset=Math.max(offset,-Math.max(0,old.height()-nearbyWater+1)/2);
        if(offset==0)return old;
        return new DrainageNetwork.Sample(old.height()+offset,old.waterLevel(),old.basin(),old.closed(),old.bedrockCoast(),old.regionalHeight(),old.regionalWaterLevel(),old.sandyBackshore());
    }
    private static double fade(double t){return t*t*t*(t*(t*6-15)+10);}
}
