package blockbranch.cubic;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;

public final class ColumnQueries {
    public static NavigableMap<Integer,LevelChunkSection> survey(ChunkAccess chunk) {
        var sparse=(SparseColumn)chunk; var result=sparse.blockbranch$sections(); var terrain=sparse.blockbranch$terrain();
        if(terrain!=null) {
            for(int y:terrain.base().boundaries()) result.putIfAbsent(y,terrain.survey(y));
            for(int y:terrain.modifiedView()) result.putIfAbsent(y,terrain.survey(y));
        }
        return result;
    }
    public static int top(ChunkAccess chunk,Predicate<BlockState> predicate,int x,int z,int ceiling) {
        var sparse=(SparseColumn)chunk; var terrain=sparse.blockbranch$terrain();
        if(terrain==null) throw new IllegalArgumentException("Procedural terrain required");
        int limit=ceiling;
        for(int sy:terrain.modifiedView().descendingSet()) {
            int bottom=sy<<4; if(bottom>limit) continue;
            int generated=terrain.base().top(predicate,x,z,limit);
            if(bottom+15<generated-1) return generated;
            var section=sparse.blockbranch$read(sy);
            for(int y=Math.min(limit,bottom+15);y>=bottom;y--) if(predicate.test(section.getBlockState(x,y&15,z))) return y+1;
            limit=Math.min(limit,bottom-1);
        }
        return terrain.base().top(predicate,x,z,limit);
    }
    private ColumnQueries() {}
}
