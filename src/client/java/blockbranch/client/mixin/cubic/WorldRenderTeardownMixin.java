package blockbranch.client.mixin.cubic;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A closed world has no later level extraction to consume the deferred renderer reset. */
@Mixin(Minecraft.class)
abstract class WorldRenderTeardownMixin {
    @Inject(method="disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",at=@At("RETURN"))
    private void blockbranch$releaseClosedWorld(CallbackInfo ci){
        var client=(Minecraft)(Object)this;
        if(client.level==null){
            client.levelRenderer.resetLevelRenderData();
            var state=client.gameRenderer.gameRenderState().levelRenderState;
            state.reset();
            state.playerCompiledSectionCallback=null;
        }
    }
}
