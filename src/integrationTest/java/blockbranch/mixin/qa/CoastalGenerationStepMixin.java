package blockbranch.mixin.qa;
import blockbranch.realistic.CoastalGenerationTrace;
import java.util.concurrent.CompletableFuture;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.*;
import net.minecraft.server.level.*;
import net.minecraft.util.StaticCache2D;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ChunkStep.class)
abstract class CoastalGenerationStepMixin {
 @WrapMethod(method="apply") private CompletableFuture<ChunkAccess> audit(WorldGenContext context,StaticCache2D<GenerationChunkHolder> cache,ChunkAccess chunk,Operation<CompletableFuture<ChunkAccess>> original){
  if(blockbranch.realistic.AdmissionWorkTrace.ON)blockbranch.realistic.AdmissionWorkTrace.cache("status."+((ChunkStep)(Object)this).targetStatus(),chunk);
  return CoastalGenerationTrace.step((ChunkStep)(Object)this,context,chunk,()->original.call(context,cache,chunk));}
}
