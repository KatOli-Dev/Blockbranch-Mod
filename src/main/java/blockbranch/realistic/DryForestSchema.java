package blockbranch.realistic;

/** Independent new-world static climatology/forest opt-in over complete schema 258. */
public final class DryForestSchema {
    public static final int FOUNDATION=258,MAX=259;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=FOUNDATION)throw new IllegalArgumentException("Dry forest requires complete surface-ownership foundation 258");
        return MAX;
    }
    public static int baseSchema(int saved,int definition){return definition==0?(saved==MAX?Integer.MAX_VALUE:saved):definition==1&&saved==MAX?FOUNDATION:Integer.MAX_VALUE;}
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private DryForestSchema(){}
}
