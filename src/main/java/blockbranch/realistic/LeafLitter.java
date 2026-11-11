package blockbranch.realistic;

/**
 * Saved leaf-litter definition 1. The original cover codes always rendered the block's default
 * state (one segment facing north), so every litter column was an identical mark. A saved
 * profile now stores one of the block's 16 facing x segment states per litter column in
 * reserved cover codes: podzol litter 65..80, rooted-dirt litter 81..96 (woodland array) and
 * 13..28 (warm array, podzol soil). Each code's soil is its base litter code's soil, so
 * variation never moves ground or plants. Bamboo, swamp and windswept litter keep their
 * single-state codes.
 *
 * <p>Segment count follows a local pile field: thin litter is mostly one or two segments and
 * thick cores include three and four. Facing is an independent uniform draw. The weights and
 * the presence field below are declared model conventions, not leaf-fall measurements; they
 * only make the block's own variants and irregular coverage visible.
 */
public final class LeafLitter {
    public static final int STATES=16,PODZOL_FIRST=65,ROOTED_FIRST=81,WOODLAND_LAST=96,WARM_FIRST=13,WARM_LAST=28;
    private static final int PODZOL=7,ROOTED=9,WARM=3;
    private static final long FACING_SALT=0x4C4954464143L,AMOUNT_SALT=0x4C4954414D54L,PILE_SALT=0x4C4954504C45L,COVER_SALT=0x4C4954434F56L,ROLL_SALT=0x4C4954524F4CL;

    public static boolean woodlandVariant(int cover){return cover>=PODZOL_FIRST&&cover<=WOODLAND_LAST;}
    public static boolean warmVariant(int cover){return cover>=WARM_FIRST&&cover<=WARM_LAST;}
    /** The unvaried litter code whose soil and plant presence a variant shares; other codes pass through. */
    public static int woodlandBase(int cover){return cover>=PODZOL_FIRST&&cover<ROOTED_FIRST?PODZOL:cover>=ROOTED_FIRST&&cover<=WOODLAND_LAST?ROOTED:cover;}
    public static int warmBase(int cover){return warmVariant(cover)?WARM:cover;}

    /** Variant index 0..15 of a reserved code: facing is index&3 (north, east, south, west) and segments are 1+(index>>2). */
    public static int woodlandIndex(int cover){if(!woodlandVariant(cover))throw new IllegalArgumentException("Woodland litter variant");return cover<ROOTED_FIRST?cover-PODZOL_FIRST:cover-ROOTED_FIRST;}
    public static int warmIndex(int cover){if(!warmVariant(cover))throw new IllegalArgumentException("Warm litter variant");return cover-WARM_FIRST;}

    /** Replace an unvaried woodland litter code (7 podzol, 9 rooted dirt) by a variant; every other code is unchanged. */
    static int woodland(long seed,int x,int z,int cover,GroundEcology.Site site){
        return cover==PODZOL?PODZOL_FIRST+variant(seed,x,z,site):cover==ROOTED?ROOTED_FIRST+variant(seed,x,z,site):cover;
    }
    static int warm(long seed,int x,int z,int cover,GroundEcology.Site site){return cover==WARM?WARM_FIRST+variant(seed,x,z,site):cover;}

    /** Facing and segment count for one column; thicker local piles shift weight toward three and four segments. */
    static int variant(long seed,int x,int z,GroundEcology.Site site){
        double pile=GroundEcology.colonyStrength(seed,PILE_SALT,x,z,20,5,12,site);
        double facing=unit(OakBirchWoodland.mix(seed^FACING_SALT^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL));
        double amount=unit(OakBirchWoodland.mix(seed^AMOUNT_SALT^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L));
        double one=.58-.34*pile,two=.27,three=.11+.14*pile;
        int segments=amount<one?0:amount<one+two?1:amount<one+two+three?2:3;
        return segments*4+Math.min(3,(int)(facing*4));
    }

    /**
     * Presence in the two woodlands that formerly placed litter on a uniform 80-86% of clear ground.
     * Pile colonies and canopy shelter now leave irregular bare soil: sheltered ground keeps litter
     * more often than open ground. These constants are a visual-irregularity convention, not a
     * measured litter cover.
     */
    static boolean present(long seed,int x,int z,GroundEcology.Site site){
        double cover=GroundEcology.colonyStrength(seed,COVER_SALT,x,z,28,8,17,site);
        double chance=site.shaded()?.42+.55*cover:.18+.45*cover;
        return unit(OakBirchWoodland.mix(seed^ROLL_SALT^x*0x9E3779B97F4A7C15L^z*0xC2B2AE3D27D4EB4FL))<chance;
    }
    private static double unit(long value){return(value>>>11)*0x1.0p-53;}
    private LeafLitter(){}
}
