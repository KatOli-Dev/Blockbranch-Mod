package blockbranch.client.mixin.cubic;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ViewArea.class)
abstract class ViewAreaMixin {
    @Shadow public abstract int minSectionY();
    @Shadow public abstract int maxSectionY();
    @Inject(method="minY",at=@At("HEAD"),cancellable=true)
    private void blockbranch$min(CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(minSectionY()<<4); }
    @Inject(method="maxY",at=@At("HEAD"),cancellable=true)
    private void blockbranch$max(CallbackInfoReturnable<Integer> cir) { cir.setReturnValue((maxSectionY()<<4)+15); }
}
