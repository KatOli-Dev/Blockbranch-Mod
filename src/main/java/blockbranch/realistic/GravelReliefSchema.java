package blockbranch.realistic;

/** Shallow gravel-desert geometry is opt-in on complete crowned foundations. */
public final class GravelReliefSchema {
    public static final int FOUNDATION=212, VERSION=213, PYRAMID_FOUNDATION=232, PYRAMID_VERSION=233, CRYPT_FOUNDATION=252, MAX=253;
    private GravelReliefSchema() {}
    public static int schema(int base,int definition) {
        if(definition==0)return base;
        if(definition!=1||(base!=FOUNDATION&&base!=PYRAMID_FOUNDATION&&base!=CRYPT_FOUNDATION))throw new IllegalArgumentException("Gravel relief requires complete foundation 212, 232 or 252");
        return base==FOUNDATION?VERSION:base==PYRAMID_FOUNDATION?PYRAMID_VERSION:MAX;
    }
    public static int baseSchema(int saved,int definition) {
        if(definition==0&&saved>=TerrainShape.SHAPE_VERSION&&saved<=BorealCrownSchema.MAX)return saved;
        return definition==1&&saved==VERSION?FOUNDATION:definition==1&&saved==PYRAMID_VERSION?PYRAMID_FOUNDATION:definition==1&&saved==MAX?CRYPT_FOUNDATION:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition) {return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
}
