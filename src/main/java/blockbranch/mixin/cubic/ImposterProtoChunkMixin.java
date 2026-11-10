package blockbranch.mixin.cubic;
import blockbranch.cubic.SparseColumn;
import java.util.NavigableMap;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.*;
@Mixin(ImposterProtoChunk.class)
abstract class ImposterProtoChunkMixin implements SparseColumn, blockbranch.cubic.SparsePostProcessingAccess {
    @Shadow @Final private LevelChunk wrapped;
    public blockbranch.cubic.SparsePostProcessing blockbranch$postProcessing(boolean create){return ((blockbranch.cubic.SparsePostProcessingAccess)wrapped).blockbranch$postProcessing(create);}
    public blockbranch.cubic.ColumnTerrain blockbranch$terrain() { return wrapped == null ? null : ((SparseColumn)wrapped).blockbranch$terrain(); }
    public void blockbranch$terrain(blockbranch.cubic.ColumnTerrain terrain) { ((SparseColumn)wrapped).blockbranch$terrain(terrain); }
    public PalettedContainerFactory blockbranch$factory() { return ((SparseColumn)wrapped).blockbranch$factory(); }
    public boolean blockbranch$expanded() { return ((SparseColumn)wrapped).blockbranch$expanded(); }
    public NavigableMap<Integer,LevelChunkSection> blockbranch$sections() { return ((SparseColumn)wrapped).blockbranch$sections(); }
    public NavigableMap<Integer,LevelChunkSection> blockbranch$extraSections() { return ((SparseColumn)wrapped).blockbranch$extraSections(); }
    public LevelChunkSection blockbranch$read(int y) { return ((SparseColumn)wrapped).blockbranch$read(y); }
    public LevelChunkSection blockbranch$write(int y) { return ((SparseColumn)wrapped).blockbranch$write(y); }
    public void blockbranch$replace(int y,LevelChunkSection section) { ((SparseColumn)wrapped).blockbranch$replace(y,section); }
    public int blockbranch$baseMinSection() { return wrapped==null ? -4 : ((SparseColumn)wrapped).blockbranch$baseMinSection(); }
}
