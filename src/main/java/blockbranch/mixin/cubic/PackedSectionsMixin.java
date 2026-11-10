package blockbranch.mixin.cubic;
import blockbranch.cubic.PackedSections;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.spongepowered.asm.mixin.*;
@Mixin(targets="net.minecraft.world.level.chunk.storage.SectionStorage$PackedChunk")
abstract class PackedSectionsMixin<P> implements PackedSections<P> {
    @Shadow @Final private Int2ObjectMap<P> sectionsByY;
    @Shadow @Final private boolean versionChanged;
    public Int2ObjectMap<P> blockbranch$packed() { return sectionsByY; }
    public boolean blockbranch$changed() { return versionChanged; }
}
