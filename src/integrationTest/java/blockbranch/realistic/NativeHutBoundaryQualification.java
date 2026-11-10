package blockbranch.realistic;
import java.util.*;import net.minecraft.core.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.world.level.levelgen.structure.structures.SwampHutPiece;
/** Exact native writes across four rotations and clipped/reloaded chunk orders. */
final class NativeHutBoundaryQualification {
 record Result(Map<BlockPos,BlockState> blocks,Map<String,Integer> entities,CompoundTag saved){}
 static void check(net.minecraft.server.level.ServerLevel level,RealisticGenerator generator,WetlandHuts.Plan witness){
  var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
  for(int turn=0;turn<4;turn++){
   var p=new WetlandHuts.Plan((witness.x()&~15)+15,witness.y(),(witness.z()&~15)+15,turn,0,witness.lake());var piece=new SwampHutPiece(net.minecraft.util.RandomSource.create(1),p.x()-p.halfX(),p.z()-p.halfZ());((WetlandHutAccess)piece).blockbranch$fitNative(p);var tag=piece.createTag(context);var plain=tag.copy();plain.remove("blockbranch:native_hut_v1");
   var forward=run(level,generator,tag,false);var reverse=run(level,generator,tag,true);var original=run(level,generator,plain,false);
   NativeHutIglooQualification.require(forward.blocks.equals(reverse.blocks)&&forward.entities.equals(reverse.entities)&&forward.saved.equals(reverse.saved),"Native clipped order and piece reload rotation"+turn);
   NativeHutIglooQualification.require(forward.blocks.equals(original.blocks)&&forward.entities.equals(original.entities),"Exact original vanilla hut writes and spawns rotation"+turn);
   NativeHutIglooQualification.require(forward.entities.values().stream().mapToInt(Integer::intValue).sum()==2,"Native once-only witch/cat");
   var b=piece.getBoundingBox();for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)blockbranch.cubic.SparsePostProcessing.process(level.getChunk(x,z),level);
  }
 }
 private static Result run(net.minecraft.server.level.ServerLevel level,RealisticGenerator generator,CompoundTag original,boolean reverse){
  var blocks=new HashMap<BlockPos,BlockState>();var entities=new TreeMap<String,Integer>();
  java.util.function.Function<BlockPos,BlockState> read=p->{var changed=blocks.get(p);if(changed!=null)return changed;return ((blockbranch.cubic.SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base().state(p.getX()&15,p.getY(),p.getZ()&15);};
  var proxy=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(NativeHutBoundaryQualification.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(o,m,args)->switch(m.getName()){
   case "getBlockState"->read.apply((BlockPos)args[0]);case "getFluidState"->read.apply((BlockPos)args[0]).getFluidState();
   case "setBlock"->{blocks.put(((BlockPos)args[0]).immutable(),(BlockState)args[1]);yield true;}
   case "addFreshEntityWithPassengers"->{var e=(net.minecraft.world.entity.Entity)args[0];entities.merge(e.getType().toString()+"@"+e.blockPosition().toShortString(),1,Integer::sum);yield null;}
   case "blockUpdated","scheduleTick","levelEvent"->null;
   default->m.invoke(level,args);
  });
  var piece=new SwampHutPiece(original.copy());var box=piece.getBoundingBox();var chunks=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)chunks.add(new net.minecraft.world.level.ChunkPos(x,z));if(reverse)Collections.reverse(chunks);
  var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
  for(var chunk:chunks){piece=new SwampHutPiece(piece.createTag(context));piece.postProcess(proxy,level.structureManager(),generator,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(chunk.pack())),new BoundingBox(chunk.getMinBlockX(),TerrainShape.FLOOR,chunk.getMinBlockZ(),chunk.getMaxBlockX(),TerrainShape.CEILING-1,chunk.getMaxBlockZ()),chunk,BlockPos.ZERO);}
  return new Result(Map.copyOf(blocks),Map.copyOf(entities),piece.createTag(context));
 }
}
