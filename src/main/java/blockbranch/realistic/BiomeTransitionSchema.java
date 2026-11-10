package blockbranch.realistic;

/** New-world vegetation barrier; historical readers reject before loading any chunks or clear plans. */
public final class BiomeTransitionSchema {
    public static final int FOUNDATION=255,MAX=256;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=FOUNDATION)throw new IllegalArgumentException("Biome transitions require complete foundation 255");
        return MAX;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0&&saved>=TerrainShape.SHAPE_VERSION&&saved<=MansionReplacementSchema.MAX)return saved;
        return definition==1&&saved==MAX?FOUNDATION:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private BiomeTransitionSchema(){}
}
