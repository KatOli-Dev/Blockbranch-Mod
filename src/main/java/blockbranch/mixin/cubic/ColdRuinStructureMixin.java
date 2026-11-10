package blockbranch.mixin.cubic;
import blockbranch.realistic.*;import java.util.Optional;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.Rotation;import net.minecraft.world.level.levelgen.structure.Structure;import net.minecraft.world.level.levelgen.structure.structures.*;import org.spongepowered.asm.mixin.Mixin;
@Mixin(OceanRuinStructure.class)
abstract class ColdRuinStructureMixin {
 @WrapMethod(method="findGenerationPoint") private Optional<Structure.GenerationStub> ruin(Structure.GenerationContext c,Operation<Optional<Structure.GenerationStub>> original){
  var nativeStructure=(OceanRuinStructure)(Object)this;
  if(!(c.chunkGenerator() instanceof RealisticGenerator g)||g.coldRuinDefinition()==0||nativeStructure.biomeTemp!=OceanRuinStructure.Type.COLD)return original.call(c);
  var planner=g.coldRuins(c.randomState());var selected=planner.forStart(c.chunkPos().x(),c.chunkPos().z());DeepColdOceanRuins deep=null;
  if(selected.isEmpty()&&g.deepColdRuinDefinition()>0){deep=g.deepColdRuins(c.randomState());selected=deep.forStart(c.chunkPos().x(),c.chunkPos().z()).map(p->new ColdOceanRuins.Plan(p.x(),p.y(),p.z(),p.variant(),p.region()));}
  if(selected.isEmpty())return Optional.empty();var plan=selected.get();
  var builder=new net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder();var rotation=Rotation.getRandom(c.random());
  OceanRuinPieces.addPieces(c.structureTemplateManager(),new BlockPos(c.chunkPos().getMinBlockX(),90,c.chunkPos().getMinBlockZ()),rotation,builder,c.random(),nativeStructure);
  var pieces=builder.build().pieces();if(pieces.size()%3!=0)throw new IllegalStateException("Incomplete native cold ruin triple");
  for(int i=0;i<pieces.size();i+=3){var box=pieces.get(i).getBoundingBox();for(int j=1;j<3;j++){var other=pieces.get(i+j).getBoundingBox();if(box.minX()!=other.minX()||box.maxX()!=other.maxX()||box.minZ()!=other.minZ()||box.maxZ()!=other.maxZ())throw new IllegalStateException("Native cold triple footprints differ");}int floor=deep==null?planner.floorFor(box):deep.floorFor(box);if(floor==Integer.MIN_VALUE)return Optional.empty();for(int j=0;j<3;j++){var access=(ColdRuinAccess)pieces.get(i+j);if(g.nativeOrdinaryDefinition()>0)((WarmRuinAccess)pieces.get(i+j)).blockbranch$fitNativeRuin(NativeOrdinaryStructures.ruinFloor(g,c.randomState(),box));else if(deep==null)access.blockbranch$fitColdRuin(floor,j);else access.blockbranch$fitDeepColdRuin(floor,j);}}
  return Optional.of(new Structure.GenerationStub(plan.center(),out->pieces.forEach(out::addPiece)));
 }
}
