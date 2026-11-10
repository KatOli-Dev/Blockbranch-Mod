package blockbranch.metallurgy;

/**
 * Exact per-section answers of the shared-template {@code *Veins.contains} rules (cell 12,
 * definition 3 and later), for full 16x16x16 section generation.
 *
 * <p>Per block, {@code contains} hashes each visited cell, and every ore repeats that for all
 * 4096 blocks. Here each cell near the section is hashed once and its ellipsoid is rasterized
 * over the blocks that visit that cell, using the same per-(block, cell) predicate and the same
 * floating-point expressions: the density threshold from the block's own depth, the centre
 * {@code (long)a*CELL+CELL*unit(...)}, {@code (x+.5-centre)/r}. {@code contains} returns true
 * when any visited cell passes, so the OR over cells is order independent and the mask equals
 * the per-block answer. Masks are built lazily per ore and are not thread-safe; use one instance
 * per generating thread and section. {@code SectionGenerationCheck} compares every rule against the
 * per-block classes. A new vein class on a different template must not be added here.
 */
public final class SectionVeins {
    public static final int TIN=0,ZINC=1,MANGANESE=2,TUNGSTEN=3,NICKEL=4,CHROMIUM=5,LEAD=6,FOOLS_DIAMOND=7,
        SILVER=8,PLATINUM=9,TITANIUM=10,MAGNETITE=11,RESONITE=12,BISMUTH=13,URANIUM=14;
    private static final int CELL=12;
    private static final long[] EMPTY=new long[64];
    record Rule(long salt,int min,int peak,int max,double chance,double radius) {}
    // Constants copied from each class; the check fails if any drifts from its source.
    static final Rule[] RULES={
        new Rule(0x54494E5F76310001L,TinVeins.MIN_DEPTH,TinVeins.PEAK_DEPTH,TinVeins.MAX_DEPTH,.42,1.85),
        new Rule(0x5A494E435F763101L,ZincVeins.MIN_DEPTH,ZincVeins.PEAK_DEPTH,ZincVeins.MAX_DEPTH,.48,1.85),
        new Rule(0x4D4E5F7631000001L,ManganeseVeins.MIN_DEPTH,ManganeseVeins.PEAK_DEPTH,ManganeseVeins.MAX_DEPTH,.32,1.7),
        new Rule(0x575F763100000001L,TungstenVeins.MIN_DEPTH,TungstenVeins.PEAK_DEPTH,TungstenVeins.MAX_DEPTH,.26,1.5),
        new Rule(0x4E495F7631000001L,NickelVeins.MIN_DEPTH,NickelVeins.PEAK_DEPTH,NickelVeins.MAX_DEPTH,.28,1.6),
        new Rule(0x43525F7631000001L,ChromiumVeins.MIN_DEPTH,ChromiumVeins.PEAK_DEPTH,ChromiumVeins.MAX_DEPTH,.24,1.5),
        new Rule(0x50425F7631000001L,LeadVeins.MIN_DEPTH,LeadVeins.PEAK_DEPTH,LeadVeins.MAX_DEPTH,.38,1.8),
        new Rule(0x464F4F4C5F763100L,FoolsDiamondVeins.MIN_DEPTH,FoolsDiamondVeins.PEAK_DEPTH,FoolsDiamondVeins.MAX_DEPTH,.18,1.35),
        new Rule(0x41475F7631000001L,SilverVeins.MIN_DEPTH,SilverVeins.PEAK_DEPTH,SilverVeins.MAX_DEPTH,.28,1.6),
        new Rule(0x50545F7631000001L,PlatinumVeins.MIN_DEPTH,PlatinumVeins.PEAK_DEPTH,PlatinumVeins.MAX_DEPTH,.16,1.4),
        new Rule(0x54495F7631000001L,TitaniumVeins.MIN_DEPTH,TitaniumVeins.PEAK_DEPTH,TitaniumVeins.MAX_DEPTH,.24,1.6),
        new Rule(0x4D475F7631000001L,MagnetiteVeins.MIN_DEPTH,MagnetiteVeins.PEAK_DEPTH,MagnetiteVeins.MAX_DEPTH,.28,1.6),
        new Rule(0x52535F7631000001L,ResoniteVeins.MIN_DEPTH,ResoniteVeins.PEAK_DEPTH,ResoniteVeins.MAX_DEPTH,.24,1.6),
        new Rule(0x42495F7631000001L,BismuthVeins.MIN_DEPTH,BismuthVeins.PEAK_DEPTH,BismuthVeins.MAX_DEPTH,.34,1.6),
        new Rule(0x55525F7631000001L,UraniumVeins.MIN_DEPTH,UraniumVeins.PEAK_DEPTH,UraniumVeins.MAX_DEPTH,.14,1.6),
    };
    private final long seed;
    private final int x0,y0,z0;
    private final int[] ground;
    private final long[][] masks=new long[RULES.length][];

