package blockbranch.mixin.cubic;
import blockbranch.realistic.*;import java.util.Optional;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.Rotation;import net.minecraft.world.level.levelgen.structure.Structure;import net.minecraft.world.level.levelgen.structure.structures.*;import org.spongepowered.asm.mixin.Mixin;
@Mixin(IglooStructure.class)
abstract class SnowShelterStructureMixin {
 @WrapMethod(method="findGenerationPoint") private Optional<Structure.GenerationStub> shelter(Structure.GenerationContext c,Operation<Optional<Structure.GenerationStub>> original){
  if(!(c.chunkGenerator() instanceof RealisticGenerator g)||g.snowShelterDefinition()==0)return original.call(c);
  return g.snowShelters(c.randomState()).forStart(c.chunkPos().x(),c.chunkPos().z()).map(p->new Structure.GenerationStub(p.center(),builder->{
   // Retain the native rotation RNG draw so the unmodified laboratory choice
   // consumes the same stream; the fitted physical turn belongs to definition1.
   Rotation.getRandom(c.random());IglooPieces.addPieces(c.structureTemplateManager(),new BlockPos(p.x()-3,90,p.z()-5),p.turn(),builder,c.random());for(var piece:builder.build().pieces())if(g.nativeHutIglooDefinition()>0)((SnowShelterAccess)piece).blockbranch$fitNative(p);else ((SnowShelterAccess)piece).blockbranch$fit(p);
  }));
 }
}
