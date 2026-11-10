package blockbranch.mixin.qa;
import blockbranch.realistic.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(DrainageNetwork.class)
abstract class AdmissionOceanMixin {
 @Inject(method="oceanWater",at=@At("HEAD"))
 private void predicate(int x,int z,DrainageNetwork.Sample sample,CallbackInfoReturnable<Boolean> cir){AdmissionOceanTrace.count(x,z,false);}
 @WrapOperation(method="oceanWater",at=@At(value="INVOKE",target="Lblockbranch/realistic/TerrainShape;height(II)I"))
 private int raw(TerrainShape shape,int x,int z,Operation<Integer> original){AdmissionOceanTrace.count(x,z,true);return original.call(shape,x,z);}
}
