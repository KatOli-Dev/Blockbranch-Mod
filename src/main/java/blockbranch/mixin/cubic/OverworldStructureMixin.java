package blockbranch.mixin.cubic;

import java.util.Optional;
import blockbranch.realistic.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Structure.class)
abstract class OverworldStructureMixin {
    @WrapMethod(method="findValidGenerationPoint")
    private Optional<Structure.GenerationStub> blockbranch$nativeOverworld(Structure.GenerationContext context,
            Operation<Optional<Structure.GenerationStub>> original){
        if(!(context.chunkGenerator() instanceof RealisticGenerator g)||g.overworldDefinition()==0)return original.call(context);
        var key=context.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey((Structure)(Object)this);
        if(key==null||!OverworldStructures.additional(key,g))return original.call(context);
        return OverworldStructures.withTerrainQueries(()->OverworldStructures.fit((Structure)(Object)this,context,c->original.call(c)));
    }
}
