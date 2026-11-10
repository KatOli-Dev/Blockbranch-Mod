package blockbranch.mixin.cubic;
import java.util.Optional;
import blockbranch.realistic.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalStructure;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(RuinedPortalStructure.class)
abstract class DesertWaygateMixin {
 @WrapMethod(method="findGenerationPoint")
 private Optional<Structure.GenerationStub> blockbranch$waygate(Structure.GenerationContext context,Operation<Optional<Structure.GenerationStub>> original){
  if(!(context.chunkGenerator() instanceof RealisticGenerator g)||g.waygateDefinition()==0||context.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.RUINED_PORTAL_DESERT).value()!=(Object)this)return original.call(context);
  if(g.nativeOrdinaryDefinition()>0){var selected=g.waygates(context.randomState()).forStart(context.chunkPos().x(),context.chunkPos().z());if(selected.isEmpty())return Optional.empty();return original.call(NativeOrdinaryStructures.portalContext(context,selected.get().y()));}
  return g.waygates(context.randomState()).forStart(context.chunkPos().x(),context.chunkPos().z()).map(p->new Structure.GenerationStub(p.center(),builder->builder.addPiece(new DesertWaygatePiece(context.structureTemplateManager(),p))));
 }
}
