package blockbranch.performance;

/** Boundaries of the device-class heuristic; no Minecraft classes are loaded. */
public final class HardwareTierCheck {
    private static final long GIB = 1L << 30;
    public static void main(String[] args) {
        // 4 GiB / 4 cores / integrated GPU: all three limits hold.
        require(HardwareTier.classify(4 * GIB, 4, false) == HardwareTier.LOW, "small SBC-class device");
        // Any two limits suffice, in every combination.
        require(HardwareTier.classify(4 * GIB, 4, true) == HardwareTier.LOW, "memory+cores with discrete GPU");
        require(HardwareTier.classify(4 * GIB, 16, false) == HardwareTier.LOW, "memory+no discrete GPU");
        require(HardwareTier.classify(32 * GIB, 4, false) == HardwareTier.LOW, "cores+no discrete GPU");
        // One weak axis alone is an ordinary machine.
        require(HardwareTier.classify(16 * GIB, 16, false) == HardwareTier.STANDARD, "integrated GPU alone");
        require(HardwareTier.classify(4 * GIB, 16, true) == HardwareTier.STANDARD, "low RAM alone");
        require(HardwareTier.classify(16 * GIB, 2, true) == HardwareTier.STANDARD, "few cores alone");
        // Inclusive thresholds.
        require(HardwareTier.classify(6 * GIB, 8, false) == HardwareTier.LOW, "6 GiB is low");
        require(HardwareTier.classify(6 * GIB + 1, 8, false) == HardwareTier.STANDARD, "just over 6 GiB");
        require(HardwareTier.classify(16 * GIB, 4, false) == HardwareTier.LOW, "4 cores is low");
        require(HardwareTier.classify(16 * GIB, 5, false) == HardwareTier.STANDARD, "5 cores");
        // Unknown measurements never count against the device.
        require(HardwareTier.classify(0, 0, true) == HardwareTier.STANDARD, "unknown memory and cores");
        require(HardwareTier.classify(-1, 8, false) == HardwareTier.STANDARD, "unknown memory, one limit");
        System.out.println("PASS");
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
