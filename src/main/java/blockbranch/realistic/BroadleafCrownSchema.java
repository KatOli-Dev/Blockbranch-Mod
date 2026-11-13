package blockbranch.realistic;

/** Independent new-world oak crown geometry over the complete mangrove-roots 273 generator.
 * Absence keeps the published crowns for every older world, including chunks that were never
 * explored and saved clear plans; no promotion or migration. */
public final class BroadleafCrownSchema {
    public static final int BASE=273,VERSION=274;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=BASE)throw new IllegalArgumentException("Broadleaf crowns require complete273");
        return VERSION;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0)return saved==VERSION?Integer.MAX_VALUE:saved;
        return definition==1&&saved==VERSION?BASE:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private BroadleafCrownSchema(){}
}
