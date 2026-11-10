package blockbranch.realistic;

/** Cold semiarid fescue bunchgrass, outside existing meadow, mesic boreal and warmer steppe domains. */
public final class ColdSteppe {
    public static boolean climateSuitable(ClimateField.Sample c){return c.meanTemperatureC()>=-6 && c.meanTemperatureC()<5 && c.moisture()>=0.12 && c.moisture()<0.32;}
    public static boolean eligible(DrainageNetwork.Sample terrain,double slope,boolean reserved,boolean meadow,ClimateField.Sample climate){return terrain.waterLevel()==DrainageNetwork.DRY && !reserved && !meadow && slope<=0.30 && climateSuitable(climate);}
    public static boolean contains(int x,int z,DrainageNetwork drainage,ClimateField climate){
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,drainage,climate);
    }
    /** 1=stony bare soil; 2=gravel gap; 3=short dry grass; 4=fescue tussock; 5=open sward; 6=silver shrub; 7=dead stems. */
    public static int cover(long seed,int x,int z,boolean habitat,boolean dry){
        if(!habitat || !dry)return 0;
        double patch=SurfacePatches.sample(seed ^ 0xC01DFE5C0EL,x,z,24);
        long cell=OakBirchWoodland.mix(seed ^ x*0x165667B19E3779F9L ^ z*0xD1B54A32D192ED03L);
        if(patch<.23)return 2;
        if(patch>.77)return 1;
        int group=(int)(cell&63);
        if(group<41){
            // Species colonies share the existing planted footprint and grass
            // substrate. Small edge mixing avoids hard rings around each tuft.
            double colony=SurfacePatches.sample(seed ^ 0x464553435545L,x,z,4);
            double edge=(((cell>>>12)&255)/255.0-.5)*.08;
            // Retain approximately 25/41 fescue within the planted footprint.
            return colony+edge>=.435?4:3;
        }
        if(group<53)return 5;
        if(group==53 && ((cell>>>8)&1)==0)return 6;
        if(group==54 && ((cell>>>9)&1)==0)return 7;
        return 1;
    }
    public record Point(int x,int z){}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ){}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,DrainageNetwork drainage,ClimateField climate){
        if(LandVegetation.surfaceReserved(drainage,x,z))return false;
        var c=climate.sample(x,z,drainage::sample);if(!climateSuitable(c))return false;
        boolean meadow=OakBirchWoodland.meadowClimateSuitable(c) && LowlandMeadow.surfaceGentleContains(x,z,(wx,wz)->LowlandMeadow.surfaceSite(drainage,wx,wz));
        double dx=(drainage.sample(x+32,z).height()-drainage.sample(x-32,z).height())/64.0,dz=(drainage.sample(x,z+32).height()-drainage.sample(x,z-32).height())/64.0;
        return eligible(drainage.sample(x,z),Math.hypot(dx,dz),false,meadow,c);
    }
    private ColdSteppe(){}
}
