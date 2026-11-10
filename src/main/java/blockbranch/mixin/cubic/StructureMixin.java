package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Structure.class)
abstract class StructureMixin {
    @ModifyVariable(method="generate",at=@At("HEAD"),argsOnly=true)
    private LevelHeightAccessor blockbranch$generationBounds(LevelHeightAccessor height) {
        if(height instanceof ChunkAccess chunk) return chunk.getHeightAccessorForGeneration();
        return ColumnLimits.expanded(height) ? ColumnLimits.TERRAIN : height;
    }
}
