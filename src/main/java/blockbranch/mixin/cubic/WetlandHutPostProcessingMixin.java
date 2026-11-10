package blockbranch.mixin.cubic;
import blockbranch.realistic.WetlandHutAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.world.level.levelgen.structure.StructurePiece;import net.minecraft.world.level.chunk.ChunkAccess;import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;
/** The temporary native body is wholly replaced by an explicitly shaped template.
 * Its discarded fences/stairs must not enter vanilla's fixed-height update array. */
@Mixin(StructurePiece.class)
abstract class WetlandHutPostProcessingMixin {
 @WrapOperation(method="placeBlock",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkAccess;markPosForPostProcessing(Lnet/minecraft/core/BlockPos;)V"))
 private void blockbranch$discardedBody(ChunkAccess chunk,BlockPos pos,Operation<Void> original){if((Object)this instanceof WetlandHutAccess hut&&hut.blockbranch$stillhouse())return;original.call(chunk,pos);}
}
