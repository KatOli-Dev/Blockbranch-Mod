package blockbranch.horizon;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
/**
 * Backoff for tiles whose sampling or meshing failed. Each failure doubles the wait before
 * the tile may be tried again, from {@code base} up to {@code max} ticks; after
 * {@code limit} failures it is not tried again until {@link #succeeded} or {@link #clear}.
 * At most {@code capacity} tiles are tracked; the oldest record is dropped first, which at
 * worst lets that tile be retried early. Thread-safe.
 */
public final class HorizonRetry {
    private final int base,max,limit,capacity;
    /** Key to {failures, first tick it may be tried again}. */
    private final Long2ObjectLinkedOpenHashMap<int[]> failures=new Long2ObjectLinkedOpenHashMap<>();
    public HorizonRetry(int base,int max,int limit,int capacity){this.base=base;this.max=max;this.limit=limit;this.capacity=capacity;}
    /** Records a failure at tick {@code now}; returns how many times the tile has failed. */
    public synchronized int failed(long key,int now) {
        var f=failures.getAndMoveToLast(key);
        if(f==null){if(failures.size()>=capacity)failures.removeFirst();f=new int[2];failures.put(key,f);}
        f[0]++;
        f[1]=now+(int)Math.min(max,(long)base<<Math.min(30,f[0]-1));
        return f[0];
    }
    /** Whether the tile may be tried at tick {@code now}. */
    public synchronized boolean due(long key,int now){var f=failures.get(key);return f==null||f[0]<limit&&now-f[1]>=0;}
    /** True once the tile has failed {@code limit} times. */
    public synchronized boolean exhausted(long key){var f=failures.get(key);return f!=null&&f[0]>=limit;}
    public synchronized void succeeded(long key){failures.remove(key);}
    public synchronized void clear(){failures.clear();}
}
