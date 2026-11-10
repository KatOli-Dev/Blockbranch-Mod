package blockbranch.realistic;

/** Broad temperate deep-water identity over the already tested aphotic basin-bed habitat. */
public final class TemperateDeepOcean {
    public static boolean contains(int x,int z,DrainageNetwork d,ClimateField c){return TemperateOcean.deepCandidate(x,z,d,c);}


    private TemperateDeepOcean(){}
}
