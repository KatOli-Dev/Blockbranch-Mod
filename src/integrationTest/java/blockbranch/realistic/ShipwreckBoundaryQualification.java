package blockbranch.realistic;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces.ShipwreckPiece;

/** Actual native piece/markers, with isolated writes and the immutable generated base as fallback. */
final class ShipwreckBoundaryQualification {
 private record Result(Map<BlockPos,BlockState> states,Map<BlockPos,net.minecraft.nbt.CompoundTag> chests){}
 static void check(net.minecraft.server.level.ServerLevel level,RealisticGenerator generator,net.minecraft.nbt.CompoundTag pieceTag){
  var forward=run(level,generator,pieceTag,false);var reverse=run(level,generator,pieceTag,true);
  WellInteraction.require(forward.states.equals(reverse.states)&&forward.chests.equals(reverse.chests),"Native template blocks and loot seeds independent of opposite chunk order and piece reload");
  WellInteraction.require(forward.chests.size()==3,"All three native marker chests survive boundary replay");
  if(pieceTag.getStringOr("Template","").contains("coastal_sloop_v2_"))connected(forward.states);
 }
 private static void connected(Map<BlockPos,BlockState> states){
  var solids=new HashSet<BlockPos>();states.forEach((p,s)->{if(!s.isAir()&&!s.is(net.minecraft.world.level.block.Blocks.STRUCTURE_BLOCK))solids.add(p);});
  int expected=solids.size();var queue=new ArrayDeque<BlockPos>();var first=solids.stream().min(Comparator.comparingInt(BlockPos::getY)).orElseThrow();solids.remove(first);queue.add(first);int reached=0;
  while(!queue.isEmpty()){var p=queue.remove();reached++;for(var d:net.minecraft.core.Direction.values()){var next=p.relative(d);if(solids.remove(next))queue.add(next);}}
  WellInteraction.require(reached==expected,"All native clipped ship blocks form one face-connected hull, including rails/stairs; detached="+solids);
 }
 private static Result run(net.minecraft.server.level.ServerLevel level,RealisticGenerator generator,net.minecraft.nbt.CompoundTag tag,boolean reverse){
  var states=new HashMap<BlockPos,BlockState>();var chests=new HashMap<BlockPos,ChestBlockEntity>();
  java.util.function.Function<BlockPos,BlockState> read=pos->{var value=states.get(pos);if(value!=null)return value;var base=((blockbranch.cubic.SparseColumn)level.getChunk(pos.getX()>>4,pos.getZ()>>4)).blockbranch$terrain().base();return base.state(pos.getX()&15,pos.getY(),pos.getZ()&15);};
  var proxy=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(ShipwreckBoundaryQualification.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(object,method,args)->{
   switch(method.getName()){
    case "getBlockState":return read.apply((BlockPos)args[0]);
    case "getFluidState":return read.apply((BlockPos)args[0]).getFluidState();
    case "setBlock":{var p=((BlockPos)args[0]).immutable();var state=(BlockState)args[1];states.put(p,state);if(!state.is(net.minecraft.world.level.block.Blocks.CHEST))chests.remove(p);return true;}
    case "getBlockEntity":{var p=((BlockPos)args[0]).immutable();if(!read.apply(p).is(net.minecraft.world.level.block.Blocks.CHEST))return null;return chests.computeIfAbsent(p,key->new ChestBlockEntity(key,read.apply(key)));}
    case "blockUpdated":case "scheduleTick":case "levelEvent":return null;
    default:return method.invoke(level,args);
   }
  });
  var piece=new ShipwreckPiece(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level).structureTemplateManager(),tag);var box=piece.getBoundingBox();var chunks=new ArrayList<net.minecraft.world.level.ChunkPos>();for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)chunks.add(new net.minecraft.world.level.ChunkPos(x,z));if(reverse)Collections.reverse(chunks);
  var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
  for(var chunk:chunks){
   // Reload the native piece between clipped invocations, as at a generation/save boundary.
   if(reverse)piece=new ShipwreckPiece(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level).structureTemplateManager(),piece.createTag(context));
   piece.postProcess(proxy,level.structureManager(),generator,net.minecraft.util.RandomSource.create(OakBirchWoodland.mix(level.getSeed()^chunk.pack())),new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getMinBlockX(),TerrainShape.FLOOR,chunk.getMinBlockZ(),chunk.getMaxBlockX(),TerrainShape.CEILING-1,chunk.getMaxBlockZ()),chunk,BlockPos.ZERO);
  }
  var saved=new HashMap<BlockPos,net.minecraft.nbt.CompoundTag>();chests.forEach((pos,chest)->saved.put(pos,chest.saveWithoutMetadata(level.registryAccess())));return new Result(Map.copyOf(states),Map.copyOf(saved));
 }
}
