package blockbranch.realistic;

/** Saved alongside native pixels; no world generation or network format change. */
public interface PendingMapPreview {
    long blockbranch$previewRetry();
    void blockbranch$previewRetry(long nanos);
    boolean blockbranch$pendingPreview();
    void blockbranch$pendingPreview(boolean pending);
    int blockbranch$previewRows();
    void blockbranch$previewRows(int rows);
}
