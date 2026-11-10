package blockbranch.mixin.cubic;
import java.util.Optional;import blockbranch.realistic.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.levelgen.structure.Structure;import net.minecraft.world.level.levelgen.structure.structures.*;import org.spongepowered.asm.mixin.Mixin;
@Mixin(SwampHutStructure.class)
abstract class WetlandHutStructureMixin {
 @WrapMethod(method="findGenerationPoint")
 private Optional<Structure.GenerationStub> blockbranch$hut(Structure.GenerationContext c,Operation<Optional<Structure.GenerationStub>> original){
  if(!(c.chunkGenerator() instanceof RealisticGenerator g)||g.wetlandHutDefinition()==0)return original.call(c);
  return g.wetlandHuts(c.randomState()).forStart(c.chunkPos().x(),c.chunkPos().z()).map(p->new Structure.GenerationStub(p.center(),builder->{var piece=new SwampHutPiece(c.random(),p.x()-p.halfX(),p.z()-p.halfZ());if(g.nativeHutIglooDefinition()>0)((WetlandHutAccess)piece).blockbranch$fitNative(p);else ((WetlandHutAccess)piece).blockbranch$fit(p);builder.addPiece(piece);}));
 }
}
