package blockbranch.realistic;
import java.util.*;
import blockbranch.cubic.*;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
/** Compare retained canopy connectivity with its immutable pre-construction native profile. */
final class ReimaginedForest {
 static Map<String,Integer> verify(ServerLevel level,BoundingBox halo,BoundingBox body){
  var baseline=new LongOpenHashSet();var actual=new LongOpenHashSet();var baselineRoots=new LongOpenHashSet();var actualRoots=new LongOpenHashSet();
  for(int z=halo.minZ()>>4;z<=halo.maxZ()>>4;z++)for(int x=halo.minX()>>4;x<=halo.maxX()>>4;x++){var base=((SparseColumn)level.getChunk(x,z)).blockbranch$terrain().base();for(int packed:base.canopy().encode()){int key=packed>>>8,y=(key>>>8)+TerrainShape.FLOOR,lx=key&15,lz=(key>>>4)&15;var p=new BlockPos(x*16+lx,y,z*16+lz);if(!halo.isInside(p))continue;var original=base.state(lx,y,lz);if(!original.is(BlockTags.LOGS)&&!original.is(BlockTags.LEAVES))continue;baseline.add(p.asLong());if(boundary(halo,p)||original.is(BlockTags.LOGS)&&rootMaterial(base.state(lx,y-1,lz)))baselineRoots.add(p.asLong());var now=level.getBlockState(p);if(!body.isInside(p)&&(now.is(BlockTags.LOGS)||now.is(BlockTags.LEAVES))){actual.add(p.asLong());if(boundary(halo,p)||now.is(BlockTags.LOGS)&&rootMaterial(level.getBlockState(p.below())))actualRoots.add(p.asLong());}}}
  var originallyRooted=rooted(baseline,baselineRoots);var nowRooted=rooted(actual,actualRoots);int unchangedOrphans=0;for(long p:actual)if(!nowRooted.contains(p)){if(originallyRooted.contains(p))throw new AssertionError("Construction disconnected previously rooted native foliage "+BlockPos.of(p));unchangedOrphans++;}
  return Map.of("baselineCanopyBlocks",baseline.size(),"retainedCanopyBlocks",actual.size(),"rootedRetainedBlocks",nowRooted.size(),"preexistingUnrootedCanopyBlocks",unchangedOrphans);
 }
 private static boolean rootMaterial(net.minecraft.world.level.block.state.BlockState s){return !s.is(BlockTags.LOGS)&&!s.is(BlockTags.LEAVES)&&s.isSolid();}
 private static boolean boundary(BoundingBox b,BlockPos p){return p.getX()==b.minX()||p.getX()==b.maxX()||p.getY()==b.minY()||p.getY()==b.maxY()||p.getZ()==b.minZ()||p.getZ()==b.maxZ();}
 private static LongOpenHashSet rooted(LongOpenHashSet all,LongOpenHashSet roots){var result=new LongOpenHashSet();var queue=new LongArrayFIFOQueue();for(long p:roots)if(all.contains(p)&&result.add(p))queue.enqueue(p);while(!queue.isEmpty()){var p=BlockPos.of(queue.dequeueLong());for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){if(dx==0&&dy==0&&dz==0)continue;long q=p.offset(dx,dy,dz).asLong();if(all.contains(q)&&result.add(q))queue.enqueue(q);}}return result;}
}
