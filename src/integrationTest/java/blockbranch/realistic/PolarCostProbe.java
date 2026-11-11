package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import com.google.gson.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtIo;

/** Matched finite generation/storage/heap measurements, never a performance pass threshold.
 * Original and candidate run in fresh JVMs/worlds at identical coordinates/settings.
 * Per-thread allocation excludes worker threads; heap deltas include shared cache warming
 * and loaded chunks. GC is outside timing. Profile gzip sizes are measured, not whole-world
 * disk attribution. Replicates use disclosed adjacent windows, not identical terrain. */
public final class PolarCostProbe {
    /** The first four sites are the original matched baseline; the last two are the
     * qualification's finite ice/rock edge and exposed freshwater-lid witnesses, so a
     * mixed-boundary cost is measured where the candidate actually has one. The edge
     * windows are reported by composition counts, not assumed to straddle the boundary. */
    public static final int[][] SITES={{272000,-720000},{-1552000,-1904000},{-1456000,-1904000},{-1616000,-1904000},{-1615115,-1904000},{-1589245,-1966053}};
    private static final String[] NAMES={"ordinary-woodland","polar-lowland","polar-upland","polar-coast-reference","ice-rock-edge","ice-freshwater-lid"};
    private static final com.sun.management.ThreadMXBean THREADS=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path dir=Path.of(System.getProperty("blockbranch.polarCostDirectory"));
        try{
            var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();
            var d=g.drainage(random);var rows=new JsonArray();long checksum=0;
            for(int site=0;site<SITES.length;site++)for(int repeat=0;repeat<3;repeat++){
                int x=(site==3?coastX(d):SITES[site][0])+repeat*256,z=SITES[site][1];
                long heapBefore=heap(),cpuBefore=processCpu(),allocatedBefore=allocated(),start=System.nanoTime();
                var chunks=new ArrayList<net.minecraft.world.level.chunk.LevelChunk>();
                for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)chunks.add(level.getChunk((x>>4)+dx,(z>>4)+dz));
                long nanos=System.nanoTime()-start,cpu=processCpu()-cpuBefore,allocation=allocated()-allocatedBefore,heapAfter=heap();
                long profileBytes=0;int ice=0,sheet=0,lids=0,maxDepth=0,water=0,marine=0,min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
                Path profiles=dir.resolve("profiles");Files.createDirectories(profiles);
                for(var chunk:chunks){var p=((SparseColumn)chunk).blockbranch$terrain().base();
                    if(!blockbranch.cubic.TerrainProfile.decode(p.encode(level.palettedContainerFactory()),level.palettedContainerFactory()).encode(level.palettedContainerFactory()).equals(p.encode(level.palettedContainerFactory())))throw new AssertionError("Profile roundtrip");
                    Path file=profiles.resolve(chunk.getPos().x()+"_"+chunk.getPos().z()+".nbt.gz");NbtIo.writeCompressed(p.encode(level.palettedContainerFactory()),file);profileBytes+=Files.size(file);
                    for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
                        int wx=chunk.getPos().x()*16+dx,wz=chunk.getPos().z()*16+dz;var s=d.sample(wx,wz);min=Math.min(min,s.height());max=Math.max(max,s.height());if(d.landIceThickness(wx,wz)>0)ice++;
                        if(s.waterLevel()!=DrainageNetwork.DRY){water++;if(d.oceanWater(wx,wz,s))marine++;}
                        int depth=d.continentalIceThickness(wx,wz);if(depth>0){sheet++;maxDepth=Math.max(maxDepth,depth);
                            for(int dy=-1;dy<=1;dy++){var pos=new net.minecraft.core.BlockPos(wx,s.height()+dy,wz);var expected=dy==0?net.minecraft.world.level.block.Blocks.SNOW_BLOCK.defaultBlockState():dy<0&&depth>1?net.minecraft.world.level.block.Blocks.PACKED_ICE.defaultBlockState():g.surfaceState(wx,s.height()+dy,wz,random);
                                if(!level.getBlockState(pos).equals(expected))throw new AssertionError("Native ice role "+pos+" "+level.getBlockState(pos)+" expected "+expected);}
                        }else if(d.permanentFreshwaterLid(wx,wz)){lids++;if(!level.getBlockState(new net.minecraft.core.BlockPos(wx,s.waterLevel()-1,wz)).is(net.minecraft.world.level.block.Blocks.PACKED_ICE))throw new AssertionError("Native permanent freshwater lid");}
                    }
                }
                // Persistence diagnostic, outside the timed region: a section is saved only when
                // it differs from its profile (ColumnMigration.matches). Count deep sections
                // Y -8..-1 per window and whether the mismatch is biome-only.
                int divergent=0,biomeDivergent=0;var pairs=new java.util.TreeMap<String,Integer>();
                for(var chunk:chunks){var column=(SparseColumn)chunk;var base=column.blockbranch$terrain().base();
                    for(int sy=-8;sy<0;sy++){var section=column.blockbranch$read(sy);
                        if(!blockbranch.cubic.ColumnMigration.matches(base,sy,section)){divergent++;
                            boolean biome=false;
                            for(int qy=0;qy<4&&!biome;qy++)for(int qz=0;qz<4&&!biome;qz++)for(int qx=0;qx<4;qx++)if(!section.getNoiseBiome(qx,qy,qz).equals(base.biome(qx,(sy<<2)+qy,qz))){biome=true;break;}
                            if(biome)biomeDivergent++;
                            for(int ly=0;ly<16;ly++)for(int lz=0;lz<16;lz++)for(int lx=0;lx<16;lx++){var actual=section.getBlockState(lx,ly,lz);var expected=base.state(lx,sy*16+ly,lz);
                                if(!actual.equals(expected))pairs.merge(actual.getBlock()+"->"+expected.getBlock(),1,Integer::sum);}}}}
                if(!pairs.isEmpty())Blockbranch.LOGGER.info("POLAR_DIVERGENT_PAIRS site={} repeat={} sections={} pairs={}",NAMES[site],repeat,divergent,pairs);
                var cached=new JsonArray();for(int pass=0;pass<3;pass++){
                    long a=allocated(),t=System.nanoTime();
                    for(var chunk:chunks)for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
                        int wx=chunk.getPos().x()*16+dx,wz=chunk.getPos().z()*16+dz;var s=d.sample(wx,wz);checksum+=s.height();
                        for(int dy=-3;dy<=4;dy++)checksum+=net.minecraft.world.level.block.Block.getId(level.getBlockState(new net.minecraft.core.BlockPos(wx,s.height()+dy,wz)));
                    }
                    var v=new JsonObject();v.addProperty("wallNs",System.nanoTime()-t);v.addProperty("serverThreadAllocatedBytes",allocated()-a);cached.add(v);
                }
                var row=new JsonObject();row.addProperty("site",NAMES[site]);row.addProperty("repeat",repeat);row.addProperty("x",x);row.addProperty("z",z);row.addProperty("chunks",9);row.addProperty("columns",2304);
                row.addProperty("freshChunkWallNs",nanos);row.addProperty("freshChunkProcessCpuNs",cpu);row.addProperty("serverThreadAllocatedBytes",allocation);row.addProperty("heapBeforeBytes",heapBefore);row.addProperty("heapAfterBytes",heapAfter);row.addProperty("profileGzipBytes",profileBytes);row.addProperty("iceColumns",ice);row.addProperty("continentalIceColumns",sheet);row.addProperty("freshwaterLidColumns",lids);row.addProperty("exposedWaterColumns",water);row.addProperty("marineColumns",marine);row.addProperty("deepSectionsDivergent",divergent);row.addProperty("deepSectionsBiomeDivergent",biomeDivergent);row.addProperty("maxContinentalDepth",maxDepth);row.addProperty("minHeight",min);row.addProperty("maxHeight",max);row.add("cachedReads",cached);rows.add(row);
                Files.writeString(dir.resolve("partial.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));
            }
            long start=System.nanoTime();server.saveEverything(false,true,true);long saveNs=System.nanoTime()-start;
            var report=new JsonObject();report.addProperty("seed",424242);report.addProperty("originalPublicMain","1f9e74a16aa29f2455cba49289e218e3b1bb04d7");report.addProperty("saveWallNs",saveNs);report.addProperty("checksum",checksum);
            report.addProperty("processors",Runtime.getRuntime().availableProcessors());report.addProperty("maxHeap",Runtime.getRuntime().maxMemory());report.add("measurements",rows);
            report.add("generator",RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow());
            Files.writeString(dir.resolve("measurements.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");Files.writeString(dir.resolve("result.txt"),"PASS finite cost recording; no speed or cost-acceptance threshold\n");
        }catch(Throwable t){Blockbranch.LOGGER.error("POLAR_COST_FAIL",t);try{Files.writeString(dir.resolve("result.txt"),"FAIL "+t+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    /** Locate a physical marine/dry transition from the original bed in a finite
     * 32 km bracket, then refine its first 256 m crossing at one-block stride.
     * The candidate must retain this reference: cameras and cost windows cannot
     * chase its new ice edge and quietly compare different geography. */
    public static int coastX(DrainageNetwork drainage){
        var d=drainage.iceReference();int z=SITES[3][1];
        for(int x=-1648000+256;x<=-1616000;x+=256){
            var a=d.sample(x-256,z);var b=d.sample(x,z);
            if(d.oceanWater(x-256,z,a)&&b.waterLevel()==DrainageNetwork.DRY){
                for(int q=x-255;q<=x;q++)if(d.sample(q,z).waterLevel()==DrainageNetwork.DRY)return q;
            }
        }
        throw new AssertionError("No mixed marine/dry boundary in fixed bracket");
    }
    private static long allocated(){return THREADS.getThreadAllocatedBytes(Thread.currentThread().threadId());}
    private static long processCpu(){return ((com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime();}
    private static long heap(){System.gc();return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();}
    private PolarCostProbe(){}
}
