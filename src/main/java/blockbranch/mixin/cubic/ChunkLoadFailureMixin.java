package blockbranch.mixin.cubic;

import blockbranch.cubic.ColumnLimits;
import blockbranch.cubic.ColumnProfileException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Never turn failed procedural data into a replacement that can overwrite the source. */
@Mixin(ChunkMap.class)
abstract class ChunkLoadFailureMixin {
    @Shadow @Final private ServerLevel level;
    // Per-world lifetime; failures remain quarantined until the world is closed and repaired.
    @Unique private final Set<Long> blockbranch$rejectedLoads=ConcurrentHashMap.newKeySet();

    @Inject(method="handleChunkLoadFailure",at=@At("HEAD"))
    private void blockbranch$reject(Throwable failure,ChunkPos pos,CallbackInfoReturnable<ChunkAccess> cir) {
        boolean protectedData=ColumnLimits.expanded(level);
        var seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
        for(Throwable cause=failure;cause!=null&&seen.add(cause);cause=cause.getCause())
            protectedData|=cause instanceof ColumnProfileException;
        if(!protectedData)return;
        blockbranch$rejectedLoads.add(pos.pack());
        var report=CrashReport.forThrowable(failure,"Refusing to replace unreadable Blockbranch chunk");
        report.addCategory("Protected chunk").setDetail("Position",pos);
        throw new ReportedException(report);
    }

    @Inject(method="save",at=@At("HEAD"),cancellable=true)
    private void blockbranch$preserve(ChunkAccess chunk,CallbackInfoReturnable<Boolean> cir) {
        // Before tryMarkSaved, POI flush, snapshot/section flush, or any region write.
        if(blockbranch$rejectedLoads.contains(chunk.getPos().pack()))cir.setReturnValue(false);
    }
}