    /** {@code ground} is indexed {@code z*16+x} relative to (x0, z0) and must not change while in use. */
    public SectionVeins(long seed,int x0,int y0,int z0,int[] ground){
        if((x0&15)!=0||(y0&15)!=0||(z0&15)!=0||ground.length!=256)throw new IllegalArgumentException("Section origin or ground");
        this.seed=seed;this.x0=x0;this.y0=y0;this.z0=z0;this.ground=ground;
    }
    public boolean covers(int y){return (y-y0)>>>4==0;}
    /** Same answer as {@code <Ore>Veins.contains(seed,x0+lx,y0+ly,z0+lz,ground[lz*16+lx]-(y0+ly))}. */
    public boolean contains(int ore,int lx,int ly,int lz){
        var mask=masks[ore];if(mask==null)masks[ore]=mask=rasterize(RULES[ore]);
        int bit=(ly<<8)|(lz<<4)|lx;
        return (mask[bit>>>6]&1L<<bit)!=0;
    }
    private long[] rasterize(Rule r){
        long[] mask=null;
        for(int a=Math.floorDiv(x0,CELL)-1;a<=Math.floorDiv(x0+15,CELL)+1;a++)
            for(int b=Math.floorDiv(y0,CELL)-1;b<=Math.floorDiv(y0+15,CELL)+1;b++)
                for(int c=Math.floorDiv(z0,CELL)-1;c<=Math.floorDiv(z0+15,CELL)+1;c++){
                    long h=mix(seed^r.salt^(long)a*0x9E3779B97F4A7C15L^(long)b*0xD6E8FEB86659FD93L^(long)c*0xA24BAED4963EE407L);
                    double u=unit(h);
                    // density never exceeds 1 and IEEE products are monotone, so u>=chance
                    // rejects this cell for every depth exactly as the per-block test would.
                    if(u>=r.chance)continue;
                    long shape=mix(h^0x7665696eL);
                    double rx=r.radius*(.9+.2*unit(shape)),ry=r.radius*(.6+.2*unit(mix(shape^0x59524144495553L))),rz=r.radius*(.75+.2*unit(mix(shape^0x5A524144495553L)));
                    double ox=(long)a*CELL+CELL*unit(mix(shape));
                    double oy=(long)b*CELL+CELL*unit(mix(shape^17));
                    double oz=(long)c*CELL+CELL*unit(mix(shape^37));
                    // A block visits its own cell, the previous cell when its offset is <2 and the
                    // next when >9: visitors of cell a are x in [12a-2, 12a+13]. A block passing
                    // the ellipsoid test lies within (o-r-.5, o+r-.5); the bounds below are wider.
                    int xa=Math.max(Math.max(x0,a*CELL-2),(int)Math.floor(ox-rx-1)),xb=Math.min(Math.min(x0+15,a*CELL+13),(int)Math.ceil(ox+rx));
                    int ya=Math.max(Math.max(y0,b*CELL-2),(int)Math.floor(oy-ry-1)),yb=Math.min(Math.min(y0+15,b*CELL+13),(int)Math.ceil(oy+ry));
                    int za=Math.max(Math.max(z0,c*CELL-2),(int)Math.floor(oz-rz-1)),zb=Math.min(Math.min(z0+15,c*CELL+13),(int)Math.ceil(oz+rz));
                    for(int y=ya;y<=yb;y++)for(int z=za;z<=zb;z++)for(int x=xa;x<=xb;x++){
                        int depth=ground[(z-z0)*16+(x-x0)]-y;
                        if(depth<=r.min||depth>=r.max)continue;
                        double density=depth<=r.peak?(depth-r.min)/(double)(r.peak-r.min):(r.max-depth)/(double)(r.max-r.peak);
                        if(u>=r.chance*density)continue;
                        double dx=(x+.5-ox)/rx,dy=(y+.5-oy)/ry,dz=(z+.5-oz)/rz;
                        if(dx*dx+dy*dy+dz*dz<1){
                            if(mask==null)mask=new long[64];
                            int bit=((y-y0)<<8)|((z-z0)<<4)|(x-x0);mask[bit>>>6]|=1L<<bit;
                        }
                    }
                }
        return mask==null?EMPTY:mask;
    }
    private static double unit(long value){return(value>>>11)*0x1.0p-53;}
    private static long mix(long value){value=(value^(value>>>30))*0xBF58476D1CE4E5B9L;value=(value^(value>>>27))*0x94D049BB133111EBL;return value^(value>>>31);}
}
