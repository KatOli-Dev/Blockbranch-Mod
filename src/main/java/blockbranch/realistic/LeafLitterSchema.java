package blockbranch.realistic;
/**
 * Leaf-litter variation alone: complete prior schema 264 plus independently saved litter variation; absent keeps
 * the original single-state litter. Saved worlds are read through {@link FoundationSchema}, which numbers this
 * opt-in 266 and its combination with continental ice 267 (ice alone is 265); this class pins the single-definition
 * meaning, and 265 is rejected here because it names a different definition set.
 */
public final class LeafLitterSchema {
    public static final int FOUNDATION=264,RESERVED=265,MAX=266;
    public static int schema(int base,int definition){if(definition==0)return base;if(definition!=1||base!=FOUNDATION)throw new IllegalArgumentException("Leaf-litter variation requires complete schema264");return MAX;}
    public static int baseSchema(int saved,int definition){
        if(saved==RESERVED)return Integer.MAX_VALUE;
        return definition==0?(saved==MAX?Integer.MAX_VALUE:saved):definition==1&&saved==MAX?FOUNDATION:Integer.MAX_VALUE;
    }
    public static boolean matches(int saved,int definition){return baseSchema(saved,definition)!=Integer.MAX_VALUE;}
    private LeafLitterSchema(){}
}
