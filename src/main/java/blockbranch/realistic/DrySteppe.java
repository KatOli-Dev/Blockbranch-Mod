package blockbranch.realistic;

import java.util.function.BiFunction;

/** Temperate/warm continental dryland, below the mesic meadow's moisture range. */
public final class DrySteppe {
    public static boolean climateSuitable(ClimateField.Sample climate) {
        return climate.meanTemperatureC()>=5 && climate.meanTemperatureC()<=24 && climate.moisture()>=0.12 && climate.moisture()<0.27;
    }
    public static boolean contains(int x,int z,BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain,
            BiFunction<Integer,Integer,Boolean> channel,ClimateField climate) {
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(x,z,terrain,channel,climate);
    }
    static boolean contains(int x,int z,DrainageNetwork drainage,ClimateField climate) {
        return contains(x,z,drainage::sample,(wx,wz)->LandVegetation.reserved(drainage,wx,wz),climate);
    }
    /** 1=bare stony soil; 2=gravel lag; 3=dry bunchgrass; 4=live silver shrub; 5=bare dry sward; 6=dead stems. */
    public static int cover(long seed,int x,int z,boolean habitat,boolean dry) {
        if(!habitat || !dry) return 0;
        double patch=SurfacePatches.sample(seed ^ 0x4452595354455050L,x,z,18);
        if(patch<.28) return 2;
        if(patch>.72) return 5;
        long cell=OakBirchWoodland.mix(seed ^ x*0x165667B19E3779F9L ^ z*0xD1B54A32D192ED03L);
        int group=(int)(cell&31);
        if(group==10) return 4;
        if(group==11 && ((cell>>>8)&1)==0) return 6;
        // Grass and its rooted sward form soft, irregular patches within the
        // existing soil palette; woody plants and exposed gravel stay fixed.
        double sward=SurfacePatches.sample(seed^0x5354455050454752L,x,z,6);
        double grass=(OakBirchWoodland.mix(cell^0x42554E4348475241L)>>>11)*0x1.0p-53;
        return grass<Math.clamp(.33+2*(sward-.5),.02,.82)?3:1;
    }
    public record SteppePoint(int x,int z) {}

    public record Transition(int insideX,int insideZ,int outsideX,int outsideZ) {}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain,
            BiFunction<Integer,Integer,Boolean> channel,ClimateField climate) {
        var center=terrain.apply(x,z);int elevation=center.height()+1-TerrainShape.SEA_LEVEL;
        if(center.waterLevel()!=DrainageNetwork.DRY || channel.apply(x,z)) return false;
        double dx=(terrain.apply(x+32,z).height()-terrain.apply(x-32,z).height())/64.0;
        double dz=(terrain.apply(x,z+32).height()-terrain.apply(x,z-32).height())/64.0;
        if(Math.hypot(dx,dz)>0.30 || !climateSuitable(climate.sample(x,z,terrain))) return false;
        if((elevation<30 || elevation>1400) && !SlopeStability.supportsSoil(x,z,center.height(),dx,dz,(wx,wz)->{var nearby=terrain.apply(wx,wz);return new SlopeStability.Site(nearby.height(),nearby.waterLevel()==DrainageNetwork.DRY&&!channel.apply(wx,wz));}))return false;
        // Keep the continental habitat away from actual sea-water crossings.
        for(int[] d:new int[][]{{2048,0},{-2048,0},{0,2048},{0,-2048}}) {
            var neighbor=terrain.apply(x+d[0],z+d[1]);
            if(!neighbor.closed() && neighbor.waterLevel()==TerrainShape.SEA_LEVEL && neighbor.height()+1<TerrainShape.SEA_LEVEL) return false;
        }
        return true;
    }
    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(int x,int z,DrainageNetwork drainage,ClimateField climate) {
        return surfaceContains(x,z,drainage::sample,(wx,wz)->LandVegetation.surfaceReserved(drainage,wx,wz),climate);
    }
    private DrySteppe() {}
}
