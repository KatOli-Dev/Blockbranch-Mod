package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(WorldBorder.class)
abstract class WorldBorderMixin {
    @ModifyConstant(method="<init>(Lnet/minecraft/world/level/border/WorldBorder$Settings;)V", constant=@Constant(intValue=29999984))
    private int blockbranch$border(int original) { return ColumnLimits.BORDER_LIMIT; }
    @ModifyConstant(method="<init>(Lnet/minecraft/world/level/border/WorldBorder$Settings;)V", constant=@Constant(doubleValue=5.9999968E7))
    private double blockbranch$size(double value) { return ColumnLimits.BORDER_LIMIT*2.0; }
    @ModifyVariable(method="setSize", at=@At("HEAD"), argsOnly=true)
    private double blockbranch$clampSize(double value) { return Math.min(value,ColumnLimits.BORDER_LIMIT*2.0); }
    @ModifyVariable(method="setCenter", at=@At("HEAD"), argsOnly=true, ordinal=0)
    private double blockbranch$first(double value) { return Math.clamp(value,-ColumnLimits.BORDER_LIMIT,ColumnLimits.BORDER_LIMIT); }
    @ModifyVariable(method="setCenter", at=@At("HEAD"), argsOnly=true, ordinal=1)
    private double blockbranch$second(double value) { return Math.clamp(value,-ColumnLimits.BORDER_LIMIT,ColumnLimits.BORDER_LIMIT); }
    @ModifyVariable(method="lerpSizeBetween", at=@At("HEAD"), argsOnly=true, ordinal=0)
    private double blockbranch$from(double value) { return Math.clamp(value,1.0,ColumnLimits.BORDER_LIMIT*2.0); }
    @ModifyVariable(method="lerpSizeBetween", at=@At("HEAD"), argsOnly=true, ordinal=1)
    private double blockbranch$to(double value) { return Math.clamp(value,1.0,ColumnLimits.BORDER_LIMIT*2.0); }
    @ModifyVariable(method="setAbsoluteMaxSize", at=@At("HEAD"), argsOnly=true)
    private int blockbranch$clampBorder(int value) { return Math.min(value, ColumnLimits.BORDER_LIMIT); }
}
