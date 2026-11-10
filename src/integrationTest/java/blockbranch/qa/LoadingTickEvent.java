package blockbranch.qa;
import jdk.jfr.*;
/** Only instantiated by the opt-in native loading audit. */
@Name("blockbranch.LoadingTick") @Label("Blockbranch loading audit tick") @Category("Blockbranch") @StackTrace(false)
public final class LoadingTickEvent extends Event {
    public String phase;
    public long threadCpuNanos;
    public int loaded;
    public int generated;
}
