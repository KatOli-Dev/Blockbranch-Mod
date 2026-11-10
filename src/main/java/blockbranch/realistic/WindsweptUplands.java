package blockbranch.realistic;

/** Existing temperate upland faces; wind exposure is a static topographic proxy, not a wind simulation. */
public final class WindsweptUplands {
    public static final int HILLS=1,FOREST=2;
    public static String key(int kind){return kind==FOREST?"windswept_forest":"windswept_hills";}
    public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return surfaceKind(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
    static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var t=d.sample(x,z);int elevation=t.height()+1-TerrainShape.SEA_LEVEL;
        if(elevation<120||elevation>2000||LandVegetation.surfaceReserved(d,x,z))return 0;
        double slope=SlopeCorrection.slope(d,x,z);if(slope<=.45||slope>1.2)return 0;
        var q=c.sample(x,z,d::sample);if(q.meanTemperatureC()<6||q.meanTemperatureC()>18||q.moisture()<.35||q.moisture()>.8)return 0;
        return BirchForest.province(seed^0x57494E445354414EL,x,z)>.52?FOREST:HILLS;
    }
    /** Supported, previously unassigned shoulders adjoining this pair; never replaces completed soil habitats. */
    static int surfaceMaterialKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        int core=surfaceKind(seed,x,z,d,c);if(core>0)return core;
        var t=d.sample(x,z);int elevation=t.height()+1-TerrainShape.SEA_LEVEL;
        if(elevation<120||elevation>2000||LandVegetation.surfaceReserved(d,x,z))return 0;
        double gx=(d.sample(x+32,z).height()-d.sample(x-32,z).height())/64.,gz=(d.sample(x,z+32).height()-d.sample(x,z-32).height())/64.,slope=Math.hypot(gx,gz);
        if(slope<=.30||slope>.45||!soil(seed,x,z,d))return 0;
        var q=c.sample(x,z,d::sample);if(q.meanTemperatureC()<6||q.meanTemperatureC()>18||q.moisture()<.35||q.moisture()>.8)return 0;
        if(LowlandMeadow.surfacePotential(x,z,d,c)||OakBirchWoodland.surfacePotential(seed,x,z,d,c)||BorealWoodland.surfaceContains(seed,x,z,d,c)
            ||WarmWoodland.surfaceContains(seed,x,z,d,c)||HumidWoodland.surfaceContains(seed,x,z,d,c)||Swamp.surfaceSoilContains(seed,x,z,d,c)||MangroveSwamp.exact(seed,x,z,d,c))return 0;
        double reach=48+12*SurfacePatches.signed(seed^0x57494E444150524FL,x,z,37);
        for(double distance:new double[]{16,reach})for(int sign:new int[]{-1,1}){
            int wx=(int)Math.round(x+sign*gx/slope*distance),wz=(int)Math.round(z+sign*gz/slope*distance),near=surfaceKind(seed,wx,wz,d,c);
            if(near>0)return near;
        }
        return 0;
    }
    /** Soil occupies local supported ledges; sharper scarps retain the existing native stone. */
    public static boolean soil(long seed,int x,int z,DrainageNetwork d){
        int h=d.sample(x,z).height();
        double dx=(d.sample(x+6,z).height()-d.sample(x-6,z).height())/12.,dz=(d.sample(x,z+6).height()-d.sample(x,z-6).height())/12.;
        double hollow=Math.clamp((d.sample(x+12,z).height()+d.sample(x-12,z).height()+d.sample(x,z+12).height()+d.sample(x,z-12).height()-4.*h)/24.,-1,1);
        double limit=Math.clamp(.88+.18*hollow+.18*SurfacePatches.signed(seed^0x57494E44534F494CL,x,z,28)+.06*SurfacePatches.signed(seed^0x57494E4445444745L,x,z,6),.68,1.05);
        if(Math.hypot(dx,dz)>limit)return false;
        for(int[] p:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})if(Math.abs(d.sample(x+p[0],z+p[1]).height()-h)>1||LandVegetation.surfaceReserved(d,x+p[0],z+p[1]))return false;
        return true;
    }
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear,int kind){
        if(!clear)return 11;
        double colony=GroundEcology.colonyStrength(seed,0x57494E4454554654L,x,z,32,7,19,site);
        double roll=(OakBirchWoodland.mix(seed^0x57494E44464C4F52L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
        if(kind==FOREST&&site.shaded()&&colony>.3){if(roll<.2*colony&&site.wetness()>.5)return 10;if(roll<.65*colony)return 9;}
        return colony>.25&&roll<.45*colony?12:11;
    }
    public record Point(int x,int z){}

    public record View(double x,double y,double z,float yaw,float pitch){}

    private WindsweptUplands(){}
}
