package blockbranch.realistic;
import java.nio.file.*;import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
/** Bounded final planner costs and native model/construction contracts. */
public final class DeepColdRuinSurvey {
 public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long id=Thread.currentThread().threadId(),cpu=bean.getCurrentThreadCpuTime(),bytes=bean.getThreadAllocatedBytes(id),start=System.nanoTime();var planner=DeepColdOceanRuins.bind(level.getSeed(),g,random);long bound=System.nanoTime()-start;var out=new LinkedHashMap<String,Object>();out.put("seed",level.getSeed());out.put("regionMaskGrid",2048);out.put("regions",planner.regionCount());out.put("candidateCells",planner.candidateCount());out.put("bindWallNanos",bound);out.put("bindThreadCpuNanos",bean.getCurrentThreadCpuTime()-cpu);out.put("bindThreadAllocatedBytes",bean.getThreadAllocatedBytes(id)-bytes);out.put("serializedIndexBytes",DeepColdOceanRuins.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,planner).getOrThrow().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length);write("ruin-bind.json",out);
  var rows=new ArrayList<Object>();for(var region:planner.ordered(BlockPos.ZERO,6000000).stream().limit(12).toList()){
   cpu=bean.getCurrentThreadCpuTime();bytes=bean.getThreadAllocatedBytes(id);start=System.nanoTime();var p=region.plan();var row=new LinkedHashMap<String,Object>();row.put("owner",region.owner());row.put("wallNanos",System.nanoTime()-start);row.put("threadCpuNanos",bean.getCurrentThreadCpuTime()-cpu);row.put("threadAllocatedBytes",bean.getThreadAllocatedBytes(id)-bytes);row.put("accepted",p.isPresent());p.ifPresent(plan->row.put("plan",plan));rows.add(row);write("ruin-regions.json",rows);
  }
  out.put("sampleRegions",rows);write("ruin-survey.json",out);Files.writeString(Path.of("ruin-result.txt"),"PASS bounded deep cold-family basin probe");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("RUIN_SURVEY_FAIL",t);try{Files.writeString(Path.of("ruin-result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
 private static void write(String name,Object value)throws Exception{Files.writeString(Path.of(name),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(value));}
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
