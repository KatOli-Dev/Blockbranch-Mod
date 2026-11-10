package blockbranch.mixin.qa;
import blockbranch.realistic.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.*;
import net.minecraft.world.level.biome.Biome;
@Mixin(DeferredMapPreview.class)
abstract class DeferredPreviewAuditMixin {
 @WrapMethod(method="defer")
 private static boolean nativeOracle(ServerLevel level,ItemStack stack,Operation<Boolean> original){
  if(DeferredPreviewQualification.reference)return false;
  var event=new DeferredPreviewQualification.StageEvent();event.stage="defer-body";event.begin();long cpu=DeferredPreviewQualification.cpu();
  try{return original.call(level,stack);}finally{event.end();event.cpuNanos=DeferredPreviewQualification.cpu()-cpu;event.commit();}
 }
 @WrapMethod(method="tick")
 private static void measuredTick(net.minecraft.server.MinecraftServer server,Operation<Void> original){
  if(!DeferredPreviewQualification.ON){original.call(server);return;}
  var event=new DeferredPreviewQualification.TickEvent();event.begin();
  long cpu=DeferredPreviewQualification.cpu(),wall=System.nanoTime();DeferredPreviewQualification.queries=0;
  try{original.call(server);}finally{long elapsedCpu=DeferredPreviewQualification.cpu()-cpu;DeferredPreviewQualification.tickMeasured(System.nanoTime()-wall,elapsedCpu);event.end();if(DeferredPreviewQualification.queries>0&&!DeferredPreviewQualification.policy){event.queries=DeferredPreviewQualification.queries;event.cpuNanos=elapsedCpu;event.phase=DeferredPreviewQualification.phase;event.commit();}}
 }
}
