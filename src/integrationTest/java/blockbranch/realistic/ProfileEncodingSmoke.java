package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.*;

/** Controlled overlap regression for simultaneous profile save and packet encoding. */
public final class ProfileEncodingSmoke {
    private static final String OVERLAP="Overlapping shared biome pack";
    private static final class GuardPalette extends PalettedContainer<Holder<Biome>> {
        final AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger();
        GuardPalette(Holder<Biome> value,Strategy<Holder<Biome>> strategy){super(value,strategy);}
        @Override public PalettedContainerRO.PackedData<Holder<Biome>> pack(Strategy<Holder<Biome>> strategy){
            int count=active.incrementAndGet();peak.accumulateAndGet(count,Math::max);
            try{
                if(count>1)throw new AssertionError(OVERLAP);
                java.util.concurrent.locks.LockSupport.parkNanos(5_000_000);
                return super.pack(strategy);
            }finally{active.decrementAndGet();}
        }
    }
    @SuppressWarnings("unchecked")
    public static void verify(ServerLevel level)throws Exception{
        var factory=level.palettedContainerFactory();var base=((SparseColumn)level.getChunk(0,0)).blockbranch$terrain().base();
        var strategy=(Strategy<Holder<Biome>>)((PaletteStrategyAccess<?>)base.biomes()).realistic$strategy();
        var registry=level.registryAccess().lookupOrThrow(Registries.BIOME);
        var palette=new GuardPalette(registry.getOrThrow(Biomes.PLAINS),strategy);palette.set(1,0,0,registry.getOrThrow(Biomes.FOREST));
        if(palette.bitsPerEntry()==0)throw new AssertionError("Regression needs a non-uniform palette");
        int negative=parallel(4,()->palette.pack(strategy),true);
        if(negative==0)throw new AssertionError("Negative control did not detect forced overlapping pack");
        palette.peak.set(0);
        int[] water=new int[256];Arrays.fill(water,DrainageNetwork.DRY);
        var profile=new TerrainProfile(-16,64,new int[256],water,palette,factory.blockStatesStrategy());
        var expected=profile.encode(factory);
        parallel(8,()->{
            for(int i=0;i<64;i++){
                var tag=profile.encode(factory);if(!tag.equals(expected))throw new AssertionError("Concurrent snapshot content changed");
                tag.getIntArray("ground").orElseThrow()[0]=123;tag.getIntArray("floating_ice").orElseThrow()[0]=123;
            }
        },false);
        if(palette.peak.get()!=1||!profile.encode(factory).equals(expected))throw new AssertionError("Profile snapshots were not isolated and serialized");
        Blockbranch.LOGGER.info("PROFILE_ENCODING_CONCURRENT_SNAPSHOT_PASS workers=8 encodes=512 negativeOverlaps={} peakPack={}",negative,palette.peak.get());
    }
    private static int parallel(int workers,Runnable action,boolean negative)throws Exception{
        var executor=Executors.newFixedThreadPool(workers,task->{var thread=new Thread(task,"profile-encode-regression");thread.setDaemon(true);return thread;});
        var ready=new CountDownLatch(workers);var start=new CountDownLatch(1);var futures=new ArrayList<Future<?>>();int overlaps=0;
        try{
            for(int i=0;i<workers;i++)futures.add(executor.submit(()->{ready.countDown();try{start.await();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}action.run();}));
            if(!ready.await(10,TimeUnit.SECONDS))throw new AssertionError("Encoding workers did not start");start.countDown();
            for(var future:futures)try{future.get(20,TimeUnit.SECONDS);}catch(ExecutionException e){if(negative&&e.getCause() instanceof AssertionError a&&OVERLAP.equals(a.getMessage()))overlaps++;else throw new AssertionError("Concurrent profile encoding failed",e.getCause());}
        }finally{start.countDown();executor.shutdownNow();}
        return overlaps;
    }
    private ProfileEncodingSmoke(){}
}
