package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
/** Procedural sections must never hide actual generated materials from predicate queries. */
public final class SurfacePredicateSmoke {
 public static void verify(ServerLevel level,RealisticGenerator g){var random=level.getChunkSource().randomState();var all=new HashSet<BlockState>();int assertions=0;
  for(var point:new net.minecraft.core.BlockPos[]{BadlandsSmoke.fixture(g,random,1),BadlandsSmoke.fixture(g,random,2),ColdUplandsSmoke.fixture(g,random,1),ColdUplandsSmoke.fixture(g,random,2)}){
   for(int cz=(point.getZ()>>4)-1;cz<=(point.getZ()>>4)+1;cz++)for(int cx=(point.getX()>>4)-1;cx<=(point.getX()>>4)+1;cx++){
    var base=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain().base();var seen=new HashSet<BlockState>();var views=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
    for(int z=0;z<16;z+=4)for(int x=0;x<16;x+=4){int wx=cx*16+x,wz=cz*16+z,h=base.groundHeight(wx,wz);
     for(int y=h-64;y<=h+20;y++){var actual=base.state(wx,y,wz);if(!seen.add(actual))continue;all.add(actual);assertions++;
      if(!views.computeIfAbsent(y>>4,base::view).maybeHas(state->state==actual))throw new AssertionError("Procedural predicate hid generated "+actual+" at "+wx+","+y+","+wz);
     }
    }
   }
  }
  Blockbranch.LOGGER.info("PROCEDURAL_GENERATED_MATERIAL_PREDICATES_PASS columns=36 assertions={} distinctStates={}",assertions,all.size());
 }
 private SurfacePredicateSmoke(){}
}
