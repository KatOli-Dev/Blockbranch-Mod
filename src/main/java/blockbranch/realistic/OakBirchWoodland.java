package blockbranch.realistic;

import java.util.function.BiFunction;

/** Mesic temperate lowland stands, with coherent open meadow corridors and glades. */
public final class OakBirchWoodland {
    public static boolean climateSuitable(ClimateField.Sample climate) {
        return climate.meanTemperatureC()>=6 && climate.meanTemperatureC()<=19 && climate.moisture()>=0.48 && climate.moisture()<=0.85;
    }
    public static boolean meadowClimateSuitable(ClimateField.Sample climate) {
        return climate.meanTemperatureC()>=3 && climate.meanTemperatureC()<=23 && climate.moisture()>=0.27 && climate.moisture()<=0.85;
    }
    public static boolean contains(long seed,int x,int z,BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain,
            BiFunction<Integer,Integer,Boolean> channel,ClimateField climate) {
        x=Math.floorDiv(x,4)*4;z=Math.floorDiv(z,4)*4;
        return surfaceContains(seed,x,z,terrain,channel,climate);
    }
    static boolean contains(long seed,int x,int z,DrainageNetwork drainage,ClimateField climate) {return potential(seed,x,z,drainage,climate)&&!Forest.potential(seed,x,z,drainage,climate);}
    /** Original full mesic domain, retained for historical audits and other-habitat exclusions. */
    static boolean potential(long seed,int x,int z,DrainageNetwork drainage,ClimateField climate) {
        return !LandVegetation.reserved(drainage,x,z) && contains(seed,x,z,drainage::sample,(wx,wz)->drainage.channelAt(wx,wz).edge()>=0,climate);
    }
    /** Seeded 384 m stands and smaller gaps model patch structure, not an extra climate variable. */
    public static double stand(long seed,int x,int z) { return smoothNoise(seed,x,z,384,411); }
    public static boolean glade(long seed,int x,int z) { return smoothNoise(seed,x,z,48,719)<0.27; }
    private static double smoothNoise(long seed,int x,int z,int scale,long salt) {
        int gx=Math.floorDiv(x,scale),gz=Math.floorDiv(z,scale);
        double tx=Math.floorMod(x,scale)/(double)scale,tz=Math.floorMod(z,scale)/(double)scale;
        tx=tx*tx*(3-2*tx);tz=tz*tz*(3-2*tz);
        double a=value(seed,gx,gz,salt)*(1-tx)+value(seed,gx+1,gz,salt)*tx;
        double b=value(seed,gx,gz+1,salt)*(1-tx)+value(seed,gx+1,gz+1,salt)*tx;
        return a*(1-tz)+b*tz;
    }
    private static double value(long seed,int x,int z,long salt) { return (mix(seed ^ x*0x9E3779B97F4A7C15L ^ z*0xC2B2AE3D27D4EB4FL ^ salt)>>>11)*0x1.0p-53; }
    public static long mix(long v) { v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;v=(v^(v>>>27))*0x94D049BB133111EBL;return v^(v>>>31); }
    /** 1=podzol, 2=coarse dirt, 3=grass gap, 4=fern, 5=mushroom, 6=gap grass, 7=leaf litter. */
    public static int cover(long seed,int x,int z,boolean habitat,boolean dry){return cover(seed,x,z,habitat,dry,GroundEcology.NEUTRAL.shade(!glade(seed,x,z)),true);}
    public static int cover(long seed,int x,int z,boolean habitat,boolean dry,GroundEcology.Site ecology,boolean clear){
        return !habitat||!dry?0:GroundEcology.forest(seed,x,z,0,ecology,clear);
    }
    public record WoodlandPoint(int x,int z) {}

    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfaceContains(long seed,int x,int z,BiFunction<Integer,Integer,DrainageNetwork.Sample> terrain,
            BiFunction<Integer,Integer,Boolean> channel,ClimateField climate) {
        var center=terrain.apply(x,z);
        if(center.waterLevel()!=DrainageNetwork.DRY || channel.apply(x,z) || stand(seed,x,z)<0.55) return false;
        double dx=(terrain.apply(x+32,z).height()-terrain.apply(x-32,z).height())/64.0;
        double dz=(terrain.apply(x,z+32).height()-terrain.apply(x,z-32).height())/64.0;
        return Math.hypot(dx,dz)<=0.35 && climateSuitable(climate.sample(x,z,terrain));
    }
    /** Physical surface eligibility; biome and vegetation routing keep the quart selector. */
    static boolean surfacePotential(long seed,int x,int z,DrainageNetwork drainage,ClimateField climate) {
        return !LandVegetation.surfaceReserved(drainage,x,z) && surfaceContains(seed,x,z,drainage::sample,(wx,wz)->drainage.channelAt(wx,wz).edge()>=0,climate);
    }
    static boolean surfaceContains(long seed,int x,int z,DrainageNetwork d,ClimateField c){return surfacePotential(seed,x,z,d,c)&&!Forest.potential(seed,x,z,d,c);}
    private OakBirchWoodland() {}
}
