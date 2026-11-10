package blockbranch.mixin.cubic;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(SkyLightSectionStorage.class)
public interface SkyStorageAccessor {
    @Invoker("getBottomSectionY") int blockbranch$bottom();
}
