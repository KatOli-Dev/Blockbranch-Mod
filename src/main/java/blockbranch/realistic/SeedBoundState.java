package blockbranch.realistic;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One saved structure-placement cache (waygates, ruins, huts...) and its binding to a world seed.
 *
 * <p>A cache decoded from a save is only a candidate: on first use for a seed it is validated against that
 * seed and either kept ({@link #restored()}) or rebuilt. Later calls for the same seed return the bound
 * value. Binding is not synchronized here; callers hold the owning generator's monitor, because rebuilding
 * calls back into other generator state under the same lock.
 *
 * @param <T> the immutable cache type
 */
final class SeedBoundState<T> {
    /** Result of validating and (re)building the candidate for one seed. */
    record Binding<T>(T value, boolean restored) {}

    private final String disabledMessage;
    private final Predicate<T> canSave;
    private volatile T value;
    private boolean bound, restored;
    private long seed, initializationNanos;

    SeedBoundState(T initial, String disabledMessage, Predicate<T> canSave) {
        this.value = initial;
        this.disabledMessage = disabledMessage;
        this.canSave = canSave;
    }

    /** Present when the saved generator enables this structure family. */
    boolean present() { return value != null; }
    /** The current candidate or bound value, for saving; null when disabled. */
    T value() { return value; }
    boolean restored() { return restored; }
    long initializationNanos() { return initializationNanos; }

    /** Whether binding produced new contents that the save does not yet hold. */
    boolean needsSave() { var current = value; return current != null && canSave.test(current) && !restored; }

    /** Returns the value bound to {@code seed}, validating or rebuilding the candidate once per seed. */
    T bind(long seed, Function<T, Binding<T>> bindCandidate) {
        var current = value;
        if (current == null) throw new IllegalStateException(disabledMessage);
        if (bound && this.seed == seed) return current;
        long started = System.nanoTime();
        var binding = bindCandidate.apply(current);
        value = binding.value();
        restored = binding.restored();
        this.seed = seed;
        bound = true;
        initializationNanos = System.nanoTime() - started;
        return binding.value();
    }
}
