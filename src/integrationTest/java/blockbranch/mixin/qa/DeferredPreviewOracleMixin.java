package blockbranch.mixin.qa;
import blockbranch.realistic.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.*;
import net.minecraft.world.level.biome.Biome;
@Mixin(net.minecraft.world.item.MapItem.class)
abstract class DeferredPreviewOracleMixin {
 @WrapMethod(method="renderBiomePreviewMap")
 private static void stage(ServerLevel level,net.minecraft.world.item.ItemStack stack,Operation<Void> original){
  if(!DeferredPreviewQualification.ON){original.call(level,stack);return;}
  var data=net.minecraft.world.item.MapItem.getSavedData(stack,level);
  var event=new DeferredPreviewQualification.StageEvent();event.stage=DeferredPreviewQualification.reference?"native-reference":data!=null&&DeferredMapPreview.writing(data)?"native-raster":"preview-entry";event.begin();long cpu=DeferredPreviewQualification.cpu();
  try{original.call(level,stack);}finally{event.end();event.cpuNanos=DeferredPreviewQualification.cpu()-cpu;event.commit();}
 }

 @WrapOperation(method="renderBiomePreviewMap",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
 private static Holder<Biome> sample(ServerLevel level,BlockPos pos,Operation<Holder<Biome>> original){
  var sample=DeferredPreviewQualification.reference?DeferredPreviewPolicyChecks.sample(level,pos):null;return sample==null?original.call(level,pos):sample;
 }
}
