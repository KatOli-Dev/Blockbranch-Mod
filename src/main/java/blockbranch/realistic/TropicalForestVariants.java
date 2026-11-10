package blockbranch.realistic;

/** Remaining warm forest transitions; preserves completed Jungle/Savanna climate domains. */
public final class TropicalForestVariants {
    public static final int SPARSE=1,BAMBOO=2;
    public static String key(int kind){return kind==BAMBOO?"bamboo_jungle":"sparse_jungle";}
    public static int kind(long seed,int x,int z,DrainageNetwork d,ClimateField c){return surfaceKind(seed,Math.floorDiv(x,4)*4,Math.floorDiv(z,4)*4,d,c);}
    static int surfaceKind(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        var q=c.sample(x,z,d::sample);int kind=q.meanTemperatureC()>23&&q.meanTemperatureC()<=30&&q.moisture()>=.55&&q.moisture()<.65?SPARSE:
            q.meanTemperatureC()>20&&q.meanTemperatureC()<=23&&q.moisture()>.72&&q.moisture()<=.95&&BirchForest.province(seed^0x42414D424F4F5052L,x,z)>.58?BAMBOO:0;
        if(kind==0||LandVegetation.surfaceReserved(d,x,z)||Swamp.surfaceSoilContains(seed,x,z,d,c)||MangroveSwamp.exact(seed,x,z,d,c))return 0;
        return SlopeCorrection.slope(d,x,z)<=.30&&LowlandMeadow.surfaceContains(x,z,(wx,wz)->LowlandMeadow.surfaceSite(d,wx,wz))?kind:0;
    }
    /** Native rooted soil/litter beneath stems, actual grass in openings; no moss proxy. */
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear,int kind){
        int plant=plantCover(seed,x,z,site,clear,kind);
        if(kind!=BAMBOO)return plant;
        int cover=BambooSoil.encode(seed,x,z,site,clear,plant);
        return plant==9&&!bambooLitter(seed,x,z,site)?BambooSoil.withoutLitter(cover):cover;
    }
    /** Existing plant masks and grass openings, independent of mineral soil variants. */
    static int plantCover(long seed,int x,int z,GroundEcology.Site site,boolean clear,int kind){
        if(!clear)return 8;
        double colony=GroundEcology.colonyStrength(seed,0x54524F504C495454L,x,z,36,10,23,site);
        double fern=GroundEcology.colonyStrength(seed,0x54524F504645524EL,x,z,24,5,13,site);
        long h=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);double roll=(h>>>11)*0x1.0p-53;
        if(kind==BAMBOO){
            if(fern>.3&&roll<.3*(fern-.15))return 10;
            if(colony>.28&&roll<.3+.65*colony)return 9;
            return bambooGrass(seed,x,z,site)?11:8;
        }
        if(site.shaded())return fern>.25&&roll<.35*fern?10:colony>.3&&roll<.35+.45*colony?9:8;
        return fern>.3&&roll<(.15+.4*fern)*Math.clamp(site.wetness(),.35,1)?12:11;
    }
    /** Restrained litter in the cores of the existing irregular colonies.
     * No new plant positions or soil/material eligibility are introduced. */
    static boolean bambooLitter(long seed,int x,int z,GroundEcology.Site site){
        double colony=GroundEcology.colonyStrength(seed,0x54524F504C495454L,x,z,36,10,23,site);
        long h=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        double roll=(h>>>11)*0x1.0p-53;
        return colony>.55&&roll<.12+.25*(colony-.55)/.45;
    }
    /** Physical grass underlay, independent of individual fern/litter placement. */
    static boolean bambooGrass(long seed,int x,int z,GroundEcology.Site site){
        double roots=GroundEcology.colonyStrength(seed,0x42414D424F4F434CL,x,z,48,12,29,site);
        double edge=SurfacePatches.signed(seed^0x54524F50534F494CL,x,z,5);
        return roots+.12*edge<=.12;
    }
    public record Point(int x,int z){}

    public record View(double x,double y,double z,float yaw,float pitch){}

    private TropicalForestVariants(){}
}
