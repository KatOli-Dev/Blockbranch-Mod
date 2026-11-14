package blockbranch.realistic;

/**
 * Saved vegetation flag (clear definitions 4097..8192) for mature old-growth-pine crowns: family-7 stems
 * ({@link OldGrowthTaiga#family}, pine) that are at least {@link #MATURE_HEIGHT} tall replace the continuous layered ovoid envelope with
 * branch-supported lateral limbs ending in separate foliage pads around a leader crown
 * ({@link TaigaTrees#tree(WoodlandTrees.Root,boolean)}). Young family-7 stems (at most 14), old-growth spruce (family 8), the other taiga
 * families, montane needleleaf and every non-boreal family keep their published geometry. Tree-clear plans replay these trees from
 * the saved generator and must erase the footprint that was grown, so the bit travels with the saved vegetation flags; plans without
 * it replay the published crown, and never-explored chunks of an older world keep it.
 */
public final class PineCrowns {
    public static final int FLAG=2048,MATURE_HEIGHT=23,FAMILY=7;
    public static boolean enabled(int flags){return (flags&FLAG)!=0;}
    private PineCrowns(){}
}
