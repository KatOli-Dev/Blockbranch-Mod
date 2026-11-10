package blockbranch.client.performance.render;

import blockbranch.Blockbranch;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Switches for the optional chunk-building and rendering patches. These patches only make chunk
 * meshing cheaper; the game is complete without them. They target vanilla internals that change
 * between Minecraft versions, so they are written to be switched off rather than ported at once:
 *
 * <ul>
 *   <li>{@link #ENABLED} turns every patch off; each {@link Patch} constant turns off one.</li>
 *   <li>On any Minecraft version other than {@link #WRITTEN_FOR} no patch mixin is loaded and vanilla
 *       code runs, with one warning in the log. After porting and re-checking the patches, update
 *       {@link #WRITTEN_FOR}.</li>
 *   <li>If the patch sources no longer compile against a new version, build with
 *       {@code -PrenderPatches=false}: Gradle then leaves out the {@code render.patch} package and
 *       the mixin plugin finds nothing to load.</li>
 *   <li>{@code -Dblockbranch.renderPatches=off} turns them off at launch, for A/B comparisons.</li>
 * </ul>
 *
 * <p>Everything a patch needs lives under {@code blockbranch.client.performance.render.patch}.
 * Code outside that package may only notice a patch through an interface the patch's mixin adds,
 * so a missing patch always means the vanilla behavior.
 */
public final class RenderPatches {
    /** Master switch for every patch below. */
    public static final boolean ENABLED = true;
    /** The Minecraft version the patches were written and checked against. */
    public static final String WRITTEN_FOR = "26.3";
    private static final String PROPERTY = "blockbranch.renderPatches";
    static final String MIXIN_PACKAGE = "blockbranch.client.performance.render.patch.mixin";

    public enum Patch {
        /**
         * Generates unedited procedural sections on the section compile worker instead of while the
         * render thread snapshots the 3x3x3 region, which stalled frames. Output is identical.
         */
        DEFERRED_BASE_SECTION_COPIES(true, "DeferredBaseSectionCopyMixin");

        final boolean enabled;
        final List<String> mixins;

        Patch(boolean enabled, String... mixins) {
            this.enabled = enabled;
            this.mixins = List.of(mixins);
        }
    }

    private static List<String> active;

    private RenderPatches() {}

    /** Mixin class names (relative to {@link #MIXIN_PACKAGE}) to load; empty when patches are off. */
    static synchronized List<String> activeMixins() {
        if (active == null) active = resolve();
        return active;
    }

    private static List<String> resolve() {
        if (!ENABLED) return List.of();
        if (System.getProperty(PROPERTY, "on").equals("off")) {
            Blockbranch.LOGGER.info("Chunk rendering patches disabled by -D{}=off", PROPERTY);
            return List.of();
        }
        String version = FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
        if (!version.equals(WRITTEN_FOR)) {
            Blockbranch.LOGGER.warn("Chunk rendering patches were written for Minecraft {} and are off on {}; vanilla chunk building is used", WRITTEN_FOR, version);
            return List.of();
        }
        var names = new ArrayList<String>();
        for (var patch : Patch.values()) {
            if (!patch.enabled) continue;
            // Absent when the build left the patch package out (-PrenderPatches=false).
            boolean compiled = patch.mixins.stream().allMatch(name -> RenderPatches.class.getClassLoader()
                    .getResource((MIXIN_PACKAGE + "." + name).replace('.', '/') + ".class") != null);
            if (compiled) names.addAll(patch.mixins);
            else Blockbranch.LOGGER.warn("Chunk rendering patch {} is enabled but was not compiled into this build", patch);
        }
        Blockbranch.LOGGER.info("Chunk rendering patches active: {}", names);
        return List.copyOf(names);
    }
}
