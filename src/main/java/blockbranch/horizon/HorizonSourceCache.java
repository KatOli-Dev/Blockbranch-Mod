package blockbranch.horizon;
import java.util.*;
/**
 * Per-chunk cache whose loads run outside its lock and may race {@link #forget}. A load
 * takes a {@link #stamp} first and {@link #commit}s with it; the value is cached only if
 * the chunk was not forgotten in between, so a slow read of a chunk's old state cannot
 * replace the invalidation that followed it. Stamps are striped by key: a collision only
 * declines a commit, which costs a repeated read, never a stale entry. Thread-safe.
 */
final class HorizonSourceCache<V> {
    private static final int STRIPES=4096;
    private final int[] stamps=new int[STRIPES];
    private final LinkedHashMap<Long,V> values;
    HorizonSourceCache(int capacity) {
        values=new LinkedHashMap<>(64,.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<Long,V> eldest){return size()>capacity;}
        };
    }
    private static int stripe(long key){return (int)it.unimi.dsi.fastutil.HashCommon.mix(key)&(STRIPES-1);}
    synchronized V get(long key){return values.get(key);}
    synchronized int stamp(long key){return stamps[stripe(key)];}
    /** Caches {@code value} unless the chunk was forgotten since {@code stamp}; returns whether it did. */
    synchronized boolean commit(long key,int stamp,V value) {
        if(stamps[stripe(key)]!=stamp)return false;
        values.put(key,value);return true;
    }
    synchronized void forget(long key){values.remove(key);stamps[stripe(key)]++;}
}
