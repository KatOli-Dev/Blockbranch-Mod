package blockbranch.client.performance.render;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Supplies the render patch mixins only when {@link RenderPatches} allows them. The config file
 * lists no mixins itself, so a disabled or uncompiled patch is never even loaded.
 */
public final class RenderPatchesMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {
        if (!mixinPackage.equals(RenderPatches.MIXIN_PACKAGE))
            throw new IllegalStateException("Render patch mixin package moved: " + mixinPackage);
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return RenderPatches.activeMixins(); }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
