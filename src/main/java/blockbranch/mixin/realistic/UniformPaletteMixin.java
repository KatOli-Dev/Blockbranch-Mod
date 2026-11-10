package blockbranch.mixin.realistic;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.Strategy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A uniform section needs a single palette value, not a 4096-entry scratch array. */
@Mixin(PalettedContainer.class)
abstract class UniformPaletteMixin<T> implements blockbranch.realistic.PaletteStrategyAccess<T> {
    @Shadow @Final private Strategy<T> strategy;
    @Override public Strategy<T> realistic$strategy() { return strategy; }
    @Shadow public abstract void acquire();
    @Shadow public abstract void release();
    @Shadow public abstract int bitsPerEntry();
    @Shadow public abstract T get(int x, int y, int z);

    @Inject(method = "pack", at = @At("HEAD"), cancellable = true)
    private void realistic$packUniform(Strategy<T> strategy, CallbackInfoReturnable<PalettedContainerRO.PackedData<T>> result) {
        acquire();
        try {
            if (bitsPerEntry() == 0)
                result.setReturnValue(new PalettedContainerRO.PackedData<>(List.of(get(0, 0, 0)), Optional.empty(), 0));
        } finally { release(); }
    }
}
