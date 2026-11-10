package blockbranch.mixin.realistic;

import blockbranch.realistic.UniformSnapshots;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Deduplicate immutable uniform biome palettes after load, copy and network reads. */
@Mixin(LevelChunkSection.class)
abstract class UniformBiomesMixin {
    @Shadow private PalettedContainerRO<Holder<Biome>> biomes;

    @Inject(method = {
        "<init>(Lnet/minecraft/world/level/chunk/PalettedContainer;Lnet/minecraft/world/level/chunk/PalettedContainerRO;)V",
        "<init>(Lnet/minecraft/world/level/chunk/PalettedContainerFactory;)V",
        "<init>(Lnet/minecraft/world/level/chunk/LevelChunkSection;)V",
        "read", "readBiomes", "fillBiomesFromNoise"
    }, at = @At("RETURN"))
    private void realistic$compactBiomes(CallbackInfo ci) {
        biomes = UniformSnapshots.compactBiomes(biomes);
    }
}
