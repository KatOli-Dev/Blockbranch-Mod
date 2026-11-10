package blockbranch.horizon;
import blockbranch.cubic.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
/**
 * Horizon reads of a chunk in memory never change it: an edited section that is not resident
 * is read from the store without entering the chunk's working set, a resident one is read in
 * place, and reads racing edits and evictions on another thread see only states that were
 * written. Run from {@link ColumnLifecycleSmoke} on a synthetic column.
 */
public final class HorizonLiveReadCheck {
    public static String run(ServerLevel level,TerrainProfile base) throws Exception {
        int sy=5,y=sy*16+1;
        var chunk=new ProtoChunk(new ChunkPos(41,-3),UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
        var sparse=(SparseColumn)chunk;var store=SectionStore.of(level);
        sparse.blockbranch$terrain(new ColumnTerrain(chunk,base,level.palettedContainerFactory(),store,new int[0]));var terrain=sparse.blockbranch$terrain();
        sparse.blockbranch$write(sy).setBlockState(1,1,1,Blocks.DIAMOND_BLOCK.defaultBlockState());terrain.flush();terrain.evictExcept(Set.of());
        require(!terrain.resident(sy)&&terrain.hasEdits(sy),"edited section saved and evicted");
        var source=HorizonSources.loaded(chunk,store);
        require(source.state(1,y,1).is(Blocks.DIAMOND_BLOCK),"evicted edit read from the store");
        require(source.top(s->s.is(Blocks.DIAMOND_BLOCK),1,1,ColumnLimits.MAX_Y-1)==y+1,"top search finds the evicted edit");
        require(!terrain.resident(sy),"horizon read did not load the section into the chunk");
        var live=sparse.blockbranch$write(sy);live.setBlockState(1,1,1,Blocks.GOLD_BLOCK.defaultBlockState());
        require(HorizonSources.loaded(chunk,store).state(1,y,1).is(Blocks.GOLD_BLOCK)&&sparse.blockbranch$read(sy)==live,"resident edit read in place");
        // Edits, saves and evictions on this thread while a worker reads the same block.
        var written=Set.of(Blocks.GOLD_BLOCK.defaultBlockState(),Blocks.EMERALD_BLOCK.defaultBlockState(),Blocks.IRON_BLOCK.defaultBlockState());
        var stop=new AtomicBoolean();var reads=new AtomicLong();var wrong=new AtomicReference<String>();
        var reader=Executors.newSingleThreadExecutor();
        var done=reader.submit(()->{
            while(!stop.get()) {
                try {
                    BlockState s=HorizonSources.loaded(chunk,store).state(1,y,1);
                    if(!written.contains(s)){wrong.compareAndSet(null,"read "+s);return;}
                    reads.incrementAndGet();
                } catch(RuntimeException failure){wrong.compareAndSet(null,failure.toString());return;}
            }
        });
        BlockState[] cycle={Blocks.EMERALD_BLOCK.defaultBlockState(),Blocks.IRON_BLOCK.defaultBlockState(),Blocks.GOLD_BLOCK.defaultBlockState()};
        int residentAfterEviction=0;
        for(int i=0;i<600&&wrong.get()==null;i++) {
            sparse.blockbranch$write(sy).setBlockState(1,1,1,cycle[i%3]);
            if(i%3==2){terrain.flush();terrain.evictExcept(Set.of());if(terrain.resident(sy))residentAfterEviction++;Thread.sleep(1);}
        }
        stop.set(true);done.get(30,TimeUnit.SECONDS);reader.shutdownNow();
        require(wrong.get()==null,"concurrent read saw only written states: "+wrong.get());
        require(residentAfterEviction==0,"concurrent reads never re-entered evicted sections: "+residentAfterEviction);
        require(reads.get()>0,"reader ran");
        return "horizonLiveReads="+reads.get();
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private HorizonLiveReadCheck(){}
}
