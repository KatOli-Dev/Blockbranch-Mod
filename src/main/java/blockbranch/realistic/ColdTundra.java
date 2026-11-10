package blockbranch.realistic;

/** Cold open shrub/moss/tussock terrain; static climate, exposure and moisture proxies only. */
public final class ColdTundra {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=-14&&c.meanTemperatureC()<-6&&c.moisture()>=.20&&c.moisture()<=.85;}
    public static boolean eligible(DrainageNetwork.Sample s,double slope,boolean reserved,ClimateField.Sample c){return s.waterLevel()==DrainageNetwork.DRY&&!reserved&&slope<=.35&&climateSuitable(c);}
    public static boolean contains(int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,d,c);
    }
    public static boolean retained(int x,int z,DrainageNetwork d,ClimateField c){return contains(x,z,d,c)&&!SnowyPlains.contains(x,z,d,c);}
    /** Fresh cold floor: 2 soil,8 grass,9 low shrub,10 gravel,11 fescue,15 organic soil,16 soil-backed moss mat.
     * Saved roles 3/6 keep their historical moss-block meanings in the profile decoder. */
    public static int cover(long seed,int x,int z,GroundEcology.Site site){
        double moist=Math.clamp((site.wetness()-.15)/.7,0,1),shelter=Math.clamp(1-.8*site.exposure()+.3*site.retention(),.1,1);
        double shrub=GroundEcology.colonyStrength(seed,0x54554E44524153L,x,z,24,7,15,site),moss=GroundEcology.colonyStrength(seed,0x54554E444D4F53L,x,z,28,10,19,site);
        double grass=GroundEcology.colonyStrength(seed,0x54554E44475241L,x,z,20,4,10,site),stone=GroundEcology.colonyStrength(seed,0x54554E4453544FL,x,z,16,5,11,site);
        long h=OakBirchWoodland.mix(seed^0x54554E445241L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        if(chance(h,1)<(.08+.7*shrub)*shelter*(.4+.6*moist))return 9;
        if(chance(h,2)<(.05+.65*moss)*moist*(.8+.2*shelter))return BorealSurface.CARPET;
        if(chance(h,3)<(.02+.55*grass)*(.4+.6*moist)*shelter)return 11;
        if(chance(h,4)<.06*(.4+.6*moist))return 8;
        if(chance(h,5)<.02+.8*moist*moss)return BorealSurface.SOIL;
        // Cluster bare gravel among exposed soil without moving plants or moss mats.
        double gravel=Math.clamp(.15+.55*site.exposure()+.35*stone*(1-.5*moist)
            +.8*SurfacePatches.signed(seed^0x54554E4453544E45L,x,z,10)-.02,.02,.96);
        return chance(h,6)<gravel?10:2;
    }
    private static double chance(long h,int salt){return(OakBirchWoodland.mix(h^salt*0x9E3779B97F4A7C15L)>>>11)*0x1.0p-53;}
    public record Point(int x,int z){}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ){}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,DrainageNetwork d,ClimateField c){
        var climate=c.sample(x,z,d::sample);
        if(!climateSuitable(climate)||LandVegetation.surfaceReserved(d,x,z))return false;
        return eligible(d.sample(x,z),SlopeCorrection.slope(d,x,z),false,climate)&&LowlandMeadow.surfaceContains(x,z,(wx,wz)->LowlandMeadow.surfaceSite(d,wx,wz));
    }
    private ColdTundra(){}
}
