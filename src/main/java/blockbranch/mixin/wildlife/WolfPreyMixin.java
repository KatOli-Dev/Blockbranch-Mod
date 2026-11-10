package blockbranch.mixin.wildlife;

import blockbranch.wildlife.RedDeer;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extend the native untamed hunting selector; retain its goals, other prey and owner utility. */
@Mixin(Wolf.class)
abstract class WolfPreyMixin {
    @Shadow @Final @Mutable public static TargetingConditions.Selector PREY_SELECTOR;

    @Inject(method="<clinit>",at=@At("TAIL"))
    private static void blockbranch$deerPrey(CallbackInfo callback){
        var nativePrey=PREY_SELECTOR;
        PREY_SELECTOR=(target,level)->nativePrey.test(target,level)||target instanceof RedDeer;
    }
}
