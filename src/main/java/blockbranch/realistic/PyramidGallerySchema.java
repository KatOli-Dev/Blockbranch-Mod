package blockbranch.realistic;

/** Independent gallery barrier applied after the existing structure combination is validated. */
public final class PyramidGallerySchema {
    public static final int FIRST=110, MAX=251;
    private PyramidGallerySchema() {}
    public static int schema(int base,int definition) {
        if(definition==0)return base;
        if(definition!=1||!((base>=102&&base<=109)||(base>=118&&base<=121)||(base>=130&&base<=133)||(base>=160&&base<=163)||(base>=200&&base<=203)||(base>=220&&base<=223)||(base>=240&&base<=243)))throw new IllegalArgumentException("Pyramid galleries require complete reimagined structures");
        return base+8;
    }
    public static int baseSchema(int saved,int definition) {
        if(definition==0)return saved;
        // The existing schema validator still owns all other feature combinations.
        return definition==1&&((saved>=FIRST&&saved<=117)||(saved>=126&&saved<=129)||(saved>=138&&saved<=141)||(saved>=168&&saved<=171)||(saved>=208&&saved<=211)||(saved>=228&&saved<=231)||(saved>=248&&saved<=MAX))?saved-8:Integer.MAX_VALUE;
    }
}
