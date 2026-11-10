package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(BlockPos.class)
abstract class BlockPosMixin {
    @ModifyConstant(method="<clinit>", constant=@Constant(intValue=30000000))
    private static int blockbranch$packing(int value) { return ColumnLimits.HORIZONTAL_LIMIT - 1; }
}
