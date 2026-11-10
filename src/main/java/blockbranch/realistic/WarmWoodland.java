package blockbranch.realistic;

/** Warm subhumid broadleaf-evergreen stands; the climate proxy does not model a dry season or fire regime. */
public final class WarmWoodland {
    private static final long SALT=0x5741524D50494E45L;
    public static boolean climateSuitable(ClimateField.Sample c) { return c.meanTemperatureC()>19 && c.meanTemperatureC()<=27 && c.moisture()>=0.35 && c.moisture()<=0.72; }
    public static boolean stand(long seed,int x,int z) { return OakBirchWoodland.stand(seed^SALT,x,z)>=0.48; }
    public static boolean glade(long seed,int x,int z) { return OakBirchWoodland.glade(seed^SALT,x,z); }
    public static boolean eligible(DrainageNetwork.Sample terrain,double slope,boolean reserved,ClimateField.Sample climate){return terrain.waterLevel()==DrainageNetwork.DRY && !reserved && slope<=0.35 && climateSuitable(climate);}
    public static boolean contains(long seed,int x,int z,DrainageNetwork drainage,ClimateField climate) {
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(seed,x,z,drainage,climate);
    }
    /** 1=coarse soil; 2=grass opening; 3=leaf litter; 4=dry grass; 5=fern; 6=fungus; 7=podzol; 8=dead stems. */
    public static int cover(long seed,int x,int z,boolean habitat,boolean dry){return cover(seed,x,z,habitat,dry,GroundEcology.NEUTRAL.shade(!glade(seed,x,z)),true);}
    public static int cover(long seed,int x,int z,boolean habitat,boolean dry,GroundEcology.Site ecology,boolean clear){
        return !habitat||!dry?0:GroundEcology.forest(seed,x,z,2,ecology,clear);
    }
    public record Point(int x,int z) {}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ) {}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork drainage,ClimateField climate) {
        if(Jungle.surfaceContains(seed,x,z,drainage,climate))return false;
        if(LandVegetation.surfaceReserved(drainage,x,z) || !stand(seed,x,z))return false;
        double dx=(drainage.sample(x+32,z).height()-drainage.sample(x-32,z).height())/64.0,dz=(drainage.sample(x,z+32).height()-drainage.sample(x,z-32).height())/64.0;
        return eligible(drainage.sample(x,z),Math.hypot(dx,dz),false,climate.sample(x,z,drainage::sample));
    }
    private WarmWoodland() {}
}
