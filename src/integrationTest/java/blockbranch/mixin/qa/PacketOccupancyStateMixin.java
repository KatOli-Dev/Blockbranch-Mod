package blockbranch.mixin.qa;
import blockbranch.cubic.TerrainProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(TerrainProfile.class)
abstract class PacketOccupancyStateMixin {
 @org.spongepowered.asm.mixin.Shadow private boolean scanEmptySection(int y){throw new AssertionError();}
 @Inject(method="sectionIsEmpty",at=@At("HEAD"),cancellable=true)
 private void blockbranch$uncached(int y,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir){if(blockbranch.qa.PacketOccupancyProbe.legacy&&Thread.currentThread()==blockbranch.qa.PacketOccupancyProbe.owner)cir.setReturnValue(scanEmptySection(y));}
 @ModifyVariable(method="state",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private int blockbranch$states(int x){blockbranch.qa.PacketOccupancyProbe.state();return x;}
}
