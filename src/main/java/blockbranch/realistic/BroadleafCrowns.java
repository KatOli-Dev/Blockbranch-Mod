package blockbranch.realistic;

/**
 * Saved vegetation flag (clear definitions 513..1024) for ordinary-forest oak crowns: mature non-birch family-1 oaks
 * divide into oblique limbs carrying separate lobes instead of one steep limb bundle under a straight pole and one fused
 * ovoid ({@link ForestTrees#tree(WoodlandTrees.Root,boolean)}). Family 1 is the ordinary and flower-forest oak; birches,
 * birch-led oak recruits, young stems and every family that reuses the forest tree (dappled, dark/cherry, dry,
 * summer-dry, windswept) keep their published geometry. The flag rides in the saved vegetation flags because tree-clear
 * plans replay these trees from the saved generator and must erase the footprint that was grown; plans without the bit
 * replay the published oak, and never-explored chunks of an older world keep it.
 */
public final class BroadleafCrowns {
    public static final int FLAG=256;
    public static boolean enabled(int flags){return (flags&FLAG)!=0;}
    private BroadleafCrowns(){}
}
