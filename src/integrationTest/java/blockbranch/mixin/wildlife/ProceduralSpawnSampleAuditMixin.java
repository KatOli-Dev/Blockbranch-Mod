package blockbranch.mixin.wildlife;
import blockbranch.wildlife.WildlifeSpawnAudit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(blockbranch.wildlife.WildlifeSpawnPosition.class)
abstract class ProceduralSpawnSampleAuditMixin {
    @Inject(method="sample",at=@At("RETURN"),remap=false)
    private static void observe(net.minecraft.server.level.ServerLevel level,net.minecraft.world.level.chunk.LevelChunk chunk,CallbackInfoReturnable<net.minecraft.core.BlockPos> result){
        if(WildlifeSpawnAudit.ENABLED)WildlifeSpawnAudit.sampled(level,result.getReturnValue());
    }
}
