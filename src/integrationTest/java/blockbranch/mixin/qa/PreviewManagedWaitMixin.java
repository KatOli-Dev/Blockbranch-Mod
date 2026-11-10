package blockbranch.mixin.qa;

import blockbranch.realistic.PreviewNativeFutureChecks;
import java.util.function.BooleanSupplier;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockableEventLoop.class)
abstract class PreviewManagedWaitMixin {
    @Inject(method="managedBlock",at=@At("HEAD"))
    private void detectWait(BooleanSupplier ready,CallbackInfo ci){
        if(PreviewNativeFutureChecks.active&&Thread.currentThread()==PreviewNativeFutureChecks.owner&&!ready.getAsBoolean()){
            PreviewNativeFutureChecks.waits++;
            throw new PreviewNativeFutureChecks.WaitAttempt();
        }
    }
}
