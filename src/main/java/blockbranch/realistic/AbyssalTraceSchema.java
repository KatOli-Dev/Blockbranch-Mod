package blockbranch.realistic;

/** Append-only opt-in over the complete published schema260 foundation. */
public final class AbyssalTraceSchema {
    public static final int FOUNDATION=260,MAX=261;
    public static int schema(int base,int definition){if(definition==0)return base;if(definition!=1||base!=FOUNDATION)throw new IllegalArgumentException("Abyssal traces require complete schema260");return MAX;}
    public static int baseSchema(int saved,int definition){return definition==0?(saved==MAX?Integer.MAX_VALUE:saved):definition==1&&saved==MAX?FOUNDATION:Integer.MAX_VALUE;}
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private AbyssalTraceSchema(){}
}
