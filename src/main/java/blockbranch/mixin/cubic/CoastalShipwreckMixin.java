package blockbranch.mixin.cubic;

import java.util.Optional;
import blockbranch.Blockbranch;
import blockbranch.realistic.RealisticGenerator;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckStructure;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces.ShipwreckPiece;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShipwreckStructure.class)
abstract class CoastalShipwreckMixin {
 @WrapMethod(method="findGenerationPoint")
 private Optional<Structure.GenerationStub> blockbranch$coastalSloop(Structure.GenerationContext context,Operation<Optional<Structure.GenerationStub>> original){
  if(!(context.chunkGenerator() instanceof RealisticGenerator generator)||generator.shipwreckDefinition()==0||!((ShipwreckStructure)(Object)this).isBeached)return original.call(context);
  var discoveries=generator.shipwrecks(context.randomState());
  return discoveries.forStart(context.chunkPos().x(),context.chunkPos().z()).map(plan->new Structure.GenerationStub(plan.center(),builder->{
   if(generator.nativeOrdinaryDefinition()>0){
    var piece=net.minecraft.world.level.levelgen.structure.structures.ShipwreckPieces.addRandomPiece(context.structureTemplateManager(),plan.origin(),plan.turn(),builder,context.random(),true);
    // Native beached pool, native pivot and native saved HeightAdjusted. Bury the
    // hull on the already-qualified beach; no custom template or added geometry.
    piece.adjustPositionHeight(plan.y());return;
   }
   var piece=new ShipwreckPiece(context.structureTemplateManager(),Blockbranch.id(discoveries.templatePath(plan.variant())),plan.origin(),plan.turn(),true);
   // Fit from immutable base terrain once. Persist native HeightAdjusted and the native template ID.
   piece.adjustPositionHeight(plan.y());builder.addPiece(piece);
  }));
 }
}
