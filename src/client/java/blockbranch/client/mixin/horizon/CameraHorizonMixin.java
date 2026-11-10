package blockbranch.client.mixin.horizon;
import blockbranch.client.horizon.HorizonClient;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Pushes the far plane past the horizon tiles; inert while the horizon draws nothing. */
@Mixin(Camera.class)
public abstract class CameraHorizonMixin {
    @Shadow private float depthFar;
    @Inject(method="update",at=@At(value="FIELD",target="Lnet/minecraft/client/Camera;depthFar:F",opcode=org.objectweb.asm.Opcodes.PUTFIELD,shift=At.Shift.AFTER))
    private void blockbranch$horizonFar(CallbackInfo ci){int r=HorizonClient.activeRadius();if(r>0)depthFar=Math.max(depthFar,r*1.25f);}
}
