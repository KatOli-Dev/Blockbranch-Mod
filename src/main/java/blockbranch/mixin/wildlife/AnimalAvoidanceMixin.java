package blockbranch.mixin.wildlife;

import blockbranch.wildlife.WildlifeCombat;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Escape selection must not ask whether an animal is permitted to attack its threat. */
@Mixin(AvoidEntityGoal.class)
abstract class AnimalAvoidanceMixin {
    @Shadow @Final protected PathfinderMob mob;

    @Redirect(method="<init>(Lnet/minecraft/world/entity/PathfinderMob;Ljava/lang/Class;Ljava/util/function/Predicate;FDDLjava/util/function/Predicate;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;forCombat()Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;"))
    private TargetingConditions blockbranch$independentFear() {
        // The constructor still applies the native range, predicates, visibility and
        // creative/spectator filters. Non-animal avoidance remains native combat targeting.
        return WildlifeCombat.isAnimal(mob)?TargetingConditions.forNonCombat():TargetingConditions.forCombat();
    }
    @Redirect(method="<init>(Lnet/minecraft/world/entity/PathfinderMob;Ljava/lang/Class;Ljava/util/function/Predicate;FDDLjava/util/function/Predicate;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;selector(Lnet/minecraft/world/entity/ai/targeting/TargetingConditions$Selector;)Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;"))
    private TargetingConditions blockbranch$retainNativeEligibility(TargetingConditions conditions,TargetingConditions.Selector predicate) {
        if(!WildlifeCombat.isAnimal(mob))return conditions.selector(predicate);
        return conditions.selector((target,level)->predicate.test(target,level)&&target.canBeSeenAsEnemy()&&!mob.isAlliedTo(target));
    }
}
