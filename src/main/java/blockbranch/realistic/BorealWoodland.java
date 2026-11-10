package blockbranch.realistic;

import java.util.function.BiFunction;

/** Cold mesic spruce-birch woodland; disjoint from the existing warmer meadow and dry steppe. */
public final class BorealWoodland {
    public static boolean climateSuitable(ClimateField.Sample climate) {
        return climate.meanTemperatureC()>=-6 && climate.meanTemperatureC()<3 && climate.moisture()>=0.32 && climate.moisture()<=0.88;
    }
    public static boolean contains(long seed,int x,int z,BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain,
            BiFunction<Integer,Integer,Boolean> channel,ClimateField climate) {
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(seed,x,z,terrain,channel,climate);
    }
    /** Physical soil eligibility at the block; contains retains the biome palette contract. */
    static boolean surfaceContains(long seed,int x,int z,BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain,
            BiFunction<Integer,Integer,Boolean> channel,ClimateField climate) {
        var center=terrain.apply(x,z);int elevation=center.height()+1-TerrainShape.SEA_LEVEL;
        if(center.waterLevel()!=DrainageNetwork.DRY || channel.apply(x,z)) return false;
        double dx=(terrain.apply(x+32,z).height()-terrain.apply(x-32,z).height())/64.0;
        double dz=(terrain.apply(x,z+32).height()-terrain.apply(x,z-32).height())/64.0;
        if(Math.hypot(dx,dz)>0.40 || !climateSuitable(climate.sample(x,z,terrain)))return false;
        if((elevation<40 || elevation>1800) && !SlopeStability.supportsSoil(x,z,center.height(),dx,dz,(wx,wz)->{var nearby=terrain.apply(wx,wz);return new SlopeStability.Site(nearby.height(),nearby.waterLevel()==DrainageNetwork.DRY&&!channel.apply(wx,wz));}))return false;
        return true;
    }
    static boolean contains(long seed,int x,int z,DrainageNetwork drainage,ClimateField climate) {
        return contains(seed,x,z,drainage::sample,(wx,wz)->LandVegetation.reserved(drainage,wx,wz),climate);
    }
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork drainage,ClimateField climate) {
        return surfaceContains(seed,x,z,drainage::sample,(wx,wz)->LandVegetation.surfaceReserved(drainage,wx,wz),climate);
    }
    /** Reuse the smooth stand field with an independent salt for small treeless moss/grass openings. */
    public static boolean glade(long seed,int x,int z) { return OakBirchWoodland.glade(seed^0x62B04EA9L,x,z); }
    /** 1=podzol; 2=coarse soil; 3=legacy moss block; 4=fern; 5=fungus; 6=legacy moss carpet; 7=berry; 8=glade grass; 15=soil bed; 16=moss carpet over soil. */
    public static int cover(long seed,int x,int z,boolean habitat,boolean dry){return cover(seed,x,z,habitat,dry,GroundEcology.NEUTRAL.shade(!glade(seed,x,z)),true);}
    public static int cover(long seed,int x,int z,boolean habitat,boolean dry,GroundEcology.Site ecology,boolean clear){
        return !habitat||!dry?0:GroundEcology.forest(seed,x,z,1,ecology,clear);
    }
    public record Point(int x,int z) {}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ) {}

    private BorealWoodland() {}
}
