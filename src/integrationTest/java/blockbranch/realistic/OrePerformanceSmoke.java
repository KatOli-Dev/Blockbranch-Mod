package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import com.google.gson.GsonBuilder;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.ChunkPos;

/** Opt-in matched native loading/probing observer; all timing spans include planning. */
public final class OrePerformanceSmoke {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
  try{
   var level=server.overworld();long seed=level.getSeed();boolean reload=Files.exists(Path.of("performance-marker"));
   int x=seed==424242?237504:seed==-17?-122692:56384,z=seed==424242?-262288:seed==-17?180834:-58450;
   var report=new LinkedHashMap<String,Object>();report.put("seed",seed);report.put("reload",reload);
   var chunks=new ArrayList<net.minecraft.world.level.chunk.LevelChunk>();
   for(int phase=0;phase<2;phase++){
    long start=System.nanoTime();for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)chunks.add(level.getChunk((x>>4)+dx+phase*8,(z>>4)+dz));report.put(phase==0?"coldLoadingNs":"steadyLoadingNs",System.nanoTime()-start);
   }
   long start=System.nanoTime(),checksum=0;int reads=0;
   for(var chunk:chunks){var profile=((SparseColumn)chunk).blockbranch$terrain().base();for(int zz=0;zz<16;zz++)for(int xx=0;xx<16;xx++)for(int depth=4;depth<=224;depth++){var state=profile.state(xx,profile.groundHeight(xx,zz)-depth,zz);checksum+=net.minecraft.world.level.block.Block.getId(state);reads++;}}
   report.put("profileProbeNs",System.nanoTime()-start);report.put("profileReads",reads);report.put("checksum",checksum);
   long[] warmed=new long[3];for(int pass=0;pass<3;pass++){long began=System.nanoTime(),check=0;for(var chunk:chunks){var profile=((SparseColumn)chunk).blockbranch$terrain().base();for(int zz=0;zz<16;zz++)for(int xx=0;xx<16;xx++)for(int depth=4;depth<=224;depth++)check+=net.minecraft.world.level.block.Block.getId(profile.state(xx,profile.groundHeight(xx,zz)-depth,zz));}warmed[pass]=System.nanoTime()-began;if(check!=checksum)throw new AssertionError("Stable warmed profile checksum");}report.put("warmedProfileProbeNs",warmed);
   start=System.nanoTime();for(var chunk:chunks)((SparseColumn)chunk).blockbranch$terrain().base().encode(level.palettedContainerFactory());report.put("encodingNs",System.nanoTime()-start);
   start=System.nanoTime();server.saveEverything(false,true,true);report.put("saveNs",System.nanoTime()-start);System.gc();report.put("postGcHeapBytes",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
   report.put("rss",Files.readAllLines(Path.of("/proc/self/status")).stream().filter(s->s.startsWith("VmRSS:")||s.startsWith("VmHWM:")).toList());
   Files.writeString(Path.of("performance-marker"),"saved");Files.writeString(Path.of(reload?"performance-reopen.json":"performance-fresh.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(Path.of("performance-result.txt"),"PASS\n");
  }catch(Throwable error){Blockbranch.LOGGER.error("ORE_PERFORMANCE_FAIL",error);try{Files.writeString(Path.of("performance-result.txt"),"FAIL "+error);}catch(Exception ignored){}}finally{server.halt(false);}
 });}
}
