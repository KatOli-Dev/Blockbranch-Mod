package blockbranch.client.mixin.horizon;
import blockbranch.client.horizon.HorizonClient;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/**
 * While horizon tiles draw, fog behaves as for a native render distance reaching the
 * horizon, so native terrain does not fade out where the tiles continue it.
 */
@Mixin(FogRenderer.class)
public abstract class FogHorizonMixin {
    @ModifyVariable(method="setupFog",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private int blockbranch$horizonFog(int renderDistanceInChunks){int r=HorizonClient.activeRadius();return r>0?Math.max(renderDistanceInChunks,r>>4):renderDistanceInChunks;}
}
