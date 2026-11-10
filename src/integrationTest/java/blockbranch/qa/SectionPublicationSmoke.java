package blockbranch.qa;

import blockbranch.cubic.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;

/** Forces the native residency/payload publication interleaving in a detached real chunk. */
public final class SectionPublicationSmoke {
    public static void verify(ServerLevel level)throws Exception{
        var factory=level.palettedContainerFactory();
        var biomes=new PalettedContainer<>(level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS),factory.biomeStrategy());
        int[] water=new int[256];Arrays.fill(water,Integer.MIN_VALUE);
        var base=new TerrainProfile(-16,64,new int[256],water,biomes,factory.blockStatesStrategy());
        var chunk=new LevelChunk(level,new ChunkPos(200,200));var sparse=(SparseColumn)chunk;
        var terrain=new ColumnTerrain(chunk,base,factory,SectionStore.of(level),new int[0]);sparse.blockbranch$terrain(terrain);
        sparse.blockbranch$write(0).setBlockState(8,1,8,Blocks.GLOWSTONE.defaultBlockState());terrain.flush();
        var field=ColumnTerrain.class.getDeclaredField("resident");field.setAccessible(true);
        @SuppressWarnings("unchecked")var original=(Set<Integer>)field.get(terrain);
        var removed=new CountDownLatch(1);var resume=new CountDownLatch(1);var once=new AtomicBoolean();
        // Test-only reflection inserts a gate at the real set removal, without changing production code.
        field.set(terrain,new AbstractSet<Integer>(){
            public Iterator<Integer> iterator(){return original.iterator();}
            public int size(){return original.size();}
            public boolean contains(Object value){return original.contains(value);}
            public boolean add(Integer value){return original.add(value);}
            public boolean remove(Object value){boolean result=original.remove(value);if(result&&once.compareAndSet(false,true)){removed.countDown();try{if(!resume.await(5,TimeUnit.SECONDS))throw new AssertionError("eviction gate timed out");}catch(InterruptedException e){throw new RuntimeException(e);}}return result;}
        });
        var jobs=Executors.newFixedThreadPool(2);var readerThread=new AtomicReference<Thread>();
        try{
            var eviction=jobs.submit(()->terrain.evictExcept(Set.of()));
            if(!removed.await(5,TimeUnit.SECONDS))throw new AssertionError("eviction did not reach residency publication");
            var reader=jobs.submit(()->{readerThread.set(Thread.currentThread());return sparse.blockbranch$read(0).getBlockState(8,1,8);});
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
            while(!reader.isDone()&&(readerThread.get()==null||readerThread.get().getState()!=Thread.State.BLOCKED)&&System.nanoTime()<deadline)Thread.yield();
            boolean serialized=!reader.isDone()&&readerThread.get()!=null&&readerThread.get().getState()==Thread.State.BLOCKED;
            resume.countDown();eviction.get(5,TimeUnit.SECONDS);
            var read=reader.get(5,TimeUnit.SECONDS);var after=sparse.blockbranch$read(0).getBlockState(8,1,8);
            if(!read.is(Blocks.GLOWSTONE)||!after.is(Blocks.GLOWSTONE))throw new AssertionError("edited-section publication: reader="+read+" after="+after+" serialized="+serialized);
            if(!serialized)throw new AssertionError("reader was not serialized with native eviction publication");
        }finally{resume.countDown();jobs.shutdownNow();jobs.awaitTermination(5,TimeUnit.SECONDS);field.set(terrain,original);}
    }
}
