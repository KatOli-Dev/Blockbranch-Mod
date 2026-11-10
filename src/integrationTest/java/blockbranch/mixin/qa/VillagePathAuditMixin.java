package blockbranch.mixin.qa;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.server.level.WorldGenRegion.class)
abstract class VillagePathAuditMixin {
    @Inject(method="setBlock",at=@At("HEAD"))
    private void blockbranch$beforePath(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> cir){
        blockbranch.realistic.VillagePathAudit.before((net.minecraft.server.level.WorldGenRegion)(Object)this,pos,state);
    }
}
