package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ServerGamePacketListenerImpl.class)
abstract class MovementMixin {
    @ModifyConstant(method="clampHorizontal",constant=@Constant(doubleValue=3.0E7))
    private static double blockbranch$max(double value) { return ColumnLimits.BORDER_LIMIT; }
    @ModifyConstant(method="clampHorizontal",constant=@Constant(doubleValue=-3.0E7))
    private static double blockbranch$min(double value) { return -ColumnLimits.BORDER_LIMIT; }
}
