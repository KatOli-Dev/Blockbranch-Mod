package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import com.google.gson.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtIo;

/** Matched gate-off/on fresh native windows. No pass threshold or hardware FPS
 * inference. Adjacent repeats differ in terrain; compare the same coordinates
 * across fresh JVMs. GC is outside timing; heap includes caches/loaded chunks,
 * thread allocation excludes workers, gzip is profiles rather than whole saves. */
public final class SeasonalForestCostProbe {
    private static final int[][] SITES={{16000,-1488000},{1488000,-304000},{1520000,-336000},{1520436,-336512}};
    private static final String[] NAMES={"ordinary-forest","sparse-jungle-to-seasonal","warm-woodland-to-seasonal","mixed-edge"};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.seasonalForestCostDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
            var random=level.getChunkSource().randomState();if(random.seed()!=1)throw new AssertionError("Matched native seed1 required");var d=g.drainage(random);var rows=new ArrayList<Object>();
            var threads=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
            var os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
            for(int site=0;site<SITES.length;site++)for(int repeat=0;repeat<3;repeat++){
                int x=SITES[site][0]+repeat*256,z=SITES[site][1];
                System.gc();long heapBefore=heap(),allocationBefore=threads.getThreadAllocatedBytes(Thread.currentThread().threadId()),cpuBefore=os.getProcessCpuTime(),start=System.nanoTime();
                var chunks=new ArrayList<net.minecraft.world.level.chunk.LevelChunk>();
                for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)chunks.add(level.getChunk((x>>4)+dx,(z>>4)+dz));
                long wall=System.nanoTime()-start,cpu=os.getProcessCpuTime()-cpuBefore,allocation=threads.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocationBefore;
                System.gc();long heapAfter=heap(),bytes=0;int owned=0;
                var profiles=dir.resolve("profiles");Files.createDirectories(profiles);
                for(var chunk:chunks){
                    var profile=((SparseColumn)chunk).blockbranch$terrain().base();var file=profiles.resolve(chunk.getPos().x()+"_"+chunk.getPos().z()+".nbt.gz");
                    NbtIo.writeCompressed(profile.encode(level.palettedContainerFactory()),file);bytes+=Files.size(file);
                    for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
                        int wx=chunk.getPos().x()*16+dx,wz=chunk.getPos().z()*16+dz;
                        if(g.seasonalForestDefinition()>0&&g.dryForestOwner(random).test(wx,wz))owned++;
                    }
                }
                var row=new LinkedHashMap<String,Object>();row.put("site",NAMES[site]);row.put("repeat",repeat);row.put("x",x);row.put("z",z);row.put("chunks",9);row.put("columns",2304);
                row.put("freshChunkWallNs",wall);row.put("processCpuNs",cpu);row.put("serverThreadAllocatedBytes",allocation);row.put("heapBeforeBytes",heapBefore);row.put("heapAfterBytes",heapAfter);row.put("profileGzipBytes",bytes);row.put("ownedColumns",owned);rows.add(row);
                Files.writeString(dir.resolve("partial.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));
            }
            var report=new LinkedHashMap<String,Object>();report.put("seed",random.seed());report.put("measurements",rows);report.put("maxHeap",Runtime.getRuntime().maxMemory());report.put("processors",Runtime.getRuntime().availableProcessors());
            report.put("generator",RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow());
            Files.writeString(dir.resolve("measurements.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
            Files.writeString(dir.resolve("result.txt"),"PASS finite matched cost recording; no speed or acceptance threshold\n");
        }catch(Throwable failure){Blockbranch.LOGGER.error("SEASONAL_FOREST_COST_FAIL",failure);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+failure+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static long heap(){return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();}
    private SeasonalForestCostProbe(){}
}
