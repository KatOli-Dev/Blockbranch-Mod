package blockbranch.realistic;

/** New-world redwood forests; historical readers reject before loading any chunks, biomes or canopy. */
public final class RedwoodForestSchema {
    public static final int FOUNDATION=BiomeTransitionSchema.MAX,MAX=257;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=FOUNDATION)throw new IllegalArgumentException("Redwood forests require complete biome-transition foundation 256");
        return MAX;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0)return saved==MAX?Integer.MAX_VALUE:saved;
        return definition==1&&saved==MAX?FOUNDATION:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private RedwoodForestSchema(){}
}
