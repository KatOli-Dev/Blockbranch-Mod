package blockbranch.realistic;

/** Independent sanctuary body opt-in; published layout combinations retain their exact barriers. */
public final class SanctuaryReplacementSchema {
    public static final int FOUNDATION=233, VERSION=234, CRYPT_FOUNDATION=253, MAX=254;
    private SanctuaryReplacementSchema() {}
    public static int schema(int base,int definition) {
        if(definition==0)return base;
        if(definition!=1||(base!=FOUNDATION&&base!=CRYPT_FOUNDATION))throw new IllegalArgumentException("Sanctuary replacement requires complete foundation 233 or 253");
        return base==FOUNDATION?VERSION:MAX;
    }
    public static int baseSchema(int saved,int definition) {
        if(definition==0&&saved>=TerrainShape.SHAPE_VERSION&&saved<=GravelReliefSchema.MAX)return saved;
        return definition==1&&saved==VERSION?FOUNDATION:definition==1&&saved==MAX?CRYPT_FOUNDATION:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
}
