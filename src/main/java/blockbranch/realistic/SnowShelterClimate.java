package blockbranch.realistic;
/** Immutable coarse labels for complete connected snowy structure regions. */
final class SnowShelterClimate {
 private final long seed;SnowShelterClimate(long seed){this.seed=seed;}
    /** Coherent regional snowy-habitat labels on the same broad structure mesh.
     * Local placement still checks the actual snowy biome and ground. */
    int structureSnowHabitat(int x,int z,DrainageNetwork d){
        long local=d.structureClimatePacked(x,z);if((local&1)!=0)return 0;
        double elevation=Math.max(0,(int)(local>>32)+1-TerrainShape.SEA_LEVEL);
        double temperature=24-20*Math.min(1,Math.abs((double)z)/2097152.0)+5*noise(x,z,240000,101)-.006*elevation;
        if(temperature< -20||temperature>=-2)return 0;
        double regional=.50+.30*noise(x,z,240000,211)+.14*noise(x,z,60000,307);
        double west20=(int)(d.structureClimatePacked(x-20000,z)>>32)+1-TerrainShape.SEA_LEVEL;
        double west60=(int)(d.structureClimatePacked(x-60000,z)>>32)+1-TerrainShape.SEA_LEVEL;
        double moisture=Math.clamp(regional+.12*structureWater(x,z,64000,d,true)+.12*structureWater(x,z,256,d,false)
            +Math.clamp((elevation-west20)/2000.0,-1,1)*.06-Math.clamp((Math.max(west20,west60)-elevation)/2000.0,0,1)*.18,0,1);
        if(temperature< -6)return moisture>=.4&&moisture<=.85?1:0;
        return moisture>=.32&&moisture<=.88&&Taiga.provinceSelected(seed,x,z)?2:0;
    }
    private static double structureWater(int x,int z,int scale,DrainageNetwork d,boolean ocean){
        int gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);double tx=fade(Math.floorMod(x,scale)/(double)scale),tz=fade(Math.floorMod(z,scale)/(double)scale);
        return lerp(tz,lerp(tx,structureWet(d.structureClimatePacked(gx*scale,gz*scale),ocean),structureWet(d.structureClimatePacked((gx+1)*scale,gz*scale),ocean)),
            lerp(tx,structureWet(d.structureClimatePacked(gx*scale,(gz+1)*scale),ocean),structureWet(d.structureClimatePacked((gx+1)*scale,(gz+1)*scale),ocean)));
    }
    private static double structureWet(long packed,boolean ocean){return (packed&1)!=0&&(!ocean||(packed&2)!=0)?1:0;}
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
