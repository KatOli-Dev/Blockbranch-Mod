package blockbranch.realistic;
import java.util.*;import net.minecraft.core.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.world.level.levelgen.structure.structures.SwampHutPiece;
final class WetlandHutBoundaryQualification {
 record Result(Map<BlockPos,BlockState> blocks,Map<String,Integer> entities,CompoundTag saved){}
 static void check(net.minecraft.server.level.ServerLevel level,RealisticGenerator generator,WetlandHuts.Plan witness){
  var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
  for(int turn=0;turn<4;turn++)for(int variant=0;variant<2;variant++){
   var p=new WetlandHuts.Plan((witness.x()&~15)+15,witness.y(),(witness.z()&~15)+15,turn,variant,witness.lake());var nativePiece=new SwampHutPiece(net.minecraft.util.RandomSource.create(1),p.x()-p.halfX(),p.z()-p.halfZ());((WetlandHutAccess)nativePiece).blockbranch$fit(p);var tag=nativePiece.createTag(context);
   var mirror=nativePiece.getMirror()==net.minecraft.world.level.block.Mirror.NONE?net.minecraft.world.level.block.Mirror.LEFT_RIGHT:net.minecraft.world.level.block.Mirror.NONE;
   var settings=new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings().setMirror(mirror).setRotation(nativePiece.getRotation());
   var template=context.structureTemplateManager().getOrCreate(blockbranch.Blockbranch.id("swamp_hut/stillhouse_v1_"+variant));var access=(WetlandHutAccess)nativePiece;
   WellInteraction.require(template.getBoundingBox(settings,access.blockbranch$local(0,0,0)).equals(nativePiece.getBoundingBox()),"Native and template coordinate footprints agree");
   for(int x:new int[]{0,6})for(int z:new int[]{0,8}){var transformed=net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.calculateRelativePosition(settings,new BlockPos(x,0,z)).offset(access.blockbranch$local(0,0,0));WellInteraction.require(transformed.equals(access.blockbranch$local(x,0,z)),"Each authored corner uses the native coordinate basis");}
   var a=run(level,generator,tag,false);var b=run(level,generator,tag,true);
   WellInteraction.require(a.blocks.equals(b.blocks)&&a.entities.equals(b.entities)&&a.saved.equals(b.saved),"Native hut clipping/reload/order, rotation"+turn+" variant"+variant);
   WellInteraction.require(a.entities.values().stream().mapToInt(Integer::intValue).sum()==2&&a.saved.getBooleanOr("Witch",false)&&a.saved.getBooleanOr("Cat",false),"Native once-only residents across chunk boundary");
   var bounds=nativePiece.getBoundingBox();for(var pos:a.blocks.keySet())WellInteraction.require(pos.getX()>=bounds.minX()&&pos.getX()<=bounds.maxX()&&pos.getZ()>=bounds.minZ()&&pos.getZ()<=bounds.maxZ()&&pos.getY()<=bounds.maxY(),"No accidental template spill beyond saved native footprint");
   WellInteraction.require(a.blocks.get(access.blockbranch$local(2,2,6)).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE)&&a.blocks.get(access.blockbranch$local(4,2,6)).is(net.minecraft.world.level.block.Blocks.CAULDRON),"Canonical furnishings align with native resident coordinates");
   WellInteraction.require(a.blocks.get(access.blockbranch$local(3,1,0)).isAir(),"First porch stair has no covering floor block");
   for(int y:new int[]{2,3})WellInteraction.require(a.blocks.get(access.blockbranch$local(3,y,2)).isAir(),"Canonical doorway stays clear after native replacement");
   var solids=new HashSet<BlockPos>();a.blocks.forEach((pos,s)->{if(!s.isAir())solids.add(pos);});var q=new ArrayDeque<BlockPos>();var first=solids.iterator().next();solids.remove(first);q.add(first);while(!q.isEmpty()){var at=q.remove();for(var d:Direction.values()){var next=at.relative(d);if(solids.remove(next))q.add(next);}}
   WellInteraction.require(solids.isEmpty(),"No detached native blocks across rotated boundary: "+solids);
  }
 }
 private static Result run(net.minecraft.server.level.ServerLevel level,RealisticGenerator generator,CompoundTag original,boolean reverse){
  var blocks=new HashMap<BlockPos,BlockState>();var entities=new TreeMap<String,Integer>();
  java.util.function.Function<BlockPos,BlockState> read=p->{var changed=blocks.get(p);if(changed!=null)return changed;return ((blockbranch.cubic.SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base().state(p.getX()&15,p.getY(),p.getZ()&15);};
  var proxy=(net.minecraft.world.level.WorldGenLevel)java.lang.reflect.Proxy.newProxyInstance(WetlandHutBoundaryQualification.class.getClassLoader(),new Class[]{net.minecraft.world.level.WorldGenLevel.class},(o,m,args)->switch(m.getName()){
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
