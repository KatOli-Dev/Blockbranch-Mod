package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(PortalForcer.class)
abstract class PortalForcerMixin {
    @Shadow @Final private ServerLevel level;
    @ModifyExpressionValue(method="createPortal",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getMinY()I"))
    private int blockbranch$min(int value,@Local(argsOnly=true) BlockPos origin) { return ColumnLimits.expanded(level) ? Math.max(ColumnLimits.MIN_Y,origin.getY()-32) : value; }
    @ModifyExpressionValue(method="createPortal",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getMaxY()I"))
    private int blockbranch$max(int value,@Local(argsOnly=true) BlockPos origin) { return ColumnLimits.expanded(level) ? Math.min(ColumnLimits.MAX_Y-1,origin.getY()+32) : value; }
    @ModifyExpressionValue(method="createPortal",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getLogicalHeight()I"))
    private int blockbranch$logical(int value) { return ColumnLimits.expanded(level) ? 65 : value; }
    @ModifyExpressionValue(method="createPortal",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"))
    private int blockbranch$surface(int value,@Local(argsOnly=true) BlockPos origin) { return ColumnLimits.expanded(level) ? Math.clamp(value,Math.max(ColumnLimits.MIN_Y,origin.getY()-32),Math.min(ColumnLimits.MAX_Y-1,origin.getY()+32)) : value; }
    @ModifyConstant(method="createPortal",constant=@Constant(intValue=70))
    private int blockbranch$fallback(int value,@Local(argsOnly=true) BlockPos origin) { return ColumnLimits.expanded(level) ? Math.max(ColumnLimits.MIN_Y+1,origin.getY()-31) : value; }
}
