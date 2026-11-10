package blockbranch.mixin.qa;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(net.minecraft.world.item.MapItem.class)
abstract class TreasureMapUseTraceMixin {
 @WrapOperation(method="update",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
 private LevelChunk blockbranch$traceChunk(Level level,int x,int z,Operation<LevelChunk> original){
  return blockbranch.realistic.TreasureMapUseTrace.chunk(level,x,z,()->original.call(level,x,z));
 }
}
