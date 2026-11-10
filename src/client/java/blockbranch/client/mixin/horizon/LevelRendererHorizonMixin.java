package blockbranch.client.mixin.horizon;
import blockbranch.client.horizon.HorizonClient;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Prepares horizon tiles with the terrain uniforms and draws them after native solid terrain. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererHorizonMixin {
    @Shadow private GpuSampler chunkLayerSampler;
    @Inject(method={"prepareChunkRenders","prepareChunkRendersIndirect"},at=@At("RETURN"))
    private void blockbranch$prepareHorizon(Matrix4fc modelView,boolean respectTranslucentOrder,CallbackInfoReturnable<?> cir){if(HorizonClient.ENABLED)HorizonClient.prepare(modelView);}
    @Inject(method="executeSolid",at=@At("TAIL"))
    private void blockbranch$drawHorizon(@Coerce Object sections,@Coerce Object frame,RenderPass pass,CallbackInfo ci){if(HorizonClient.ENABLED&&chunkLayerSampler!=null)HorizonClient.draw(pass,chunkLayerSampler);}
}
