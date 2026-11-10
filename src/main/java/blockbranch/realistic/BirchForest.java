package blockbranch.realistic;

/** Cool mesic closed-stand composition with dominant birch,not a label applied to unchanged mixed trees. */
public final class BirchForest {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=6&&c.meanTemperatureC()<=14&&c.moisture()>=.55&&c.moisture()<=.85;}
    public static double province(long seed,int x,int z){
        int scale=16000,gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);double a=Math.floorMod(x,scale)/(double)scale,b=Math.floorMod(z,scale)/(double)scale;a=a*a*(3-2*a);b=b*b*(3-2*b);
        return(value(seed,gx,gz)*(1-a)+value(seed,gx+1,gz)*a)*(1-b)+(value(seed,gx,gz+1)*(1-a)+value(seed,gx+1,gz+1)*a)*b;
    }
    private static double value(long seed,int x,int z){return(OakBirchWoodland.mix(seed^0x4249524348535444L^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL)>>>11)*0x1.0p-53;}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(seed,x,z,d,c);
    }
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        int cover=GroundEcology.forest(seed,x,z,0,site,clear);
        // Grassier light gaps and leaf-rich shade express the lighter deciduous canopy using existing materials.
        if(cover==1&&!site.shaded())cover=3;
        if(cover==1&&GroundEcology.colonyStrength(seed,0x42495243484C4954L,x,z,32,9,20,site)>.35)cover=7;
        return WoodlandDetails.temperateFloor(seed,x,z,site,clear,cover);
    }
    public record Point(int x,int z){}


    /** Block-resolution material eligibility; the original selector retains biome routing. */
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        return climateSuitable(c.sample(x,z,d::sample))&&province(seed,x,z)>=.60&&Forest.surfacePotential(seed,x,z,d,c);
    }
    private BirchForest(){}
}
