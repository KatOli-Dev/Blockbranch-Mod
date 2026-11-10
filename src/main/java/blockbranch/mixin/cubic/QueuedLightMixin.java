package blockbranch.mixin.cubic;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LayerLightSectionStorage.class)
abstract class QueuedLightMixin {
    @Shadow @Final protected Long2ObjectMap<DataLayer> queuedSections;

    @WrapMethod(method="markNewInconsistencies")
    private void blockbranch$drainQueued(LightEngine<?,?> engine,Operation<Void> original) {
        // Vanilla drains this synchronized map through an unguarded iterator.
        // Sparse save snapshots also enumerate it from the server thread, so the
        // drain must honor the same monitor as the snapshot and native put/remove.
        synchronized(queuedSections) { original.call(engine); }
    }
}
