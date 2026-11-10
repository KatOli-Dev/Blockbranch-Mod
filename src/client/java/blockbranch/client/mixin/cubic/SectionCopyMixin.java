package blockbranch.client.mixin.cubic;
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
        if(sparse.blockbranch$expanded() && (sparse.blockbranch$terrain()!=null || index<0 || index>=chunk.getSections().length)) { var value=sparse.blockbranch$read(chunk.getSectionYFromSectionIndex(index)); section=value.hasOnlyAir() ? null : blockbranch.client.cubic.ProceduralSectionCopies.copy(sparse,chunk.getSectionYFromSectionIndex(index)); }
    }
}
