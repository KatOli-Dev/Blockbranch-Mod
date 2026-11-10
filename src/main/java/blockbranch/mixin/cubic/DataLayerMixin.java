package blockbranch.mixin.cubic;

import net.minecraft.world.level.chunk.DataLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restore compact homogeneous light storage after disk/network decoding. */
@Mixin(DataLayer.class)
abstract class DataLayerMixin {
    @Shadow public abstract void fill(int value);

    @Inject(method = "<init>([B)V", at = @At("RETURN"))
    private void blockbranch$compact(byte[] bytes, CallbackInfo ci) {
        int packed = bytes[0] & 255;
        int value = packed & 15;
        if (value != packed >>> 4) return;
        for (int i = 1; i < bytes.length; i++) if ((bytes[i] & 255) != packed) return;
        fill(value);
    }
}
