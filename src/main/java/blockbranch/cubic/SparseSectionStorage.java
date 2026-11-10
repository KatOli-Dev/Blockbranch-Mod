package blockbranch.cubic;
import java.util.stream.Stream;
import net.minecraft.world.level.ChunkPos;
public interface SparseSectionStorage<R> { boolean blockbranch$sparseStorage(); Stream<R> blockbranch$inChunk(ChunkPos pos); }
