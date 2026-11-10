package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ProtoChunk.class)
abstract class ProtoChunkMixin {
    @Inject(method="markPosForPostProcessing",at=@At("HEAD"),cancellable=true)
    private void blockbranch$sparsePost(net.minecraft.core.BlockPos pos,CallbackInfo ci){
        if(((SparseColumn)this).blockbranch$expanded()&&ColumnLimits.containsY(pos.getY())&&ColumnLimits.TERRAIN.isOutsideBuildHeight(pos)){
            ((SparsePostProcessingAccess)this).blockbranch$postProcessing(true).add(SparsePostProcessing.pack(pos));((ProtoChunk)(Object)this).markUnsaved();ci.cancel();
        }
    }
    @Inject(method="setBlockState",at=@At("HEAD"),cancellable=true)
    private void blockbranch$terrainOnly(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,int flags,CallbackInfoReturnable<net.minecraft.world.level.block.state.BlockState> cir) {
        // Vanilla generators retain their normal write window. Realistic FEATURES
        // writes into a persisted procedural column at its actual elevation.
        if(((SparseColumn)this).blockbranch$expanded() && ((SparseColumn)this).blockbranch$terrain()==null && ColumnLimits.TERRAIN.isOutsideBuildHeight(pos)) cir.setReturnValue(net.minecraft.world.level.block.Blocks.VOID_AIR.defaultBlockState());
    }
    @Inject(method="getHeightAccessorForGeneration",at=@At("HEAD"),cancellable=true)
    private void blockbranch$generation(CallbackInfoReturnable<LevelHeightAccessor> cir) {
        if(((SparseColumn)this).blockbranch$expanded()) cir.setReturnValue(ColumnLimits.TERRAIN);
    }
    @Redirect(method="setBlockState",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ProtoChunk;getSection(I)Lnet/minecraft/world/level/chunk/LevelChunkSection;"))
    private LevelChunkSection blockbranch$write(ProtoChunk chunk,int index) { var sparse=(SparseColumn)chunk; return sparse.blockbranch$expanded() ? sparse.blockbranch$write(chunk.getSectionYFromSectionIndex(index)) : chunk.getSection(index); }
}
