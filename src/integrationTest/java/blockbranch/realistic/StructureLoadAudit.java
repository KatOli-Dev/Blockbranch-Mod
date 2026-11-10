package blockbranch.realistic;
import java.nio.file.*;
import java.lang.management.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
public final class StructureLoadAudit {
 private static long started,allocated,initializationNanos,initializationBytes;
 private static String thread;private static List<String> stack;private static int calls;
 private static final com.sun.management.ThreadMXBean BEAN=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
 public static void begin(){started=System.nanoTime();allocated=BEAN.getThreadAllocatedBytes(Thread.currentThread().threadId());thread=Thread.currentThread().getName();stack=Arrays.stream(Thread.currentThread().getStackTrace()).map(Object::toString).limit(14).toList();calls++;}
 public static void end(){initializationNanos=System.nanoTime()-started;initializationBytes=BEAN.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocated;}
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  long ready=ManagementFactory.getRuntimeMXBean().getUptime();var g=(RealisticGenerator)server.overworld().getChunkSource().getGenerator();
  WellInteraction.require(calls==1,"Exactly one initialization, including reopen");
  long retained=0;if(g.structureDefinition()==3){var w=g.wells(server.overworld().getChunkSource().randomState());retained=w.retainedPlanBytes()+w.atlas().retainedArrayBytes();}
  System.gc();long heap=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
  var report=new LinkedHashMap<String,Object>();report.put("savedGeneratorIsLive",server.getWorldGenSettings().dimensions().overworld()==g);report.put("definition",g.structureDefinition());report.put("cacheBytes",g.cachedStructureBytes());report.put("cacheRestored",g.structureDefinition()==3&&g.wells(server.overworld().getChunkSource().randomState()).restoredFromSave());report.put("createStateNanos",initializationNanos);report.put("createStateAllocatedBytes",initializationBytes);report.put("jvmReadyMillis",ready);report.put("heapAfterGcBytes",heap);report.put("retainedPlacementBytes",retained);report.put("thread",thread);report.put("stack",stack);report.put("calls",calls);
  Files.writeString(Path.of("load.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(Path.of("load-result.txt"),"PASS");
 }catch(Throwable e){blockbranch.Blockbranch.LOGGER.error("LOAD_AUDIT_FAIL",e);try{Files.writeString(Path.of("load-result.txt"),"FAIL "+e);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
