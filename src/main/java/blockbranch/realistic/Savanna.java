package blockbranch.realistic;

/** Warm open acacia grassland. Moisture is a mean-climate proxy, not seasonal rainfall. */
public final class Savanna {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>23&&c.meanTemperatureC()<=30&&c.moisture()>=.18&&c.moisture()<.55;}
    public static boolean eligible(DrainageNetwork.Sample s,double slope,boolean reserved,ClimateField.Sample c){return s.waterLevel()==DrainageNetwork.DRY&&!reserved&&slope<=.30&&climateSuitable(c);}
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(seed,x,z,d,c);
    }
    /** Codes 9..12 extend the warm-floor palette: grass soil, dry grass, green grass and bare coarse soil. */
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear)return 12;
        double grass=GroundEcology.colonyStrength(seed,0x5341564752415353L,x,z,28,10,21,site);
        double vigor=Math.clamp(.32+.9*site.wetness()+.12*site.retention()-.2*site.exposure(),.2,.9);
        long h=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        double chance=(h>>>11)*0x1.0p-53;
        if(chance<(.18+.72*grass)*vigor)return site.wetness()>.38&&((h>>>8)&7)<3?11:10;
        return chance>.96+.02*site.retention()?12:9;
    }
    public record Point(int x,int z){}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        if(LandVegetation.surfaceReserved(d,x,z)||!climateSuitable(c.sample(x,z,d::sample)))return false;
        if(!eligible(d.sample(x,z),SlopeCorrection.slope(d,x,z),false,c.sample(x,z,d::sample)))return false;
        // These are the only overlapping prior land climates; preserve their existing assignments.
        return !WarmWoodland.surfaceContains(seed,x,z,d,c)&&!LowlandMeadow.surfaceContains(x,z,d,c)&&!DrySteppe.surfaceContains(x,z,d,c);
    }
    private Savanna(){}
}
