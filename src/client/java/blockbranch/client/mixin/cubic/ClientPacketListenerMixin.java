package blockbranch.client.mixin.cubic;
import blockbranch.cubic.*;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method="lambda$queueLightRemoval$0",at=@At("HEAD"),cancellable=true)
    private void blockbranch$removeLight(net.minecraft.world.level.ChunkPos pos,CallbackInfo ci) {
        if(!ColumnLimits.expanded(level)) return;
        var engine=level.getLightEngine();
        var sections=((SparseLight)engine).blockbranch$updatingLightSections(pos.pack());
        engine.setLightEnabled(pos,false);
        for(int y:sections) {
            var section=SectionPos.of(pos,y);
            engine.queueSectionData(net.minecraft.world.level.LightLayer.BLOCK,section,null);
            engine.queueSectionData(net.minecraft.world.level.LightLayer.SKY,section,null);
            engine.updateSectionStatus(section,true);
        }
        ci.cancel();
    }
    @Shadow private ClientLevel level;
    @Inject(method="enableChunkLight",at=@At("HEAD"),cancellable=true)
    private void blockbranch$enable(LevelChunk chunk,int x,int z,CallbackInfo ci) {
        var sparse=(SparseColumn)chunk; if(!sparse.blockbranch$expanded()) return;
        sparse.blockbranch$sections().forEach((y,s)->level.getLightEngine().updateSectionStatus(SectionPos.of(chunk.getPos(),y),s.hasOnlyAir()));
        int center=net.minecraft.client.Minecraft.getInstance().player == null ? 0 : net.minecraft.client.Minecraft.getInstance().player.blockPosition().getY()>>4;
        int radius=net.minecraft.client.Minecraft.getInstance().options.getEffectiveRenderDistance()+1;
        level.setSectionRangeDirty(x-1,Math.max(level.getMinSectionY(),center-radius),z-1,x+1,Math.min(level.getMaxSectionY(),center+radius),z+1);
        ci.cancel();
    }
}
