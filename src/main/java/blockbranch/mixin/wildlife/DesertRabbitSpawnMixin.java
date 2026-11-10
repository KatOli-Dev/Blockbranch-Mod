package blockbranch.mixin.wildlife;

import blockbranch.wildlife.WildlifeHabitat;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restrict the new gravel-desert rabbit role to real soils with nearby vegetation. */
@Mixin(SpawnPlacements.class)
abstract class DesertRabbitSpawnMixin {
    @Inject(method="checkSpawnRules",at=@At("HEAD"),cancellable=true)
    private static void blockbranch$vegetatedPocket(EntityType<?> type,ServerLevelAccessor level,EntitySpawnReason reason,BlockPos pos,RandomSource random,CallbackInfoReturnable<Boolean> result){
        Boolean local=WildlifeHabitat.desertRabbitSpawn(type,level,pos);
        if(local!=null)result.setReturnValue(local);
    }
}
