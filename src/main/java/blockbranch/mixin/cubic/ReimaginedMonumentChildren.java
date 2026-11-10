package blockbranch.mixin.cubic;
import java.util.List;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.gen.Accessor;import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
@Mixin(OceanMonumentPieces.MonumentBuilding.class)
public interface ReimaginedMonumentChildren {@Accessor("childPieces")List<net.minecraft.world.level.levelgen.structure.StructurePiece> blockbranch$children();}
