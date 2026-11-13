package blockbranch.realistic;

/**
 * Saved vegetation flag (clear definitions 2049..4096) for mature old-growth-birch crowns: stems of family
 * {@link TemperateForestVariants#BIRCH_FAMILY} that are birch and at least {@link #MATURE_HEIGHT} tall replace the published tall,
 * densely stacked column with graded ascending limbs carrying separate lobes
 * ({@link ForestTrees#tree(WoodlandTrees.Root,boolean,boolean,boolean)}). Young stems (at most 12), the oak recruits, ordinary
 * birch (family 2) and every other family keep their published geometry. The flag rides in the saved vegetation flags because
 * tree-clear plans replay these trees from the saved generator and must erase the footprint that was grown; plans without the bit
 * replay the published crown, and never-explored chunks of an older world keep it.
 */
public final class BirchCrowns {
    public static final int FLAG=1024,MATURE_HEIGHT=13;
    public static boolean enabled(int flags){return (flags&FLAG)!=0;}
    private BirchCrowns(){}
}
