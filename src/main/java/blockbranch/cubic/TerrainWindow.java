package blockbranch.cubic;
import net.minecraft.world.level.LevelHeightAccessor;
/** Marks the compact native section window of an expanded column. */
public final class TerrainWindow implements LevelHeightAccessor {
    @Override public int getMinY() { return -64; }
    @Override public int getHeight() { return 384; }
}
