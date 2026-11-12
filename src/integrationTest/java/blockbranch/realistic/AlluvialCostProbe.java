package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import com.google.gson.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Fresh matched native windows, one asynchronous request per tick in both arms.
 * Wall includes server scheduling; process CPU includes workers/background work.
 * Thread allocation excludes workers. GC and compressed-profile writes are outside
 * timing. Heap includes reference views, bounded caches and retained native chunks;
 * it is not a precise feature allocation measurement or a hardware FPS benchmark. */
public final class AlluvialCostProbe {
    private static Run run;
    public static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{run=new Run(server.overworld());}catch(Throwable e){fail(server,e);}});
        ServerTickEvents.END_SERVER_TICK.register(server->{try{if(run!=null&&run.tick()){run=null;server.halt(false);}}catch(Throwable e){run=null;fail(server,e);}});
    }
    private static final class Run {
        final ServerLevel level;final RealisticGenerator g;final Path dir;final List<Object> rows=new ArrayList<>();
        final com.sun.management.ThreadMXBean threads=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        final com.sun.management.OperatingSystemMXBean os=(com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        final int[][] sites={{16000,-1488000},{1478348,-996764},{1468516,-675537},{0,0},{772752,384603}};
        final String[] names={"ordinary-forest","broad-bank","mixed-bank","confluence","wide-reach-bank"};
        final List<ChunkPos> chunks=new ArrayList<>();final List<CompletableFuture<ChunkResult<ChunkAccess>>> futures=new ArrayList<>();
        final long began=System.nanoTime();long start,cpu,allocation,heapBefore;int row,requested;
        Run(ServerLevel level)throws Exception{
            this.level=level;g=(RealisticGenerator)level.getChunkSource().getGenerator();dir=Path.of(System.getProperty("blockbranch.alluvialCostDirectory"));
            if(level.getSeed()!=1)throw new AssertionError("Matched seed1");
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,level.getServer());
            var d=g.drainage(level.getChunkSource().randomState());int[] incoming=new int[d.size()];for(int i=0;i<d.size();i++)if(d.river(i))incoming[d.parent(i)]++;
            double nearest=Double.POSITIVE_INFINITY;
            for(int node=0;node<d.size();node++)if(incoming[node]>=2&&d.waterLevel(node)>TerrainShape.SEA_LEVEL+8){double dx=d.nodeX(node)-sites[1][0],dz=d.nodeZ(node)-sites[1][1],distance=dx*dx+dz*dz;if(distance<nearest){nearest=distance;sites[3]=new int[]{d.nodeX(node),d.nodeZ(node)};}}
            if(sites[3][0]==0)throw new AssertionError("Bounded real confluence");
            Files.createDirectories(dir.resolve("profiles"));
        }
        boolean tick()throws Exception{
            if(System.nanoTime()-began>900_000_000_000L)throw new AssertionError("Finite cost deadline");
            if(row==sites.length*3){var report=new LinkedHashMap<String,Object>();report.put("seed",1);report.put("alluvialDefinition",g.alluvialDefinition());report.put("measurements",rows);report.put("maxHeap",Runtime.getRuntime().maxMemory());report.put("processors",Runtime.getRuntime().availableProcessors());report.put("scope","Fresh matched asynchronous9-chunk windows; wall includes ticks, CPU includes process, allocation server thread only, retained heap includes growing loaded set and bounded caches, gzip profiles only. Adjacent repeats differ in terrain. No acceptance threshold or FPS inference.");Files.writeString(dir.resolve("measurements.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(dir.resolve("result.txt"),"PASS finite native cost recording\n");return true;}
            int site=row/3,repeat=row%3,x=sites[site][0]+repeat*256,z=sites[site][1];
            if(chunks.isEmpty()){
                for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)chunks.add(new ChunkPos((x>>4)+dx,(z>>4)+dz));
                System.gc();heapBefore=heap();allocation=threads.getThreadAllocatedBytes(Thread.currentThread().threadId());cpu=os.getProcessCpuTime();start=System.nanoTime();
            }
            if(requested<9){var p=chunks.get(requested++);level.setChunkForced(p.x(),p.z(),true);futures.add(((blockbranch.mixin.cubic.PreviewChunkFutureAccessor)level.getChunkSource()).blockbranch$previewFuture(p.x(),p.z(),ChunkStatus.FULL,true));return false;}
            if(futures.stream().anyMatch(f->!f.isDone()))return false;
            long wall=System.nanoTime()-start,processCpu=os.getProcessCpuTime()-cpu,allocated=threads.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocation;
            for(var future:futures)if(!future.join().isSuccess())throw new AssertionError(future.join().getError());
            System.gc();long after=heap(),bytes=0;int changed=0,core=0;
            var d=g.drainage(level.getChunkSource().randomState());
            for(var p:chunks){var chunk=level.getChunk(p.x(),p.z());var profile=((SparseColumn)chunk).blockbranch$terrain().base();var file=dir.resolve("profiles/"+p.x()+"_"+p.z()+".nbt.gz");NbtIo.writeCompressed(profile.encode(level.palettedContainerFactory()),file);bytes+=Files.size(file);
                if(g.alluvialDefinition()>0)for(int wz=p.getMinBlockZ();wz<=p.getMaxBlockZ();wz++)for(int wx=p.getMinBlockX();wx<=p.getMaxBlockX();wx++){var plan=d.alluvialPlan(wx,wz);if(plan.changed())changed++;if(plan.connectedCore())core++;}
            }
            var r=new LinkedHashMap<String,Object>();r.put("site",names[site]);r.put("repeat",repeat);r.put("x",x);r.put("z",z);r.put("chunks",9);r.put("columns",2304);r.put("freshChunkWallNs",wall);r.put("processCpuNs",processCpu);r.put("serverThreadAllocatedBytes",allocated);r.put("heapBeforeBytes",heapBefore);r.put("heapAfterBytes",after);r.put("profileGzipBytes",bytes);r.put("changedColumns",changed);r.put("certifiedCore4",core);rows.add(r);
            Files.writeString(dir.resolve("partial.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));for(var p:chunks)level.setChunkForced(p.x(),p.z(),false);chunks.clear();futures.clear();requested=0;row++;return false;
        }
    }
    private static long heap(){return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();}
    private static void fail(net.minecraft.server.MinecraftServer server,Throwable e){Blockbranch.LOGGER.error("ALLUVIAL_COST_FAIL",e);try{Files.writeString(Path.of(System.getProperty("blockbranch.alluvialCostDirectory")).resolve("result.txt"),"FAIL "+e+"\n");}catch(Exception ignored){}server.halt(false);}
    private AlluvialCostProbe(){}
}
