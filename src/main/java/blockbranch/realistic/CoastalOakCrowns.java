package blockbranch.realistic;

/**
 * Saved vegetation flag (clear definitions 8193..16384) for mature coastal winter-rain oaks: {@link SummerDryWoodland#FAMILY} stems of
 * at least {@link #MATURE_HEIGHT} replace the published near-vertical pole under overlapping stacked crowns with a leaning main axis that
 * usually (about 60%) divides into two diverging stems, 3..5 lateral limbs of graded reach, and a separate foliage lobe on each limb and axis top
 * ({@link ForestTrees#tree(WoodlandTrees.Root,boolean,boolean,boolean,boolean)}). Heights, root positions and cohorts are unchanged and all
 * geometry stays within {@link #EXTENT} blocks horizontally of the root (the generation halo is 6) and at or below the root height. Young
 * stems (6..10), the 3..5 shrub layer and every other family keep their published geometry. Tree-clear plans replay these trees from the
 * saved generator, so the bit travels with the saved vegetation flags; plans without it replay the published crown and never-explored chunks
 * of an older world keep it. The summer-dry owner is still required for any stem to exist.
 */
public final class CoastalOakCrowns {
    public static final int FLAG=4096,MATURE_HEIGHT=12,EXTENT=6;
    public static boolean enabled(int flags){return (flags&FLAG)!=0;}
    private CoastalOakCrowns(){}
}
