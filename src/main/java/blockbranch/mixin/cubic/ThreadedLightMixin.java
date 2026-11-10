package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ThreadedLevelLightEngine.class)
abstract class ThreadedLightMixin extends LevelLightEngine {
    protected ThreadedLightMixin(LightChunkGetter getter,boolean block,boolean sky) { super(getter,block,sky); }
    @Inject(method="lambda$initializeLight$0",at=@At("HEAD"),cancellable=true)
    private void blockbranch$initialize(ChunkAccess chunk,ChunkPos pos,CallbackInfo ci) {
        var sparse=(SparseColumn)chunk; if(!sparse.blockbranch$expanded()) return;
        // This callback already runs on the native lighting executor; do not re-enqueue it.
        if(sparse.blockbranch$terrain()!=null) {
            var terrain=sparse.blockbranch$terrain();
            // Features and structures can occupy a section absent from the immutable
            // terrain boundary index. Native source discovery reads those edits, so
            // initialize their light layers before native propagation begins.
            var sections=new java.util.TreeSet<Integer>(terrain.base().boundaries());
            sections.addAll(terrain.modifiedView());
            for(int y:sections) super.updateSectionStatus(SectionPos.of(pos,y),sparse.blockbranch$read(y).hasOnlyAir());
        } else sparse.blockbranch$sections().forEach((y,s)-> { if(!s.hasOnlyAir()) super.updateSectionStatus(SectionPos.of(pos,y),false); });
        ci.cancel();
    }
    @Inject(method="lambda$updateChunkStatus$1",at=@At("HEAD"),cancellable=true)
    private void blockbranch$unload(ChunkPos pos,CallbackInfo ci) {
        if(!ColumnLimits.expanded(levelHeightAccessor)) return;
        super.retainData(pos,false);
        super.setLightEnabled(pos,false);
        for(int y:((SparseLight)this).blockbranch$updatingLightSections(pos.pack())) {
            var section=SectionPos.of(pos,y);
            super.queueSectionData(LightLayer.BLOCK,section,null);
            super.queueSectionData(LightLayer.SKY,section,null);
            super.updateSectionStatus(section,true);
        }
        ci.cancel();
    }
}
