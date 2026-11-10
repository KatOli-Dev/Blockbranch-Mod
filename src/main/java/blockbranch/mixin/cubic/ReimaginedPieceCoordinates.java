package blockbranch.mixin.cubic;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.levelgen.structure.StructurePiece;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(StructurePiece.class)
public interface ReimaginedPieceCoordinates {@Invoker("getWorldPos") BlockPos.MutableBlockPos blockbranch$local(int x,int y,int z);}
