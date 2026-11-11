package blockbranch.realistic;
/** Complete prior physical/biome foundation plus independently saved static land ice. */
public final class LandIceSchema {
    public static final int FOUNDATION=262,MAX=263;
    public static int schema(int base,int definition){if(definition==0)return base;if(definition!=1||base!=FOUNDATION)throw new IllegalArgumentException("Land ice requires complete schema262");return MAX;}
    public static int baseSchema(int saved,int definition){return definition==0?(saved==MAX?Integer.MAX_VALUE:saved):definition==1&&saved==MAX?FOUNDATION:Integer.MAX_VALUE;}
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private LandIceSchema(){}
}
