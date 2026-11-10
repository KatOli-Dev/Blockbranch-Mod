package blockbranch.mixin.qa;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import blockbranch.realistic.HutExplorationProbe;
import blockbranch.realistic.WetlandHuts;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.*;
@Mixin(targets="blockbranch.realistic.WetlandHutRegions$Region",remap=false)
abstract class HutRegionTimingMixin {
 @Shadow private volatile Optional<WetlandHuts.Plan> result;
 @Shadow @Final private int lake;
 @WrapMethod(method="resolve") private Optional<WetlandHuts.Plan> audit(BooleanSupplier cancelled,Operation<Optional<WetlandHuts.Plan>> original){
  if(!HutExplorationProbe.ON||result!=null)return original.call(cancelled);
  var thread=Thread.currentThread();var stack=thread.getStackTrace();String phase=HutExplorationProbe.phase;var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long cpu=bean.getCurrentThreadCpuTime(),bytes=bean.getThreadAllocatedBytes(thread.threadId()),start=System.nanoTime();
  HutExplorationProbe.active.incrementAndGet();Optional<WetlandHuts.Plan> value;try{value=original.call(cancelled);}finally{HutExplorationProbe.active.decrementAndGet();}long wall=System.nanoTime()-start;
  HutExplorationProbe.record(lake,phase,thread.getName(),stack,wall,bean.getCurrentThreadCpuTime()-cpu,bean.getThreadAllocatedBytes(thread.threadId())-bytes,value.isPresent());return value;
 }
}
