package blockbranch.mixin.cubic;

import blockbranch.cubic.SparseColumn;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Bound retained save snapshots/NBT while the region writer catches up. */
@Mixin(ChunkMap.class)
abstract class ChunkSaveMixin {
    @Unique private final ArrayDeque<Pending> blockbranch$writes = new ArrayDeque<>();
    @Unique private int blockbranch$pendingSections;
    @Unique private int blockbranch$currentSections;
    public record Pending(CompletableFuture<?> future, int sections) {}
    // Vanilla's chunk-count limit assumes a few dozen sections per chunk.
    // Budget section count instead, including native sections and light padding.
    @Unique private static final int BLOCKBRANCH_SAVE_SECTIONS = 8192;

    @WrapOperation(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/SerializableChunkData;copyOf(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;)Lnet/minecraft/world/level/chunk/storage/SerializableChunkData;"))
    private SerializableChunkData blockbranch$budget(ServerLevel level, ChunkAccess chunk, Operation<SerializableChunkData> original) {
        var sparse = (SparseColumn)chunk;
        int sections = sparse.blockbranch$expanded() ? sparse.blockbranch$extraSections().size() + chunk.getSections().length + 2 : 0;
        while (!blockbranch$writes.isEmpty()) {
            var first = blockbranch$writes.peek();
            if (!first.future().isDone() && blockbranch$pendingSections + sections <= BLOCKBRANCH_SAVE_SECTIONS) break;
            // Region writes and encoding finish on worker threads. Preserve the
            // original save-failure reporter rather than rethrowing its failure.
            first.future().handle((result, failure) -> null).join();
            blockbranch$pendingSections -= blockbranch$writes.remove().sections();
        }
        blockbranch$currentSections = sections;
        return original.call(level, chunk);
    }

    @WrapOperation(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;write(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> blockbranch$track(ChunkMap map, ChunkPos pos, Supplier<CompoundTag> tag, Operation<CompletableFuture<Void>> original) {
        var future = original.call(map, pos, tag);
        if (blockbranch$currentSections > 0) {
            blockbranch$writes.add(new Pending(future, blockbranch$currentSections));
            blockbranch$pendingSections += blockbranch$currentSections;
        }
        return future;
    }
}
