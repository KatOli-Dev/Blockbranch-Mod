package blockbranch.mixin.wildlife;
import blockbranch.realistic.RealisticGenerator;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses vanilla's once-per-chunk generation stage, never a second runtime spawner. */
@Mixin(RealisticGenerator.class)
abstract class InitialWildlifeMixin {
    @Inject(method="spawnOriginalMobs", at=@At("HEAD"))
    private void blockbranch$initialAnimals(WorldGenRegion region, CallbackInfo callback) {
        var chunk = region.getCenter();
        var random = new WorldgenRandom(new LegacyRandomSource(0));
        random.setDecorationSeed(region.getSeed(), chunk.getMinBlockX(), chunk.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(region, chunk.getWorldPosition().atY(region.getMaxY()), chunk, random);
    }
}
