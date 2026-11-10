package blockbranch.realistic;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/** Cache hits, collisions, eviction and concurrency must equal the uncached exact field. */
public final class DrainageCacheCheck {
    private record Probe(int x,int z,DrainageNetwork.Sample expected){}
    public static void main(String[]args)throws Exception{
        Method reference=DrainageNetwork.class.getDeclaredMethod("sample",int.class,int.class,boolean.class);
        reference.setAccessible(true);
        for(long seed:new long[]{424242,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed);var probes=new ArrayList<Probe>();
            for(int[] p:new int[][]{{0,0},{-1,0},{0,-1},{-1,-1},{1064664,1284184},{1359388,-1694456},{-1331918,-1900024}})probes.add(probe(reference,d,p[0],p[1]));
            for(int i=0;i<512;i++)probes.add(probe(reference,d,-1700000+(i*7319)%3400000,-1900000+(i*12983)%3800000));
            for(var p:probes)verify(d,p);
            // More distinct block coordinates than the bounded cache can hold.
            for(int i=0;i<65536;i++)d.sample(-1700000+(i*53)%3400000,-1900000+(i*97)%3800000);
            Collections.reverse(probes);for(var p:probes)verify(d,p);
            // Structural relief callbacks have their own field semantics, never
            // sharing a detailed value just because the coordinates coincide.
            for(int i=0;i<32;i++){var p=probes.get(i);var expected=(DrainageNetwork.Sample)reference.invoke(d,p.x(),p.z(),false);if(!d.structuralSample(p.x(),p.z()).equals(expected))throw new AssertionError("Structural/detailed cache separation");verify(d,p);}
            try(var workers=Executors.newFixedThreadPool(4)){
                var tasks=new ArrayList<Callable<Void>>();
                for(int task=0;task<16;task++){final int phase=task;tasks.add(()->{for(int i=0;i<1024;i++)verify(d,probes.get((i*193+phase*37)%probes.size()));return null;});}
                for(var result:workers.invokeAll(tasks))result.get();
            }
            System.out.println("DRAINAGE_CACHE seed="+seed+" probes="+probes.size()+" uniqueTraversal=65536 concurrentQueries=16384");
        }
        String structural=System.getProperty("blockbranch.terrainStructuralComparison"),fixture=System.getProperty("blockbranch.realisticClientFixture");
        try{
            System.setProperty("blockbranch.terrainStructuralComparison","true");System.setProperty("blockbranch.realisticClientFixture","true");
            var d=new DrainageNetwork(new TerrainShape(1),1);
            for(int i=0;i<128;i++){int x=-1332080+i,z=-1900080+i;var expected=(DrainageNetwork.Sample)reference.invoke(d,x,z,false);if(!d.sample(x,z).equals(expected)||!d.sample(x,z).equals(expected))throw new AssertionError("Comparison-world cache preserves its fixed structural mode");}
        }finally{restore("blockbranch.terrainStructuralComparison",structural);restore("blockbranch.realisticClientFixture",fixture);}
        System.out.println("DRAINAGE_EXACT_CACHE_ORDER_EVICTION_CONCURRENCY_MODE_PASS");
    }
    private static Probe probe(Method method,DrainageNetwork d,int x,int z)throws Exception{return new Probe(x,z,(DrainageNetwork.Sample)method.invoke(d,x,z,true));}
    private static void verify(DrainageNetwork d,Probe p){if(!d.sample(p.x(),p.z()).equals(p.expected()))throw new AssertionError("Cached field differs at "+p.x()+","+p.z());}
    private static void restore(String key,String value){if(value==null)System.clearProperty(key);else System.setProperty(key,value);}
}
