package blockbranch.cubic;

import java.util.NavigableMap;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Additional sections belong to their vanilla X/Z column, with absolute section Y keys. */
public interface SparseColumn {
    ColumnTerrain blockbranch$terrain();
    void blockbranch$terrain(ColumnTerrain terrain);
    net.minecraft.world.level.chunk.PalettedContainerFactory blockbranch$factory();
    boolean blockbranch$expanded();
    NavigableMap<Integer, LevelChunkSection> blockbranch$sections();
    /** Read-only live view for column-local simulation, excluding native terrain sections. */
    NavigableMap<Integer, LevelChunkSection> blockbranch$extraSections();
    LevelChunkSection blockbranch$read(int sectionY);
    LevelChunkSection blockbranch$write(int sectionY);
    void blockbranch$replace(int sectionY, LevelChunkSection section);
    int blockbranch$baseMinSection();
}
