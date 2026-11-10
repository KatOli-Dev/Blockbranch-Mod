package blockbranch.mixin.qa;

import blockbranch.realistic.AdmissionPreparationChecks;
import java.util.Set;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** QA-only fault at the actual post-installation heightmap boundary. */
@Mixin(Heightmap.class)
abstract class AdmissionHeightmapMixin {
    @WrapMethod(method="primeHeightmaps")
    private static void failInstalled(ChunkAccess chunk,Set<Heightmap.Types> types,Operation<Void> original){
        AdmissionPreparationChecks.beforeHeightmap(chunk);
        original.call(chunk,types);
    }
}
