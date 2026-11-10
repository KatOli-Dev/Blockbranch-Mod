package blockbranch.mixin.wildlife;
import blockbranch.wildlife.WildlifeCombat;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Mob.class)
abstract class AnimalTargetMixin {
    @Inject(method="canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at=@At("HEAD"), cancellable=true)
    private void blockbranch$animalDifficulty(LivingEntity target, CallbackInfoReturnable<Boolean> result) {
        if (!WildlifeCombat.permits((Mob)(Object)this, target)) result.setReturnValue(false);
    }
}
