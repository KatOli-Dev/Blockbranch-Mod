package blockbranch.realistic;

/** Independent new-world mangrove root geometry over the complete boreal-slope 272 generator.
 * Absence keeps the published root shapes for every older world, including chunks that were
 * never explored; no promotion or migration. */
public final class MangroveRootsSchema {
    public static final int BASE=272,VERSION=273;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=BASE)throw new IllegalArgumentException("Mangrove roots require complete272");
        return VERSION;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0)return saved==VERSION?Integer.MAX_VALUE:saved;
        return definition==1&&saved==VERSION?BASE:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private MangroveRootsSchema(){}
}
