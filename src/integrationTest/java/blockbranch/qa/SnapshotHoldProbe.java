package blockbranch.qa;
import java.lang.management.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import jdk.jfr.*;
/** QA only. Monitor-enter wait is recorded separately by native JFR. */
public final class SnapshotHoldProbe {
 private static final boolean ON=Boolean.getBoolean("blockbranch.integratedWalking");
 private static final ThreadMXBean CPU=ManagementFactory.getThreadMXBean();
 private static final String[] KINDS={"uniform-biomes","uniform-snapshots","client-copy-cache"};
 public static volatile String phase="startup";
 private static final class Stats {final LongAdder calls=new LongAdder(),heldCalls=new LongAdder(),methodNs=new LongAdder(),holdNs=new LongAdder();final LongAccumulator maxHoldNs=new LongAccumulator(Long::max,0);}
 private static final ConcurrentMap<Long,Stats[]> STATS=new ConcurrentHashMap<>();
 private static final ConcurrentMap<Long,String> NAMES=new ConcurrentHashMap<>();
 private static final ThreadLocal<long[]> TIMES=ThreadLocal.withInitial(()->new long[12]);
 private static long cpu(){return CPU.isCurrentThreadCpuTimeSupported()&&CPU.isThreadCpuTimeEnabled()?CPU.getCurrentThreadCpuTime():-1;}
 public static void begin(int kind,boolean acquired){if(!ON)return;var a=TIMES.get();int i=kind*4;a[i]=System.nanoTime();a[i+1]=acquired?a[i]:0;a[i+2]=acquired?cpu():-1;}
 public static void acquired(int kind){if(!ON)return;var a=TIMES.get();a[kind*4+1]=System.nanoTime();a[kind*4+2]=cpu();}
 public static void end(int kind){
  if(!ON)return;long now=System.nanoTime();var a=TIMES.get();int i=kind*4;if(a[i]==0)return;
  long method=now-a[i],hold=a[i+1]==0?0:now-a[i+1],thread=Thread.currentThread().threadId();var stats=STATS.computeIfAbsent(thread,k->new Stats[]{new Stats(),new Stats(),new Stats()})[kind];NAMES.putIfAbsent(thread,Thread.currentThread().getName());stats.calls.increment();stats.methodNs.add(method);
  if(a[i+1]!=0){stats.heldCalls.increment();stats.holdNs.add(hold);stats.maxHoldNs.accumulate(hold);}
  if(method>=1_000_000L){var event=new Hold();event.kind=KINDS[kind];event.phase=phase;event.threadId=thread;event.methodNs=method;event.preAcquireNs=a[i+1]==0?-1:a[i+1]-a[i];event.approximateHoldNs=hold;long endCpu=cpu();event.holdCpuNs=a[i+2]<0||endCpu<0?-1:endCpu-a[i+2];event.commit();}
  a[i]=a[i+1]=0;
 }
 @Name("blockbranch.SnapshotHold") @Label("QA snapshot method/hold boundary") @StackTrace(false)
 static final class Hold extends Event {String kind,phase;long threadId,methodNs,preAcquireNs,approximateHoldNs,holdCpuNs;}
 public static void write(Path dir)throws Exception{var rows=new ArrayList<Map<String,Object>>();STATS.forEach((tid,stats)->{for(int k=0;k<3;k++){var s=stats[k];rows.add(Map.of("threadId",tid,"threadName",NAMES.get(tid),"kind",KINDS[k],"calls",s.calls.sum(),"heldCalls",s.heldCalls.sum(),"methodNs",s.methodNs.sum(),"approximateHoldNs",s.holdNs.sum(),"maxApproximateHoldNs",s.maxHoldNs.get()));}});Files.writeString(dir.resolve("snapshot-holds.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));}
}
