package blockbranch.mixin.cubic;
import blockbranch.Blockbranch;import blockbranch.realistic.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.nbt.CompoundTag;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.world.level.levelgen.structure.pieces.*;import net.minecraft.world.level.levelgen.structure.structures.SwampHutPiece;import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Native placement/spawn-once state is retained, followed by a complete body reskin. */
@Mixin(SwampHutPiece.class)
abstract class WetlandHutPieceMixin extends ScatteredFeaturePiece implements WetlandHutAccess {
 @Unique private int blockbranch$variant=-1;
 @Unique private boolean blockbranch$nativeBody;
 protected WetlandHutPieceMixin(StructurePieceType type,CompoundTag tag){super(type,tag);}
 public net.minecraft.core.BlockPos blockbranch$local(int x,int y,int z){return getWorldPos(x,y,z).immutable();}
 public boolean blockbranch$stillhouse(){return blockbranch$variant>=0;}
 public void blockbranch$fitNative(WetlandHuts.Plan p){blockbranch$fit(p);blockbranch$variant=-1;blockbranch$nativeBody=true;}
 public void blockbranch$fit(WetlandHuts.Plan p){blockbranch$variant=p.variant();setOrientation(p.facing());heightPosition=p.y();boundingBox=new BoundingBox(p.x()-p.halfX(),p.y(),p.z()-p.halfZ(),p.x()+p.halfX(),p.y()+6,p.z()+p.halfZ());}
 @Inject(method="<init>(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
 private void blockbranch$read(CompoundTag tag,CallbackInfo ci){if(tag.contains("blockbranch:stillhouse_v1")){blockbranch$variant=tag.getIntOr("blockbranch:stillhouse_v1",-1);if(blockbranch$variant<0||blockbranch$variant>1)throw new IllegalArgumentException("Invalid saved stillhouse variant");}if(tag.contains("blockbranch:native_hut_v1")){if(tag.getIntOr("blockbranch:native_hut_v1",0)!=1||blockbranch$variant>=0)throw new IllegalArgumentException("Invalid native hut body marker");blockbranch$nativeBody=true;}}
 @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
 private void blockbranch$save(StructurePieceSerializationContext c,CompoundTag tag,CallbackInfo ci){if(blockbranch$variant>=0)tag.putInt("blockbranch:stillhouse_v1",blockbranch$variant);if(blockbranch$nativeBody)tag.putInt("blockbranch:native_hut_v1",1);}
 @WrapMethod(method="postProcess")
 private void blockbranch$body(net.minecraft.world.level.WorldGenLevel world,net.minecraft.world.level.StructureManager structures,net.minecraft.world.level.chunk.ChunkGenerator generator,net.minecraft.util.RandomSource random,BoundingBox bounds,net.minecraft.world.level.ChunkPos chunk,net.minecraft.core.BlockPos pivot,Operation<Void> original){
  // Native supports, furnishings, Witch/Cat persistence and spawn-once logic run
  // unchanged. Explicit air clears the original above-floor body cells, leaving no
  // old walls/roof fragments. Below-floor soil and native supports remain.
  original.call(world,structures,generator,random,bounds,chunk,pivot);
  if(blockbranch$variant<0)return;
  var manager=StructurePieceSerializationContext.fromLevel(world.getLevel()).structureTemplateManager();
  var template=manager.getOrCreate(Blockbranch.id("swamp_hut/stillhouse_v1_"+blockbranch$variant));
  // Native piece mirror/rotation describe block-state facing, not its local
  // coordinate basis. Templates start with +Z; native pieces start with -Z.
  var bodyMirror=getMirror()==net.minecraft.world.level.block.Mirror.NONE?net.minecraft.world.level.block.Mirror.LEFT_RIGHT:net.minecraft.world.level.block.Mirror.NONE;
  var settings=new StructurePlaceSettings().setMirror(bodyMirror).setRotation(getRotation()).setBoundingBox(bounds).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
  var origin=getWorldPos(0,0,0).immutable();template.placeInWorld(world,origin,origin,settings,random,2);
 }
}
