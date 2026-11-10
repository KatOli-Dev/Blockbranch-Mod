package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
/** Complete flat generator metadata decoding, with matched schema88 control; no terrain construction. */
final class WaygateMetadataAudit {
 static void run(net.minecraft.resources.RegistryOps<com.google.gson.JsonElement> ops,com.google.gson.JsonObject current)throws Exception{
  current=current.deepCopy();current.remove("shipwrecks");current.remove("small_discovery_definition");current.remove("treasure_cache");current.addProperty("shape_version",91);
  var control=current.deepCopy();control.remove("desert_waygates");control.addProperty("shape_version",88);
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long thread=Thread.currentThread().threadId();var oldTimes=new ArrayList<Long>();var newTimes=new ArrayList<Long>();var oldBytes=new ArrayList<Long>();var newBytes=new ArrayList<Long>();
  for(int trial=0;trial<7;trial++)for(int phase=0;phase<2;phase++){
   boolean enabled=(phase==0)==((trial&1)==0);var input=enabled?current:control;long start=System.nanoTime(),bytes=bean.getThreadAllocatedBytes(thread);
   var decoded=RealisticGenerator.CODEC.codec().parse(ops,input).getOrThrow();long nanos=System.nanoTime()-start;bytes=bean.getThreadAllocatedBytes(thread)-bytes;
   (enabled?newTimes:oldTimes).add(nanos);(enabled?newBytes:oldBytes).add(bytes);
   WellInteraction.require(decoded.waygateDefinition()==(enabled?1:0),"Decoded feature flag stays explicit");
   WellInteraction.require(RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(input),"Full metadata round trip preserves schema, caches and all generation options");
  }
  var report=Map.of("schema88DecodeNanos",oldTimes,"schema91DecodeNanos",newTimes,"schema88AllocatedBytes",oldBytes,"schema91AllocatedBytes",newBytes,"meaning","Complete generator codec parse including waygate route construction, excluding immutable terrain initialization. Alternating order; all trials retained; not end-to-end world load timings.");
  Files.writeString(Path.of("metadata-decode.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
 }
}
