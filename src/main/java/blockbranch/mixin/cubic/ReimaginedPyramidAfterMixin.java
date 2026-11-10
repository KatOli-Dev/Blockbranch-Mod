package blockbranch.mixin.cubic;
import blockbranch.realistic.*;import net.minecraft.world.level.*;import net.minecraft.world.level.chunk.ChunkGenerator;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;import net.minecraft.world.level.levelgen.structure.structures.*;import net.minecraft.util.RandomSource;import org.spongepowered.asm.mixin.Mixin;import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(DesertPyramidStructure.class)
abstract class ReimaginedPyramidAfterMixin {
 @WrapMethod(method="afterPlace") private void blockbranch$coreOnly(WorldGenLevel world,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,PiecesContainer pieces,Operation<Void> original){if(pieces.pieces().stream().anyMatch(p->p instanceof ReimaginedLayoutPiece)&&pieces.pieces().stream().filter(p->p instanceof DesertPyramidPiece).noneMatch(p->p.getBoundingBox().intersects(clip)))return;original.call(world,manager,generator,random,clip,chunk,pieces);}
}
