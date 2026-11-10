package blockbranch.mixin.wildlife;
import blockbranch.cubic.SparseColumn;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Procedural surfaces have no resident solid-section array to search for spawning. */
@Mixin(value=NaturalSpawner.class, priority=1100)
abstract class ProceduralSpawnPositionMixin {
    @Inject(method="getRandomPosWithin", at=@At("HEAD"), cancellable=true)
    private static void blockbranch$surfaceSpawn(Level level, LevelChunk chunk, CallbackInfoReturnable<BlockPos> result) {
        var terrain = ((SparseColumn)chunk).blockbranch$terrain();
        if (terrain == null) return;
        if(level instanceof net.minecraft.server.level.ServerLevel serverLevel){
            var sampled=blockbranch.wildlife.WildlifeSpawnPosition.sample(serverLevel,chunk);
            result.setReturnValue(sampled);
        }
    }
}
