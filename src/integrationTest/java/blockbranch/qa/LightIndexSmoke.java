package blockbranch.qa;

import blockbranch.cubic.LightColumnIndex;
import it.unimi.dsi.fastutil.longs.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.*;

/** Native sky/block parity, independent snapshots and fixed-work allocation guard. */
public final class LightIndexSmoke {
    public static void verify() throws Exception {
        var missing=new DataLayer(13);var raw=new Long2ObjectOpenHashMap<DataLayer>();raw.defaultReturnValue(missing);
        var indexed=new blockbranch.cubic.IndexedDataLayerMap(raw);var clone=indexed.clone();
        check(indexed.defaultReturnValue()==missing&&clone.defaultReturnValue()==missing,"native default return value survives wrapping and clone");
        check(indexed.get(0)==missing&&indexed.getOrDefault(0,null)==null&&indexed.sectionYs(0).isEmpty(),"absent primitive zero key is not an indexed layer");
        var initial=new Long2ObjectOpenHashMap<DataLayer>();
        for(int x:new int[]{-100,0,100})for(int z:new int[]{0,7})for(int y:new int[]{-16384,-500,-1,0,200,16383})initial.put(SectionPos.asLong(x,y,z),new DataLayer(3));
        var block=Class.forName("net.minecraft.world.level.lighting.BlockLightSectionStorage$BlockDataLayerStorageMap").getDeclaredConstructor(Long2ObjectOpenHashMap.class);block.setAccessible(true);
        exercise((DataLayerStorageMap<?>)block.newInstance(initial.clone()));
        var sky=Class.forName("net.minecraft.world.level.lighting.SkyLightSectionStorage$SkyDataLayerStorageMap").getDeclaredConstructor(Long2ObjectOpenHashMap.class,Long2IntOpenHashMap.class,int.class);sky.setAccessible(true);
        exercise((DataLayerStorageMap<?>)sky.newInstance(initial.clone(),new Long2IntOpenHashMap(),-16384));
    }
    public static void measure(net.minecraft.server.level.ServerLevel level) throws Exception {
        var initial=new Long2ObjectOpenHashMap<DataLayer>();
        for(int x=-16;x<16;x++)for(int z=-16;z<16;z++)for(int y=-20;y<47;y++) initial.put(SectionPos.asLong(x,y,z),new DataLayer(3));
        var ctor=Class.forName("net.minecraft.world.level.lighting.BlockLightSectionStorage$BlockDataLayerStorageMap").getDeclaredConstructor(Long2ObjectOpenHashMap.class);ctor.setAccessible(true);
        var map=(DataLayerStorageMap<?>)ctor.newInstance(initial);
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        check(bean.isThreadAllocatedMemorySupported()&&bean.isThreadAllocatedMemoryEnabled(),"allocation counter must be available");
        long tid=Thread.currentThread().threadId();
        for(int n=0;n<6;n++)map.copy();
        long bytes=bean.getThreadAllocatedBytes(tid),cpu=bean.getCurrentThreadCpuTime(),wall=System.nanoTime();
        for(int n=0;n<16;n++){
            var copy=map.copy();
            check(keys(copy,SectionPos.getZeroNode(0,0)).size()==67,"measured copy content");
        }
        long allocated=bean.getThreadAllocatedBytes(tid)-bytes;
        blockbranch.Blockbranch.LOGGER.info("LIGHT_INDEX_FIXED_WORK copies=16 entries=68608 bytes={} cpuNs={} wallNs={}",allocated,bean.getCurrentThreadCpuTime()-cpu,System.nanoTime()-wall);
        mutationCost(map,1);mutationCost(map,1024);
        lookupCost(map,level);
        check(allocated>=0&&allocated<30_000_000L,"light-index fixed-copy allocation bound (matched main >70 MB)");
    }
    private static long[] context(){long count=0,time=0;for(var gc:java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()){count+=gc.getCollectionCount();time+=gc.getCollectionTime();}return new long[]{count,time,java.lang.management.ManagementFactory.getCompilationMXBean().getTotalCompilationTime()};}
    private static void lookupCost(DataLayerStorageMap<?> map,net.minecraft.server.level.ServerLevel level)throws Exception{
        var storageClass=Class.forName("net.minecraft.world.level.lighting.SkyLightSectionStorage");
        var constructor=java.util.Arrays.stream(storageClass.getDeclaredConstructors()).filter(c->c.getParameterCount()==1&&c.getParameterTypes()[0].isInstance(level.getChunkSource())).findFirst().orElseThrow();constructor.setAccessible(true);
        var storage=constructor.newInstance(level.getChunkSource());
        var visible=Class.forName("net.minecraft.world.level.lighting.LayerLightSectionStorage").getDeclaredField("visibleSectionData");visible.setAccessible(true);
        var getter=storageClass.getDeclaredMethod("getLightValue",long.class,boolean.class);getter.setAccessible(true);
        Object[][] arguments=new Object[1024][];int[] expected=new int[1024];
        for(int i=0;i<1024;i++){int x=i%32-16,z=i/32-16,y=i%67-20;long node=SectionPos.asLong(x,y,z);map.copyDataLayer(node).fill(i&15);int queryY=i%7==0?47:i%7==1?-21:y;int selected=Math.max(queryY,-20);expected[i]=queryY>46?15:map.getLayer(SectionPos.asLong(x,selected,z)).get(8,queryY==selected?8:0,8);arguments[i]=new Object[]{net.minecraft.core.BlockPos.asLong(x*16+8,queryY*16+8,z*16+8),false};}
        var nativeMap=DataLayerStorageMap.class.getDeclaredField("map");nativeMap.setAccessible(true);
        var skyMap=Class.forName("net.minecraft.world.level.lighting.SkyLightSectionStorage$SkyDataLayerStorageMap").getDeclaredConstructor(Long2ObjectOpenHashMap.class,Long2IntOpenHashMap.class,int.class);skyMap.setAccessible(true);
        visible.set(storage,skyMap.newInstance(((Long2ObjectOpenHashMap<?>)nativeMap.get(map)).clone(),new Long2IntOpenHashMap(),-16384));
        for(int i=0;i<1024;i++)check((Integer)getter.invoke(storage,arguments[i])==expected[i],"native sky query exact across colored layers and height gaps");
        for(int i=0;i<20000;i++)getter.invoke(storage,arguments[i&1023]);
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long id=Thread.currentThread().threadId();var before=context();long bytes=bean.getThreadAllocatedBytes(id),cpu=bean.getCurrentThreadCpuTime(),wall=System.nanoTime();int sum=0,wanted=0;
        for(int i=0;i<100000;i++){sum+=(Integer)getter.invoke(storage,arguments[i&1023]);wanted+=expected[i&1023];}
        long w=System.nanoTime()-wall,c=bean.getCurrentThreadCpuTime()-cpu,b=bean.getThreadAllocatedBytes(id)-bytes;var after=context();check(sum==wanted,"actual native sky-value queries match source layers");
        blockbranch.Blockbranch.LOGGER.info("LIGHT_INDEX_LOOKUP queries=100000 bytes={} cpuNs={} wallNs={} gcCount={} gcMs={} compilationMs={}",b,c,w,after[0]-before[0],after[1]-before[1],after[2]-before[2]);
    }
    private static volatile Object sink;
    private static void mutationCost(DataLayerStorageMap<?> map,int columns){
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        var contextBefore=context();
        long id=Thread.currentThread().threadId(),bytes=bean.getThreadAllocatedBytes(id),cpu=bean.getCurrentThreadCpuTime(),wall=System.nanoTime();
        for(int round=0;round<8;round++){
            var snapshot=map.copy();sink=snapshot;
            for(int column=0;column<columns;column++){
                long node=SectionPos.asLong(column%32-16,0,column/32-16);int before=snapshot.getLayer(node).get(0,0,0);
                map.copyDataLayer(node).set(0,0,0,round);
                check(snapshot.getLayer(node).get(0,0,0)==before&&keys(snapshot,SectionPos.getZeroNode(node)).contains(0),"copy/write workload snapshot remains exact");
            }
        }
        long elapsed=System.nanoTime()-wall,used=bean.getCurrentThreadCpuTime()-cpu,allocated=bean.getThreadAllocatedBytes(id)-bytes;var contextAfter=context();
        blockbranch.Blockbranch.LOGGER.info("LIGHT_INDEX_MUTATION copies=8 columns={} bytes={} cpuNs={} wallNs={} gcCount={} gcMs={} compilationMs={}",columns,allocated,used,elapsed,contextAfter[0]-contextBefore[0],contextAfter[1]-contextBefore[1],contextAfter[2]-contextBefore[2]);sink=null;
    }
    private static void exercise(DataLayerStorageMap<?> updating) throws Exception {
        long special=SectionPos.asLong(0,0,0);
        var specialCopy=updating.copy();
        updating.setLayer(special,new DataLayer(11));
        check(index(updating,0).get(0)==updating.getLayer(special),"zero primitive key replacement identity");
        check(specialCopy.getLayer(special).get(0,0,0)==3,"zero key snapshot isolation");
        for(int y:List.copyOf(index(updating,0).keySet()))updating.removeLayer(SectionPos.asLong(0,y,0));
        check(index(updating,0).isEmpty()&&!index(specialCopy,0).isEmpty(),"zero column removal and snapshot preservation");
        long zero=SectionPos.getZeroNode(SectionPos.asLong(0,0,7)),key=SectionPos.asLong(0,-500,7),other=SectionPos.getZeroNode(SectionPos.asLong(100,0,7));
        var old=keys(updating,zero);var visible=updating.copy();
        check(old==keys(visible,zero),"unchanged snapshot column shares immutable view");
        updating.setLayer(key,new DataLayer(5));var changed=keys(updating,zero);
        check(changed==old&&keys(visible,zero)==old,"value-only writes preserve shared topology");
        check(keys(updating,other)==keys(visible,other),"unrelated column remains shared");
        updating.setLayer(SectionPos.asLong(0,200,7),new DataLayer(6));check(keys(updating,zero)==changed,"subsequent own-column writes reuse tree");
        check(visible.getLayer(key).get(0,0,0)==3&&index(visible,zero).get(-500).get(0,0,0)==3,"snapshot retains native and indexed values");
        updating.copyDataLayer(key).set(0,0,0,9);
        check(index(updating,zero).get(-500)==updating.getLayer(key),"native layer copy updates index identity");
        check(visible.getLayer(key).get(0,0,0)==3,"native copy-before-write leaves snapshot intact");
        visible.removeLayer(key);check(!index(visible,zero).containsKey(-500)&&index(updating,zero).containsKey(-500),"snapshot-side removal isolated");
        var second=updating.copy();var published=index(second,zero);
        for(int probe:new int[]{-20000,-16384,-501,-500,-499,-1,1,200,201,16383,20000}){
            var expected=new TreeMap<Integer,DataLayer>();for(int y:new int[]{-16384,-500,-1,0,200,16383}){var layer=second.getLayer(SectionPos.asLong(0,y,7));if(layer!=null)expected.put(y,layer);}
            check(Objects.equals(published.ceilingKey(probe),expected.ceilingKey(probe))&&Objects.equals(published.floorKey(probe),expected.floorKey(probe)),"sparse ceiling/floor across negative and positive heights");
        }
        boolean rejected=false;
        try{published.put(-500,new DataLayer(1));}catch(UnsupportedOperationException expected){rejected=true;}
        check(rejected,"read-only index cannot bypass native storage");
        var third=second.copy();third.setLayer(key,new DataLayer(12));
        check(index(second,zero).get(-500).get(0,0,0)==9&&index(third,zero).get(-500).get(0,0,0)==12,"snapshot-side set detaches through multiple generations");
        long addedKey=SectionPos.asLong(0,201,7);var unchanged=keys(second,zero);
        third.setLayer(addedKey,new DataLayer(2));
        check(keys(third,zero)!=unchanged&&keys(third,zero).contains(201)&&!keys(second,zero).contains(201),"topology addition detaches only writer");
        var afterAdd=third.copy();third.removeLayer(addedKey);
        check(!keys(third,zero).contains(201)&&keys(afterAdd,zero).contains(201)&&afterAdd.getLayer(addedKey).get(0,0,0)==2,"topology removal preserves older snapshots");
        rejected=false;try{keys(second,zero).add(201);}catch(UnsupportedOperationException expected){rejected=true;}check(rejected,"read-only height topology");
        var jobs=Executors.newFixedThreadPool(2);
        try{
            var reader=jobs.submit(()->{for(int n=0;n<1000;n++)check(published.get(-500).get(0,0,0)==9,"published reader stays immutable");});
            var writer=jobs.submit(()->{for(int n=0;n<1000;n++)updating.copyDataLayer(key).set(0,0,0,n&15);});
            reader.get();writer.get();
        }finally{jobs.shutdownNow();}
        for(var entry:published.entrySet())check(entry.getValue()==second.getLayer(SectionPos.asLong(0,entry.getKey(),7)),"published native/index parity");
        // Native swapSectionMap publishes a copied, cache-disabled map. Only its
        // source has one writer; simultaneous mutation of the SAME map is unsupported.
        for(boolean writeCopy:new boolean[]{false,true})for(boolean addFirst:new boolean[]{false,true})
            structuralSnapshot(updating,zero,key,writeCopy,addFirst);
        // Views are read-only; callers reacquire after writes. Published snapshots stay stable.
        for(int y:List.copyOf(index(updating,other).keySet()))updating.removeLayer(SectionPos.asLong(100,y,7));
        check(index(updating,other).isEmpty()&&!index(second,other).isEmpty(),"last-layer removal releases only source column");
    }
    private static void structuralSnapshot(DataLayerStorageMap<?> source,long zero,long existing,boolean writeCopy,boolean addFirst)throws Exception {
        var copy=source.copy();var writer=writeCopy?copy:source;var visible=writeCopy?source:copy;
        visible.disableCache(); // Match native published-reader behavior, not its mutable two-entry cache.
        var heights=keys(visible,zero);var expected=new TreeSet<>(heights);
        check(heights==keys(writer,zero),"fixture exercises actually shared height topology");
        long added=SectionPos.asLong(SectionPos.x(existing),2048,SectionPos.z(existing));
        check(!expected.contains(2048)&&expected.contains(SectionPos.y(existing)),"structural fixture has absent and present heights");
        var original=writer.getLayer(existing);var addedLayer=new DataLayer(12);
        var visibleLayer=((LightColumnIndex)visible).blockbranch$layer(existing);
        var boundary=new CyclicBarrier(2);var jobs=Executors.newFixedThreadPool(2);
        try{
            var reader=jobs.submit(()->{
                for(int round=0;round<64;round++){
                    // Hold the actual production iterator before the first detach.
                    // A mistakenly shared structural write must fail deterministically.
                    var iterator=heights.iterator();
                    boundary.await(5,TimeUnit.SECONDS);boundary.await(5,TimeUnit.SECONDS);
                    var seen=new ArrayList<Integer>();iterator.forEachRemaining(seen::add);
                    check(seen.equals(new ArrayList<>(expected)),"held published iterator survives source structural writes");
                    check(keys(visible,zero)==heights&&new TreeSet<>(keys(visible,zero)).equals(expected),"published production height view stays unchanged");
                    check(new ArrayList<>(heights.descendingSet()).equals(new ArrayList<>(expected.descendingSet())),"published descending traversal");
                    for(int probe:new int[]{-20000,-500,0,2048,20000}){
                        check(Objects.equals(heights.ceiling(probe),expected.ceiling(probe))&&Objects.equals(heights.lower(probe),expected.lower(probe)),"published sparse navigation during structural writes");
                    }
                    check(((LightColumnIndex)visible).blockbranch$layer(existing)==visibleLayer,"published native value belongs to unchanged snapshot");
                }return true;
            });
            var mutation=jobs.submit(()->{
                for(int round=0;round<64;round++){
                    boundary.await(5,TimeUnit.SECONDS);
                    if(addFirst){writer.setLayer(added,addedLayer);writer.removeLayer(existing);}
                    else{writer.removeLayer(existing);writer.setLayer(added,addedLayer);}
                    check(keys(writer,zero)!=heights&&keys(writer,zero).contains(2048)&&!keys(writer,zero).contains(SectionPos.y(existing)),"writer detaches and changes its own topology");
                    boundary.await(5,TimeUnit.SECONDS);
                    // This restoration overlaps the reader's traversal; only the
                    // writer's private tree and native map may change.
                    writer.removeLayer(added);writer.setLayer(existing,original);
                }return true;
            });
            check(reader.get(30,TimeUnit.SECONDS)&&mutation.get(30,TimeUnit.SECONDS),"structural snapshot workers complete");
        }finally{jobs.shutdownNow();check(jobs.awaitTermination(5,TimeUnit.SECONDS),"structural workers release");}
        check(new TreeSet<>(heights).equals(expected)&&!heights.contains(2048),"published height topology preserved after writer completion");
    }
    private static NavigableSet<Integer> keys(DataLayerStorageMap<?> map,long zero){return ((LightColumnIndex)map).blockbranch$sectionYs(zero);}
    private static NavigableMap<Integer,DataLayer> index(DataLayerStorageMap<?> map,long zero){return ((LightColumnIndex)map).blockbranch$layers(zero);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
