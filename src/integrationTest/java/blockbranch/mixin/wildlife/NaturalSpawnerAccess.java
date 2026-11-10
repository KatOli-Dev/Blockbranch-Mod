package blockbranch.mixin.wildlife;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Native fixture entry to the actual injected sampler, not a separate test algorithm. */
@Mixin(NaturalSpawner.class)
public interface NaturalSpawnerAccess {
    @Invoker("getRandomPosWithin")
    static BlockPos blockbranch$sample(Level level,LevelChunk chunk){throw new AssertionError("Mixin not applied");}
}
