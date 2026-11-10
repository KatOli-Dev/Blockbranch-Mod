package blockbranch.mixin.wildlife;
import blockbranch.wildlife.WildlifeCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
abstract class AnimalDamageMixin {
    @Inject(method="hurtServer", at=@At("HEAD"), cancellable=true)
    private void blockbranch$animalDifficulty(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> result) {
        if (source.getEntity() != null && !WildlifeCombat.permits(source.getEntity(), (Entity)(Object)this)) result.setReturnValue(false);
    }
}
