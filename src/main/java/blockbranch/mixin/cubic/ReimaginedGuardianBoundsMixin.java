package blockbranch.mixin.cubic;
import blockbranch.realistic.ReimaginedSanctuaryPiece;import net.minecraft.world.level.chunk.ChunkGenerator;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.At;import com.llamalad7.mixinextras.injector.wrapoperation.*;
@Mixin(ChunkGenerator.class)
abstract class ReimaginedGuardianBoundsMixin {
 @WrapOperation(method="lambda$getMobsAt$1",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/structure/StructureStart;getBoundingBox()Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"))
 private static BoundingBox blockbranch$nativeGuardianArea(StructureStart start,Operation<BoundingBox> original){if(start.getPieces().stream().anyMatch(p->p instanceof ReimaginedSanctuaryPiece))return start.getPieces().stream().filter(p->p instanceof OceanMonumentPieces.MonumentBuilding).findFirst().orElseThrow().getBoundingBox();return original.call(start);}
}
