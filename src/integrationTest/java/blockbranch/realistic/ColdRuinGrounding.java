package blockbranch.realistic;
import java.util.*;import net.minecraft.core.*;
/** Completed native-world oracle: every collidable component reaches seabed. */
final class ColdRuinGrounding {
 static String hash(String value){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new RuntimeException(e);}}
 static String hash(net.minecraft.server.level.ServerLevel level,net.minecraft.world.level.levelgen.structure.BoundingBox box){var out=new StringBuilder();for(int y=box.minY()-6;y<=box.maxY();y++)for(int z=box.minZ();z<=box.maxZ();z++)for(int x=box.minX();x<=box.maxX();x++){var at=new BlockPos(x,y,z);var state=level.getBlockState(at);out.append(state.getCollisionShape(level,at).isEmpty()?"fluid/replaceable":state.toString()).append(';');}return hash(out.toString());}
 static int check(net.minecraft.server.level.ServerLevel level,net.minecraft.world.level.levelgen.structure.BoundingBox box){
  var blocks=new HashSet<BlockPos>();for(int y=box.minY();y<=box.maxY();y++)for(int z=box.minZ();z<=box.maxZ();z++)for(int x=box.minX();x<=box.maxX();x++){var at=new BlockPos(x,y,z);if(!level.getBlockState(at).getCollisionShape(level,at).isEmpty())blocks.add(at);}int count=blocks.size();
  while(!blocks.isEmpty()){var q=new ArrayDeque<BlockPos>();var first=blocks.iterator().next();blocks.remove(first);q.add(first);boolean grounded=false;while(!q.isEmpty()){var at=q.removeFirst();if(at.getY()==box.minY()&&!level.getBlockState(at.below()).getCollisionShape(level,at.below()).isEmpty())grounded=true;for(var direction:Direction.values()){var next=at.relative(direction);if(blocks.remove(next))q.add(next);}}if(!grounded)throw new AssertionError("Unsupported native component near "+first);}
  return count;
 }
}
