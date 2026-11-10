package blockbranch.client.performance.render.patch.mixin;

import blockbranch.client.cubic.ProceduralSectionCopies;
import blockbranch.cubic.TerrainProfile;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla copies all 27 sections of a compile region on the render thread. For an unedited
 * Blockbranch section that copy means generating procedural terrain on the render thread.
 * Such a base section is immutable,
 * so it is instead generated the first time the compile worker reads it.
 *
 * <p>The result is the same container the eager path produces. A later edit makes the section
 * resident and dirty, so it is recompiled from a fresh snapshot, exactly as with an eager copy taken
 * before the edit. One copy serves up to 27 regions compiling on different workers, hence the lock.
 */
@Mixin(SectionCopy.class)
abstract class DeferredBaseSectionCopyMixin implements ProceduralSectionCopies.DeferredBase {
    // Written before the copy is handed to a worker; the executor queue publishes them.
    @Unique private TerrainProfile blockbranch$base;
    @Unique private int blockbranch$baseY;
    @Unique private long blockbranch$epoch;
    @Unique private volatile PalettedContainer<BlockState> blockbranch$states;

    @Override public void blockbranch$deferBase(TerrainProfile base, int y) {
        blockbranch$base = base;
        blockbranch$baseY = y;
        blockbranch$epoch = ProceduralSectionCopies.epoch();
    }

    @ModifyExpressionValue(method = "getBlockState", require = 2, at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/renderer/chunk/SectionCopy;section:Lnet/minecraft/world/level/chunk/PalettedContainer;"))
    private PalettedContainer<BlockState> blockbranch$resolve(PalettedContainer<BlockState> section) {
        if (section != null || blockbranch$base == null) return section;
        var states = blockbranch$states;
        if (states == null) {
            synchronized (this) {
                states = blockbranch$states;
                if (states == null) blockbranch$states = states = ProceduralSectionCopies.copyBase(blockbranch$base, blockbranch$baseY, blockbranch$epoch);
            }
        }
        return states;
    }
}
