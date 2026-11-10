package blockbranch.client.cubic;

import blockbranch.cubic.*;
import java.lang.ref.*;
import java.util.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;

/** Bounded, weakly owned base snapshots. Returned containers always belong to the caller. */
public final class ProceduralSectionCopies {
    static final Cache DEFAULT=new Cache(64L*1024*1024,8192);
    static final class Cache {
    private final long MAX_WEIGHT;
    private final int MAX_ENTRIES;
    Cache(long bytes,int entries){MAX_WEIGHT=bytes;MAX_ENTRIES=entries;}
    private final ReferenceQueue<TerrainProfile> QUEUE=new ReferenceQueue<>();
    private final LinkedHashMap<Key,Entry> CACHE=new LinkedHashMap<>(16,.75f,true);
    private long weight,hits,misses;
    private record Entry(PalettedContainer<BlockState> states,int weight) {}
    private final class Key extends WeakReference<TerrainProfile> {
        final int y,hash;
        Key(TerrainProfile profile,int y){super(profile,QUEUE);this.y=y;hash=31*System.identityHashCode(profile)+y;}
        @Override public int hashCode(){return hash;}
        @Override public boolean equals(Object other){return this==other||other instanceof Key k&&y==k.y&&get()!=null&&get()==k.get();}
    }
    synchronized PalettedContainer<BlockState> copyBase(TerrainProfile profile,int y) {
        Key expired;while((expired=(Key)QUEUE.poll())!=null){var removed=CACHE.remove(expired);if(removed!=null)weight-=removed.weight;}
        var key=new Key(profile,y);var entry=CACHE.get(key);
        if(entry==null){
            // Preserve the existing hit path. Proven air needs no LRU slot, but
            // queue cleanup above still runs during air-only traffic.
            var empty=profile.copyEmptyStatesIfKnown(y);if(empty!=null)return empty;
            misses++;var states=profile.generateStates(y);entry=new Entry(states,states.getSerializedSize()+256);
            if(entry.weight<=MAX_WEIGHT){
                while(!CACHE.isEmpty()&&(CACHE.size()>=MAX_ENTRIES||weight+entry.weight>MAX_WEIGHT)){
                    var iterator=CACHE.entrySet().iterator();var oldest=iterator.next();weight-=oldest.getValue().weight;iterator.remove();
                }
                CACHE.put(key,entry);weight+=entry.weight;
            }
        }else hits++;
        return entry.states.copy();
    }
    synchronized void clear(){CACHE.clear();weight=hits=misses=0;while(QUEUE.poll()!=null){};}
    synchronized long[] statistics(){return new long[]{hits,misses,CACHE.size(),weight};}
    }
    public static PalettedContainer<BlockState> copy(SparseColumn sparse,int y) {
        var terrain=sparse.blockbranch$terrain();
        // Working sections and received overlays remain authoritative, including pending client edits.
        if(terrain==null||terrain.resident(y)||terrain.modifiedView().contains(y))
            return sparse.blockbranch$read(y).getStates().copy();
        return DEFAULT.copyBase(terrain.base(),y);
    }
    /** Defensive diagnostic snapshot: cacheable hits/misses, entries, serialized-size weight; exact-air bypasses are excluded. */
    public static long[] statistics(){return DEFAULT.statistics();}
    public static void clear(){DEFAULT.clear();}
    private ProceduralSectionCopies(){}
}
