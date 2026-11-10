package blockbranch.mixin.wildlife;

import blockbranch.wildlife.WildlifeHabitat;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native freshwater depth band follows local procedural water elevation, rather than global sea level. */
@Mixin(SpawnPlacements.class)
abstract class FreshwaterSpawnRulesMixin {
    @Inject(method="checkSpawnRules",at=@At("HEAD"),cancellable=true)
    private static void blockbranch$localFreshwater(EntityType<?> type,ServerLevelAccessor level,EntitySpawnReason reason,BlockPos pos,RandomSource random,CallbackInfoReturnable<Boolean> result){
        Boolean local=WildlifeHabitat.vanillaFreshwaterSpawn(type,level,pos);
        if(local!=null)result.setReturnValue(local);
    }
}
