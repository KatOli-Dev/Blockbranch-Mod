package blockbranch.mixin.cubic;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ChunkGenerator.class)
abstract class GenerationMixin {
    @Redirect(method="doCreateBiomes",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkAccess;getMinY()I"))
    private int blockbranch$biomeMin(ChunkAccess chunk) { return chunk.getHeightAccessorForGeneration().getMinY(); }
    @Redirect(method="doCreateBiomes",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkAccess;getHeight()I"))
    private int blockbranch$biomeHeight(ChunkAccess chunk) { return chunk.getHeightAccessorForGeneration().getHeight(); }
}
