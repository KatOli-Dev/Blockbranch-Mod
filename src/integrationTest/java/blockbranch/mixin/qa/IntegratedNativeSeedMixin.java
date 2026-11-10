package blockbranch.mixin.qa;
import blockbranch.realistic.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=OverworldStructures.class,remap=false)
abstract class IntegratedNativeSeedMixin {
 @WrapOperation(method="decorate",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/WorldgenRandom;setFeatureSeed(JII)V"))
 private static void blockbranch$nativeSeed(WorldgenRandom random,long seed,int index,int step,Operation<Void> original){IntegratedSeedAudit.seed(index,step);original.call(random,seed,index,step);}
}
