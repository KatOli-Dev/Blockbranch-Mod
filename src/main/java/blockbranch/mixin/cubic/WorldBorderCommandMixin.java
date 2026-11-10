package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.server.commands.WorldBorderCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(WorldBorderCommand.class)
abstract class WorldBorderCommandMixin {
    @ModifyConstant(method={"register","setSize","<clinit>"},constant=@Constant(doubleValue=5.9999968E7))
    private static double blockbranch$maxSize(double value) { return ColumnLimits.BORDER_LIMIT*2.0; }
    @ModifyConstant(method="register",constant=@Constant(doubleValue=-5.9999968E7))
    private static double blockbranch$minSize(double value) { return -ColumnLimits.BORDER_LIMIT*2.0; }
    @ModifyConstant(method={"setCenter","<clinit>"},constant=@Constant(doubleValue=2.9999984E7))
    private static double blockbranch$center(double value) { return ColumnLimits.BORDER_LIMIT; }
}
