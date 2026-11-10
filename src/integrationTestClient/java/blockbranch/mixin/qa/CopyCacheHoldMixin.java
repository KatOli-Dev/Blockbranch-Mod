package blockbranch.mixin.qa;
import blockbranch.qa.SnapshotHoldProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="blockbranch.client.cubic.ProceduralSectionCopies$Cache",remap=false)
abstract class CopyCacheHoldMixin {
 @Inject(method="copyBase",at=@At("HEAD")) private void begin(CallbackInfoReturnable<?> ci){SnapshotHoldProbe.begin(2,true);}
 @Inject(method="copyBase",at=@At("RETURN")) private void end(CallbackInfoReturnable<?> ci){SnapshotHoldProbe.end(2);}
}
