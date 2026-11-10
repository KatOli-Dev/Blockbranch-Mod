package blockbranch.mixin.cubic;
import blockbranch.realistic.*;import java.util.*;
import net.minecraft.core.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.resources.Identifier;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.levelgen.Heightmap;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.world.level.levelgen.structure.pieces.*;import net.minecraft.world.level.levelgen.structure.structures.OceanRuinPieces;import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.*;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(OceanRuinPieces.OceanRuinPiece.class)
abstract class WarmRuinPieceMixin extends TemplateStructurePiece implements WarmRuinAccess {
 @Unique private boolean blockbranch$nativeRuin;
 public boolean blockbranch$isNativeRuin(){return blockbranch$nativeRuin;}
 public void blockbranch$fitNativeRuin(int y){blockbranch$nativeRuin=true;move(0,y-templatePosition.getY(),0);}
 @Unique private int blockbranch$ruin=-1;
 @Unique private boolean blockbranch$deepWarm;
 public boolean blockbranch$isDeepWarm(){return blockbranch$deepWarm;}
 public void blockbranch$fitDeepWarmRuin(int y,int variant){blockbranch$deepWarm=true;blockbranch$fitRuin(y,variant);}
 protected WarmRuinPieceMixin(StructurePieceType type,CompoundTag tag,StructureTemplateManager manager,java.util.function.Function<Identifier,StructurePlaceSettings> settings){super(type,tag,manager,settings);}
 public int blockbranch$ruinVariant(){return blockbranch$ruin;}
 public Vec3i blockbranch$ruinSize(){return template.getSize();}
 public BlockPos blockbranch$ruinLocal(int x,int y,int z){return templatePosition.offset(StructureTemplate.calculateRelativePosition(placeSettings,new BlockPos(x,y,z)));}
 public void blockbranch$fitRuin(int y,int variant){blockbranch$ruin=variant;move(0,y-templatePosition.getY(),0);}
 @Inject(method="<init>(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/block/Rotation;FLnet/minecraft/world/level/levelgen/structure/structures/OceanRuinStructure$Type;Z)V",at=@At("TAIL")) private void read(StructureTemplateManager manager,CompoundTag tag,Rotation rotation,float integrity,net.minecraft.world.level.levelgen.structure.structures.OceanRuinStructure.Type type,boolean large,CallbackInfo ci){if(tag.contains("blockbranch:native_ocean_ruin_v1")){if(tag.getIntOr("blockbranch:native_ocean_ruin_v1",0)!=1||tag.contains("blockbranch:warm_ruin_v1")||tag.contains("blockbranch:cold_ruin_v1")||tag.contains("blockbranch:deep_warm_ruin_v1")||tag.contains("blockbranch:deep_cold_ruin_v1"))throw new IllegalArgumentException("Invalid native ruin marker");blockbranch$nativeRuin=true;}if(tag.contains("blockbranch:warm_ruin_v1")){blockbranch$ruin=tag.getIntOr("blockbranch:warm_ruin_v1",-1);if(blockbranch$ruin<0||blockbranch$ruin>1)throw new IllegalArgumentException("Invalid warm ruin definition1 palette");}if(tag.contains("blockbranch:deep_warm_ruin_v1")){if(tag.getIntOr("blockbranch:deep_warm_ruin_v1",0)!=1||blockbranch$ruin<0)throw new IllegalArgumentException("Invalid deep warm ruin setting");blockbranch$deepWarm=true;}}
 @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void save(StructurePieceSerializationContext c,CompoundTag tag,CallbackInfo ci){if(blockbranch$nativeRuin)tag.putInt("blockbranch:native_ocean_ruin_v1",1);if(blockbranch$ruin>=0)tag.putInt("blockbranch:warm_ruin_v1",blockbranch$ruin);if(blockbranch$deepWarm)tag.putInt("blockbranch:deep_warm_ruin_v1",1);}
 @WrapOperation(method="postProcess",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/WorldGenLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I")) private int fittedHeight(WorldGenLevel world,Heightmap.Types kind,int x,int z,Operation<Integer> original){return blockbranch$ruin>=0||blockbranch$nativeRuin?templatePosition.getY():original.call(world,kind,x,z);}
 @WrapMethod(method="getHeight") private int fittedGround(BlockPos a,BlockGetter world,BlockPos b,Operation<Integer> original){return blockbranch$ruin>=0||blockbranch$nativeRuin?templatePosition.getY():original.call(a,world,b);}
 @WrapMethod(method="postProcess") private void body(WorldGenLevel world,StructureManager structures,net.minecraft.world.level.chunk.ChunkGenerator generator,net.minecraft.util.RandomSource random,BoundingBox bounds,ChunkPos chunk,BlockPos pivot,Operation<Void> original){
  // Original template processors and all functional markers run first, unchanged.
  original.call(world,structures,generator,random,bounds,chunk,pivot);if(blockbranch$ruin<0)return;
  var protectedCells=new HashSet<BlockPos>();for(var marker:template.filterBlocks(templatePosition,placeSettings,Blocks.STRUCTURE_BLOCK)){if(marker.nbt()==null)continue;String kind=marker.nbt().getStringOr("metadata","");if(kind.equals("chest")||kind.equals("drowned"))for(int dy=0;dy<3;dy++)protectedCells.add(marker.pos().above(dy));}
  int width=template.getSize().getX(),length=template.getSize().getZ(),height=template.getSize().getY();var stone=(blockbranch$ruin==0?Blocks.CUT_SANDSTONE:Blocks.SMOOTH_SANDSTONE).defaultBlockState();var accent=(blockbranch$ruin==0?Blocks.CHISELED_SANDSTONE:Blocks.CUT_RED_SANDSTONE).defaultBlockState();
  for(int z=0;z<length;z++)for(int x=0;x<width;x++)for(int y=0;y<height;y++){
   var at=blockbranch$ruinLocal(x,y,z);if(!bounds.isInside(at))continue;var old=world.getBlockState(at);if(protectedCells.contains(at)||old.is(Blocks.SAND)||old.is(Blocks.SUSPICIOUS_SAND)||old.is(Blocks.CHEST))continue;
   BlockState next=null;boolean corner=(x==0||x==width-1)&&(z==0||z==length-1);
   if(corner&&y<=4)next=y==3?accent:stone;
   else if(width>=12&&z==0&&(x==width/3||x==width*2/3)&&y<=3)next=y==3?accent:stone;
   else if(z==0&&y==4)next=(x==width/2||x==width/2-1)?accent:stone;
   else if(z==length-1&&y<=1+(x*3+blockbranch$ruin)%3)next=stone;
   else if((x==0||x==width-1)&&z>1&&z<length-2&&y<=1+(z+blockbranch$ruin)%2)next=stone;
   else if(y==0&&(x==0||x==width-1||z==0||z==length-1))next=stone;
   else if(y>0)next=(at.getY()<TerrainShape.SEA_LEVEL?Blocks.WATER:Blocks.AIR).defaultBlockState();
   if(next!=null)world.setBlock(at,next,2);
  }
  // Support every retained excavation column and new masonry column. Original
  // sand/archaeology stays in place; bounded supports prevent physics loss.
  for(int z=0;z<length;z++)for(int x=0;x<width;x++)for(int y=0;y<height;y++){
   var at=blockbranch$ruinLocal(x,y,z);if(!bounds.isInside(at))continue;var state=world.getBlockState(at);if(state.isAir()||state.is(Blocks.WATER)||state.canBeReplaced()&&!state.is(Blocks.SAND) )continue;if(y>0&&!state.is(Blocks.SAND)&&!state.is(Blocks.SUSPICIOUS_SAND)&&!state.is(Blocks.CHEST))continue;
   for(int down=1;down<=(blockbranch$deepWarm?24:10);down++){var below=at.below(down);if(!bounds.isInside(below))break;var under=world.getBlockState(below);if(!under.isAir()&&!under.is(Blocks.WATER)&&!under.canBeReplaced())break;world.setBlock(below,Blocks.SANDSTONE.defaultBlockState(),2);}
  }
 }
}
