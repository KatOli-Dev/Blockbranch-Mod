package blockbranch.mixin.cubic;

import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Server-thread request without the public getChunkFuture method's managed wait. */
@Mixin(ServerChunkCache.class)
public interface PreviewChunkFutureAccessor {
    @Invoker("getChunkFutureMainThread")
    CompletableFuture<ChunkResult<ChunkAccess>> blockbranch$previewFuture(int x,int z,ChunkStatus status,boolean create);
}
