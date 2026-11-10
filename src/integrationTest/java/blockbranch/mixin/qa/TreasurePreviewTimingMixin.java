package blockbranch.mixin.qa;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

/** QA-only timing of the native preview, separately from loot generation and locating. */
@Mixin(net.minecraft.world.item.MapItem.class)
abstract class TreasurePreviewTimingMixin {
 @WrapMethod(method="renderBiomePreviewMap")
 private static void blockbranch$timePreview(net.minecraft.server.level.ServerLevel level,net.minecraft.world.item.ItemStack stack,Operation<Void> original){
  if(!Boolean.getBoolean("blockbranch.treasureMapQualification")&&!blockbranch.realistic.TreasureConnectedProbe.ON){original.call(level,stack);return;}
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long thread=Thread.currentThread().threadId(),allocated=bean.getThreadAllocatedBytes(thread);
  long start=System.nanoTime();try{original.call(level,stack);}finally{blockbranch.realistic.TreasureMapQualification.PREVIEW_TIMES.add(System.nanoTime()-start);blockbranch.realistic.TreasureMapQualification.PREVIEW_ALLOCATIONS.add(bean.getThreadAllocatedBytes(thread)-allocated);if(blockbranch.realistic.TreasureConnectedProbe.ON)blockbranch.realistic.TreasureConnectedProbe.preview(System.nanoTime()-start,bean.getThreadAllocatedBytes(thread)-allocated);}
 }
}
