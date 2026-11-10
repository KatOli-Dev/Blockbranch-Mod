package blockbranch.realistic;

/**
 * Maritime redwood forest: a closed, cool, very wet stand of giant conifers inside the existing
 * mesic woodland domain. The selector is a static climate/soil proxy (mean temperature, moisture,
 * elevation, slope) with a regional province field for unmodelled stand history; it does not
 * simulate coastal fog, rainfall or disturbance. It is enabled per world by
 * {@link RedwoodForestSchema}; with the flag clear every method of this class is unreachable.
 */
public final class RedwoodForest {
    /** Bit 1 of the shared vegetation mask (bit 0 is the forest–grassland transition definition). */
    public static final int FLAG=2;
    public static final int FAMILY=17;
    public static final String KEY="redwood_forest";
    private static final long SALT=0x5245445742303031L;
    public static boolean enabled(int vegetationFlags){return (vegetationFlags&FLAG)!=0;}
    public static boolean climateSuitable(ClimateField.Sample c){
        return c.meanTemperatureC()>=8&&c.meanTemperatureC()<=16&&c.moisture()>=.68;
    }
    /** Smooth 24 km field: which cool, wet lowlands carried redwood rather than mixed hardwood stands. */
    public static double province(long seed,int x,int z){
        int scale=24000,gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);
        double a=Math.floorMod(x,scale)/(double)scale,b=Math.floorMod(z,scale)/(double)scale;a=a*a*(3-2*a);b=b*b*(3-2*b);
        return(value(seed,gx,gz)*(1-a)+value(seed,gx+1,gz)*a)*(1-b)+(value(seed,gx,gz+1)*(1-a)+value(seed,gx+1,gz+1)*a)*b;
    }
    private static double value(long seed,int x,int z){return unit(OakBirchWoodland.mix(seed^SALT^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL));}
    /** Quart-resolution selector used for biome painting and tree placement. */
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return selected(seed,x,z,d,c,false);
    }
    /** Block-resolution soil eligibility for the floor palette. */
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return selected(seed,x,z,d,c,true);}
    private static boolean selected(long seed,int x,int z,DrainageNetwork d,ClimateField c,boolean physical){
        if(province(seed,x,z)<.42)return false;
        var q=c.sample(x,z,d::sample);
        if(!climateSuitable(q))return false;
        int elevation=d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;
        if(elevation<0||elevation>900)return false;
        if(!(physical?OakBirchWoodland.surfacePotential(seed,x,z,d,c):OakBirchWoodland.potential(seed,x,z,d,c)))return false;
        // Redwoods stay inside the plain woodland array (never the Forest/BirchForest arrays), so cover-array exclusivity is inherited.
        if(physical?Forest.surfacePotential(seed,x,z,d,c):Forest.potential(seed,x,z,d,c))return false;
        return !(physical?Swamp.surfaceSoilContains(seed,x,z,d,c):Swamp.contains(seed,x,z,d,c));
    }
    /** Dense stands: only small, irregular light gaps where a canopy giant has fallen. */
    public static boolean glade(long seed,int x,int z){return OakBirchWoodland.stand(seed^SALT^0x474C414445L,x*6,z*6)<.12;}
    /**
     * Woodland-array floor codes: 1 podzol duff; 4 sword fern; 5 brown mushroom; 6 grassy gap;
     * 7 leaf litter on podzol. Large ferns are applied by the caller where two-block clearance exists.
     */
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear)return 1;
        double moist=Math.clamp((site.wetness()-.2)*2,.35,1);
        double fern=GroundEcology.colonyStrength(seed,0x5245445745524E31L,x,z,28,8,20,site);
        double fungus=GroundEcology.colonyStrength(seed,0x5245444655474E31L,x,z,24,3,9,site);
        double litter=GroundEcology.colonyStrength(seed,0x524544494C495452L,x,z,32,9,22,site);
        double roll=unit(OakBirchWoodland.mix(seed^SALT^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L));
        if(site.shaded()&&site.wetness()>.5&&roll<.12*fungus*moist)return 5;
        // Fern carpets are the dominant understory; they thin out where the canopy opens.
        if(roll<(.10+.78*fern)*moist*(site.shaded()?1:.55))return 4;
        if(roll<.50+.40*litter)return 7;
        if(!site.shaded()&&roll>.93)return 6;
        return 1;
    }
    /** A mature-fern colony needs humid shade and real two-block clearance. */
    public static int withLargeFern(long seed,int x,int z,GroundEcology.Site site,int cover,boolean headroom){
        return cover==4&&headroom&&site.shaded()&&site.wetness()>.6
            &&GroundEcology.colonyStrength(seed,0x524544464C524731L,x,z,36,8,19,site)>.3?53:cover;
    }
    private static double unit(long h){return(h>>>11)*0x1.0p-53;}
    private RedwoodForest(){}
}
