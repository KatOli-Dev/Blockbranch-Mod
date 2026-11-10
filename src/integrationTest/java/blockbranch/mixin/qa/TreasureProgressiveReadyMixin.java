package blockbranch.mixin.qa;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.chunk.LevelChunk;
@Mixin(net.minecraft.server.level.ServerChunkCache.class)
abstract class TreasureProgressiveReadyMixin {
 @Inject(method="getChunkNow",at=@At("HEAD"),cancellable=true)
 private void unavailableOnce(int x,int z,CallbackInfoReturnable<LevelChunk> ci){if(blockbranch.realistic.TreasureProgressiveFailureProbe.hide((net.minecraft.server.level.ServerChunkCache)(Object)this,x,z))ci.setReturnValue(null);}
}
