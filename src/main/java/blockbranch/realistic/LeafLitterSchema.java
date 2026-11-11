package blockbranch.realistic;
/**
 * Complete prior schema 264 plus independently saved leaf-litter variation; absent keeps the original
 * single-state litter. A saved schema number must identify exactly one set of definitions, so this
 * foundation uses 266 and rejects 265: 265 is reserved because a separate, unmerged foundation also
 * wrapped complete 264 as 265, and a reader that accepted both would reinterpret one world as the other.
 * A world that combines foundations needs its own number and reader; never reuse 265 or 266.
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
