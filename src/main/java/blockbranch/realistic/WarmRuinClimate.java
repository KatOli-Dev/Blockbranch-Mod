package blockbranch.realistic;
/** Immutable coarse labels for complete connected warm shelf structure regions. */
final class WarmRuinClimate {
 private final long seed;WarmRuinClimate(long seed){this.seed=seed;}
    int structureWarmShelf(int x,int z,DrainageNetwork d){
        long local=d.structureClimatePacked(x,z);int height=(int)(local>>32);
        if((local&3)!=3||height< -136||height>53)return 0;
        double temperature=24-20*Math.min(1,Math.abs((double)z)/2097152.0)+5*noise(x,z,240000,101);
        return temperature>=24?1:temperature>=18?2:0;
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
