package blockbranch.realistic;

/** Independent cool-desert opt-in over the complete published schema259 preset. */
public final class CoolDesertSchema {
    public static final int FOUNDATION=259,MAX=260;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=FOUNDATION)throw new IllegalArgumentException("Cool desert requires complete dry-forest foundation 259");
        return MAX;
    }
    public static int baseSchema(int saved,int definition){return definition==0?(saved==MAX?Integer.MAX_VALUE:saved):definition==1&&saved==MAX?FOUNDATION:Integer.MAX_VALUE;}
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private CoolDesertSchema(){}
}
