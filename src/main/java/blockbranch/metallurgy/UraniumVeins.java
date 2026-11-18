package blockbranch.metallurgy;

/**
 * Uranium depth window. The rule itself is {@link SectionVeins#URANIUM}; these names stay because frozen
 * historical readers compile against them.
 */
public final class UraniumVeins {
    public static final int MIN_DEPTH=8000, PEAK_DEPTH=9000, MAX_DEPTH=10000;
    private UraniumVeins(){}
    public static boolean contains(long seed,int x,int y,int z,int depth){return SectionVeins.reference(SectionVeins.URANIUM,seed,x,y,z,depth);}
}
