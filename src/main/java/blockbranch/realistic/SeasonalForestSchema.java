package blockbranch.realistic;

/** The independent seasonal-forest refinement wraps complete published268.
 * Absence retains all earlier definitions, including unexplored generation. */
public final class SeasonalForestSchema {
    public static final int BASE=268,VERSION=269;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=BASE)throw new IllegalArgumentException("Seasonal forest requires complete268");
        return VERSION;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0)return saved==VERSION?Integer.MAX_VALUE:saved;
        return definition==1&&saved==VERSION?BASE:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private SeasonalForestSchema(){}
}
