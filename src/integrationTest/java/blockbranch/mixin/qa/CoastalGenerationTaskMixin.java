package blockbranch.mixin.qa;
import blockbranch.realistic.CoastalGenerationTrace;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChunkGenerationTask.class)
abstract class CoastalGenerationTaskMixin {
 @WrapMethod(method="create") private static ChunkGenerationTask created(GeneratingChunkMap map,ChunkStatus status,ChunkPos pos,Operation<ChunkGenerationTask> original){var task=original.call(map,status,pos);CoastalGenerationTrace.admitted(task);return task;}
 @Inject(method="releaseClaim",at=@At("HEAD")) private void released(CallbackInfo ci){CoastalGenerationTrace.released(this);}
}
