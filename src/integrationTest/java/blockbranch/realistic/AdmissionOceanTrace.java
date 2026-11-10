package blockbranch.realistic;
import java.util.*;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
/** Exact call counts; bounded coordinate sets become explicit lower bounds on overflow. */
public final class AdmissionOceanTrace {
    private static final int LIMIT=131072,MAX_THREADS=8;
    private static final List<Counter> counters=new ArrayList<>();
    private static int droppedThreads;
    private static final ThreadLocal<Counter> local=ThreadLocal.withInitial(AdmissionOceanTrace::register);
    private static final class Phase {
        final LongOpenHashSet ocean=new LongOpenHashSet(),raw=new LongOpenHashSet();
        long oceanCalls,rawCalls,oceanOverflow,rawOverflow;
    }
    private static final class Counter { final Phase[] phases={new Phase(),new Phase(),new Phase()}; }
    private static synchronized Counter register(){if(counters.size()==MAX_THREADS){droppedThreads++;return null;}var c=new Counter();counters.add(c);return c;}
    public static void count(int x,int z,boolean raw) {
        if(!AdmissionWorkTrace.ON)return;int phase=AdmissionWorkTrace.currentPhase();if(phase<1||phase>3)return;
        var counter=local.get();if(counter==null)return;var p=counter.phases[phase-1];long key=(x&0xffffffffL)|((long)z<<32);
        var set=raw?p.raw:p.ocean;if(raw)p.rawCalls++;else p.oceanCalls++;
        if(set.size()<LIMIT)set.add(key);
        else if(!set.contains(key)){if(raw)p.rawOverflow++;else p.oceanOverflow++;}
    }
    public static synchronized List<Object> snapshot() {
        var result=new ArrayList<Object>();
        for(int phase=0;phase<3;phase++){
            var ocean=new LongOpenHashSet();var raw=new LongOpenHashSet();long calls=0,heights=0,oc=0,rc=0;
            for(var c:counters){var p=c.phases[phase];ocean.addAll(p.ocean);raw.addAll(p.raw);calls+=p.oceanCalls;heights+=p.rawCalls;oc+=p.oceanOverflow;rc+=p.rawOverflow;}
            result.add(Map.of("phase",phase+1,"oceanCalls",calls,"rawHeightCalls",heights,"oceanDistinctAtLeast",ocean.size(),"rawDistinctAtLeast",raw.size(),
                "oceanOverflowObservations",oc,"rawOverflowObservations",rc,"droppedThreads",droppedThreads,"threads",counters.size(),"perThreadPerKindCoordinateLimit",LIMIT));
        }
        return result;
    }
    private AdmissionOceanTrace(){}
}
