package blockbranch.realistic;

/** Independent new-world dry-forest crown geometry over the complete broadleaf-crown 274 generator.
 * Absence keeps the published crowns for every older world, including chunks that were never
 * explored and saved clear plans; no promotion or migration. */
public final class SeasonalCrownSchema {
    public static final int BASE=274,VERSION=275;
    public static int schema(int base,int definition){
        if(definition==0)return base;
        if(definition!=1||base!=BASE)throw new IllegalArgumentException("Seasonal crowns require complete274");
        return VERSION;
    }
    public static int baseSchema(int saved,int definition){
        if(definition==0)return saved==VERSION?Integer.MAX_VALUE:saved;
        return definition==1&&saved==VERSION?BASE:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private SeasonalCrownSchema(){}
}
