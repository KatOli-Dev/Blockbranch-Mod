package blockbranch.cubic;

import net.minecraft.world.level.LevelHeightAccessor;

/** Coordinate packing is global; expanded build bounds apply only to the Overworld. */
public final class ColumnLimits {
    public static final int MIN_Y = -262144, MAX_Y = 262144, HEIGHT = MAX_Y - MIN_Y;
    public static final int HORIZONTAL_LIMIT = 1 << 21;
    public static final int BORDER_LIMIT = HORIZONTAL_LIMIT - 16;
    public static final int FORMAT_VERSION = 2;
    public static final LevelHeightAccessor BUILD = LevelHeightAccessor.create(MIN_Y, HEIGHT);
    public static final LevelHeightAccessor TERRAIN = LevelHeightAccessor.create(-64, 384);
    public static boolean expanded(LevelHeightAccessor height) { return height.getHeight() == HEIGHT; }
    public static boolean containsY(int y) { return y >= MIN_Y && y < MAX_Y; }
    private ColumnLimits() {}
}
