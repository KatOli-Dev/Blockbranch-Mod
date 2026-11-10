package blockbranch.realistic;

/** Warm moist broadleaf woodland: static climate and soil proxies, no season or rainfall simulation. */
public final class HumidWoodland {
    private static final long SALT=0x48554D49444F414BL;
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>19&&c.meanTemperatureC()<=29&&c.moisture()>.72&&c.moisture()<=.95;}
    public static double canopyDensity(long seed,int x,int z){return .72+.28*OakBirchWoodland.stand(seed^SALT,x,z);}
    public static boolean glade(long seed,int x,int z){return OakBirchWoodland.stand(seed^SALT^0x474150L,x*8,z*8)<.17;}
    public static boolean eligible(DrainageNetwork.Sample s,double slope,boolean reserved,ClimateField.Sample c){return s.waterLevel()==DrainageNetwork.DRY&&!reserved&&slope<=.35&&climateSuitable(c);}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(seed,x,z,d,c);
    }
    /** Reuses the established broadleaf floor palette: soil, grass gaps, ferns, fungi and litter. */
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear)return 1;
        double fern=GroundEcology.colonyStrength(seed,0x48554645524EL,x,z,20,6,13,site);
        double litter=GroundEcology.colonyStrength(seed,0x48554C49544CL,x,z,28,9,19,site);
        long h=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        double v=(h>>>11)*0x1.0p-53,moist=Math.clamp((site.wetness()-.3)*2,.2,1);
        if(site.shaded()){
            double fungus=GroundEcology.colonyStrength(seed,0x485546554E47L,x,z,24,3,8,site);
            if(site.wetness()>.55&&v<.13*fungus*moist)return 5;
            if(v<(.08+.72*fern)*moist*(1-.3*Math.min(1,Math.hypot(site.dx(),site.dz())/.35)))return 4;
            if(v<.35+.6*litter)return 7;
            return 1;
        }
        if(v<.12*fern*moist)return 4;
        if(v<.1+.48*fern*moist)return 6;
        return v>.97?2:3;
    }
    public record Point(int x,int z){}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ){}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        if(Jungle.surfaceContains(seed,x,z,d,c))return false;
        if(LandVegetation.surfaceReserved(d,x,z))return false;
        double slope=SlopeCorrection.slope(d,x,z);
        return eligible(d.sample(x,z),slope,false,c.sample(x,z,d::sample))&&LowlandMeadow.surfaceContains(x,z,(wx,wz)->LowlandMeadow.surfaceSite(d,wx,wz));
    }
    private HumidWoodland(){}
}
