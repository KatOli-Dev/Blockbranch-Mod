package blockbranch.mixin.qa;

import blockbranch.realistic.PreviewNativeFutureChecks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerChunkCache.class)
abstract class PreviewNativeFutureMixin {
    @Inject(method="getChunkFutureMainThread",at=@At("HEAD"),cancellable=true)
    private void controlledFuture(int x,int z,ChunkStatus status,boolean create,
            CallbackInfoReturnable<CompletableFuture<ChunkResult<ChunkAccess>>> ci){
        if(PreviewNativeFutureChecks.active&&Thread.currentThread()==PreviewNativeFutureChecks.owner){
            if(create||status!=ChunkStatus.BIOMES)throw new AssertionError("Preview changed native ticket/status ownership");
            PreviewNativeFutureChecks.requests++;
            ci.setReturnValue(PreviewNativeFutureChecks.future);
        }
    }
}
