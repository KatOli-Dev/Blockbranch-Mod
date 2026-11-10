package blockbranch.mixin.qa;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=blockbranch.realistic.RealisticGenerator.class,remap=false)
abstract class VillagePathProbeMixin {
    @Inject(method="applyBiomeDecoration",at=@At("TAIL"))
    private void blockbranch$controlledPath(net.minecraft.world.level.WorldGenLevel level,net.minecraft.world.level.chunk.ChunkAccess chunk,net.minecraft.world.level.StructureManager structures,CallbackInfo ci){
        if(Boolean.getBoolean("blockbranch.villageProbe")&&chunk.getPos().equals(blockbranch.realistic.VillagePathProbe.CHUNK))blockbranch.realistic.VillagePathProbe.generate(level);
    }
}
