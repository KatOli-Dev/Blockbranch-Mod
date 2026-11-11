package blockbranch.realistic;
/**
 * Saved schema number for the independent opt-ins that each wrap the complete published 264 definition:
 * continental ice (polar geography, {@link ContinentalIceSchema}) and leaf-litter variation
 * ({@link LeafLitterSchema}). A saved number must identify exactly one set of definitions, so each
 * combination has its own: 265 = ice only, 266 = litter only, 267 = both. Absent definitions keep every
 * earlier number, and a number is read only under exactly the definitions it names, so no world is
 * reinterpreted as another. Neither definition changes the other's meaning: ice alters climate, cover
 * ownership, water and underground datum; litter only varies the state of litter blocks.
 */
public final class FoundationSchema {
    public static final int FOUNDATION=264,MAX=267;
    private static int number(int ice,int litter){return FOUNDATION+ice+2*litter;}
    private static boolean valid(int definition){return definition==0||definition==1;}
    public static int schema(int base,int ice,int litter){
        if(!valid(ice)||!valid(litter))throw new IllegalArgumentException("Unknown foundation definition");
        if(ice==0&&litter==0)return base;
        if(base!=FOUNDATION)throw new IllegalArgumentException("Continental ice and leaf-litter variation require complete schema264");
        return number(ice,litter);
    }
    /** The complete schema the saved number wraps, or {@code Integer.MAX_VALUE} when it does not name these definitions. */
    public static int baseSchema(int saved,int ice,int litter){
        if(!valid(ice)||!valid(litter))return Integer.MAX_VALUE;
        if(ice==0&&litter==0)return saved>FOUNDATION&&saved<=MAX?Integer.MAX_VALUE:saved;
        return saved==number(ice,litter)?FOUNDATION:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int ice,int litter){return baseSchema(saved,ice,litter)!=Integer.MAX_VALUE;}
    private FoundationSchema(){}
}
