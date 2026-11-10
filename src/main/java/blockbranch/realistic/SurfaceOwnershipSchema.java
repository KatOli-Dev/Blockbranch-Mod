package blockbranch.realistic;

/** Surface-only ownership opt-in; old readers reject before loading new biome identities. */
public final class SurfaceOwnershipSchema {
    public static final int FOUNDATION = 257, MAX = 258;
    public static int schema(int base, int definition) {
        if (definition == 0) return base;
        if (definition != 1 || base != FOUNDATION) throw new IllegalArgumentException("Surface ownership requires complete foundation 257");
        return MAX;
    }
    public static int baseSchema(int saved, int definition) {
        if (definition == 0) return saved == MAX ? Integer.MAX_VALUE : saved;
        return definition == 1 && saved == MAX ? FOUNDATION : Integer.MAX_VALUE;
    }
    public static boolean matches(int saved, int definition) { return baseSchema(saved, definition) != Integer.MAX_VALUE; }
    private SurfaceOwnershipSchema() {}
}
