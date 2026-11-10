package blockbranch.horizon;

/**
 * Quadtree layout of the visual-only distant-terrain horizon.
 *
 * <p>A tile always holds {@link #CELLS}×{@link #CELLS} square cells; a level-L cell is
 * 2<sup>L</sup> blocks wide, so a level-0 tile is exactly one chunk and the nearest
 * horizon geometry has native block resolution. Samples cover the tile plus a one-cell
 * border so every tile can build its edge faces without waiting for neighbours.
 * Each cell is represented by the column at its centre (the block itself at level 0).
 */
public final class HorizonTiles {
    public static final int CELLS=16,SAMPLES=CELLS+2,MAX_LEVEL=6;
    /**
     * Ints per sample: top block y, top state, ground block y, ground state, deep state,
     * water top y, biome, and the lowest y of the overlay run that starts at the top. A run
     * ending more than one block above the ground is a floating canopy: the cell then has
     * ground below an air gap instead of one solid column, so trees do not become pillars.
     */
    public static final int FIELDS=8,TOP=0,TOP_STATE=1,GROUND=2,GROUND_STATE=3,DEEP_STATE=4,WATER=5,BIOME=6,CANOPY_BOTTOM=7;
    /** True when the sample's top run floats above its ground with a gap of air. */
    public static boolean floating(int[] d,int o){return d[o+CANOPY_BOTTOM]>d[o+GROUND]+1;}
    public static final int NONE=Integer.MIN_VALUE;
    private static final int COORD_BITS=28,COORD_MASK=(1<<COORD_BITS)-1;

    public static long key(int level,int tx,int tz) {
        if(level<0||level>MAX_LEVEL)throw new IllegalArgumentException("Horizon level "+level);
        return ((long)level<<56)|((long)(tx&COORD_MASK)<<COORD_BITS)|(tz&COORD_MASK);
    }
    public static int level(long key){return (int)(key>>>56);}
    public static int x(long key){return ((int)(key>>>COORD_BITS)<<(32-COORD_BITS))>>(32-COORD_BITS);}
    public static int z(long key){return ((int)key<<(32-COORD_BITS))>>(32-COORD_BITS);}
    public static int cell(int level){return 1<<level;}
    public static int span(int level){return CELLS<<level;}
    public static int originX(long key){return x(key)*span(level(key));}
    public static int originZ(long key){return z(key)*span(level(key));}
    /** World block coordinate represented by sample {@code i} (0..SAMPLES-1, 1..CELLS inside the tile). */
    public static int sampleCoordinate(int origin,int level,int i){return origin+(i-1)*cell(level)+(cell(level)>>1);}
    public static long child(long key,int quadrant){int l=level(key)-1;return key(l,x(key)*2+(quadrant&1),z(key)*2+(quadrant>>1));}
    public static long parent(long key){int l=level(key)+1;return key(l,Math.floorDiv(x(key),2),Math.floorDiv(z(key),2));}
    /** Horizontal distance from (x,z) to the nearest point of the tile, in blocks. */
    public static double distance(long key,double x,double z) {
        int s=span(level(key));double minX=originX(key),minZ=originZ(key);
        double dx=Math.max(0,Math.max(minX-x,x-(minX+s))),dz=Math.max(0,Math.max(minZ-z,z-(minZ+s)));
        return Math.sqrt(dx*dx+dz*dz);
    }
    /**
     * Every tile, at every level, with a sample (border samples included) inside the chunk;
     * these are the tiles a change to that chunk can alter.
     */
    public static long[] sampling(int chunkX,int chunkZ) {
        var out=new it.unimi.dsi.fastutil.longs.LongArrayList();
        for(int level=0;level<=MAX_LEVEL;level++) {
            int[] xs=axis(chunkX*16,level),zs=axis(chunkZ*16,level);
            for(int tz:zs)for(int tx:xs)out.add(key(level,tx,tz));
        }
        return out.toLongArray();
    }
    /** Tile indices along one axis whose samples fall in the 16 blocks from {@code min}. */
    private static int[] axis(int min,int level) {
        int c=cell(level),s=span(level),half=c>>1;
        var out=new it.unimi.dsi.fastutil.ints.IntArrayList();
        for(int t=Math.floorDiv(min-s-c,s);t<=Math.floorDiv(min+15+c,s);t++) {
            // Sample i sits at t*s+(i-1)*c+half; keep tiles with some i in [0,SAMPLES) inside the chunk.
            int first=Math.floorDiv(min-t*s-half+c-1,c)+1,last=Math.floorDiv(min+15-t*s-half,c)+1;
            if(Math.max(first,0)<=Math.min(last,SAMPLES-1))out.add(t);
        }
        return out.toIntArray();
    }
    /** Rejects malformed keys and tiles outside the vanilla world border range. */
    public static boolean valid(long key){int l=level(key);return l>=0&&l<=MAX_LEVEL&&Math.abs((long)x(key)*span(l))<30_000_000L&&Math.abs((long)z(key)*span(l))<30_000_000L;}
    private HorizonTiles(){}
}
