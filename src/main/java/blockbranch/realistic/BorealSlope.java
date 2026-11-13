package blockbranch.realistic;

/** Slope-tolerant boreal stem placement, carried by saved vegetation flags.
 *
 * <p>The woodland palette admits ground up to a 0.40 slope, but the published
 * stem rule demanded every footprint sample within three blocks of the stem,
 * which fails above roughly 0.27. Steep palette-assigned ground therefore kept
 * its woodland label while receiving no stems. The tolerant relief is the
 * domain's own cap over the 8 m footprint diagonal. Footprint, water and
 * channel rules are unchanged, so stems still need dry, unrouted ground. */
public final class BorealSlope {
    public static final int FLAG=128;
    public static final int PUBLISHED_RELIEF=3;
    /** {@code ceil(0.40*8*sqrt2)}; measured to leave every crown voxel above terrain. */
    public static final int RELIEF=5;
    /** @param flags saved vegetation flags, as {@code RealisticGenerator.vegetationFlags()} or {@code (clearDefinition-1)/2}; never a raw clear definition */
    public static int relief(int flags){return (flags&FLAG)!=0?RELIEF:PUBLISHED_RELIEF;}
    private BorealSlope(){}
}
