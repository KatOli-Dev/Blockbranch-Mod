package blockbranch.horizon;
import static blockbranch.horizon.HorizonTiles.*;
import it.unimi.dsi.fastutil.longs.*;
import java.util.function.LongPredicate;
/**
 * Level-of-detail choice for the horizon quadtree; shared by requests and drawing.
 *
 * <p>Every tile that reaches into the native render radius splits down to level 0, so
 * the horizon meets native terrain at block resolution. Beyond that, a level-L tile
 * splits while it is nearer than {@code native + quality·2^L}: detail falls off with
 * distance past the native edge, one level per doubling of that distance. Level-0 tiles
 * whose chunk is drawn natively are skipped, and nothing beyond {@code maxRadius} is used.
 */
public final class HorizonSelection {
    public record View(double x,double z,double nativeRadius,double maxRadius,double quality) {}
    /** Which chunks native terrain draws. */
    @FunctionalInterface public interface NativeCoverage {
        /** True when native terrain already draws this chunk. */
        boolean covers(int chunkX,int chunkZ);
        /**
         * True when native terrain will draw this chunk once it has loaded and compiled, so
         * the horizon need not request it. False for a chunk native terrain never reaches,
         * such as one whose surface lies beyond the vertical view distance of a high camera.
         * Only asked for chunks well inside the native radius; by default they all are.
         */
        default boolean expected(int chunkX,int chunkZ){return true;}
    }

    static boolean split(long key,double distance,View v){int l=level(key);return l>0&&distance<v.nativeRadius+v.quality*(1<<l);}

    /** Roots: level-MAX tiles overlapping the max radius. */
    static LongArrayList roots(View v) {
        var roots=new LongArrayList();int s=span(MAX_LEVEL);
        int x0=(int)Math.floor((v.x-v.maxRadius)/s),x1=(int)Math.floor((v.x+v.maxRadius)/s),z0=(int)Math.floor((v.z-v.maxRadius)/s),z1=(int)Math.floor((v.z+v.maxRadius)/s);
        for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++){long k=key(MAX_LEVEL,x,z);if(distance(k,v.x,v.z)<v.maxRadius)roots.add(k);}
        return roots;
    }

    /**
     * Ideal tiles at their target level, nearest relative to their size first, so coarse
     * tiles that cover much of the view arrive early and refine afterwards. Level-0 tiles
     * native terrain draws are omitted, as are those well inside the native radius that it
     * is expected to draw once loaded.
     */
    public static long[] wanted(View v,NativeCoverage coverage) {
        var out=new LongArrayList();var stack=roots(v);
        while(!stack.isEmpty()) {
            long k=stack.popLong();double d=distance(k,v.x,v.z);
            if(d>=v.maxRadius)continue;
            if(split(k,d,v)){for(int q=0;q<4;q++)stack.add(child(k,q));continue;}
            if(level(k)==0&&(coverage.covers(x(k),z(k))||d<v.nativeRadius-32&&coverage.expected(x(k),z(k))))continue;
            out.add(k);
        }
        long[] keys=out.toLongArray();
        return LongArrayList.wrap(keys).stream().sorted(java.util.Comparator.comparingDouble(k->distance(k,v.x,v.z)/span(level(k)))).mapToLong(Long::longValue).toArray();
    }

    /**
     * Tiles to draw this frame. A split tile whose children are not all available draws
     * itself instead when it has data and lies wholly outside the native area (so it never
     * overlaps native terrain); otherwise the available children are drawn alone.
     */
    public static long[] drawn(View v,LongPredicate ready,NativeCoverage coverage) {
        var out=new LongArrayList();
        for(long root:roots(v))visit(root,v,ready,coverage,out);
        return out.toLongArray();
    }
    private static boolean visit(long k,View v,LongPredicate ready,NativeCoverage coverage,LongArrayList out) {
        double d=distance(k,v.x,v.z);
        if(d>=v.maxRadius)return true;
        if(level(k)==0){if(coverage.covers(x(k),z(k)))return true;if(ready.test(k)){out.add(k);return true;}return false;}
        if(!split(k,d,v)){if(ready.test(k)){out.add(k);return true;}return false;}
        int mark=out.size();boolean complete=true;
        for(int q=0;q<4;q++)complete&=visit(child(k,q),v,ready,coverage,out);
        if(complete)return true;
        if(d>=v.nativeRadius+16&&ready.test(k)){out.size(mark);out.add(k);return true;}
        return false;
    }
    private HorizonSelection(){}
}
