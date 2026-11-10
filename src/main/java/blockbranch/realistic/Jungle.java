package blockbranch.realistic;

/** Tropical humid forest; mean moisture/retention proxies, not a rainfall or season simulation. */
public final class Jungle {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>23&&c.meanTemperatureC()<=30&&c.moisture()>=.65&&c.moisture()<=.95;}
    public static boolean eligible(DrainageNetwork.Sample s,double slope,boolean reserved,ClimateField.Sample c){return s.waterLevel()==DrainageNetwork.DRY&&!reserved&&slope<=.30&&climateSuitable(c);}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(seed,x,z,d,c);
    }
    public static boolean glade(long seed,int x,int z){return OakBirchWoodland.stand(seed^0x4A554E474C454741L,x*4,z*4)<.20;}
    /** Woodland codes 8..12: rooted floor, litter, fern, grass opening, grassy understory. */
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear)return 8;
        double fern=GroundEcology.colonyStrength(seed,0x4A554E474645524EL,x,z,24,7,15,site);
        double litter=GroundEcology.colonyStrength(seed,0x4A554E474C495454L,x,z,32,10,22,site);
        double chance=(OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L)>>>11)*0x1.0p-53;
        if(site.shaded())return chance<(.06+.62*fern)*Math.clamp(site.wetness(),.4,1)?10:chance<.25+.7*litter?9:8;
        return chance<.05*fern?10:chance<.08+.5*fern?12:11;
    }
    public record Point(int x,int z){}
    /** A mature-fern colony needs humid shade and real two-block clearance. */
    static int withLargeFern(long seed,int x,int z,GroundEcology.Site site,int cover,boolean headroom){
        return cover==10&&headroom&&site.shaded()&&site.wetness()>.65
            &&GroundEcology.colonyStrength(seed,0x4A554E474C464552L,x,z,40,9,21,site)>.35?53:cover;
    }

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        if(LandVegetation.surfaceReserved(d,x,z)||!climateSuitable(c.sample(x,z,d::sample)))return false;
        return eligible(d.sample(x,z),SlopeCorrection.slope(d,x,z),false,c.sample(x,z,d::sample))&&LowlandMeadow.surfaceContains(x,z,(wx,wz)->LowlandMeadow.surfaceSite(d,wx,wz));
    }
    private Jungle(){}
}
