package blockbranch.realistic;

/** A distinct number wraps complete published267. Earlier worlds retain their
 * definitions, including unexplored terrain; absent never promotes a world. */
public final class WarmXericSchema {
    public static final int BASE=267,VERSION=268;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=BASE)throw new IllegalArgumentException("Warm xeric shrubs require complete267");
        return VERSION;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0)return saved==VERSION?Integer.MAX_VALUE:saved;
        return definition==1&&saved==VERSION?BASE:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private WarmXericSchema(){}
}
