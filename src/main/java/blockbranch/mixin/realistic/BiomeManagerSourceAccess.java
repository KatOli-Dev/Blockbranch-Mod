package blockbranch.mixin.realistic;
import net.minecraft.world.level.biome.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BiomeManager.class)
public interface BiomeManagerSourceAccess {
    @Accessor("noiseBiomeSource") BiomeResolver blockbranch$source();
}
