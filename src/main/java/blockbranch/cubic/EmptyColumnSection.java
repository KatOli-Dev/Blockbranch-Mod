package blockbranch.cubic;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
/** A per-column read sentinel. Mutation must first obtain a writable sparse section. */
public final class EmptyColumnSection extends LevelChunkSection {
    public EmptyColumnSection(PalettedContainerFactory factory) { super(factory); }
    @Override public BlockState setBlockState(int x,int y,int z,BlockState state,boolean lock) { throw new IllegalStateException("Write through an unallocated sparse section"); }
}
