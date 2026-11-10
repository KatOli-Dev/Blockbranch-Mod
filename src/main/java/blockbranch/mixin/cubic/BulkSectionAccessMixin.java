package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(BulkSectionAccess.class)
abstract class BulkSectionAccessMixin {
    @Shadow @Final private LevelAccessor level;
    @Redirect(method="lambda$getSection$0",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkAccess;getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;"))
    private LevelChunkSection blockbranch$section(ChunkAccess chunk,int index) {
        var sparse=(SparseColumn)chunk;
        return sparse.blockbranch$expanded() ? sparse.blockbranch$write(level.getSectionYFromSectionIndex(index)) : chunk.getSection(index);
    }
}
