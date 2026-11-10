package blockbranch.client.mixin.cubic;
import blockbranch.client.cubic.ProceduralSectionCopies;
import blockbranch.cubic.SparseColumn;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(SectionCopy.class)
abstract class SectionCopyMixin {
    @Shadow @Final @Mutable private PalettedContainer<BlockState> section;
    @Inject(method="<init>",at=@At("RETURN"))
    private void blockbranch$copy(LevelChunk chunk,int index,CallbackInfo ci) {
        var sparse=(SparseColumn)chunk;
        if(sparse.blockbranch$expanded() && (sparse.blockbranch$terrain()!=null || index<0 || index>=chunk.getSections().length)) {
            int y=chunk.getSectionYFromSectionIndex(index);
            // An optional render patch resolves an unedited base section on the compile worker.
            if((Object)this instanceof ProceduralSectionCopies.DeferredBase deferred&&ProceduralSectionCopies.isBase(sparse,y)){deferred.blockbranch$deferBase(sparse.blockbranch$terrain().base(),y);section=null;return;}
            var value=sparse.blockbranch$read(y); section=value.hasOnlyAir() ? null : ProceduralSectionCopies.copy(sparse,y);
        }
    }
}
