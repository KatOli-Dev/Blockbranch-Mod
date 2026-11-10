package blockbranch.realistic;

/** Opt-in new-world crown geometry; earlier readers must stop before loading structures. */
public final class BorealCrownSchema {
    public static final int VERSION=142, MANSION_PERIMETER_VERSION=172, JUNGLE_REPLACEMENT_VERSION=212, PYRAMID_REPLACEMENT_VERSION=232, MAX=252;
    private BorealCrownSchema() {}
    public static int schema(int base,int definition) {
        if(definition==0)return base;
        if(definition!=1||(base!=141&&base!=171&&base!=211&&base!=231&&base!=251))throw new IllegalArgumentException("Boreal crowns require complete crypt/gallery foundation 141 or mansion perimeter foundation 171 or jungle replacement foundation 211 or pyramid replacement foundation 231 or crypt replacement foundation 251");
        return base==141?VERSION:base==171?MANSION_PERIMETER_VERSION:base==211?JUNGLE_REPLACEMENT_VERSION:base==231?PYRAMID_REPLACEMENT_VERSION:MAX;
    }
    public static int baseSchema(int saved,int definition) {
        return definition==0?saved:definition==1&&saved==VERSION?141:definition==1&&saved==MANSION_PERIMETER_VERSION?171:definition==1&&saved==JUNGLE_REPLACEMENT_VERSION?211:definition==1&&saved==PYRAMID_REPLACEMENT_VERSION?231:definition==1&&saved==MAX?251:Integer.MAX_VALUE;
    }
}
