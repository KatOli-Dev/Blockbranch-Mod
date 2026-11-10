package blockbranch.realistic;

/** Closed temperate stand interiors, retaining the original dappled woodland fringes and glades. */
public final class Forest {
    public static boolean potential(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfacePotential(seed,x,z,d,c);
    }
    public static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return potential(seed,x,z,d,c)&&!BirchForest.contains(seed,x,z,d,c);}
    public static int cover(long seed,int x,int z,GroundEcology.Site site,boolean clear){return WoodlandDetails.temperateFloor(seed,x,z,site,clear,GroundEcology.forest(seed,x,z,0,site,clear));}
    public record Point(int x,int z){}

    /** Block-resolution material eligibility; the original selector retains biome routing. */
    static boolean surfacePotential(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        return OakBirchWoodland.stand(seed,x,z)>=.65&&OakBirchWoodland.surfacePotential(seed,x,z,d,c);
    }
    private Forest(){}
}
