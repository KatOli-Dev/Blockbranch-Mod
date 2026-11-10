package blockbranch.performance;

/**
 * Coarse device class used to choose first-launch defaults. It deliberately avoids board or
 * vendor names: a device is {@link #LOW} when at least two of three independent limits hold,
 * so one weak axis (an integrated GPU in an otherwise ordinary laptop) never downgrades it.
 */
public enum HardwareTier {
    STANDARD, LOW;

    /** At or below this the OS, JVM heap and GPU buffers compete for the same memory. */
    public static final long LOW_MEMORY_BYTES = 6L << 30;
    /** Integrated-server worldgen, chunk meshing and rendering contend at this thread count. */
    public static final int LOW_CORE_COUNT = 4;

    /** @param physicalMemoryBytes installed RAM, or {@code <=0} when unknown (counts as not low). */
    public static HardwareTier classify(long physicalMemoryBytes, int logicalCores, boolean dedicatedGpu) {
        int limits = 0;
        if (physicalMemoryBytes > 0 && physicalMemoryBytes <= LOW_MEMORY_BYTES) limits++;
        if (logicalCores > 0 && logicalCores <= LOW_CORE_COUNT) limits++;
        if (!dedicatedGpu) limits++;
        return limits >= 2 ? LOW : STANDARD;
    }
}
