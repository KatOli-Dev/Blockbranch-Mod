package blockbranch.client.mixin.cubic;

import blockbranch.cubic.ColumnLimits;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Incremental discovery must revisit a node when a new incoming visibility path arrives. */
@Mixin(SectionOcclusionGraph.Node.class)
abstract class OcclusionSourceMixin {
    @Shadow @Final protected SectionRenderDispatcher.RenderSection section;
    @Shadow private byte sourceDirections;
    @Unique private int blockbranch$previousSources;
    @Inject(method="addSourceDirection",at=@At("HEAD"))
    private void blockbranch$before(Direction direction,CallbackInfo ci){
        if(RenderSystem.isOnRenderThread())blockbranch$previousSources=sourceDirections&255;
    }
    @Inject(method="addSourceDirection",at=@At("RETURN"))
    private void blockbranch$revisit(Direction direction,CallbackInfo ci){
        if(!RenderSystem.isOnRenderThread()||blockbranch$previousSources==(sourceDirections&255))return;
        var client=Minecraft.getInstance();
        if(client.level!=null&&ColumnLimits.expanded(client.level))client.levelRenderer.sectionOcclusionGraph().schedulePropagationFrom(section);
    }
}
