package blockbranch.realistic;

import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;

/** Focused native-registry checks for the immutable snapshot cache's retained semantics. */
public final class UniformSnapshotSmoke {
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        var result=Path.of(System.getProperty("blockbranch.uniformSnapshotResult"));
        try{
            var level=server.overworld();var factory=level.palettedContainerFactory();
            var source=new LevelChunkSection(new PalettedContainer<>(Blocks.STONE.defaultBlockState(),factory.blockStatesStrategy()),factory.createForBiomes());
            var snapshot=UniformSnapshots.copyOf(source);
            require(snapshot==UniformSnapshots.copyOf(source),"Live uniform snapshots share identity");
            var palette=UniformSnapshots.compactBiomes(factory.createForBiomes());
            require(palette==UniformSnapshots.compactBiomes(factory.createForBiomes()),"Live uniform biome palettes share identity");
            try(var workers=java.util.concurrent.Executors.newFixedThreadPool(2)){
                var jobs=new java.util.ArrayList<java.util.concurrent.Future<?>>();
                for(int i=0;i<128;i++)jobs.add(workers.submit(()->{
                    require(UniformSnapshots.copyOf(source)==snapshot,"Concurrent lookup returns live snapshot");
                    require(UniformSnapshots.compactBiomes(factory.createForBiomes())==palette,"Concurrent lookup returns live palette");
                }));
                for(var job:jobs)job.get(10,java.util.concurrent.TimeUnit.SECONDS);
            }
            var edited=snapshot.copy();edited.setBlockState(1,1,1,Blocks.GOLD_BLOCK.defaultBlockState());
            require(snapshot.getBlockState(1,1,1).is(Blocks.STONE),"Block copy edit is independent");
            source.setBlockState(2,2,2,Blocks.DIRT.defaultBlockState());
            require(snapshot.getBlockState(2,2,2).is(Blocks.STONE),"Live source edit cannot alter cached snapshot");
            boolean frozen=false;try{snapshot.setBlockState(0,0,0,Blocks.DIRT.defaultBlockState());}catch(UnsupportedOperationException expected){frozen=true;}
            require(frozen,"Cached snapshot rejects mutation");
            var biome=source.getNoiseBiome(0,0,0);
            var alternate=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(biome.is(net.minecraft.world.level.biome.Biomes.DESERT)?net.minecraft.world.level.biome.Biomes.PLAINS:net.minecraft.world.level.biome.Biomes.DESERT);
            edited.fillBiomesFromNoise((x,y,z)->x==0?alternate:biome,0,0,0);
            require(snapshot.getNoiseBiome(0,0,0).equals(biome)&&edited.getNoiseBiome(0,0,0).equals(alternate),"Biome copy edit is independent");
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{
                edited.write(buffer);var loaded=new LevelChunkSection(factory);loaded.read(buffer);
                require(loaded.getBlockState(1,1,1).is(Blocks.GOLD_BLOCK)&&loaded.getNoiseBiome(0,0,0).equals(alternate),"Native section encoding preserves edited state and biome");
            }finally{buffer.release();}
            verifyHotLookup(factory,alternate,result.getParent());
            Files.writeString(result,"PASS warm lookup bypasses actual global monitor; shared identity; concurrent lookups; immutable snapshots; independent block/biome edits; native section roundtrip\n");
        }catch(Throwable error){blockbranch.Blockbranch.LOGGER.error("UNIFORM_SNAPSHOT_FAIL",error);try{Files.writeString(result,"FAIL "+error+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static volatile int sink;
    private static void verifyHotLookup(PalettedContainerFactory factory,net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> alternate,Path directory)throws Exception{
        var cpu=java.lang.management.ManagementFactory.getThreadMXBean();
        var allocation=(com.sun.management.ThreadMXBean)cpu;
        var source=factory.createForBiomes();var expected=UniformSnapshots.compactBiomes(source);
        for(int i=0;i<20000;i++)require(UniformSnapshots.compactBiomes(source)==expected,"Warm identity");
        var rows=new java.util.ArrayList<java.util.Map<String,Long>>();
        for(int r=0;r<6;r++){
            long beforeCpu=cpu.getCurrentThreadCpuTime(),beforeBytes=allocation.getThreadAllocatedBytes(Thread.currentThread().threadId());int checksum=0;
            for(int i=0;i<100000;i++)checksum^=System.identityHashCode(UniformSnapshots.compactBiomes(source));
            sink=checksum;rows.add(java.util.Map.of("calls",100000L,"cpuNs",cpu.getCurrentThreadCpuTime()-beforeCpu,"allocatedBytes",allocation.getThreadAllocatedBytes(Thread.currentThread().threadId())-beforeBytes));
        }
        Files.writeString(directory.resolve("hot-lookup.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));
        var field=UniformSnapshots.class.getDeclaredField("BIOMES");field.setAccessible(true);Object monitor=field.get(null);
        var caller=new java.util.concurrent.atomic.AtomicReference<Thread>();
        try(var worker=java.util.concurrent.Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"UniformBiomeWarmProbe");caller.set(t);return t;})){
            require(worker.submit(()->UniformSnapshots.compactBiomes(source)).get()==expected,"Caller warmed against actual production cache");
            synchronized(monitor){
                var started=new java.util.concurrent.CountDownLatch(1);
                var hot=worker.submit(()->{started.countDown();return UniformSnapshots.compactBiomes(source);});
                require(started.await(2,java.util.concurrent.TimeUnit.SECONDS),"Controlled caller started");
                long until=System.nanoTime()+2_000_000_000L;
                while(!hot.isDone()&&System.nanoTime()<until){var info=cpu.getThreadInfo(caller.get().threadId());if(info!=null&&info.getThreadState()==Thread.State.BLOCKED&&info.getLockOwnerId()==Thread.currentThread().threadId())break;Thread.sleep(1);}
                require(hot.isDone(),"Warm lookup blocked by actual BIOMES monitor");require(hot.get()==expected,"Nonblocking lookup preserves canonical identity");
            }
            var otherBiome=new PalettedContainer<>(alternate,factory.biomeStrategy());
            var different=worker.submit(()->UniformSnapshots.compactBiomes(otherBiome)).get();
            require(different!=expected&&different.get(0,0,0).equals(alternate),"Different biome cannot reuse last palette");
            var otherStrategy=Strategy.createForBiomes(factory.biomeStrategy().globalMap());
            var other=new PalettedContainer<>(factory.defaultBiome(),otherStrategy);
            var distinct=worker.submit(()->UniformSnapshots.compactBiomes(other)).get();
            require(distinct!=expected&&((PaletteStrategyAccess<?>)distinct).realistic$strategy()==otherStrategy,"Different strategy identity cannot reuse last palette");
            var witness=worker.submit(()->witness(factory)).get();
            for(int i=0;i<8&&(witness.palette.get()!=null||witness.strategy.get()!=null);i++){System.gc();Thread.sleep(20);}
            require(witness.palette.get()==null&&witness.strategy.get()==null,"Live worker's last-result cache retains neither palette nor strategy");
            require(worker.submit(()->UniformSnapshots.compactBiomes(source)).get()==expected,"Cleared last-result hint recovers the canonical palette on the same worker");
            require(worker.submit(()->Thread.currentThread().threadId()).get()==caller.get().threadId(),"Lifetime checked while same worker remains alive");
        }
    }
    private record Witness(java.lang.ref.WeakReference<Object> palette,java.lang.ref.WeakReference<Object> strategy){}
    private static Witness witness(PalettedContainerFactory factory){
        var strategy=Strategy.createForBiomes(factory.biomeStrategy().globalMap());
        var palette=UniformSnapshots.compactBiomes(new PalettedContainer<>(factory.defaultBiome(),strategy));
        return new Witness(new java.lang.ref.WeakReference<>(palette),new java.lang.ref.WeakReference<>(strategy));
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
