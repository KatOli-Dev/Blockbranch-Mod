package blockbranch.mixin.qa;
import blockbranch.cubic.TerrainProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(TerrainProfile.class)
abstract class SectionGenerationWorkMixin {
 @Inject(method="sectionBiomes",at=@At("HEAD"))
 private void blockbranch$palette(int y,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<?> cir){blockbranch.realistic.SectionGenerationWorkProbe.palette();}
 @ModifyVariable(method="state",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private int blockbranch$count(int x){return blockbranch.realistic.SectionGenerationWorkProbe.count(x);}
}
