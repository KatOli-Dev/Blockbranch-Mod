package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Level.class)
abstract class LevelMixin {
    // Level inherits these methods from LevelReader. Keep dimension codecs and generator bounds vanilla.
    public int getMinY() { var level = (Level)(Object)this; return level.dimension().equals(Level.OVERWORLD) ? ColumnLimits.MIN_Y : level.dimensionType().minY(); }
    public int getHeight() { var level = (Level)(Object)this; return level.dimension().equals(Level.OVERWORLD) ? ColumnLimits.HEIGHT : level.dimensionType().height(); }
    @ModifyConstant(method={"isInWorldBoundsHorizontal", "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"}, constant=@Constant(intValue=30000000))
    private static int blockbranch$positive(int original) { return ColumnLimits.HORIZONTAL_LIMIT; }
    @ModifyConstant(method={"isInWorldBoundsHorizontal", "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"}, constant=@Constant(intValue=-30000000))
    private static int blockbranch$negative(int original) { return -ColumnLimits.HORIZONTAL_LIMIT; }
}
