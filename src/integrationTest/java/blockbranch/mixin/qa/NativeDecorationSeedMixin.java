package blockbranch.mixin.qa;
import blockbranch.realistic.NativeDecorationRegression;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.biome.FeatureSorter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ChunkGenerator.class)
abstract class NativeDecorationSeedMixin {
 @WrapOperation(method="applyBiomeDecoration",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/WorldgenRandom;setFeatureSeed(JII)V",ordinal=0))
 private void structure(WorldgenRandom random,long seed,int index,int step,Operation<Void> original,@Local Structure structure,@Local(argsOnly=true) WorldGenLevel world,@Local(argsOnly=true) ChunkAccess chunk){NativeDecorationRegression.structure(world,chunk,structure,seed,index,step);original.call(random,seed,index,step);}
 @WrapOperation(method="applyBiomeDecoration",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/WorldgenRandom;setFeatureSeed(JII)V",ordinal=1))
 private void feature(WorldgenRandom random,long seed,int index,int step,Operation<Void> original,@Local PlacedFeature feature,@Local FeatureSorter.StepFeatureData data,@Local(argsOnly=true) WorldGenLevel world,@Local(argsOnly=true) ChunkAccess chunk){NativeDecorationRegression.feature(world,chunk,feature,data,seed,index,step);original.call(random,seed,index,step);}
}
