package blockbranch.realistic;
/** Cohesive opt-in: polar climate, grounded static ice and cold freshwater admission. */
public final class ContinentalIceSchema {
    public static final int FOUNDATION=264,MAX=265;
    public static int schema(int base,int definition){if(definition==0)return base;if(definition!=1||base!=FOUNDATION)throw new IllegalArgumentException("Continental ice requires complete schema264");return MAX;}
    public static int baseSchema(int saved,int definition){return definition==0?(saved==MAX?Integer.MAX_VALUE:saved):definition==1&&saved==MAX?FOUNDATION:Integer.MAX_VALUE;}
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private ContinentalIceSchema(){}
}
