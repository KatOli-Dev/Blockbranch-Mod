package blockbranch.client.performance;

import blockbranch.Blockbranch;
import blockbranch.performance.HardwareTier;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.DeviceType;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;

/**
 * First-launch defaults for weak devices. Vanilla ships a FAST preset but never selects it; on a
 * fresh install (no options.txt) this applies it, with slightly shorter distances, when the
 * device classifies as {@link HardwareTier#LOW}. An existing options file is never modified, and
 * the player's later choices are saved as usual.
 *
 * <p>{@code -Dblockbranch.hardwareProfile=off|low} overrides detection (default {@code auto}).
 */
public final class LowEndProfile {
    private static final String PROPERTY = "blockbranch.hardwareProfile";
    static final int RENDER_DISTANCE = 6, SIMULATION_DISTANCE = 5;
    /** Below this heap, Realistic generation and chunk meshing spend their time in GC. */
    private static final long MIN_HEAP_BYTES = 1536L << 20;
    private static boolean freshInstall;

    private LowEndProfile() {}

    /** Must run from the client entrypoint, before Minecraft can write its first options file. */
    public static void register() {
        freshInstall = !Files.exists(FabricLoader.getInstance().getGameDir().resolve("options.txt"));
        ClientLifecycleEvents.CLIENT_STARTED.register(LowEndProfile::apply);
    }

    private static void apply(Minecraft client) {
        var mode = System.getProperty(PROPERTY, "auto");
        if (mode.equals("off")) return;
        var tier = mode.equals("low") ? HardwareTier.LOW : detect();
        if (tier != HardwareTier.LOW) return;
        long maxHeap = Runtime.getRuntime().maxMemory();
        if (maxHeap < MIN_HEAP_BYTES)
            Blockbranch.LOGGER.warn("Low-end device with only {} MiB of JVM heap; add -Xmx2G (or more) to the launcher's JVM arguments", maxHeap >> 20);
        if (!freshInstall) return;
        var options = client.options;
        options.applyGraphicsPreset(GraphicsPreset.FAST);
        options.renderDistance().set(RENDER_DISTANCE);
        options.simulationDistance().set(SIMULATION_DISTANCE);
        options.save();
        Blockbranch.LOGGER.info("Applied low-end defaults to a new installation: FAST graphics, render distance {}, simulation distance {}", RENDER_DISTANCE, SIMULATION_DISTANCE);
    }

    private static HardwareTier detect() {
        long memory = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize();
        var gpu = RenderSystem.getDevice().getDeviceInfo().type();
        // Only a positively identified integrated or software device counts as weak; an
        // unreported type (OTHER) must not penalize a machine with a real GPU.
        boolean dedicated = gpu != DeviceType.INTEGRATED && gpu != DeviceType.CPU;
        var tier = HardwareTier.classify(memory, Runtime.getRuntime().availableProcessors(), dedicated);
        Blockbranch.LOGGER.info("Hardware tier {} ({} MiB RAM, {} cores, {} GPU)", tier, memory >> 20, Runtime.getRuntime().availableProcessors(), gpu);
        return tier;
    }
}
