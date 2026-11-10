package blockbranch.mixin.wildlife;
import blockbranch.wildlife.WildlifeSpawnAudit;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SpawnPlacements.class)
abstract class SpawnPredicateAuditMixin {
    @Inject(method="checkSpawnRules",at=@At("RETURN"))
    private static void observe(EntityType<?> type,ServerLevelAccessor level,EntitySpawnReason reason,BlockPos pos,RandomSource random,CallbackInfoReturnable<Boolean> result){if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.predicate(level.getLevel(),pos,type,reason,result.getReturnValue());}
}
