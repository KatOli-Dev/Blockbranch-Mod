package blockbranch.mixin.cubic;
import blockbranch.realistic.*;import java.util.*;
import net.minecraft.core.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.resources.Identifier;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.levelgen.Heightmap;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.world.level.levelgen.structure.pieces.*;import net.minecraft.world.level.levelgen.structure.structures.OceanRuinPieces;import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.*;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(OceanRuinPieces.OceanRuinPiece.class)
abstract class ColdRuinPieceMixin extends TemplateStructurePiece implements ColdRuinAccess {
 @Unique private int blockbranch$cold=-1;
 @Unique private boolean blockbranch$deepCold;
 public boolean blockbranch$isDeepCold(){return blockbranch$deepCold;}
 public void blockbranch$fitDeepColdRuin(int y,int stage){blockbranch$deepCold=true;blockbranch$fitColdRuin(y,stage);}
 @Unique private int blockbranch$relief(){return blockbranch$deepCold?12:4;}
 protected ColdRuinPieceMixin(StructurePieceType type,CompoundTag tag,StructureTemplateManager manager,java.util.function.Function<Identifier,StructurePlaceSettings> settings){super(type,tag,manager,settings);}
 public int blockbranch$coldStage(){return blockbranch$cold;}
 public Vec3i blockbranch$coldSize(){return template.getSize();}
 public BlockPos blockbranch$coldLocal(int x,int y,int z){return templatePosition.offset(StructureTemplate.calculateRelativePosition(placeSettings,new BlockPos(x,y,z)));}
 public void blockbranch$fitColdRuin(int y,int stage){blockbranch$cold=stage;move(0,y-templatePosition.getY(),0);}
 @Inject(method="<init>(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/block/Rotation;FLnet/minecraft/world/level/levelgen/structure/structures/OceanRuinStructure$Type;Z)V",at=@At("TAIL")) private void read(StructureTemplateManager manager,CompoundTag tag,Rotation rotation,float integrity,net.minecraft.world.level.levelgen.structure.structures.OceanRuinStructure.Type type,boolean large,CallbackInfo ci){if(tag.contains("blockbranch:cold_ruin_v1")){blockbranch$cold=tag.getIntOr("blockbranch:cold_ruin_v1",-1);if(blockbranch$cold<0||blockbranch$cold>2)throw new IllegalArgumentException("Invalid cold ruin definition1 layer");}if(tag.contains("blockbranch:deep_cold_ruin_v1")){if(tag.getIntOr("blockbranch:deep_cold_ruin_v1",0)!=1||blockbranch$cold<0)throw new IllegalArgumentException("Invalid deep cold ruin definition1 grounding");blockbranch$deepCold=true;}}
 @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void save(StructurePieceSerializationContext c,CompoundTag tag,CallbackInfo ci){if(blockbranch$cold>=0)tag.putInt("blockbranch:cold_ruin_v1",blockbranch$cold);if(blockbranch$deepCold)tag.putInt("blockbranch:deep_cold_ruin_v1",1);}
 @WrapOperation(method="postProcess",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/WorldGenLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I")) private int fittedHeight(WorldGenLevel world,Heightmap.Types kind,int x,int z,Operation<Integer> original){return blockbranch$cold>=0?templatePosition.getY():original.call(world,kind,x,z);}
 @WrapMethod(method="getHeight") private int fittedGround(BlockPos a,BlockGetter world,BlockPos b,Operation<Integer> original){return blockbranch$cold>=0?templatePosition.getY():original.call(a,world,b);}
 @WrapMethod(method="postProcess") private void wharf(WorldGenLevel world,StructureManager structures,net.minecraft.world.level.chunk.ChunkGenerator generator,net.minecraft.util.RandomSource random,BoundingBox bounds,ChunkPos chunk,BlockPos pivot,Operation<Void> original){
  // Every native brick/cracked/mossy pass keeps its processors, integrity and markers.
  original.call(world,structures,generator,random,bounds,chunk,pivot);
  if(blockbranch$cold!=2)return;
  var protectedCells=new HashSet<BlockPos>();
  var manager=world.getLevel().getServer().getStructureTemplateManager();
  for(String layer:new String[]{"brick_","cracked_","mossy_"}){
   var model=manager.getOrCreate(Identifier.parse(templateName.replace("mossy_",layer)));
   for(var marker:model.filterBlocks(templatePosition,placeSettings,Blocks.STRUCTURE_BLOCK)){
    if(marker.nbt()==null)continue;String kind=marker.nbt().getStringOr("metadata","");
    if(kind.equals("chest")||kind.equals("drowned"))for(int dy=0;dy<3;dy++)protectedCells.add(marker.pos().above(dy));
   }
  }
  int width=template.getSize().getX(),length=template.getSize().getZ(),height=template.getSize().getY();
  // Add a broken, buttressed quay enclosure only in water/air. Original weathered
  // blocks, gravel, archaeology, lamps and magma remain byte-for-byte native states.
  for(int z=0;z<length;z++)for(int x=0;x<width;x++){
   boolean corner=(x==0||x==width-1)&&(z==0||z==length-1);
   boolean pier=(x==0||x==width-1)&&(z==length/2);
   int rise=corner?5:pier?4:(x==0||x==width-1)?1+(z%3==0?1:0):z==length-1?1+(x%4==0?1:0):-1;
   for(int y=0;y<=rise&&y<height;y++){
    var at=blockbranch$coldLocal(x,y,z);if(!bounds.isInside(at)||protectedCells.contains(at))continue;
    var old=world.getBlockState(at);if(!old.isAir()&&!old.is(Blocks.WATER))continue;
    BlockState state=(y==rise&&(corner||pier)?Blocks.CHISELED_STONE_BRICKS:y==0?Blocks.COBBLESTONE:((x+z+y)%5==0?Blocks.MOSSY_STONE_BRICKS:Blocks.STONE_BRICKS)).defaultBlockState();
    world.setBlock(at,state,2);
   }
  }
  // Foundation under every retained floor or gravity-sensitive excavation column.
  // Fitted footprint relief<=4 plus native gravel height<=6 fits this bounded depth.
  for(int z=0;z<length;z++)for(int x=0;x<width;x++)for(int y=0;y<height;y++){
   var at=blockbranch$coldLocal(x,y,z);if(!bounds.isInside(at))continue;var state=world.getBlockState(at);
   if(state.isAir()||state.is(Blocks.WATER)||state.canBeReplaced()&&!state.is(Blocks.GRAVEL))continue;
   if(y>0&&!state.is(Blocks.GRAVEL)&&!state.is(Blocks.SUSPICIOUS_GRAVEL)&&!state.is(Blocks.CHEST))continue;
   for(int down=1;down<=(blockbranch$deepCold?24:12);down++){var below=at.below(down);if(!bounds.isInside(below))break;var under=world.getBlockState(below);if(!under.isAir()&&!under.is(Blocks.WATER)&&!under.canBeReplaced())break;world.setBlock(below,Blocks.COBBLESTONE.defaultBlockState(),2);}
  }
  blockbranch$groundRemnants(world,bounds,protectedCells,width,length,height);
 }
 @Unique private static boolean blockbranch$solid(WorldGenLevel world,BlockPos at){return !world.getBlockState(at).getCollisionShape(world,at).isEmpty();}
 @Unique private void blockbranch$groundRemnants(WorldGenLevel world,BoundingBox bounds,Set<BlockPos> protectedCells,int width,int length,int height){
  // Work only within this piece/chunk intersection: no neighbor admission, ordering
  // dependency, or read of a future chunk. One support per isolated component.
  var remaining=new HashSet<BlockPos>();
  for(int z=0;z<length;z++)for(int x=0;x<width;x++)for(int y=0;y<height;y++){var at=blockbranch$coldLocal(x,y,z);if(bounds.isInside(at)&&blockbranch$solid(world,at))remaining.add(at);}
  var islands=new ArrayList<List<BlockPos>>();var rooted=new HashSet<BlockPos>();
  while(!remaining.isEmpty()){
   var queue=new ArrayDeque<BlockPos>();var component=new ArrayList<BlockPos>();var first=remaining.iterator().next();remaining.remove(first);queue.add(first);boolean grounded=false;
   while(!queue.isEmpty()){var at=queue.removeFirst();component.add(at);grounded|=at.getY()==templatePosition.getY()&&blockbranch$solid(world,at.below());for(var direction:Direction.values()){var next=at.relative(direction);if(remaining.remove(next))queue.add(next);}}
   if(grounded)rooted.addAll(component);else{component.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));islands.add(component);}
  }
  islands.sort(Comparator.<List<BlockPos>>comparingInt(v->v.getFirst().getY()).thenComparingInt(v->v.getFirst().getX()).thenComparingInt(v->v.getFirst().getZ()));
  for(var component:islands){boolean supported=false;
   for(var at:component){boolean clear=true;for(int y=at.getY()-1;y>=templatePosition.getY()-blockbranch$relief();y--){var below=new BlockPos(at.getX(),y,at.getZ());if(blockbranch$solid(world,below)){if(y<templatePosition.getY()||rooted.contains(below))break;continue;}if(!bounds.isInside(below)||protectedCells.contains(below)){clear=false;break;}}
    if(!clear)continue;for(int y=at.getY()-1;y>=templatePosition.getY()-blockbranch$relief();y--){var below=new BlockPos(at.getX(),y,at.getZ());if(blockbranch$solid(world,below)){if(y<templatePosition.getY()||rooted.contains(below))break;continue;}world.setBlock(below,Blocks.COBBLESTONE.defaultBlockState(),2);}rooted.addAll(component);supported=true;break;
   }
   // A support may stop only at terrain or an already rooted component;
   // encountering another island cannot create a circular support dependency.
   // Try a short corbel wholly inside the same piece/chunk intersection.
   if(!supported)outer:for(var at:component)for(var direction:new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST})for(int distance=1;distance<=2;distance++){
    var end=at.relative(direction,distance);boolean clear=getBoundingBox().isInside(end)&&bounds.isInside(end);
    for(int step=1;clear&&step<=distance;step++){var bridge=at.relative(direction,step);clear=!protectedCells.contains(bridge)&&!blockbranch$solid(world,bridge);}
    for(int y=end.getY()-1;clear&&y>=templatePosition.getY()-blockbranch$relief();y--){var below=new BlockPos(end.getX(),y,end.getZ());if(blockbranch$solid(world,below)){if(y<templatePosition.getY()||rooted.contains(below))break;continue;}if(!bounds.isInside(below)||protectedCells.contains(below))clear=false;}
    if(!clear)continue;
    for(int step=1;step<=distance;step++)world.setBlock(at.relative(direction,step),Blocks.COBBLESTONE.defaultBlockState(),2);
    for(int y=end.getY()-1;y>=templatePosition.getY()-blockbranch$relief();y--){var below=new BlockPos(end.getX(),y,end.getZ());if(blockbranch$solid(world,below)){if(y<templatePosition.getY()||rooted.contains(below))break;continue;}world.setBlock(below,Blocks.COBBLESTONE.defaultBlockState(),2);}rooted.addAll(component);supported=true;break outer;
   }
   // Native integrity can leave decoration directly over a protected marker in a
   // one-column clipped edge. Let that isolated decoration be absent, instead of
   // obstructing the marker or throwing from live generation. Native containers
   // and archaeology were grounded separately and are never discarded.
   if(!supported)for(var at:component)if(world.getBlockEntity(at)==null&&!world.getBlockState(at).is(Blocks.SUSPICIOUS_GRAVEL)&&!world.getBlockState(at).is(Blocks.GRAVEL))world.setBlock(at,(at.getY()<TerrainShape.SEA_LEVEL?Blocks.WATER:Blocks.AIR).defaultBlockState(),2);

  }
 }
}
