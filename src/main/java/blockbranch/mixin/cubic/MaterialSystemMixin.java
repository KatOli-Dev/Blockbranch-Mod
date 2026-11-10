package blockbranch.mixin.cubic;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.material.MaterialSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Surface scans must stop at the generator floor, not the expanded build floor. */
@Mixin(MaterialSystem.class)
abstract class MaterialSystemMixin {
    @Redirect(method={"buildSurface","erodedBadlandsExtension"},at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkAccess;getMinY()I"))
    private int blockbranch$terrainFloor(ChunkAccess chunk) {
        return chunk.getHeightAccessorForGeneration().getMinY();
    }
}
