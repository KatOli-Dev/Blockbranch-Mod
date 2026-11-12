package blockbranch.realistic;

/** Independent new-world boreal slope tolerance; no promotion or migration.
 * It wraps the complete published270 alluvial generator. Absence keeps every
 * earlier definition, including unexplored terrain in older saves. */
public final class BorealSlopeSchema {
    public static final int BASE=270,VERSION=271;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=BASE)throw new IllegalArgumentException("Boreal slope tolerance requires complete270");
        return VERSION;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0)return saved==VERSION?Integer.MAX_VALUE:saved;
        return definition==1&&saved==VERSION?BASE:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private BorealSlopeSchema(){}
}
