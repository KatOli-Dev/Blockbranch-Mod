package blockbranch.realistic;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.nio.file.*;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.*;
import net.minecraft.server.level.*;
/** QA-only keyed lifecycle audit. It neither admits nor cancels native work. */
public final class CoastalGenerationTrace {
 public static final boolean ON=Boolean.getBoolean("blockbranch.coastalDrained");
 private static final Set<Object> tasks=Collections.newSetFromMap(new IdentityHashMap<>());
 private static final TreeMap<String,Integer> steps=new TreeMap<>(),admissions=new TreeMap<>();
 private static final TreeMap<String,Object> memory=new TreeMap<>();private static int active;
 public static synchronized void admitted(ChunkGenerationTask task){if(!ON)return;tasks.add(task);var p=task.getCenter().getPos();admissions.merge(CoastalMatchedProbe.phase+":"+p.x()+","+p.z()+":"+task.targetStatus,1,Integer::sum);}
 public static synchronized void released(Object task){if(ON)tasks.remove(task);}
 public static synchronized Map<String,Integer> progress(){return Map.of("tasks",tasks.size(),"steps",active);}
 public static synchronized boolean drained(){return tasks.isEmpty()&&active==0;}
 public static CompletableFuture<ChunkAccess> step(ChunkStep step,WorldGenContext context,ChunkAccess chunk,java.util.function.Supplier<CompletableFuture<ChunkAccess>> original){
  if(!ON)return original.get();String key=context.level().dimension().identifier()+":"+CoastalMatchedProbe.phase+":"+chunk.getPos().x()+","+chunk.getPos().z()+":"+step.targetStatus()+":"+chunk.getPersistedStatus();
  synchronized(CoastalGenerationTrace.class){if(steps.size()>100000)throw new AssertionError("Bounded trace");steps.merge(key,1,Integer::sum);active++;}
  try{return original.get().whenComplete((result,error)->{synchronized(CoastalGenerationTrace.class){active--;if(error!=null)steps.merge("FAIL:"+key,1,Integer::sum);}});}catch(Throwable t){synchronized(CoastalGenerationTrace.class){active--;}throw t;}
 }
 public static void memory(String label)throws Exception{
  if(!ON)return;long start=System.nanoTime();var server=ManagementFactory.getPlatformMBeanServer();String histogram=(String)server.invoke(new ObjectName("com.sun.management:type=DiagnosticCommand"),"gcClassHistogram",new Object[]{new String[0]},new String[]{String[].class.getName()});
  var used=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();var nativeMem=Files.readAllLines(Path.of("/proc/self/status")).stream().filter(s->s.startsWith("VmRSS:")||s.startsWith("VmHWM:")).toList();
  Files.writeString(CoastalMatchedProbe.dir().resolve("live-"+label+".txt"),histogram);
  synchronized(CoastalGenerationTrace.class){memory.put(label,Map.of("heapUsed",used.getUsed(),"heapCommitted",used.getCommitted(),"nativeMemory",nativeMem,"excludedDiagnosticNanos",System.nanoTime()-start,"activeSteps",active,"activeTasks",tasks.size()));}
 }
 public static synchronized Map<String,Object> snapshot(){return Map.of("steps",new TreeMap<>(steps),"admissions",new TreeMap<>(admissions),"memory",new TreeMap<>(memory),"activeSteps",active,"activeTasks",tasks.size());}
}
