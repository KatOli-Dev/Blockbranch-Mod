package blockbranch.mixin.wildlife;
import blockbranch.wildlife.WildlifeSpawnAudit;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Mob.class)
abstract class MobSpawnAuditMixin {
    @Inject(method="finalizeSpawn",at=@At("RETURN"))
    private void observe(ServerLevelAccessor level,DifficultyInstance difficulty,EntitySpawnReason reason,SpawnGroupData group,CallbackInfoReturnable<SpawnGroupData> result){if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.finalized(level.getLevel(),(Mob)(Object)this,reason);}
}
