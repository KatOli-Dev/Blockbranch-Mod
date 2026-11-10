package blockbranch.mixin.qa;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Native baseline control, confined to the development test mod. */
@Mixin(value=blockbranch.realistic.VillagePaths.class,remap=false)
abstract class VillagePathBaselineMixin {
    @Inject(method="place",at=@At("HEAD"),cancellable=true)
    private static void blockbranch$uncorrected(net.minecraft.world.level.WorldGenLevel level,Runnable placement,CallbackInfo ci){
        if(Boolean.getBoolean("blockbranch.villageUncorrected")){placement.run();ci.cancel();}
    }
}
