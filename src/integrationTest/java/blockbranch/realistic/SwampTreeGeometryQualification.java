package blockbranch.realistic;

import java.lang.management.ManagementFactory;
import java.util.List;

/** Exact frozen-geometry comparison over all generated heights and signed variants. */
public final class SwampTreeGeometryQualification {
    private static volatile long sink;
    public static void main(String[] args) {
        int checked=0;long voxels=0;
        for(int height=5;height<=14;height++)for(int n=0;n<256;n++) {
            int variant=switch(n){case 0->Integer.MIN_VALUE;case 1->Integer.MAX_VALUE;case 2->0;case 3->-1;default->(int)OakBirchWoodland.mix(n*1297L+height*997L);};
            var root=new WoodlandTrees.Root(n%2==0?-2097100:2097100,64+n%601,n%3==0?-17:8,false,height,variant,3);
            var expected=SwampTreesReference.tree(root);var actual=SwampTrees.tree(root);
            if(!expected.equals(actual))throw new AssertionError("Exact sorted voxel mismatch "+root+" expected="+expected.size()+" actual="+actual.size());
            checked++;voxels+=actual.size();
        }
        // Both implementations receive the same roots after the exactness warmup.
        var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        for(int round=0;round<4;round++)for(int phase=0;phase<2;phase++) {
            boolean reference=(phase==0)==((round&1)==0);System.gc();long thread=Thread.currentThread().threadId();
            long allocated=bean.getThreadAllocatedBytes(thread),cpu=bean.getCurrentThreadCpuTime(),start=System.nanoTime();
            long count=0;
            for(int n=0;n<512;n++) {
                var root=new WoodlandTrees.Root(0,70,0,false,5+n%10,(int)OakBirchWoodland.mix(n*1297L),3);
                List<WoodlandTrees.Voxel> result=reference?SwampTreesReference.tree(root):SwampTrees.tree(root);
                count+=result.size();
            }
            sink=count;
            System.out.println("GEOMETRY round="+round+" implementation="+(reference?"d61ff42":"candidate")+" wallNanos="+(System.nanoTime()-start)+" threadCpuNanos="+(bean.getCurrentThreadCpuTime()-cpu)+" allocatedBytes="+(bean.getThreadAllocatedBytes(thread)-allocated)+" voxels="+count);
        }
        System.out.println("PASS "+checked+" exact ordered trees / "+voxels+" voxels; all generated heights, boundary variants and translated coordinates");
    }
}
