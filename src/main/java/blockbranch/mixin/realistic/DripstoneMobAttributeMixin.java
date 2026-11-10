package blockbranch.mixin.realistic;

import blockbranch.realistic.DripstoneSpawnRouting;
import net.minecraft.world.attribute.*;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Preserve native dimension/timeline/structure policy; replace only its biome sampler. */
@Mixin(EnvironmentAttributeSystem.class)
abstract class DripstoneMobAttributeMixin {
    @ModifyVariable(method="addBiomeLayerForAttribute",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static BiomeManager blockbranch$surfaceSpawnBiomes(BiomeManager value,EnvironmentAttributeSystem.Builder builder,EnvironmentAttribute<?> attribute,BiomeManager original){
        if(attribute!=EnvironmentAttributes.NATURAL_MOB_SPAWNS)return value;
        var source=((BiomeManagerSourceAccess)value).blockbranch$source();var surface=DripstoneSpawnRouting.surfaceMobs(source);
        return surface==source?value:value.withDifferentSource(surface);
    }
}
