package blockbranch.client.cubic;

import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.client.multiplayer.ClientLevel;

/** Opt-in native client checks over an isolated scratch chunk; never changes the received world. */
public final class ProceduralSectionCopiesSmoke {
    private static boolean done;
    public static void verify(ClientLevel level,TerrainProfile base,ChunkPos pos) {
        if(done||!Boolean.getBoolean("blockbranch.sectionCopiesSmoke"))return;
        if(Boolean.getBoolean("blockbranch.sectionCopiesRequireDripstone")&&base.dripstone().placements().length==0)return;done=true;
        var chunk=new ProtoChunk(pos,UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
        var sparse=(SparseColumn)chunk;var terrain=new ColumnTerrain(chunk,base,level.palettedContainerFactory(),null,new int[0]);sparse.blockbranch$terrain(terrain);
        int[] drip=base.dripstone().placements();
        int sy=drip.length>0?Math.floorDiv(drip[1],16):Math.floorDiv(base.groundHeight(0,0)-40,16);
        var cache=new ProceduralSectionCopies.Cache(1024*1024,2);
        var first=cache.copyBase(base,sy);
        for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)check(first.get(x,y,z)==base.state(x,sy*16+y,z),"snapshot material");
        // Whole-section generation (section ore/cave tables) against per-block state, through every ore window.
        int generated=0;
        for(int section=Math.max(blockbranch.realistic.TerrainShape.FLOOR,base.groundHeight(0,0)-320)>>4;section<=(base.groundHeight(0,0)+64)>>4;section++){
            var states=base.generateStates(section);generated++;
            for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)check(states.get(x,y,z)==base.state(x,section*16+y,z),"whole-section material "+section);
        }
        blockbranch.Blockbranch.LOGGER.info("SECTION_COPIES_GENERATED_SECTIONS checked={}",generated);
        first.set(0,0,0,Blocks.GOLD_BLOCK.defaultBlockState());
        check(cache.copyBase(base,sy).get(0,0,0)==base.state(0,sy*16,0),"caller isolation");
        check(cache.statistics()[0]==1&&cache.statistics()[1]==1,"reuse");
        int[] emptyWater=new int[256];Arrays.fill(emptyWater,Integer.MIN_VALUE);
        var solid=new TerrainProfile(-128,128,new int[256],emptyWater,base.biomes(),level.palettedContainerFactory().blockStatesStrategy());
        verifyConcurrency(solid);
        verifySnapshot(level,base,pos,sy);
        var eviction=new ProceduralSectionCopies.Cache(1024*1024,2);
        eviction.copyBase(solid,-3);eviction.copyBase(solid,-4);eviction.copyBase(solid,-5);eviction.copyBase(solid,-3);
        check(eviction.statistics()[2]==2&&eviction.statistics()[1]==4,"nonempty entry eviction");
        int knownAir=0;
        for(int section=sy-8;section<=sy+8;section++)if(base.sectionKnownEmpty(section)){
            var air=cache.copyBase(base,section);knownAir++;
            for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)
                check(air.get(x,y,z)==base.state(x,section*16+y,z),"received exact air proof matches immutable materials");
        }
        blockbranch.Blockbranch.LOGGER.info("SECTION_COPIES_KNOWN_AIR checked={}",knownAir);
        cache.clear();
        var jobs=new java.util.ArrayList<java.util.concurrent.CompletableFuture<PalettedContainer<net.minecraft.world.level.block.state.BlockState>>>();
        for(int i=0;i<4;i++)jobs.add(java.util.concurrent.CompletableFuture.supplyAsync(()->cache.copyBase(base,sy)));
        for(var job:jobs)check(job.join().get(0,0,0)==base.state(0,sy*16,0),"concurrent copies");
        check(cache.statistics()[1]==1&&cache.statistics()[0]==3,"concurrent miss is published once");
        var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
        long misses=cache.statistics()[1];cache.copyBase(decoded,sy);check(cache.statistics()[1]==misses+1,"profile identity isolation");
        var airProof=new ProceduralSectionCopies.Cache(1024*1024,2);airProof.copyBase(solid,-1);
        for(int y=200;y<220;y++)check(airProof.copyBase(solid,y).get(0,0,0).isAir(),"known empty exact palette");
        check(airProof.statistics()[2]==1,"known air must not evict expensive cache entries");airProof.copyBase(solid,-1);
        check(airProof.statistics()[0]==1,"nonempty entry remains reusable after air traffic");
        var cleanup=new ProceduralSectionCopies.Cache(1024*1024,2);cleanup.copyBase(solid,-3);
        try{
            var field=ProceduralSectionCopies.Cache.class.getDeclaredField("CACHE");field.setAccessible(true);
            var entries=(java.util.Map<?,?>)field.get(cleanup);var key=(java.lang.ref.Reference<?>)entries.keySet().iterator().next();
            key.enqueue();cleanup.copyBase(solid,200);
            check(cleanup.statistics()[2]==0&&cleanup.statistics()[3]==0,"air-only traffic drains expired owner queue");
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        var airA=airProof.copyBase(solid,200);airA.set(0,0,0,Blocks.GOLD_BLOCK.defaultBlockState());
        check(airProof.copyBase(solid,200).get(0,0,0).isAir(),"bypassed air remains caller owned");
        var unit=new ProceduralSectionCopies.Cache(Long.MAX_VALUE,100);unit.copyBase(solid,-3);long unitWeight=unit.statistics()[3];
        var weighted=new ProceduralSectionCopies.Cache(unitWeight*2,100);
        weighted.copyBase(solid,-3);weighted.copyBase(solid,-4);weighted.copyBase(solid,-5);
        check(weighted.statistics()[2]==2&&weighted.statistics()[3]<=unitWeight*2,"cumulative weight eviction");
        var tiny=new ProceduralSectionCopies.Cache(1,2);tiny.copyBase(solid,-1);check(tiny.statistics()[2]==0,"weight admission bound");
        ProceduralSectionCopies.clear();ProceduralSectionCopies.copy(sparse,sy);
        sparse.blockbranch$write(sy).setBlockState(0,0,0,Blocks.DIAMOND_BLOCK.defaultBlockState());
        check(ProceduralSectionCopies.copy(sparse,sy).get(0,0,0).is(Blocks.DIAMOND_BLOCK),"resident edit bypass");
        sparse.blockbranch$write(sy).setBlockState(0,0,0,Blocks.GLOWSTONE.defaultBlockState());
        check(sparse.blockbranch$read(sy).maybeHas(s->s.getLightEmission()>0),"genuine resident emitter remains discoverable");
        check(ProceduralSectionCopies.copy(sparse,sy).get(0,0,0).is(Blocks.GLOWSTONE),"resident emitter bypasses cached base");
        sparse.blockbranch$replace(sy,null);
        check(ProceduralSectionCopies.copy(sparse,sy).get(0,0,0).isAir(),"pending overlay bypass");
        var factory=level.palettedContainerFactory();
        int[] dry=new int[256];Arrays.fill(dry,Integer.MIN_VALUE);
        var plain=new TerrainProfile(-16,64,new int[256],dry,base.biomes(),factory.blockStatesStrategy());
        for(int layer:new int[]{-500,-1,0,200})check(!plain.view(layer).maybeHas(s->s.getLightEmission()>0),"empty surface palette must not advertise mushrooms");
        var tag=plain.encode(factory);byte[] mushrooms=new byte[256];Arrays.fill(mushrooms,(byte)6);tag.putByteArray("warm",mushrooms);
        var mushroomProfile=TerrainProfile.decode(tag,factory);
        check(mushroomProfile.state(0,1,0).is(Blocks.BROWN_MUSHROOM),"mushroom test occupancy");
        check(mushroomProfile.view(0).maybeHas(s->s.getLightEmission()>0),"surface mushroom light retained");
        check(!mushroomProfile.view(-1).maybeHas(s->s.getLightEmission()>0),"underground excludes surface mushroom");
        var wood=new CanopyLayer(new int[]{pack(1,1)});var pickle=new CanopyLayer(new int[]{pack(1,160)});
        check(!wood.maybeHas(s->s.getLightEmission()>0),"wood must not scan for nonexistent emitters");
        check(pickle.maybeHas(0,s->s.getLightEmission()>0),"pickle emitter retained");
        check(!pickle.maybeHas(-1,s->s.getLightEmission()>0),"other section excludes pickle");
        cache.clear();check(cache.statistics()[2]==0&&cache.statistics()[3]==0,"clear releases cache");
        measurePolicies(base,sy,solid);
        ProceduralSectionCopies.clear();
        try{java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("blockbranch.sectionCopiesSmokeResult")),"PASS material equality, reuse, caller isolation, entry/weight bounds, edit/pending-overlay bypass, canopy emitters\n");}catch(Exception e){throw new RuntimeException(e);}
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            check(ProceduralSectionCopies.DEFAULT.statistics()[2]==0&&ProceduralSectionCopies.DEFAULT.statistics()[3]==0,"actual disconnect clears default cache");
            try{java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("blockbranch.sectionCopiesSmokeResult")),"PASS actual disconnect clears bounded cache\n",java.nio.file.StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}
        });
        blockbranch.Blockbranch.LOGGER.info("SECTION_COPIES_SMOKE PASS profile={} dripPlacements={} section={}",base.encode(factory).getIntOr("version",0),drip.length/3,sy);
    }
    public static void beforeDisconnect(){
        if(!done||!Boolean.getBoolean("blockbranch.sectionCopiesSmoke"))return;
        long[] stats=ProceduralSectionCopies.DEFAULT.statistics();check(stats[2]>0,"actual renderer populated default snapshot cache");
        check(stats[2]<=8192&&stats[3]<=64L*1024*1024,"actual default cache respects entry/accounting bounds");
        try{java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("blockbranch.sectionCopiesSmokeResult")),"PASS renderer cache before disconnect="+java.util.Arrays.toString(stats)+"\n",java.nio.file.StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}
    }
    private static void verifySnapshot(ClientLevel level,TerrainProfile base,ChunkPos pos,int sy){
        var chunk=new LevelChunk(level,pos);var sparse=(SparseColumn)chunk;
        sparse.blockbranch$terrain(new ColumnTerrain(chunk,base,level.palettedContainerFactory(),null,new int[0]));
        var before=new net.minecraft.client.renderer.chunk.SectionCopy(chunk,chunk.getSectionIndexFromSectionY(sy));
        var at=new net.minecraft.core.BlockPos(pos.getMinBlockX(),sy*16,pos.getMinBlockZ());
        sparse.blockbranch$write(sy).setBlockState(0,0,0,Blocks.GOLD_BLOCK.defaultBlockState());
        var after=new net.minecraft.client.renderer.chunk.SectionCopy(chunk,chunk.getSectionIndexFromSectionY(sy));
        check(before.getBlockState(at)==base.state(0,sy*16,0),"snapshot created before edit resolves immutable base");
        check(after.getBlockState(at).is(Blocks.GOLD_BLOCK),"fresh snapshot includes resident edit");
        sparse.blockbranch$replace(sy,null);
        var overlay=new net.minecraft.client.renderer.chunk.SectionCopy(chunk,chunk.getSectionIndexFromSectionY(sy));
        check(overlay.getBlockState(at).isAir(),"fresh snapshot includes received air overlay");
        check(after.getBlockState(at).is(Blocks.GOLD_BLOCK),"later overlay cannot mutate existing snapshot");
        blockbranch.Blockbranch.LOGGER.info("SECTION_COPY_SNAPSHOT PASS deferred={}",(Object)before instanceof ProceduralSectionCopies.DeferredBase);
    }
    private static void verifyConcurrency(TerrainProfile base){
        var jobs=java.util.concurrent.Executors.newFixedThreadPool(4);
        var entered=new java.util.concurrent.CountDownLatch(1);
        var release=new java.util.concurrent.CountDownLatch(1);
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var cache=new ProceduralSectionCopies.Cache(1024*1024,4,(p,y)->{
            if(calls.incrementAndGet()==1){entered.countDown();await(release);}
            return p.generateStates(y);
        });
        try{
            var old=jobs.submit(()->cache.copyBase(base,-3));
            check(entered.await(10,java.util.concurrent.TimeUnit.SECONDS),"generation entered");
            // A different key must finish while the first generator is held outside the monitor.
            jobs.submit(()->cache.copyBase(base,-4)).get(10,java.util.concurrent.TimeUnit.SECONDS);
            cache.clear();
            jobs.submit(()->cache.copyBase(base,-3)).get(10,java.util.concurrent.TimeUnit.SECONDS);
            check(cache.statistics()[1]==1&&cache.statistics()[2]==1,"new epoch owns its entry");
            release.countDown();old.get(10,java.util.concurrent.TimeUnit.SECONDS);
            check(cache.statistics()[1]==1&&cache.statistics()[2]==1,"stale completion cannot repopulate cache");
            long staleEpoch=cache.epoch();cache.clear();
            cache.copyBase(base,-3,staleEpoch);
            check(cache.statistics()[2]==0,"deferred read begun after clear cannot populate cache");
            calls.set(0);
            var start=new java.util.concurrent.CountDownLatch(1);
            var results=new ArrayList<java.util.concurrent.Future<PalettedContainer<net.minecraft.world.level.block.state.BlockState>>>();
            for(int n=0;n<4;n++)results.add(jobs.submit(()->{await(start);return cache.copyBase(base,-3);}));
            start.countDown();
            var first=results.getFirst().get(10,java.util.concurrent.TimeUnit.SECONDS);
            first.set(0,0,0,Blocks.GOLD_BLOCK.defaultBlockState());
            for(int n=1;n<4;n++)check(!results.get(n).get(10,java.util.concurrent.TimeUnit.SECONDS).get(0,0,0).is(Blocks.GOLD_BLOCK),"concurrent caller isolation");
            check(calls.get()==1,"same-key generation is coalesced");
            var attempts=new java.util.concurrent.atomic.AtomicInteger();
            var failing=new ProceduralSectionCopies.Cache(1024*1024,4,(p,y)->{if(attempts.getAndIncrement()==0)throw new IllegalStateException("injected");return p.generateStates(y);});
            try{failing.copyBase(base,-3);throw new AssertionError("expected failure");}catch(IllegalStateException expected){}
            check(failing.copyBase(base,-3)!=null&&attempts.get()==2,"failed generation can retry");
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)check(base.spawnSurfaceTop(x,z)==base.top(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque(),x,z,ColumnLimits.MAX_Y-1),"memoized spawn top");
        }catch(Exception e){throw new RuntimeException(e);}
        finally{release.countDown();jobs.shutdownNow();}
    }
    private static void await(java.util.concurrent.CountDownLatch latch){
        try{check(latch.await(10,java.util.concurrent.TimeUnit.SECONDS),"latch timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}
    }
    private static volatile Object sink;
    private static void measurePolicies(TerrainProfile base,int section,TerrainProfile plain){
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long id=Thread.currentThread().threadId();var rows=new java.util.ArrayList<Object>();
        for(int scenario=0;scenario<2;scenario++){
            var p=scenario==0?plain:base;int y=scenario==0?8:section;
            var old=new LegacySectionCopyCache(64L*1024*1024,8192);var next=new ProceduralSectionCopies.Cache(64L*1024*1024,8192);
            for(int i=0;i<2000;i++){sink=old.copyBase(p,y);sink=next.copyBase(p,y);}
            var expected=p.state(0,y*16,0);
            for(int round=0;round<4;round++)for(int order=0;order<2;order++){
                int arm=(round+order)&1;long cpu=bean.getCurrentThreadCpuTime(),bytes=bean.getThreadAllocatedBytes(id),wall=System.nanoTime();
                for(int i=0;i<4096;i++){var value=arm==0?old.copyBase(p,y):next.copyBase(p,y);check(value.get(0,0,0)==expected,"warm policy output");sink=value;}
                rows.add(java.util.Map.of("scenario",scenario==0?"warm-air":"warm-complex","round",round,"reference",arm==0,"copies",4096,"cpuNs",bean.getCurrentThreadCpuTime()-cpu,"allocatedBytes",bean.getThreadAllocatedBytes(id)-bytes,"wallNs",System.nanoTime()-wall));
            }
            old.clear();next.clear();
        }
        sink=null;try{java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("blockbranch.sectionCopiesSmokeResult")).resolveSibling("copy-cache-work.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));}catch(Exception e){throw new RuntimeException(e);}
    }
    private static int pack(int y,int code){return ((y-blockbranch.realistic.TerrainShape.FLOOR)<<16)|code;}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
