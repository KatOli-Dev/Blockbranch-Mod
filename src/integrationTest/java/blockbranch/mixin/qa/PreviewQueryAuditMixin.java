package blockbranch.mixin.qa;
import blockbranch.realistic.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
@Mixin(PreviewBiomeQueries.class)
abstract class PreviewQueryAuditMixin {
 @WrapMethod(method="loaded")
 private static PreviewBiomeQueries.Read loaded(ServerLevel level,PreviewBiomeQueries.Point point,Operation<PreviewBiomeQueries.Read> original){
  if(!DeferredPreviewQualification.ON)return original.call(level,point);
  DeferredPreviewQualification.queries++;long start=System.nanoTime();
  try{var result=DeferredPreviewPolicyChecks.synthetic?new PreviewBiomeQueries.Read(!DeferredPreviewPolicyChecks.pendingRead,DeferredPreviewPolicyChecks.override(point)):original.call(level,point);if(!result.ready())DeferredPreviewQualification.pendingReads++;return result;}
  finally{DeferredPreviewQualification.queryMax=Math.max(DeferredPreviewQualification.queryMax,System.nanoTime()-start);}
 }
 @WrapMethod(method="fallback")
 private static Holder<Biome> fallback(BiomeResolver resolver,PreviewBiomeQueries.Point point,Operation<Holder<Biome>> original){
  if(!DeferredPreviewQualification.ON)return original.call(resolver,point);
  if(DeferredPreviewPolicyChecks.release!=null)DeferredPreviewPolicyChecks.heldPoint=point;
  DeferredPreviewPolicyChecks.awaitWorker();
  if(DeferredPreviewQualification.failOnce){DeferredPreviewQualification.failOnce=false;throw new IllegalStateException("Intentional deferred-preview worker failure");}
  if(DeferredPreviewPolicyChecks.synthetic)return DeferredPreviewPolicyChecks.quart(point);
  var event=new DeferredPreviewQualification.QueryEvent();event.begin();long cpu=DeferredPreviewQualification.cpu(),start=System.nanoTime();
  try{return original.call(resolver,point);}finally{
   long wall=System.nanoTime()-start,used=DeferredPreviewQualification.cpu()-cpu;DeferredPreviewQualification.workerQueries.incrementAndGet();DeferredPreviewQualification.workerCpu.addAndGet(used);DeferredPreviewQualification.workerWall.addAndGet(wall);DeferredPreviewQualification.workerMax.accumulateAndGet(wall,Math::max);event.end();event.cpuNanos=used;event.x=point.x();event.z=point.z();event.commit();
  }
 }
}
