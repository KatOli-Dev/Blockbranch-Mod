package blockbranch.mixin.cubic;
import blockbranch.realistic.*;import java.util.Optional;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.Rotation;import net.minecraft.world.level.levelgen.structure.Structure;import net.minecraft.world.level.levelgen.structure.structures.*;import org.spongepowered.asm.mixin.Mixin;
@Mixin(OceanRuinStructure.class)
abstract class WarmRuinStructureMixin {
 @WrapMethod(method="findGenerationPoint") private Optional<Structure.GenerationStub> ruin(Structure.GenerationContext c,Operation<Optional<Structure.GenerationStub>> original){
  var nativeStructure=(OceanRuinStructure)(Object)this;
  if(!(c.chunkGenerator() instanceof RealisticGenerator g)||g.warmRuinDefinition()==0||nativeStructure.biomeTemp!=OceanRuinStructure.Type.WARM)return original.call(c);
  var planner=g.warmRuins(c.randomState());var selected=planner.forStart(c.chunkPos().x(),c.chunkPos().z());DeepWarmOceanRuins deep=null;if(selected.isEmpty()&&g.deepWarmRuinDefinition()>0){deep=g.deepWarmRuins(c.randomState());selected=deep.forStart(c.chunkPos().x(),c.chunkPos().z()).map(p->new WarmOceanRuins.Plan(p.x(),p.y(),p.z(),p.variant(),p.region()));}if(selected.isEmpty())return Optional.empty();var plan=selected.get();
  var builder=new net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder();var rotation=Rotation.getRandom(c.random());
  OceanRuinPieces.addPieces(c.structureTemplateManager(),new BlockPos(c.chunkPos().getMinBlockX(),90,c.chunkPos().getMinBlockZ()),rotation,builder,c.random(),nativeStructure);
  var pieces=builder.build().pieces();for(var piece:pieces){int floor=deep==null?planner.floorFor(piece.getBoundingBox()):deep.floorFor(piece.getBoundingBox());if(floor==Integer.MIN_VALUE)return Optional.empty();if(g.nativeOrdinaryDefinition()>0)((WarmRuinAccess)piece).blockbranch$fitNativeRuin(NativeOrdinaryStructures.ruinFloor(g,c.randomState(),piece.getBoundingBox()));else if(deep==null)((WarmRuinAccess)piece).blockbranch$fitRuin(floor,plan.variant());else ((WarmRuinAccess)piece).blockbranch$fitDeepWarmRuin(floor,plan.variant());}
  return Optional.of(new Structure.GenerationStub(plan.center(),out->pieces.forEach(out::addPiece)));
 }
}
