package blockbranch.realistic;

import java.util.function.BiFunction;

/** Rolling forb-grass terrain suitability; climate is selected by the seeded generator, without an independent altitude cutoff. */
public final class LowlandMeadow {
    public static final double GENTLE_SLOPE=0.20, MAX_SLOPE=0.35;
    public record Site(int height,int water,boolean river) {}
    static Site site(DrainageNetwork drainage,int x,int z) {
        var sample=drainage.sample(x,z);
        return new Site(sample.height(),sample.waterLevel(),LandVegetation.reserved(drainage,x,z));
    }
    public static boolean eligible(Site site) {
        return site.water()==DrainageNetwork.DRY && !site.river();
    }
    public static boolean contains(int x,int z,BiFunction<Integer,Integer,Site> terrain) {
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,terrain);
    }
    /** The original gentle domain remains explicit to retain cold-steppe overlap and regression witnesses. */
    static boolean gentleContains(int x,int z,BiFunction<Integer,Integer,Site> terrain){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceGentleContains(x,z,terrain);
    }
    static boolean contains(int x,int z,DrainageNetwork drainage,ClimateField climate){return potential(x,z,drainage,climate)&&!Plains.potential(climate.seed(),x,z,drainage,climate);}
    static boolean retained(int x,int z,DrainageNetwork d,ClimateField c){return contains(x,z,d,c)&&!MontaneMeadow.contains(c.seed(),x,z,d,c);}
    /** Frozen meadow suitability before broad grassland partition; also used by historical audits. */
    static boolean potential(int x,int z,DrainageNetwork drainage,ClimateField climate){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfacePotential(x,z,drainage,climate);
    }
    /** 1=bare grass sward; 2=short grass; 3=fescue; 4=daisy; 5=cornflower; 6=dandelion. */
    public static int cover(long seed,int x,int z,boolean habitat,Site site){return cover(seed,x,z,habitat,site,GroundEcology.NEUTRAL);}
    public static int cover(long seed,int x,int z,boolean habitat,Site site,GroundEcology.Site ecology){
        return cover(seed,x,z,habitat,site,ecology,true);
    }
    public static int cover(long seed,int x,int z,boolean habitat,Site site,GroundEcology.Site ecology,boolean clear){
        if(!habitat||!eligible(site))return 0;
        int original=GroundEcology.meadow(seed,x,z,ecology);
        // Flower colonies should not also require an unrelated grass colony.
        // Retain every original plant, including short grass, and shaded/occupied gaps.
        if(original!=1||ecology.shaded()||!clear)return original;
        double flowerSite=Math.clamp(1-Math.abs(ecology.wetness()-.58)*1.5,.2,1)*Math.clamp(1-.45*ecology.exposure(),.3,1);
        long cell=OakBirchWoodland.mix(seed^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);
        double roll=(OakBirchWoodland.mix(cell^0xF10AE25L)>>>11)*0x1.0p-53;
        if(roll>=.75*flowerSite)return original;
        var flowers=GroundEcology.colony(seed,0x464C4F574552L,x,z,32,5,12,ecology);
        return roll<.75*flowers.strength()*flowerSite?4+(int)Long.remainderUnsigned(flowers.seed(),3):original;
    }
    public record MeadowPoint(int x,int z) {}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static Site surfaceSite(DrainageNetwork drainage,int x,int z) {
        var sample=drainage.sample(x,z);
        return new Site(sample.height(),sample.waterLevel(),LandVegetation.surfaceReserved(drainage,x,z));
    }
    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,BiFunction<Integer,Integer,Site> terrain) {
        var center=terrain.apply(x,z);
        if(!eligible(center)) return false;
        double dx=(terrain.apply(x+32,z).height()-terrain.apply(x-32,z).height())/64.0;
        double dz=(terrain.apply(x,z+32).height()-terrain.apply(x,z-32).height())/64.0;
        double slope=Math.hypot(dx,dz);
        return slope<=GENTLE_SLOPE || slope<=MAX_SLOPE && SlopeStability.supportsSoil(x,z,center.height(),dx,dz,(wx,wz)->{var site=terrain.apply(wx,wz);return new SlopeStability.Site(site.height(),eligible(site));});
    }
    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceGentleContains(int x,int z,BiFunction<Integer,Integer,Site> terrain){
        if(!eligible(terrain.apply(x,z)))return false;
        return Math.hypot((terrain.apply(x+32,z).height()-terrain.apply(x-32,z).height())/64.0,(terrain.apply(x,z+32).height()-terrain.apply(x,z-32).height())/64.0)<=GENTLE_SLOPE;
    }
    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfacePotential(int x,int z,DrainageNetwork drainage,ClimateField climate){
        var c=climate.sample(x,z,drainage::sample);if(!OakBirchWoodland.meadowClimateSuitable(c))return false;
        // Existing cold fescue steppe keeps its cool/dry moderate slopes.
        return (!ColdSteppe.climateSuitable(c)||!ColdSteppe.surfaceContains(x,z,drainage,climate))&&surfaceContains(x,z,(wx,wz)->surfaceSite(drainage,wx,wz));
    }
    static boolean surfaceContains(int x,int z,DrainageNetwork d,ClimateField c){return surfacePotential(x,z,d,c)&&!Plains.potential(c.seed(),x,z,d,c);}
    private LowlandMeadow() {}
}
