package blockbranch.mixin.cubic;
import net.minecraft.world.level.lighting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LightEngine.class)
public interface LightEngineAccessor {
    @Accessor("storage") LayerLightSectionStorage<?> blockbranch$storage();
}
