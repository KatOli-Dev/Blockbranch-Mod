package blockbranch.realistic;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Controls only native future completion; exercises the actual production lookup and native wait boundary. */
public final class PreviewNativeFutureChecks {
    public static boolean active;
    public static Thread owner;
    public static int requests,waits;
    public static CompletableFuture<ChunkResult<ChunkAccess>> future;
    public static final class WaitAttempt extends RuntimeException {}
    static void check(ServerLevel level,ServerPlayer player)throws Exception{
        var chunk=level.getChunkAt(player.blockPosition());
        var point=new PreviewBiomeQueries.Point(player.getBlockX()>>2,level.getSeaLevel()>>2,player.getBlockZ()>>2);
        owner=Thread.currentThread();active=true;requests=waits=0;future=new CompletableFuture<>();
        try{
            // The original production API attempts an actual native managed wait before returning.
            try{level.getChunkSource().getChunkFuture(point.x()>>2,point.z()>>2,ChunkStatus.BIOMES,false);
                throw new AssertionError("Native blocking control did not reach its wait");}
            catch(WaitAttempt expected){}
            require(waits==1&&requests==1,"Original native call boundary attempts a wait");
            var pending=PreviewBiomeQueries.loaded(level,point);
            require(!pending.ready()&&waits==1&&requests==2,"Production poll returns pending without native wait");
            future.complete(ChunkResult.of(chunk));
            var ready=PreviewBiomeQueries.loaded(level,point);
            require(ready.ready()&&ready.value()==chunk.getNoiseBiome(point.x(),point.y(),point.z()),"Completed future preserves authoritative loaded holder");
            future=CompletableFuture.completedFuture(ChunkResult.error("Controlled unloaded coordinate"));
            var absent=PreviewBiomeQueries.loaded(level,point);
            require(absent.ready()&&absent.value()==null,"Unloaded result permits exact fallback");
            future=CompletableFuture.failedFuture(new IllegalStateException("Controlled chunk failure"));
            try{PreviewBiomeQueries.loaded(level,point);throw new AssertionError("Failed native future swallowed");}
            catch(java.util.concurrent.CompletionException expected){}
            require(waits==1,"No production poll entered managed wait");
        }finally{active=false;owner=null;future=null;}
        Files.writeString(DeferredPreviewQualification.dir().resolve("native-future.json"),
                "{\"nativeBlockingControlWaits\":1,\"productionWaits\":0,\"pendingThenLoaded\":true,\"unloadedFallback\":true,\"exceptionPreserved\":true}");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PreviewNativeFutureChecks(){}
}
