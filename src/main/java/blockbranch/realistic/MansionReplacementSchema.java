package blockbranch.realistic;

/** Independent mansion body barrier outside all published structure and terrain wrappers. */
public final class MansionReplacementSchema {
    public static final int FOUNDATION=254, MAX=255;
    private MansionReplacementSchema() {}
    public static int schema(int base,int definition) {
        if(definition==0)return base;
        if(definition!=1||base!=FOUNDATION)throw new IllegalArgumentException("Mansion replacement requires complete foundation 254");
        return MAX;
    }
    public static int baseSchema(int saved,int definition) {
        if(definition==0&&saved>=TerrainShape.SHAPE_VERSION&&saved<=SanctuaryReplacementSchema.MAX)return saved;
        return definition==1&&saved==MAX?FOUNDATION:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
}
