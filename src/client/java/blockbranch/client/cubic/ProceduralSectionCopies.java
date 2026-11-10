package blockbranch.client.cubic;

import blockbranch.cubic.*;
import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;

/** Bounded, weakly owned base snapshots. Returned containers always belong to the caller. */
public final class ProceduralSectionCopies {
    static final Cache DEFAULT=new Cache(64L*1024*1024,8192);
    static final class Cache {
    private final long MAX_WEIGHT;
    private final int MAX_ENTRIES;
    private final java.util.function.BiFunction<TerrainProfile,Integer,PalettedContainer<BlockState>> generator;
    Cache(long bytes,int entries){this(bytes,entries,TerrainProfile::generateStates);}
    Cache(long bytes,int entries,java.util.function.BiFunction<TerrainProfile,Integer,PalettedContainer<BlockState>> generator){MAX_WEIGHT=bytes;MAX_ENTRIES=entries;this.generator=generator;}
    private final ReferenceQueue<TerrainProfile> QUEUE=new ReferenceQueue<>();
    private final LinkedHashMap<Key,Entry> CACHE=new LinkedHashMap<>(16,.75f,true);
    // Misses being generated outside the lock. Concurrent requests for the same
    // section wait for that one result (counted as hits) instead of regenerating it.
    private final HashMap<Key,CompletableFuture<PalettedContainer<BlockState>>> PENDING=new HashMap<>();
    private long weight,hits,misses,epoch;
    private record Entry(PalettedContainer<BlockState> states,int weight) {}
    private final class Key extends WeakReference<TerrainProfile> {
        final int y,hash;
        Key(TerrainProfile profile,int y){super(profile,QUEUE);this.y=y;hash=31*System.identityHashCode(profile)+y;}
        @Override public int hashCode(){return hash;}
        @Override public boolean equals(Object other){return this==other||other instanceof Key k&&y==k.y&&get()!=null&&get()==k.get();}
    }
    synchronized long epoch(){return epoch;}
    PalettedContainer<BlockState> copyBase(TerrainProfile profile,int y){return copyBase(profile,y,epoch());}
    PalettedContainer<BlockState> copyBase(TerrainProfile profile,int y,long expectedEpoch) {
        Key key;CompletableFuture<PalettedContainer<BlockState>> pending,owned;long generation;
        synchronized(this){
            if(expectedEpoch!=epoch){key=null;pending=owned=null;generation=epoch;}
            else {
                Key expired;while((expired=(Key)QUEUE.poll())!=null){var removed=CACHE.remove(expired);if(removed!=null)weight-=removed.weight;}
                key=new Key(profile,y);var entry=CACHE.get(key);
                if(entry!=null){hits++;return entry.states.copy();}
                // Preserve the existing hit path. Proven air needs no LRU slot, but
                // queue cleanup above still runs during air-only traffic.
                var empty=profile.copyEmptyStatesIfKnown(y);if(empty!=null)return empty;
                pending=PENDING.get(key);generation=epoch;
                owned=pending==null?new CompletableFuture<>():null;
                if(pending==null){misses++;PENDING.put(key,owned);}
            }
        }
        if(key==null)return generator.apply(profile,y);
        if(pending!=null){var states=pending.join();synchronized(this){if(generation==epoch)hits++;return states.copy();}}
        // Generation is the expensive part; distinct sections may generate in parallel.
        PalettedContainer<BlockState> states;
        try{states=generator.apply(profile,y);}
        catch(RuntimeException|Error failure){synchronized(this){PENDING.remove(key,owned);owned.completeExceptionally(failure);}throw failure;}
        synchronized(this){
            var entry=new Entry(states,states.getSerializedSize()+256);
            if(generation==epoch&&entry.weight<=MAX_WEIGHT){
                while(!CACHE.isEmpty()&&(CACHE.size()>=MAX_ENTRIES||weight+entry.weight>MAX_WEIGHT)){
                    var iterator=CACHE.entrySet().iterator();var oldest=iterator.next();weight-=oldest.getValue().weight;iterator.remove();
                }
                CACHE.put(key,entry);weight+=entry.weight;
            }
            PENDING.remove(key,owned);owned.complete(states);
            return states.copy();
        }
    }
    // In-flight callers may finish their immutable snapshot, but cannot repopulate a cleared cache.
    synchronized void clear(){epoch++;PENDING.clear();CACHE.clear();weight=hits=misses=0;while(QUEUE.poll()!=null){};}
    synchronized long[] statistics(){return new long[]{hits,misses,CACHE.size(),weight};}
    }
    /**
     * Implemented by a {@code SectionCopy} when the optional deferred-copy render patch is loaded
     * (see {@code RenderPatches}). The copy then resolves the base section through
     * {@link #copyBase} on the compile worker instead of during the render-thread snapshot.
     */
    public interface DeferredBase { void blockbranch$deferBase(TerrainProfile base,int y); }
    /** True when section y is the unedited, immutable procedural base, which is safe to copy later. */
    public static boolean isBase(SparseColumn sparse,int y) {
        var terrain=sparse.blockbranch$terrain();
        return terrain!=null&&!terrain.resident(y)&&!terrain.modifiedView().contains(y);
    }
    public static PalettedContainer<BlockState> copy(SparseColumn sparse,int y) {
        // Working sections and received overlays remain authoritative, including pending client edits.
        if(!isBase(sparse,y))
            return sparse.blockbranch$read(y).getStates().copy();
        return DEFAULT.copyBase(sparse.blockbranch$terrain().base(),y);
    }
    /** Cache generation captured when a deferred snapshot is created. */
    public static long epoch(){return DEFAULT.epoch();}
    public static PalettedContainer<BlockState> copyBase(TerrainProfile base,int y,long epoch){return DEFAULT.copyBase(base,y,epoch);}
    public static PalettedContainer<BlockState> copyBase(TerrainProfile base,int y){return DEFAULT.copyBase(base,y);}
    /** Defensive diagnostic snapshot: cacheable hits/misses, entries, serialized-size weight; exact-air bypasses are excluded. */
    public static long[] statistics(){return DEFAULT.statistics();}
    public static void clear(){DEFAULT.clear();}
    private ProceduralSectionCopies(){}
}
