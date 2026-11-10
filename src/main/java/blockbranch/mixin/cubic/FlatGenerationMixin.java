package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(FlatLevelSource.class)
abstract class FlatGenerationMixin {
    @Redirect(method="buildTerrain",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkAccess;getMinY()I"))
    private int blockbranch$min(ChunkAccess chunk) { return chunk.getHeightAccessorForGeneration().getMinY(); }
    @Redirect(method="buildTerrain",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkAccess;getHeight()I"))
    private int blockbranch$height(ChunkAccess chunk) { return chunk.getHeightAccessorForGeneration().getHeight(); }
    @ModifyVariable(method={"getSpawnHeight","getBaseHeight","getBaseColumn"},at=@At("HEAD"),argsOnly=true)
    private LevelHeightAccessor blockbranch$bounds(LevelHeightAccessor height) { return ColumnLimits.expanded(height) ? ColumnLimits.TERRAIN : height; }
}
