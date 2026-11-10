package blockbranch.realistic;

/** Small planar helpers for terrain sampling hot paths. */
final class PlanarMath {
    private PlanarMath() {}

    /**
     * Euclidean length of (dx, dz). {@link Math#hypot} guarantees 1 ulp and avoids overflow, which
     * costs about 17x as much (~10 ns vs ~0.6 ns) and is the single largest term in terrain
     * sampling. Terrain distances here are at most a few million metres, so squaring cannot
     * overflow; results differ from hypot by at most about 1 ulp. This is deliberately not
     * bit-identical generation: rare rounded heights may differ by a block, including in
     * unexplored columns of old worlds. No saved geometry opt-in protects that change.
     */
    static double length(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }
}
