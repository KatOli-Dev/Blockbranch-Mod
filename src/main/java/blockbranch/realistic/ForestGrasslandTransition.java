package blockbranch.realistic;

/** Definition 1 feathers the seeded temperate stand boundary without changing biome or terrain fields. */
public final class ForestGrasslandTransition {
    // The historical stand boundary is .55. Outside this narrow interval the
    // original selectors, special habitats and all tree geometry remain exact.
    public static final double OPEN=.49,CLOSED=.61;
    public static double forestWeight(double stand){double t=Math.clamp((stand-OPEN)/(CLOSED-OPEN),0,1);return t*t*(3-2*t);}
    public static boolean active(long seed,int x,int z,DrainageNetwork d,ClimateField c){
        double stand=OakBirchWoodland.stand(seed,x,z);
        if(stand<=OPEN||stand>=CLOSED)return false;
        if(!OakBirchWoodland.climateSuitable(c.sample(x,z,d::sample))||!LowlandMeadow.surfacePotential(x,z,d,c)||LandVegetation.reserved(d,x,z)||LandVegetation.surfaceReserved(d,x,z))return false;
        // These selectors own distinct wet, tropical, warm and upland habitats.
        // A stand threshold alone is not permission to place temperate plants there.
        return !StonyShore.surfaceContains(x,z,d)&&!Swamp.surfaceSoilContains(seed,x,z,d,c)
            &&!MangroveSwamp.exact(seed,x,z,d,c)&&!HumidWoodland.surfaceContains(seed,x,z,d,c)
            &&!WarmWoodland.surfaceContains(seed,x,z,d,c)&&!Jungle.surfaceContains(seed,x,z,d,c)
            &&TropicalForestVariants.surfaceKind(seed,x,z,d,c)==0&&WindsweptUplands.surfaceMaterialKind(seed,x,z,d,c)==0
            &&ColdUplands.surfaceKind(seed,x,z,d,c)==0&&Badlands.surfaceKind(seed,x,z,d,c)==0;
    }
    public static boolean tree(long seed,TreeScatter.Point point){
        return unit(OakBirchWoodland.mix(point.priority()^0x4544474554524545L))<forestWeight(OakBirchWoodland.stand(seed,point.x(),point.z()));
    }
    public static boolean forestFloor(long seed,int x,int z){
        return unit(OakBirchWoodland.mix(seed^0x45444745464C4F4FL^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL))<forestWeight(OakBirchWoodland.stand(seed,x,z));
    }
    private static double unit(long value){return(value>>>11)*0x1.0p-53;}
    private ForestGrasslandTransition(){}
}
