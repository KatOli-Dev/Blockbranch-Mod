package blockbranch.realistic;

import blockbranch.cubic.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Exercises the production handoff, native biome fill and serialization. */
public final class AdmissionPreparationChecks {
    private static final java.util.concurrent.atomic.AtomicBoolean failNext = new java.util.concurrent.atomic.AtomicBoolean();
    private record Gate(Set<ChunkAccess> targets,java.util.concurrent.CountDownLatch entered,java.util.concurrent.CountDownLatch release) {}
    private static volatile Gate gate;
    private static volatile ChunkAccess failHeightmap;
    public static void beforeHeightmap(ChunkAccess chunk) {
        if(failHeightmap==chunk){
            failHeightmap=null;
            require(((SparseColumn)chunk).blockbranch$terrain()!=null,"Heightmap failure occurs after column installation");
            throw new IllegalStateException("QA injected heightmap failure");
        }
    }
    public static void injectFailure(ChunkAccess chunk) {
        var current=gate;
        if(current!=null&&current.targets.contains(chunk)){
            current.entered.countDown();
            try {if(!current.release.await(10,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("QA gate was not released");}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
        }
        if(failNext.compareAndSet(true,false))throw new IllegalStateException("QA injected profile failure");
    }
    private static ProtoChunk proto(ServerLevel l,ChunkPos p){return new ProtoChunk(p,UpgradeData.EMPTY,l,l.palettedContainerFactory(),null);}
    @SuppressWarnings("unchecked") private static Map<ChunkAccess,TerrainProfile> pending(RealisticGenerator g)throws Exception{
        var f=RealisticGenerator.class.getDeclaredField("preparedProfiles");f.setAccessible(true);return (Map<ChunkAccess,TerrainProfile>)f.get(g);
    }
    public static void run(MinecraftServer server)throws Exception {
        var l=server.overworld();var g=(RealisticGenerator)l.getChunkSource().getGenerator();var r=l.getChunkSource().randomState();var factory=l.palettedContainerFactory();
        scheduling(l,g,r);
        installedFailure(l,g,r);
        var pending=pending(g);var rows=new ArrayList<Object>();ChunkPos positive=null;int absent=0;
        var inputs=List.of(new ChunkPos(1931097>>4,-87591>>4),new ChunkPos(404324>>4,-1213758>>4),
            new ChunkPos(2081638>>4,-1567918>>4),new ChunkPos(-1,-1),new ChunkPos(-83949>>4,623061>>4));
        for(var pos:inputs){
            var c=proto(l,pos);var expected=g.profile(c,r);var tag=expected.encode(factory);boolean approach=expected.underground().hasApproach();
            g.prepareCaveProfile(c,r);TerrainProfile prepared;synchronized(pending){prepared=pending.get(c);}
            if(!approach){require(prepared==null,"No full profile prepared without an approach: "+pos);absent++;}
            else {require(prepared!=null,"Approach preserves native preparation");positive=pos;}
            var source=(ScreeBiomeSource)g.getBiomeSource();var full=source.createUncachedResolver(r);
            g.createBiomes(r,Blender.empty(),l.structureManager(),c).join();
            // ChunkStep normally publishes this status after generator completion.
            c.setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);
            // Before terrain construction, native BIOMES owns the base section
            // window. Sparse out-of-window biomes belong to the later profile.
            int min=((SparseColumn)c).blockbranch$baseMinSection()*4;
            for(int y=min;y<min+c.getSections().length*4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++)
                require(expected.biome(x,y,z).equals(c.getNoiseBiome(pos.x()*4+x,y,pos.z()*4+z)),"Native BIOMES base-window output");
            g.buildTerrain(c,Blender.empty(),r,l.structureManager(),l.getBiomeManager(),null,Set.of()).join();
            var actual=((SparseColumn)c).blockbranch$terrain().base();require(actual.encode(factory).equals(tag),"Exact complete native descriptor");
            if(approach)require(actual==prepared,"Actual terrain stage consumes prepared owner");
            require(TerrainProfile.decode(actual.encode(factory),factory).encode(factory).equals(tag),"Exact saved profile round trip");
            for(int z=0;z<4;z++)for(int x=0;x<4;x++)for(int d:new int[]{-16,0,12,64,128,256,512}){
                int y=Math.floorDiv(actual.groundHeight(x*4,z*4)-d,4);
                require(actual.biome(x,y,z).equals(full.getNoiseBiome(pos.x()*4+x,y,pos.z()*4+z)),"Uncached biome/profile agreement");
                require(actual.biome(x,y,z).equals(c.getNoiseBiome(pos.x()*4+x,y,pos.z()*4+z)),"Actual native BIOMES result");
            }
            synchronized(pending){require(!pending.containsKey(c),"Handoff consumed");}
            rows.add(Map.of("x",pos.x(),"z",pos.z(),"approach",approach,"mossPlacements",actual.litCaves().placements().length/2,"exactProfile",true));
        }
        require(absent>=3&&positive!=null,"Both negative and genuine entrance paths covered");
        var deferred=proto(l,inputs.getFirst());
        g.createBiomes(r,Blender.empty(),l.structureManager(),deferred).join();
        deferred.setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);
        failNext.set(true);boolean terrainFailed=false;
        try {g.buildTerrain(deferred,Blender.empty(),r,l.structureManager(),l.getBiomeManager(),null,Set.of()).join();}
        catch(RuntimeException expected){var cause=expected instanceof java.util.concurrent.CompletionException?expected.getCause():expected;terrainFailed=cause instanceof IllegalStateException&&cause.getMessage().equals("QA injected profile failure");}
        require(terrainFailed&&!failNext.get(),"Deferred terrain failure still propagates");
        require(((SparseColumn)deferred).blockbranch$terrain()==null,"Failed terrain publishes no partial column");
        g.buildTerrain(deferred,Blender.empty(),r,l.structureManager(),l.getBiomeManager(),null,Set.of()).join();
        require(((SparseColumn)deferred).blockbranch$terrain().base().encode(factory).equals(g.profile(proto(l,inputs.getFirst()),r).encode(factory)),"Deferred terrain retry exact");
        var p=positive;var first=proto(l,p);g.prepareCaveProfile(first,r);TerrainProfile saved;synchronized(pending){saved=pending.get(first);}
        require(g.profileForBuild(proto(l,p),r)!=saved,"Replacement chunk cannot consume another owner's handoff");
        require(g.profileForBuild(first,r)==saved,"Original chunk consumes exact immutable handoff");
        var broken=proto(l,p);failNext.set(true);boolean failed=false;
        try {g.createBiomes(r,Blender.empty(),l.structureManager(),broken).join();}catch(java.util.concurrent.CompletionException expected){failed=expected.getCause() instanceof IllegalStateException;}
        require(failed&&!failNext.get(),"Actual asynchronous preparation failure propagated");
        synchronized(pending){require(!pending.containsKey(broken),"Failed preparation leaves no partial handoff");}
        g.createBiomes(r,Blender.empty(),l.structureManager(),broken).join();
        require(g.profileForBuild(broken,r).encode(factory).equals(saved.encode(factory)),"Failure retry exact");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(3);
        try {var jobs=new ArrayList<java.util.concurrent.Callable<Boolean>>();for(int i=0;i<3;i++)jobs.add(()->{var c=proto(l,p);g.prepareCaveProfile(c,r);return g.profileForBuild(c,r).encode(factory).equals(saved.encode(factory));});for(var f:pool.invokeAll(jobs))require(f.get(),"Concurrent independent owners exact");}finally{pool.shutdownNow();}
        var held=new ArrayList<ProtoChunk>();for(int i=0;i<65;i++){var c=proto(l,p);g.prepareCaveProfile(c,r);held.add(c);}
        synchronized(pending){require(pending.size()==64,"Preparation remains capped at64");}
        var weak=new ArrayList<java.lang.ref.WeakReference<ProtoChunk>>();for(var c:held)weak.add(new java.lang.ref.WeakReference<>(c));held=null;
        for(int i=0;i<6&&weak.stream().anyMatch(w->w.get()!=null);i++){System.gc();Thread.sleep(20);}
        require(weak.stream().allMatch(w->w.get()==null),"Abandoned profile owners collectible");
        var paletteField=RealisticGenerator.class.getDeclaredField("palette");paletteField.setAccessible(true);var legacy=new RealisticGenerator((HabitatPalette)paletteField.get(g),true,true,true,true,true,2,1,0);
        var old=proto(l,p);legacy.prepareCaveProfile(old,r);require(pending(legacy).isEmpty(),"Legacy cave-biome opt-out unchanged");
        require(legacy.profileForBuild(old,r).encode(factory).equals(legacy.profile(old,r).encode(factory)),"Legacy profile unchanged");
        DiagnosticCurrentSave.verify(l);
        server.saveEverything(false,true,true);
        var report=new TreeMap<String,Object>();report.put("cases",rows);report.put("nonblockingTerrainDispatch",true);report.put("failureRetry",true);report.put("deferredTerrainFailureRetry",true);report.put("installedColumnFailureRetry",true);report.put("concurrentOwners",3);report.put("boundedWeakOwners",65);report.put("legacyOptOut",true);report.put("schema101AndSavedEdit",true);
        var executor=net.minecraft.util.Util.backgroundExecutor().service();report.put("backgroundParallelism",executor instanceof java.util.concurrent.ForkJoinPool backgroundPool?backgroundPool.getParallelism():-1);
        NativeOrdinaryQualification.write("preparation-checks.json",report);
        Files.writeString(Path.of("result.txt"),"PASS");
    }
    private static void scheduling(ServerLevel l,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r)throws Exception {
        var a=proto(l,new ChunkPos(120693,-5475));var b=proto(l,new ChunkPos(120694,-5475));
        for(var c:List.of(a,b)){g.createBiomes(r,Blender.empty(),l.structureManager(),c).join();c.setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);}
        var held=new Gate(Set.of(a,b),new java.util.concurrent.CountDownLatch(1),new java.util.concurrent.CountDownLatch(1));
        var caller=java.util.concurrent.Executors.newSingleThreadExecutor();boolean entered=false,returned=false,unpublished=false;
        gate=held;
        try {
            // A single caller models the native consecutive dispatcher. The
            // production method must return both futures before profile completion.
            // Only one task needs to enter: a second may be queued on a one-worker pool.
            var submitted=caller.submit(()->List.of(
                g.buildTerrain(a,Blender.empty(),r,l.structureManager(),l.getBiomeManager(),null,Set.of()),
                g.buildTerrain(b,Blender.empty(),r,l.structureManager(),l.getBiomeManager(),null,Set.of())));
            try {
                entered=held.entered.await(3,java.util.concurrent.TimeUnit.SECONDS);
                if(entered){try{submitted.get(1,java.util.concurrent.TimeUnit.SECONDS);returned=true;}catch(java.util.concurrent.TimeoutException ignored){}}
                unpublished=((SparseColumn)a).blockbranch$terrain()==null&&((SparseColumn)b).blockbranch$terrain()==null;
            } finally {held.release.countDown();}
            for(var f:submitted.get(30,java.util.concurrent.TimeUnit.SECONDS))f.get(30,java.util.concurrent.TimeUnit.SECONDS);
        } finally {held.release.countDown();gate=null;caller.shutdownNow();}
        require(entered&&returned&&unpublished,"Terrain dispatch returns both futures while profiles are held; no early publication");
        for(var c:List.of(a,b))require(((SparseColumn)c).blockbranch$terrain().base().encode(l.palettedContainerFactory()).equals(g.profile(proto(l,c.getPos()),r).encode(l.palettedContainerFactory())),"Concurrent terrain publication exact");
    }
    private static void installedFailure(ServerLevel l,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r) {
        var c=proto(l,new ChunkPos(120693,-5475));
        g.createBiomes(r,Blender.empty(),l.structureManager(),c).join();
        c.setPersistedStatus(net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES);
        failHeightmap=c;boolean failed=false;
        try {g.buildTerrain(c,Blender.empty(),r,l.structureManager(),l.getBiomeManager(),null,Set.of()).join();}
        catch(RuntimeException e){var cause=e instanceof java.util.concurrent.CompletionException?e.getCause():e;failed=cause instanceof IllegalStateException&&cause.getMessage().equals("QA injected heightmap failure");}
        finally {failHeightmap=null;}
        require(failed,"Failure after column installation propagates");
        var partial=((SparseColumn)c).blockbranch$terrain();
        require(partial!=null&&c.getPersistedStatus()==net.minecraft.world.level.chunk.status.ChunkStatus.BIOMES,"Installed column is not rolled back or marked terrain-ready");
        g.buildTerrain(c,Blender.empty(),r,l.structureManager(),l.getBiomeManager(),null,Set.of()).join();
        require(((SparseColumn)c).blockbranch$terrain().base().encode(l.palettedContainerFactory()).equals(partial.base().encode(l.palettedContainerFactory())),"Post-installation retry exact");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
