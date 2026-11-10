package blockbranch.cubic;

import java.util.*;
import net.minecraft.world.level.chunk.*;

/** Loaded working set and persisted section index, separate from the procedural base. */
public final class ColumnTerrain {
    private final ChunkAccess chunk;
    private final TerrainProfile base;
    private final PalettedContainerFactory factory;
    private final SectionStore store;
    private final Set<Integer> resident=java.util.concurrent.ConcurrentHashMap.newKeySet(), dirty=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final NavigableSet<Integer> modified=new java.util.concurrent.ConcurrentSkipListSet<>();
    private final LinkedHashMap<Integer,LevelChunkSection> views=new LinkedHashMap<>(8,.75f,true);
    private final LevelChunkSection unavailable;
    public final Set<Integer> lit=new HashSet<>();
    public int[] skySources;
    public ColumnTerrain(ChunkAccess chunk,TerrainProfile base,PalettedContainerFactory factory,SectionStore store,int[] modified) {
        if(!base.underground().owns(chunk.getPos().x(),chunk.getPos().z())||!base.ores().owns(chunk.getPos().x(),chunk.getPos().z())||!base.dripstone().owns(chunk.getPos().x(),chunk.getPos().z())||!base.litCaves().owns(chunk.getPos().x(),chunk.getPos().z()))throw new IllegalArgumentException("Procedural profile column mismatch");
        this.chunk=chunk; this.base=base; this.factory=factory; this.store=store; this.unavailable=new EmptyColumnSection(factory);
        for(int y:modified) { if(y<(ColumnLimits.MIN_Y>>4) || y>=(ColumnLimits.MAX_Y>>4)) throw new IllegalArgumentException("Invalid edited section Y"); this.modified.add(y); }
    }
    public TerrainProfile base() { return base; }
    public PalettedContainerFactory factory() { return factory; }
    public boolean resident(int y) { return resident.contains(y); }
    public void resident(int y,boolean present) { if(present) resident.add(y); else resident.remove(y); }
    public void modified(int y) { modified.add(y); dirty.add(y); }
    public boolean hasEdits(int sectionY) { return modified.contains(sectionY); }
    public int[] modified() { return modified.stream().mapToInt(Integer::intValue).toArray(); }
    public NavigableSet<Integer> modifiedView() { return Collections.unmodifiableNavigableSet(modified); }
    public synchronized LevelChunkSection read(int y) {
        // Deferred survey views can outlive materialization and subsequent edits.
        if(resident.contains(y)) return ((SparseColumn)chunk).blockbranch$read(y);
        if(modified.contains(y)) {
            if(store==null) return unavailable; // A client overlay is not ready until its section packet arrives.
            var section=materialize(y); ((SparseColumn)chunk).blockbranch$replace(y,section); return section;
        }
        var view=views.get(y);
        if(view==null) {
            if(views.size()>=4) { var first=views.keySet().iterator(); first.next(); first.remove(); }
            view=base.view(y); views.put(y,view);
        }
        return view;
    }
    public synchronized LevelChunkSection materialize(int y) { return modified.contains(y) && store!=null ? store.load(chunk.getPos(),y,factory) : base.generate(y); }
    /** Traversal only: defer loading an edited layer until the search reaches it. */
    public LevelChunkSection survey(int y) {
        if(!modified.contains(y)) return base.view(y);
        return new LevelChunkSection(factory) {
            @Override public boolean hasOnlyAir() { return ColumnTerrain.this.read(y).hasOnlyAir(); }
            @Override public net.minecraft.world.level.block.state.BlockState getBlockState(int x,int ly,int z) { return ColumnTerrain.this.read(y).getBlockState(x,ly,z); }
        };
    }
    public void flush() {
        if(store==null) return;
        var sparse=(SparseColumn)chunk;
        for(int y:List.copyOf(dirty)) persist(y);
    }
    private void persist(int y) {
        var section=((SparseColumn)chunk).blockbranch$read(y);
        if(ColumnMigration.matches(base,y,section)) modified.remove(y);
        else store.save(chunk.getPos(),y,section,factory);
        dirty.remove(y);
    }
    public void evictOutside(int min,int max) {
        var active=new HashSet<Integer>(); for(int y=min;y<=max;y++) active.add(y); evictExcept(active);
    }
    public synchronized void evictExcept(Set<Integer> active) {
        if(store==null) return;
        var sparse=(SparseColumn)chunk;
        for(int y:List.copyOf(resident)) if(!active.contains(y)) {
            if(dirty.contains(y)) persist(y);
            sparse.blockbranch$replace(y,null);
        }
        views.clear();
    }
    public ColumnTerrain rebind(ChunkAccess destination) {
        var result=new ColumnTerrain(destination,base,factory,store,modified());
        result.resident.addAll(resident); result.dirty.addAll(dirty); result.lit.addAll(lit); return result;
    }
}
