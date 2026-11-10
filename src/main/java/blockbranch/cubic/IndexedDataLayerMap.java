package blockbranch.cubic;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;

/** Native clones share section-height topology; only added/removed heights detach a column. Values stay in the native map. */
public final class IndexedDataLayerMap extends Long2ObjectOpenHashMap<DataLayer> {
    private Columns columns=new Columns();
    public IndexedDataLayerMap(Long2ObjectOpenHashMap<DataLayer> initial){
        super(initial);
        defaultReturnValue(initial.defaultReturnValue());
        var entries=initial.long2ObjectEntrySet().fastIterator();
        while(entries.hasNext()){var entry=entries.next();columns.put(entry.getLongKey());}
    }
    @Override public IndexedDataLayerMap clone(){
        var copy=(IndexedDataLayerMap)super.clone();
        copy.columns=columns.snapshot();
        return copy;
    }
    public void indexAdd(long node){columns.put(node);}
    public void indexRemove(long node){columns.remove(node);}
    public NavigableSet<Integer> sectionYs(long zeroNode){return columns.layers(zeroNode);}

    private static final class Column {
        final Object owner;
        final TreeSet<Integer> layers;
        final NavigableSet<Integer> readOnly;
        Column(Object owner,TreeSet<Integer> layers){this.owner=owner;this.layers=layers;readOnly=Collections.unmodifiableNavigableSet(layers);}
    }
    private static final class Columns {
        private Object owner=new Object();
        private Long2ObjectOpenHashMap<Column> values=new Long2ObjectOpenHashMap<>();
        Columns snapshot(){
            var result=new Columns();result.values=values.clone();
            // Invalidate ownership on BOTH sides before the native snapshot can be published.
            owner=new Object();return result;
        }
        private Column writable(long key,Column old){
            if(old!=null&&old.owner==owner)return old;
            var value=new Column(owner,old==null?new TreeSet<>():new TreeSet<>(old.layers));values.put(key,value);return value;
        }
        void put(long node){
            long key=SectionPos.getZeroNode(node);int y=SectionPos.y(node);var old=values.get(key);
            if(old!=null&&old.owner==owner){old.layers.add(y);return;}
            if(old!=null&&old.layers.contains(y))return;
            writable(key,old).layers.add(y);
        }
        void remove(long node){
            long key=SectionPos.getZeroNode(node);var old=values.get(key);if(old==null||!old.layers.contains(SectionPos.y(node)))return;
            var column=writable(key,old);column.layers.remove(SectionPos.y(node));if(column.layers.isEmpty())values.remove(key);
        }
        NavigableSet<Integer> layers(long key){var column=values.get(key);return column==null?Collections.emptyNavigableSet():column.readOnly;}
    }
}
