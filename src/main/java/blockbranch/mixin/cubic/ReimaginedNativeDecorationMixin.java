package blockbranch.mixin.cubic;

import blockbranch.realistic.ReimaginedSites;
import blockbranch.realistic.ReimaginedStructures;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The opt-in crypt has its own decoration pass and is not part of vanilla's seed index. */
@Mixin(ChunkGenerator.class)
abstract class ReimaginedNativeDecorationMixin {
    @WrapOperation(method="applyBiomeDecoration",at=@At(value="INVOKE",target="Lnet/minecraft/core/Registry;stream()Ljava/util/stream/Stream;"))
    private Stream<Structure> blockbranch$nativeStructureOrder(Registry<Structure> registry,Operation<Stream<Structure>> original){
        var crypt=ReimaginedStructures.key(ReimaginedSites.CRYPT).identifier();
        return original.call(registry).filter(structure->!crypt.equals(registry.getKey(structure)));
    }
}
