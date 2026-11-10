package blockbranch.mixin.cubic;

import blockbranch.realistic.MapPreviewSamples;
import blockbranch.realistic.DeferredMapPreview;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.At;
import blockbranch.realistic.RealisticGenerator;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.world.item.MapItem.class)
abstract class MapPreviewSamplesMixin {
    @WrapMethod(method = "renderBiomePreviewMap")
    private static void blockbranch$exactPreviewSamples(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.item.ItemStack stack, Operation<Void> original) {
        if (DeferredMapPreview.defer(level,stack)) return;
        if (level.getChunkSource().getGenerator() instanceof RealisticGenerator generator
                && generator.discoveryDefinition() > 0) {
            try (var ignored = MapPreviewSamples.open()) { original.call(level, stack); }
        } else original.call(level, stack);
    }
    @WrapOperation(method="renderBiomePreviewMap",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
    private static net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> sampledBiome(net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos,Operation<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> original){
        var sample=DeferredMapPreview.sampled(pos);return sample==null?original.call(level,pos):sample;
    }
    @WrapOperation(method="renderBiomePreviewMap",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;setColor(IIB)V"))
    private static void completedRows(net.minecraft.world.level.saveddata.maps.MapItemSavedData data,int x,int z,byte color,Operation<Void> original){
        if(DeferredMapPreview.writePixel(data,z))original.call(data,x,z,color);
    }
}
