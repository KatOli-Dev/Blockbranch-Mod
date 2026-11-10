package blockbranch.mixin.qa;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import blockbranch.realistic.*;

@Mixin(net.minecraft.world.item.MapItem.class)
abstract class TreasureConnectedMapMixin {
 @WrapMethod(method="inventoryTick")
 private void blockbranch$connectedMap(net.minecraft.world.item.ItemStack stack,net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Entity entity,net.minecraft.world.entity.EquipmentSlot slot,Operation<Void> original){
  if(!TreasureConnectedProbe.measure(stack,entity)){long start=System.nanoTime();try{original.call(stack,level,entity,slot);}finally{TreasureConnectedProbe.noteUpdate(stack,System.nanoTime()-start);ShipwreckConnectedProbe.note(stack,System.nanoTime()-start);}return;}
  TreasureConnectedProbe.begin();long start=System.nanoTime();
  try(var trace=TreasureMapUseTrace.open(entity,TreasureConnectedProbe.dir().resolve(TreasureConnectedProbe.caseIndex==0?"cold-chunks.json":"warm-chunks.json").toString())){original.call(stack,level,entity,slot);}
  finally{try{TreasureConnectedProbe.noteUpdate(stack,System.nanoTime()-start);ShipwreckConnectedProbe.note(stack,System.nanoTime()-start);TreasureConnectedProbe.end(System.nanoTime()-start);}catch(Exception e){throw new RuntimeException(e);}}
 }
}
