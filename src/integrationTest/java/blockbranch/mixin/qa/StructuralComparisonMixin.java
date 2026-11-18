package blockbranch.mixin.qa;
import blockbranch.realistic.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Disposable client terrain comparison: raw drainage channels without later relief. QA runs only. */
@Mixin(RealisticGenerator.class)
abstract class StructuralComparisonMixin {
 @Inject(method="drainageOptions",at=@At("RETURN"),cancellable=true)
 private void structuralComparison(CallbackInfoReturnable<DrainageNetwork.Options> ci){
  if(Boolean.getBoolean("blockbranch.terrainStructuralComparison")&&Boolean.getBoolean("blockbranch.realisticClientFixture"))
   ci.setReturnValue(ci.getReturnValue().withStructuralComparison(true));
 }
}
