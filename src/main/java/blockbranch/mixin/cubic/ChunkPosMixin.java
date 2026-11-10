package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ChunkPos.class)
abstract class ChunkPosMixin {
    @Inject(method="isValid(II)Z",at=@At("HEAD"),cancellable=true)
    private static void blockbranch$valid(int x,int z,CallbackInfoReturnable<Boolean> cir) {
        int limit=ColumnLimits.HORIZONTAL_LIMIT>>4;
        cir.setReturnValue(x>=-limit && x<limit && z>=-limit && z<limit);
    }
}
