package blockbranch.realistic;

/** Reproducible witnesses for the three conditional extensions, independent of Minecraft chunk order. */
public final class SlopeCorrection {
    public enum Kind { MEADOW, BOREAL, STEPPE }
    public record Point(int x,int z){}
    public static double slope(DrainageNetwork d,int x,int z){return Math.hypot((d.sample(x+32,z).height()-d.sample(x-32,z).height())/64.0,(d.sample(x,z+32).height()-d.sample(x,z-32).height())/64.0);}
    public static boolean added(Kind kind,long seed,int x,int z,DrainageNetwork d,ClimateField c){
        int e=d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL;
        return switch(kind){
            case MEADOW->!LowlandMeadow.gentleContains(x,z,(wx,wz)->LowlandMeadow.site(d,wx,wz))&&LowlandMeadow.contains(x,z,d,c)&&!OakBirchWoodland.potential(seed,x,z,d,c)&&!WarmWoodland.contains(seed,x,z,d,c);
            case BOREAL->(e<40||e>1800)&&BorealWoodland.contains(seed,x,z,d,c);
            case STEPPE->(e<30||e>1400)&&DrySteppe.contains(x,z,d,c);
        };
    }

    private SlopeCorrection(){}
}
