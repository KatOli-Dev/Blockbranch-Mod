package blockbranch.mixin.realistic;

import blockbranch.realistic.RealisticGenerator;
import blockbranch.realistic.TerrainShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep the explicit marine open-water habitat from regrowing a native ice sheet. */
@Mixin(Biome.class)
abstract class IcebergOpenWaterMixin {
    @Inject(method="shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",at=@At("HEAD"),cancellable=true)
    private void blockbranch$openMarineWater(LevelReader level,BlockPos pos,boolean neighbours,CallbackInfoReturnable<Boolean> result){
        if(pos.getY()==TerrainShape.SEA_LEVEL-1&&level instanceof ServerLevel server
                &&server.getChunkSource().getGenerator() instanceof RealisticGenerator g
                &&g.keepsIcebergWaterOpen(pos.getX(),pos.getZ(),server.getChunkSource().randomState()))result.setReturnValue(false);
    }
}
