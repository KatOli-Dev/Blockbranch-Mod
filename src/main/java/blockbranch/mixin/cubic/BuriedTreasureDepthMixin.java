package blockbranch.mixin.cubic;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.level.WorldGenLevel;
/** The native piece searches for ordinary stone, which this generator places above Y=0.
 * A failed coast fit must not scan the expanded quarter-million-block depth. */
@Mixin(net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces.BuriedTreasurePiece.class)
abstract class BuriedTreasureDepthMixin {
 @WrapOperation(method="postProcess",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/WorldGenLevel;getMinY()I"))
 private int blockbranch$boundedSearch(WorldGenLevel level,Operation<Integer> original){
  int floor=original.call(level);
  return level.getLevel().getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator g&&g.discoveryDefinition()>0?Math.max(floor,0):floor;
 }
}
