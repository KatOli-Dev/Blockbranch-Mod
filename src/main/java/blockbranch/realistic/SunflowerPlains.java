package blockbranch.realistic;

/** Warm mesic Plains composition province; static suitability, not a flowering-season simulation. */
public final class SunflowerPlains {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=9&&c.meanTemperatureC()<=22&&c.moisture()>=.36&&c.moisture()<=.55;}
    public static double province(long seed,int x,int z){
        int scale=24000,gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);double tx=Math.floorMod(x,scale)/(double)scale,tz=Math.floorMod(z,scale)/(double)scale;tx=tx*tx*(3-2*tx);tz=tz*tz*(3-2*tz);
        return(value(seed,gx,gz)*(1-tx)+value(seed,gx+1,gz)*tx)*(1-tz)+(value(seed,gx,gz+1)*(1-tx)+value(seed,gx+1,gz+1)*tx)*tz;
    }
    private static double value(long seed,int x,int z){return(OakBirchWoodland.mix(seed^0x53554E464C4F5745L^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL)>>>11)*0x1.0p-53;}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return climateSuitable(c.sample(x,z,d::sample))&&province(seed,x,z)>=.62&&Plains.potential(seed,x,z,d,c);
    }
    /** Code10 is a native LOWER/UPPER sunflower; surrounding grass structure is shared with Plains. */
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear||site.shaded())return Plains.cover(seed,x,z,site,clear);
        double soil=Math.clamp(.5+site.wetness()+.25*site.retention()-.35*site.exposure(),.15,1);
        long h=OakBirchWoodland.mix(seed^0x53554E504C414E54L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        double roll=(h>>>11)*0x1.0p-53;
        if(roll<.65*soil){
            // Continuous grass gaps cut across the stands, rather than surrounding
            // each jittered centre with a filled ellipse. Native flowers stay unchanged.
            double stand=Math.clamp((MontaneTerrain.patch(seed^0x53554E4652494E47L,x,z,24)-.35)*3.5,0,1);
            if(roll<.65*soil*stand){
                // Maximum along-axis radius 38.4 stays inside the 40m neighbour search.
                double colony=GroundEcology.colonyStrength(seed,0x53554E464C4F5743L,x,z,40,14,32,site);
                if(roll<.65*colony*soil*stand)return 10;
            }
        }
        return Plains.cover(seed,x,z,site,true);
    }
    public record Point(int x,int z){}

    private SunflowerPlains(){}
}
