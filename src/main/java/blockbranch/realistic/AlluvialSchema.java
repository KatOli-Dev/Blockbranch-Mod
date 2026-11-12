package blockbranch.realistic;

/** Independent new-world static alluvial foundation; no promotion or migration. */
public final class AlluvialSchema {
    public static final int BASE=269,VERSION=270;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=BASE)throw new IllegalArgumentException("Alluvial foundation requires complete269");
        return VERSION;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0)return saved==VERSION?Integer.MAX_VALUE:saved;
        return definition==1&&saved==VERSION?BASE:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private AlluvialSchema(){}
}
