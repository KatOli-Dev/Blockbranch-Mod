package blockbranch.cubic;

/** Identifies procedural data failures even when the containing dimension is not expanded. */
public final class ColumnProfileException extends IllegalArgumentException {
    public ColumnProfileException(RuntimeException cause) {
        super("Cannot load Blockbranch procedural chunk; original data must be preserved",cause);
    }
}
