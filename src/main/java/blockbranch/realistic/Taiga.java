package blockbranch.realistic;

/** Evergreen-dominant boreal provinces;the existing mixed spruce-birch habitat remains a substantial partition. */
public final class Taiga {
    public static double province(long seed,int x,int z){int scale=32000,gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);double a=Math.floorMod(x,scale)/(double)scale,b=Math.floorMod(z,scale)/(double)scale;a=fade(a);b=fade(b);return(value(seed,gx,gz)*(1-a)+value(seed,gx+1,gz)*a)*(1-b)+(value(seed,gx,gz+1)*(1-a)+value(seed,gx+1,gz+1)*a)*b;}
    private static double value(long seed,int x,int z){return(OakBirchWoodland.mix(seed^0x5441494741524547L^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL)>>>11)*0x1.0p-53;}
    private static double fade(double a){return a*a*a*(a*(a*6-15)+10);}
    /** This field expresses unmodelled stand-composition/history variation,not a second climate simulation. */
    public static boolean provinceSelected(long seed,int x,int z){return province(seed,x,z)>=.42;}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;return provinceSelected(seed,x,z)&&BorealWoodland.contains(seed,x,z,d,c)&&c.sample(x,z,d::sample).meanTemperatureC()>=-2;}
    public static boolean snowyCandidate(long seed,int x,int z,DrainageNetwork d,ClimateField c){x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;return provinceSelected(seed,x,z)&&BorealWoodland.contains(seed,x,z,d,c)&&c.sample(x,z,d::sample).meanTemperatureC()<-2;}
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return provinceSelected(seed,x,z)&&BorealWoodland.surfaceContains(seed,x,z,d,c)&&c.sample(x,z,d::sample).meanTemperatureC()>=-2;}
    static boolean snowySurfaceCandidate(long seed,int x,int z,DrainageNetwork d,ClimateField c){return provinceSelected(seed,x,z)&&BorealWoodland.surfaceContains(seed,x,z,d,c)&&c.sample(x,z,d::sample).meanTemperatureC()<-2;}
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear)return 1;
        double patch=groundPatch(seed,x,z),colony=GroundEcology.colonyStrength(seed,0x5441494741464552L,x,z,32,8,19,site);
        double roll=(OakBirchWoodland.mix(seed^x*1297L^z*1237L)>>>11)*0x1.0p-53;
        if(patch>.25&&site.wetness()>.45){
            boolean carpet=roll<.16*colony;
            return GroundEcology.borealMossSite(site)?(carpet?BorealSurface.CARPET:BorealSurface.SOIL):site.shaded()?1:carpet?8:12;
        }
        if(!site.shaded()&&patch<-.25)return roll<.18*colony?8:12;
        if(roll<.22*colony)return 4;
        if(!site.shaded()&&roll>.97&&colony>.4)return 7;
        return 1;
    }
    static double groundPatch(long seed,int x,int z){return 1.4*SurfacePatches.signed(seed^0x47524F554E44L,x,z,32);}
    public record Point(int x,int z){}

    private Taiga(){}
}
