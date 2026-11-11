package blockbranch.mixin.qa;
import blockbranch.client.qa.IntegratedWalkingAudit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(net.minecraft.client.Minecraft.class)
abstract class IntegratedFrameMixin {
 @Inject(method="runTick(Z)V",at=@At("HEAD")) private void begin(boolean render,CallbackInfo ci){IntegratedWalkingAudit.frameBegin(render);blockbranch.realistic.PolarVisualReview.frameBegin(render);}
 @Inject(method="runTick(Z)V",at=@At("RETURN")) private void end(boolean render,CallbackInfo ci){IntegratedWalkingAudit.frameEnd();blockbranch.realistic.PolarVisualReview.frameEnd();if(render)blockbranch.client.qa.InitialLoadingAudit.frameEnd();}
}
