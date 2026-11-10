package blockbranch.cubic;

import java.util.Arrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.server.level.ServerLevel;

/** Bounded native generation updates outside the fixed vanilla section window.
 * In-flight work remains in snapshots until its native operations all succeed. */
public final class SparsePostProcessing {
    public static final int LIMIT=65536, FORMAT=3;
    public static final String KEY="blockbranch:sparse_post_processing_v1";
    private final IntOpenHashSet pending=new IntOpenHashSet();
    private IntOpenHashSet active;
    public static int pack(BlockPos pos){if(!ColumnLimits.containsY(pos.getY()))throw new IllegalArgumentException("Post-processing Y outside expanded world");return ((pos.getY()-ColumnLimits.MIN_Y)<<8)|((pos.getZ()&15)<<4)|(pos.getX()&15);}
    public static BlockPos unpack(int packed,ChunkPos chunk){return new BlockPos(chunk.getMinBlockX()+(packed&15),ColumnLimits.MIN_Y+(packed>>>8),chunk.getMinBlockZ()+((packed>>>4)&15));}
    public static int[] validate(int[] values){
        if(values.length>LIMIT)throw new IllegalArgumentException("Too many sparse generation updates");
        for(int i=0;i<values.length;i++){int value=values[i],y=ColumnLimits.MIN_Y+(value>>>8);if(value<0||value>=(ColumnLimits.HEIGHT<<8)||y>=-64&&y<320||i>0&&value<=values[i-1])throw new IllegalArgumentException("Invalid sparse generation update");}
        return values.clone();
    }
    public synchronized void add(int packed){
        if(pending.contains(packed))return;
        if(pending.size()+(active==null?0:active.size())>=LIMIT)throw new IllegalStateException("Sparse native generation update budget exceeded");
        pending.add(packed);
    }
    public synchronized int[] snapshot(){var all=new IntOpenHashSet(pending);if(active!=null)all.addAll(active);int[] result=all.toIntArray();Arrays.sort(result);return result;}
    public synchronized void restore(int[] values){if(active!=null)throw new IllegalStateException("Cannot replace active updates");int[] checked=validate(values);pending.clear();for(int value:checked)pending.add(value);}
    public synchronized int[] begin(){if(active!=null||pending.isEmpty())return new int[0];active=new IntOpenHashSet(pending);pending.clear();int[] result=active.toIntArray();Arrays.sort(result);return result;}
    public synchronized void finish(boolean success){if(active==null)return;if(!success)pending.addAll(active);active=null;}
    public static void process(LevelChunk chunk,ServerLevel level){
        var queue=((SparsePostProcessingAccess)chunk).blockbranch$postProcessing(false);if(queue==null)return;
        int[] batch=queue.begin();if(batch.length==0)return;boolean success=false;
        try{
            for(int packed:batch){var at=unpack(packed,chunk.getPos());var state=chunk.getBlockState(at);var fluid=state.getFluidState();
                // Same operations and flags as native26.3 LevelChunk.postProcessGeneration.
                if(!fluid.isEmpty())fluid.tick(level,at,state);
                if(state.getBlock() instanceof LiquidBlock)state.tick(level,at,level.getRandom());
                else{var updated=Block.updateFromNeighbourShapes(state,level,at);if(updated!=state)level.setBlock(at,updated,276);}
            }
            success=true;
        }finally{queue.finish(success);chunk.markUnsaved();}
    }
}
