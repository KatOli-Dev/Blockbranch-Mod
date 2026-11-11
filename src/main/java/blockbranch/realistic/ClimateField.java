package blockbranch.realistic;

import java.util.function.BiFunction;

/** Deterministic mean-climate suitability proxies; read-only with respect to terrain and water. */
public final class ClimateField {
    private final long seed;
    public ClimateField(long seed) { this(seed,false); }
    private final boolean polar;
    public ClimateField(long seed,boolean polar) { this.seed=seed;this.polar=polar; }
    public boolean polar(){return polar;}
    long seed(){return seed;}
    public record Sample(double meanTemperatureC,double moisture) {
        public Sample {
            if(!Double.isFinite(meanTemperatureC) || !Double.isFinite(moisture) || moisture<0 || moisture>1)
                throw new IllegalArgumentException("Invalid climate sample");
        }
    }
    public Sample sample(int x,int z,BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain) {
        var local=terrain.apply(x,z);
        double elevation=Math.max(0,local.regionalHeight()+1-TerrainShape.SEA_LEVEL);
        // The finite map spans a warm central belt to cool north/south edges. This is an
        // explicit map convention, not a claim to model a planet or astronomical latitude.
        double latitude=Math.min(1,Math.abs((double)z)/2097152.0);
        double temperature=24-20*latitude+5*noise(x,z,240000,101)-0.006*elevation;
        if(polar)temperature-=PolarClimate.cooling(latitude);
        double regional=0.50+0.30*noise(x,z,240000,211)+0.14*noise(x,z,60000,307);
        double maritime=interpolateTerrain(x,z,64000,terrain,true);
        double wetGround=interpolateTerrain(x,z,256,terrain,false);
        // New local coastal uplift retains the pre-existing macroclimate reference height.
        // Fixed prevailing westerlies: rising terrain is a modest windward proxy;
        // a higher western barrier gives a stronger lee-side drying proxy.
        double west20=terrain.apply(x-20000,z).regionalHeight()+1-TerrainShape.SEA_LEVEL;
        double west60=terrain.apply(x-60000,z).regionalHeight()+1-TerrainShape.SEA_LEVEL;
        double windward=Math.clamp((elevation-west20)/2000.0,-1,1)*0.06;
        double rainShadow=Math.clamp((Math.max(west20,west60)-elevation)/2000.0,0,1)*0.18;
        return new Sample(temperature,Math.clamp(regional+0.12*maritime+0.12*wetGround+windward-rainShadow,0,1));
    }
    /** New climatologies may use this atmospheric suitability, never infer rain
     * from the clamped total moisture (which also contains local wet ground).
     * It remains unitless; assigning any rainfall scale is a separate model. */
    double atmosphericSupply(int x,int z,BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain) {
        double elevation=Math.max(0,terrain.apply(x,z).regionalHeight()+1-TerrainShape.SEA_LEVEL);
        double regional=.50+.30*noise(x,z,240000,211)+.14*noise(x,z,60000,307);
        double west20=terrain.apply(x-20000,z).regionalHeight()+1-TerrainShape.SEA_LEVEL;
        double west60=terrain.apply(x-60000,z).regionalHeight()+1-TerrainShape.SEA_LEVEL;
        return Math.clamp(regional+.12*interpolateTerrain(x,z,64000,terrain,true)
            +Math.clamp((elevation-west20)/2000.,-1,1)*.06
            -Math.clamp((Math.max(west20,west60)-elevation)/2000.,0,1)*.18,0,1);
    }
    /** Conservative interval for an entire terminal-lake proposal footprint.
     * Quintic fade has derivative <=1.875; signed value-noise partials are
     * <=3.75/scale. Closed banks bypass coastal uplift, and erosion only lowers
     * actual ground by at most40, so a qualifying bank bounds regional elevation.
     * This may keep impossible lakes but cannot discard a qualifying proposal. */
    boolean swampBankPossible(int x,int z,int radius,int water,DrainageNetwork d){
        double minLat=Math.min(1,Math.max(0,Math.abs((double)z)-radius)/2097152.),maxLat=Math.min(1,(Math.abs((double)z)+radius)/2097152.);
        double temperatureNoise=5*noise(x,z,240000,101),temperatureDelta=5*3.75*2*radius/240000.;
        double minElevation=Math.max(0,water-TerrainShape.SEA_LEVEL),maxElevation=Math.max(0,water+3+ErodedBadlands.MAX_INCISION-TerrainShape.SEA_LEVEL);
        double low=24-20*maxLat+temperatureNoise-temperatureDelta-.006*maxElevation,high=24-20*minLat+temperatureNoise+temperatureDelta-.006*minElevation;
        if(polar){low-=PolarClimate.cooling(maxLat);high-=PolarClimate.cooling(minLat);}
        if(high<10-1e-9||low>27+1e-9)return false;
        double regional=.50+.30*noise(x,z,240000,211)+.14*noise(x,z,60000,307);
        double delta=(.30/240000+.14/60000)*3.75*2*radius;
        double maritimeMax=0;
        for(int gz=Math.floorDiv(z-radius,64000);gz<=Math.floorDiv(z+radius,64000)+1;gz++)for(int gx=Math.floorDiv(x-radius,64000);gx<=Math.floorDiv(x+radius,64000)+1;gx++){
            var s=d.sample(gx*64000,gz*64000);if(s.regionalWaterLevel()>s.regionalHeight()+1&&!s.closed()&&s.regionalWaterLevel()==TerrainShape.SEA_LEVEL)maritimeMax=1;
        }
        // Wet-ground interpolation is in[0,1], windward in[-.06,.06],
        // and rain shadow in[0,.18]. All bounds are deliberately inclusive.
        return regional+delta+.12*maritimeMax+.12+.06>=.55-1e-9&&regional-delta-.06-.18<=.95+1e-9;
    }
    /** Same climate formula over the structure atlas mesh, without callback/array allocations. */
    boolean structureDesert(int x,int z,DrainageNetwork d){
        long local=d.structureClimatePacked(x,z);if((local&1)!=0)return false;
        double elevation=Math.max(0,(int)(local>>32)+1-TerrainShape.SEA_LEVEL);
        double temperature=24-20*Math.min(1,Math.abs((double)z)/2097152.0)+5*noise(x,z,240000,101)-.006*elevation;
        if(polar)temperature-=PolarClimate.cooling(PolarClimate.latitude(z));
        if(temperature<10||temperature>30)return false;
        double regional=.50+.30*noise(x,z,240000,211)+.14*noise(x,z,60000,307);
        double west20=(int)(d.structureClimatePacked(x-20000,z)>>32)+1-TerrainShape.SEA_LEVEL;
        double west60=(int)(d.structureClimatePacked(x-60000,z)>>32)+1-TerrainShape.SEA_LEVEL;
        double moisture=Math.clamp(regional+.12*structureWater(x,z,64000,d,true)+.12*structureWater(x,z,256,d,false)
            +Math.clamp((elevation-west20)/2000.0,-1,1)*.06-Math.clamp((Math.max(west20,west60)-elevation)/2000.0,0,1)*.18,0,1);
        return moisture<.12;
    }
    /** New reimagined-definition ownership mesh only; existing biome formulas remain untouched. */
    Sample structureSample(int x,int z,DrainageNetwork d){
        long local=d.structureClimatePacked(x,z);
        double elevation=Math.max(0,(int)(local>>32)+1-TerrainShape.SEA_LEVEL);
        double temperature=24-20*Math.min(1,Math.abs((double)z)/2097152.0)+5*noise(x,z,240000,101)-.006*elevation;
        if(polar)temperature-=PolarClimate.cooling(PolarClimate.latitude(z));
        double regional=.50+.30*noise(x,z,240000,211)+.14*noise(x,z,60000,307);
        double west20=(int)(d.structureClimatePacked(x-20000,z)>>32)+1-TerrainShape.SEA_LEVEL;
        double west60=(int)(d.structureClimatePacked(x-60000,z)>>32)+1-TerrainShape.SEA_LEVEL;
        return new Sample(temperature,Math.clamp(regional+.12*structureWater(x,z,64000,d,true)+.12*structureWater(x,z,256,d,false)
            +Math.clamp((elevation-west20)/2000.0,-1,1)*.06-Math.clamp((Math.max(west20,west60)-elevation)/2000.0,0,1)*.18,0,1));
    }
    private static double structureWater(int x,int z,int scale,DrainageNetwork d,boolean ocean){
        int gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);double tx=fade(Math.floorMod(x,scale)/(double)scale),tz=fade(Math.floorMod(z,scale)/(double)scale);
        return lerp(tz,lerp(tx,structureWet(d.structureClimatePacked(gx*scale,gz*scale),ocean),structureWet(d.structureClimatePacked((gx+1)*scale,gz*scale),ocean)),
            lerp(tx,structureWet(d.structureClimatePacked(gx*scale,(gz+1)*scale),ocean),structureWet(d.structureClimatePacked((gx+1)*scale,(gz+1)*scale),ocean)));
    }
    private static double structureWet(long packed,boolean ocean){return (packed&1)!=0&&(!ocean||(packed&2)!=0)?1:0;}
    private static double interpolateTerrain(int x,int z,int scale,BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain,boolean ocean) {
        int gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);
        double tx=fade(Math.floorMod(x,scale)/(double)scale),tz=fade(Math.floorMod(z,scale)/(double)scale);
        double[] corners=new double[4];
        for(int dz=0;dz<2;dz++) for(int dx=0;dx<2;dx++) {
            var s=terrain.apply((gx+dx)*scale,(gz+dz)*scale);
            corners[dz*2+dx]=s.regionalWaterLevel()>s.regionalHeight()+1 && (!ocean || (!s.closed() && s.regionalWaterLevel()==TerrainShape.SEA_LEVEL)) ? 1 : 0;
        }
        return lerp(tz,lerp(tx,corners[0],corners[1]),lerp(tx,corners[2],corners[3]));
    }
    private double noise(int x,int z,int scale,long salt) {
        int gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);
        double tx=fade(Math.floorMod(x,scale)/(double)scale),tz=fade(Math.floorMod(z,scale)/(double)scale);
        return lerp(tz,lerp(tx,value(gx,gz,salt),value(gx+1,gz,salt)),lerp(tx,value(gx,gz+1,salt),value(gx+1,gz+1,salt)));
    }
    private double value(int x,int z,long salt) {
        long v=seed ^ x*0x9E3779B97F4A7C15L ^ z*0xC2B2AE3D27D4EB4FL ^ salt*0x165667B19E3779F9L;
        v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;v=(v^(v>>>27))*0x94D049BB133111EBL;v^=v>>>31;
        return (v>>>11)*0x1.0p-53*2-1;
    }
    private static double fade(double t) { return t*t*t*(t*(t*6-15)+10); }
    private static double lerp(double t,double a,double b) { return a+t*(b-a); }
}
