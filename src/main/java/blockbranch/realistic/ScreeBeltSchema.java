package blockbranch.realistic;

/** Independent new-world cold scree-belt label over the complete humid-crown 279 generator.
 * Absence keeps the published grassland label for every older world, including chunks that were never
 * explored; explored chunks keep their stored quarts. No promotion or migration. */
public final class ScreeBeltSchema {
    public static final int BASE=279,VERSION=280;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=BASE)throw new IllegalArgumentException("Scree belt requires complete279");
        return VERSION;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0)return saved==VERSION?Integer.MAX_VALUE:saved;
        return definition==1&&saved==VERSION?BASE:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private ScreeBeltSchema(){}
}
