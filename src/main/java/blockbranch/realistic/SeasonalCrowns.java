package blockbranch.realistic;

/**
 * Saved vegetation flag (clear definitions 1025..2048) for the seasonal dry broadleaf forest's crowns. Its stems are
 * family {@link DryForest#FAMILY}, produced only by {@link DryForestTrees#roots}; mature ones keep the published straight pole under
 * one fused ovoid unless this bit divides the leader into oblique limbs carrying separate, reach-capped lobes
 * ({@link ForestTrees#tree(WoodlandTrees.Root,boolean,boolean)}). Heights, cohorts, placement, spacing, materials and every
 * other family are unchanged, and it is independent of {@link BroadleafCrowns#FLAG} (ordinary-forest oaks). The flag rides in the
 * saved vegetation flags because tree-clear plans replay these trees from the saved generator and must erase the footprint that was
 * grown; plans without the bit replay the published crown, and never-explored chunks of an older world keep it.
 */
public final class SeasonalCrowns {
    public static final int FLAG=512;
    public static boolean enabled(int flags){return (flags&FLAG)!=0;}
    private SeasonalCrowns(){}
}
